package com.kevin.moneyboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinanceDb extends SQLiteOpenHelper {
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_DEBT = "DEBT";

    private static final int DB_VERSION = 2;

    public FinanceDb(Context context) {
        super(context, "money_board.db", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createBaseTables(db);
        createV2Tables(db);
    }

    private void createBaseTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT NOT NULL," +
                "purpose TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "note TEXT," +
                "occurred_at INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(occurred_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS month_plans (" +
                "month_key TEXT PRIMARY KEY," +
                "budget REAL NOT NULL DEFAULT 0," +
                "target REAL NOT NULL DEFAULT 0" +
                ")");
    }

    private void createV2Tables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS category_budgets (" +
                "cycle_key TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "budget REAL NOT NULL DEFAULT 0," +
                "PRIMARY KEY(cycle_key, category)" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createV2Tables(db);
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

    public Map<Long, Double> getDayTotals(String type, long startInclusive, long endExclusive) {
        Map<Long, Double> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT (occurred_at/86400000) day_key, SUM(amount) total FROM transactions " +
                        "WHERE type=? AND occurred_at>=? AND occurred_at<? GROUP BY day_key ORDER BY day_key ASC",
                new String[]{type, String.valueOf(startInclusive), String.valueOf(endExclusive)});
        try {
            while (c.moveToNext()) out.put(c.getLong(0), c.getDouble(1));
        } finally {
            c.close();
        }
        return out;
    }

    public List<Record> getRecent(String typeOrNull, int limit) {
        return getRecords(typeOrNull, Long.MIN_VALUE, Long.MAX_VALUE, limit);
    }

    public List<Record> getRecords(String typeOrNull, long startInclusive, long endExclusive, int limit) {
        List<Record> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id,type,purpose,amount,note,occurred_at FROM transactions WHERE occurred_at>=? AND occurred_at<?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(startInclusive));
        args.add(String.valueOf(endExclusive));
        if (typeOrNull != null) {
            sql.append(" AND type=?");
            args.add(typeOrNull);
        }
        sql.append(" ORDER BY occurred_at DESC,id DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
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

    public Record getLargestRecord(String type, long startInclusive, long endExclusive) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,type,purpose,amount,note,occurred_at FROM transactions " +
                        "WHERE type=? AND occurred_at>=? AND occurred_at<? ORDER BY amount DESC LIMIT 1",
                new String[]{type, String.valueOf(startInclusive), String.valueOf(endExclusive)});
        try {
            if (!c.moveToFirst()) return null;
            Record r = new Record();
            r.id = c.getLong(0);
            r.type = c.getString(1);
            r.purpose = c.getString(2);
            r.amount = c.getDouble(3);
            r.note = c.getString(4);
            r.occurredAt = c.getLong(5);
            return r;
        } finally {
            c.close();
        }
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

    public Map<String, Double> getCategoryBudgets(String cycleKey) {
        Map<String, Double> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT category,budget FROM category_budgets WHERE cycle_key=? ORDER BY category COLLATE NOCASE ASC",
                new String[]{cycleKey});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getDouble(1));
        } finally {
            c.close();
        }
        return out;
    }

    public void saveCategoryBudget(String cycleKey, String category, double budget) {
        if (category == null || category.trim().isEmpty()) return;
        ContentValues v = new ContentValues();
        v.put("cycle_key", cycleKey);
        v.put("category", category.trim());
        v.put("budget", Math.max(0, budget));
        getWritableDatabase().insertWithOnConflict(
                "category_budgets", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String exportJson() throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "MoneyBoardBackup");
        root.put("version", 1);

        JSONArray transactions = new JSONArray();
        Cursor tc = getReadableDatabase().rawQuery(
                "SELECT id,type,purpose,amount,note,occurred_at FROM transactions ORDER BY id ASC", null);
        try {
            while (tc.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("type", tc.getString(1));
                o.put("purpose", tc.getString(2));
                o.put("amount", tc.getDouble(3));
                o.put("note", tc.getString(4));
                o.put("occurred_at", tc.getLong(5));
                transactions.put(o);
            }
        } finally { tc.close(); }
        root.put("transactions", transactions);

        JSONArray plans = new JSONArray();
        Cursor pc = getReadableDatabase().rawQuery("SELECT month_key,budget,target FROM month_plans", null);
        try {
            while (pc.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("month_key", pc.getString(0));
                o.put("budget", pc.getDouble(1));
                o.put("target", pc.getDouble(2));
                plans.put(o);
            }
        } finally { pc.close(); }
        root.put("plans", plans);

        JSONArray categoryBudgets = new JSONArray();
        Cursor cc = getReadableDatabase().rawQuery("SELECT cycle_key,category,budget FROM category_budgets", null);
        try {
            while (cc.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("cycle_key", cc.getString(0));
                o.put("category", cc.getString(1));
                o.put("budget", cc.getDouble(2));
                categoryBudgets.put(o);
            }
        } finally { cc.close(); }
        root.put("category_budgets", categoryBudgets);
        return root.toString(2);
    }

    public void importJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"MoneyBoardBackup".equals(root.optString("format"))) {
            throw new IllegalArgumentException("不是 MoneyBoard 备份文件");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("transactions", null, null);
            db.delete("month_plans", null, null);
            db.delete("category_budgets", null, null);

            JSONArray transactions = root.optJSONArray("transactions");
            if (transactions != null) {
                for (int i = 0; i < transactions.length(); i++) {
                    JSONObject o = transactions.getJSONObject(i);
                    ContentValues v = new ContentValues();
                    v.put("type", o.getString("type"));
                    v.put("purpose", o.getString("purpose"));
                    v.put("amount", o.getDouble("amount"));
                    v.put("note", o.optString("note", ""));
                    v.put("occurred_at", o.getLong("occurred_at"));
                    db.insert("transactions", null, v);
                }
            }

            JSONArray plans = root.optJSONArray("plans");
            if (plans != null) {
                for (int i = 0; i < plans.length(); i++) {
                    JSONObject o = plans.getJSONObject(i);
                    ContentValues v = new ContentValues();
                    v.put("month_key", o.getString("month_key"));
                    v.put("budget", o.optDouble("budget", 0));
                    v.put("target", o.optDouble("target", 0));
                    db.insertWithOnConflict("month_plans", null, v, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            JSONArray categoryBudgets = root.optJSONArray("category_budgets");
            if (categoryBudgets != null) {
                for (int i = 0; i < categoryBudgets.length(); i++) {
                    JSONObject o = categoryBudgets.getJSONObject(i);
                    ContentValues v = new ContentValues();
                    v.put("cycle_key", o.getString("cycle_key"));
                    v.put("category", o.getString("category"));
                    v.put("budget", o.optDouble("budget", 0));
                    db.insertWithOnConflict("category_budgets", null, v, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String exportCsv() {
        StringBuilder out = new StringBuilder("日期,类型,用途,金额,备注\n");
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT type,purpose,amount,note,occurred_at FROM transactions ORDER BY occurred_at DESC,id DESC", null);
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA);
            while (c.moveToNext()) {
                out.append(csv(f.format(new java.util.Date(c.getLong(4))))).append(',')
                        .append(csv(typeZh(c.getString(0)))).append(',')
                        .append(csv(c.getString(1))).append(',')
                        .append(c.getDouble(2)).append(',')
                        .append(csv(c.getString(3))).append('\n');
            }
        } finally { c.close(); }
        return out.toString();
    }

    private static String csv(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String typeZh(String type) {
        if (TYPE_EXPENSE.equals(type)) return "支出";
        if (TYPE_INCOME.equals(type)) return "收入";
        return "负债";
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
