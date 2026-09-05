package com.kevin.moneyboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinanceDb extends SQLiteOpenHelper {
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_DEBT = "DEBT";

    public FinanceDb(Context context) {
        super(context, "money_board.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT NOT NULL," +
                "purpose TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "note TEXT," +
                "occurred_at INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX idx_transactions_date ON transactions(occurred_at)");
        db.execSQL("CREATE TABLE month_plans (" +
                "month_key TEXT PRIMARY KEY," +
                "budget REAL NOT NULL DEFAULT 0," +
                "target REAL NOT NULL DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 only.
    }

    public long addRecord(String type, String purpose, double amount, String note, long occurredAt) {
        ContentValues v = new ContentValues();
        v.put("type", type);
        v.put("purpose", purpose.trim());
        v.put("amount", amount);
        v.put("note", note == null ? "" : note.trim());
        v.put("occurred_at", occurredAt);
        return getWritableDatabase().insert("transactions", null, v);
    }

    public void deleteRecord(long id) {
        getWritableDatabase().delete("transactions", "id=?", new String[]{String.valueOf(id)});
    }

    public Summary getSummary(long startInclusive, long endExclusive) {
        Summary s = new Summary();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT type, COALESCE(SUM(amount),0) FROM transactions " +
                        "WHERE occurred_at>=? AND occurred_at<? GROUP BY type",
                new String[]{String.valueOf(startInclusive), String.valueOf(endExclusive)});
        try {
            while (c.moveToNext()) {
                String type = c.getString(0);
                double value = c.getDouble(1);
                if (TYPE_EXPENSE.equals(type)) s.expense = value;
                else if (TYPE_INCOME.equals(type)) s.income = value;
                else if (TYPE_DEBT.equals(type)) s.debt = value;
            }
        } finally {
            c.close();
        }
        return s;
    }

    public Map<String, Double> getPurposeTotals(String type, long startInclusive, long endExclusive) {
        Map<String, Double> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT purpose, SUM(amount) total FROM transactions " +
                        "WHERE type=? AND occurred_at>=? AND occurred_at<? " +
                        "GROUP BY purpose ORDER BY total DESC",
                new String[]{type, String.valueOf(startInclusive), String.valueOf(endExclusive)});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getDouble(1));
        } finally {
            c.close();
        }
        return out;
    }

    public List<Record> getRecent(String typeOrNull, int limit) {
        List<Record> out = new ArrayList<>();
        String sql;
        String[] args;
        if (typeOrNull == null) {
            sql = "SELECT id,type,purpose,amount,note,occurred_at FROM transactions ORDER BY occurred_at DESC,id DESC LIMIT ?";
            args = new String[]{String.valueOf(limit)};
        } else {
            sql = "SELECT id,type,purpose,amount,note,occurred_at FROM transactions WHERE type=? ORDER BY occurred_at DESC,id DESC LIMIT ?";
            args = new String[]{typeOrNull, String.valueOf(limit)};
        }
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) {
                Record r = new Record();
                r.id = c.getLong(0);
                r.type = c.getString(1);
                r.purpose = c.getString(2);
                r.amount = c.getDouble(3);
                r.note = c.getString(4);
                r.occurredAt = c.getLong(5);
                out.add(r);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public Plan getPlan(String monthKey) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT budget,target FROM month_plans WHERE month_key=?",
                new String[]{monthKey});
        try {
            if (c.moveToFirst()) return new Plan(c.getDouble(0), c.getDouble(1));
            return new Plan(0, 0);
        } finally {
            c.close();
        }
    }

    public void savePlan(String monthKey, double budget, double target) {
        ContentValues v = new ContentValues();
        v.put("month_key", monthKey);
        v.put("budget", budget);
        v.put("target", target);
        getWritableDatabase().insertWithOnConflict(
                "month_plans", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static class Summary {
        public double expense;
        public double income;
        public double debt;
        public double net() { return income - expense - debt; }
    }

    public static class Plan {
        public final double budget;
        public final double target;
        public Plan(double budget, double target) {
            this.budget = budget;
            this.target = target;
        }
    }

    public static class Record {
        public long id;
        public String type;
        public String purpose;
        public double amount;
        public String note;
        public long occurredAt;
    }
}
