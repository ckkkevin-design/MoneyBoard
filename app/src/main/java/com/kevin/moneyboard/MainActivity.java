package com.kevin.moneyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(247, 248, 250);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(17, 24, 39);
    private static final int MUTED = Color.rgb(107, 114, 128);
    private static final int LINE = Color.rgb(229, 231, 235);

    private FinanceDb db;
    private LinearLayout pageHost;
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private Calendar addDate = Calendar.getInstance();
    private Calendar planMonth = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new FinanceDb(this);
        setContentView(buildRoot());
        showDashboard();
    }

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(14), dp(18), dp(10));
        TextView title = text("钱迹看板", 24, true, TEXT);
        TextView sub = text("消费 · 收入 · 负债，一眼看清", 13, false, MUTED);
        header.addView(title);
        header.addView(sub);
        root.addView(header);

        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(CARD);
        String[] labels = {"看板", "记一笔", "分析", "明细", "设置"};
        View.OnClickListener[] listeners = {
                v -> showDashboard(), v -> showAdd(), v -> showAnalysis(),
                v -> showRecords(), v -> showSettings()
        };
        for (int i = 0; i < labels.length; i++) {
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setOnClickListener(listeners[i]);
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(58), 1f));
        }
        root.addView(nav);
        return root;
    }

    private void showDashboard() {
        LinearLayout body = pageBody();
        Calendar now = Calendar.getInstance();
        long[] monthRange = monthRange(now);
        FinanceDb.Summary s = db.getSummary(monthRange[0], monthRange[1]);
        String key = monthKey(now);
        FinanceDb.Plan p = db.getPlan(key);

        body.addView(sectionTitle(displayMonth(now) + " 看板"));
        body.addView(metricGrid(s));

        double remaining = p.budget - s.expense;
        body.addView(progressCard("本月消费额度",
                p.budget > 0 ? "已用 ¥" + money.format(s.expense) + " / ¥" + money.format(p.budget)
                        : "尚未设置月消费额度",
                p.budget > 0 ? s.expense / p.budget : 0,
                p.budget > 0 ? (remaining >= 0 ? "还可消费 ¥" + money.format(remaining)
                        : "已超出 ¥" + money.format(-remaining)) : "去设置页填写预算"));

        double targetRate = p.target > 0 ? s.net() / p.target : 0;
        body.addView(progressCard("净结余目标",
                p.target > 0 ? "当前 ¥" + money.format(s.net()) + " / 目标 ¥" + money.format(p.target)
                        : "尚未设置本月目标",
                targetRate,
                p.target > 0 ? "完成 " + percent(targetRate) : "净结余 = 收入 - 消费 - 负债"));

        long[] weekRange = weekRange(now);
        FinanceDb.Summary w = db.getSummary(weekRange[0], weekRange[1]);
        body.addView(simpleCard("本周速览",
                "收入  ¥" + money.format(w.income) + "\n" +
                        "消费  ¥" + money.format(w.expense) + "\n" +
                        "负债  ¥" + money.format(w.debt) + "\n" +
                        "净结余  ¥" + money.format(w.net())));

        mount(body);
    }

    private View metricGrid(FinanceDb.Summary s) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout r1 = new LinearLayout(this);
        LinearLayout r2 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r1.addView(metricCard("收入", s.income), weightParams());
        r1.addView(metricCard("消费", s.expense), weightParams());
        r2.addView(metricCard("负债", s.debt), weightParams());
        r2.addView(metricCard("净结余", s.net()), weightParams());
        outer.addView(r1);
        outer.addView(r2);
        return outer;
    }

    private View metricCard(String label, double value) {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView l = text(label, 13, false, MUTED);
        TextView v = text("¥" + money.format(value), 20, true, TEXT);
        c.addView(l);
        c.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        c.setLayoutParams(lp);
        return c;
    }

    private void showAdd() {
        LinearLayout body = pageBody();
        body.addView(sectionTitle("记一笔"));

        Spinner type = new Spinner(this);
        String[] types = {"消费", "收入", "负债"};
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        body.addView(fieldLabel("类型"));
        body.addView(type);

        EditText purpose = edit("例如：午餐、工资、信用卡");
        body.addView(fieldLabel("用途 / 来源名称（可自定义）"));
        body.addView(purpose);

        EditText amount = edit("金额");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(fieldLabel("金额"));
        body.addView(amount);

        EditText note = edit("备注（可选）");
        body.addView(fieldLabel("备注"));
        body.addView(note);

        Button date = new Button(this);
        date.setAllCaps(false);
        updateDateButton(date);
        date.setOnClickListener(v -> {
            DatePickerDialog d = new DatePickerDialog(this, (view, y, m, day) -> {
                addDate.set(Calendar.YEAR, y);
                addDate.set(Calendar.MONTH, m);
                addDate.set(Calendar.DAY_OF_MONTH, day);
                updateDateButton(date);
            }, addDate.get(Calendar.YEAR), addDate.get(Calendar.MONTH), addDate.get(Calendar.DAY_OF_MONTH));
            d.show();
        });
        body.addView(fieldLabel("日期"));
        body.addView(date);

        Button save = primaryButton("保存这笔账");
        save.setOnClickListener(v -> {
            String p = purpose.getText().toString().trim();
            String a = amount.getText().toString().trim();
            if (p.isEmpty()) {
                toast("请填写用途 / 来源名称");
                return;
            }
            double val;
            try { val = Double.parseDouble(a); }
            catch (Exception e) { toast("请输入正确金额"); return; }
            if (val <= 0) { toast("金额必须大于 0"); return; }

            String dbType = type.getSelectedItemPosition() == 0 ? FinanceDb.TYPE_EXPENSE
                    : type.getSelectedItemPosition() == 1 ? FinanceDb.TYPE_INCOME : FinanceDb.TYPE_DEBT;
            Calendar t = (Calendar) addDate.clone();
            Calendar n = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, n.get(Calendar.HOUR_OF_DAY));
            t.set(Calendar.MINUTE, n.get(Calendar.MINUTE));
            t.set(Calendar.SECOND, n.get(Calendar.SECOND));
            db.addRecord(dbType, p, val, note.getText().toString(), t.getTimeInMillis());
            purpose.setText("");
            amount.setText("");
            note.setText("");
            toast("已记账");
            showDashboard();
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        slp.setMargins(0, dp(18), 0, dp(12));
        body.addView(save, slp);
        mount(body);
    }

    private void showAnalysis() {
        LinearLayout body = pageBody();
        Calendar now = Calendar.getInstance();
        body.addView(sectionTitle("周 / 月分析"));

        long[] wr = weekRange(now);
        long[] mr = monthRange(now);
        FinanceDb.Summary ws = db.getSummary(wr[0], wr[1]);
        FinanceDb.Summary ms = db.getSummary(mr[0], mr[1]);
        body.addView(summaryCard("本周", ws));
        body.addView(summaryCard("本月", ms));

        body.addView(subTitle("本周消费用途"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_EXPENSE, wr[0], wr[1]));
        body.addView(subTitle("本月消费用途"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_EXPENSE, mr[0], mr[1]));
        body.addView(subTitle("本月收入来源"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_INCOME, mr[0], mr[1]));
        body.addView(subTitle("本月负债项目"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_DEBT, mr[0], mr[1]));
        mount(body);
    }

    private View summaryCard(String title, FinanceDb.Summary s) {
        return simpleCard(title,
                "收入 ¥" + money.format(s.income) + "    消费 ¥" + money.format(s.expense) + "\n" +
                        "负债 ¥" + money.format(s.debt) + "    净结余 ¥" + money.format(s.net()));
    }

    private void addPurposeBars(LinearLayout body, Map<String, Double> map) {
        if (map.isEmpty()) {
            TextView empty = text("暂无记录", 14, false, MUTED);
            empty.setPadding(dp(4), dp(8), dp(4), dp(12));
            body.addView(empty);
            return;
        }
        double max = 0;
        for (double v : map.values()) max = Math.max(max, v);
        for (Map.Entry<String, Double> e : map.entrySet()) {
            LinearLayout c = card();
            TextView line = text(e.getKey() + "   ¥" + money.format(e.getValue()), 14, true, TEXT);
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(1000);
            pb.setProgress(max == 0 ? 0 : (int) Math.round(e.getValue() / max * 1000));
            c.addView(line);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
            pp.setMargins(0, dp(8), 0, 0);
            c.addView(pb, pp);
            body.addView(c, cardParams());
        }
    }

    private void showRecords() {
        LinearLayout body = pageBody();
        body.addView(sectionTitle("账目明细"));
        Spinner filter = new Spinner(this);
        String[] f = {"全部", "消费", "收入", "负债"};
        filter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, f));
        body.addView(filter);

        LinearLayout listHost = new LinearLayout(this);
        listHost.setOrientation(LinearLayout.VERTICAL);
        body.addView(listHost);

        Runnable load = () -> {
            listHost.removeAllViews();
            String type = null;
            int pos = filter.getSelectedItemPosition();
            if (pos == 1) type = FinanceDb.TYPE_EXPENSE;
            else if (pos == 2) type = FinanceDb.TYPE_INCOME;
            else if (pos == 3) type = FinanceDb.TYPE_DEBT;
            List<FinanceDb.Record> rows = db.getRecent(type, 200);
            if (rows.isEmpty()) {
                TextView empty = text("还没有账目。", 14, false, MUTED);
                empty.setPadding(0, dp(18), 0, 0);
                listHost.addView(empty);
            }
            for (FinanceDb.Record r : rows) listHost.addView(recordRow(r), cardParams());
        };
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { load.run(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        load.run();
        mount(body);
    }

    private View recordRow(FinanceDb.Record r) {
        LinearLayout c = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView left = text(typeZh(r.type) + " · " + r.purpose, 15, true, TEXT);
        TextView right = text("¥" + money.format(r.amount), 16, true, TEXT);
        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(right);
        c.addView(top);
        String meta = dateFmt.format(new Date(r.occurredAt));
        if (r.note != null && !r.note.trim().isEmpty()) meta += "  ·  " + r.note;
        c.addView(text(meta, 12, false, MUTED));
        Button del = new Button(this);
        del.setText("删除");
        del.setAllCaps(false);
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("删除这笔账？")
                .setMessage(typeZh(r.type) + " · " + r.purpose + " · ¥" + money.format(r.amount))
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, which) -> {
                    db.deleteRecord(r.id);
                    showRecords();
                }).show());
        c.addView(del);
        return c;
    }

    private void showSettings() {
        LinearLayout body = pageBody();
        body.addView(sectionTitle("月预算与目标"));

        LinearLayout monthNav = new LinearLayout(this);
        monthNav.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = new Button(this);
        Button next = new Button(this);
        TextView monthLabel = text(displayMonth(planMonth), 18, true, TEXT);
        monthLabel.setGravity(Gravity.CENTER);
        prev.setText("‹"); next.setText("›");
        monthNav.addView(prev, new LinearLayout.LayoutParams(dp(60), dp(48)));
        monthNav.addView(monthLabel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        monthNav.addView(next, new LinearLayout.LayoutParams(dp(60), dp(48)));
        body.addView(monthNav);

        EditText budget = edit("例如 3000");
        EditText target = edit("例如 2000");
        budget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(fieldLabel("这个月最多能花多少钱"));
        body.addView(budget);
        body.addView(fieldLabel("这个月希望净结余多少"));
        body.addView(target);
        TextView help = text("净结余 = 收入 - 消费 - 负债。预算只统计“消费”。每个月可以设置不同额度与目标。", 13, false, MUTED);
        help.setPadding(0, dp(10), 0, dp(8));
        body.addView(help);

        Runnable reload = () -> {
            monthLabel.setText(displayMonth(planMonth));
            FinanceDb.Plan p = db.getPlan(monthKey(planMonth));
            budget.setText(p.budget == 0 ? "" : stripZero(p.budget));
            target.setText(p.target == 0 ? "" : stripZero(p.target));
        };
        prev.setOnClickListener(v -> { planMonth.add(Calendar.MONTH, -1); reload.run(); });
        next.setOnClickListener(v -> { planMonth.add(Calendar.MONTH, 1); reload.run(); });

        Button save = primaryButton("保存本月设置");
        save.setOnClickListener(v -> {
            double b = parseNonNegative(budget.getText().toString());
            double t = parseNonNegative(target.getText().toString());
            if (b < 0 || t < 0) { toast("请输入 0 或正数"); return; }
            db.savePlan(monthKey(planMonth), b, t);
            toast("已保存 " + displayMonth(planMonth));
            reload.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(14), 0, dp(10));
        body.addView(save, lp);
        reload.run();
        mount(body);
    }

    private View progressCard(String title, String detail, double rate, String foot) {
        LinearLayout c = card();
        c.addView(text(title, 16, true, TEXT));
        TextView d = text(detail, 14, false, MUTED);
        d.setPadding(0, dp(6), 0, dp(6));
        c.addView(d);
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);
        p.setProgress((int) Math.max(0, Math.min(1000, Math.round(rate * 1000))));
        c.addView(p, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)));
        TextView f = text(foot, 13, false, MUTED);
        f.setPadding(0, dp(6), 0, 0);
        c.addView(f);
        c.setLayoutParams(cardParams());
        return c;
    }

    private View simpleCard(String title, String value) {
        LinearLayout c = card();
        c.addView(text(title, 16, true, TEXT));
        TextView v = text(value, 14, false, TEXT);
        v.setPadding(0, dp(8), 0, 0);
        c.addView(v);
        c.setLayoutParams(cardParams());
        return c;
    }

    private LinearLayout pageBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(8), dp(14), dp(26));
        return body;
    }

    private void mount(LinearLayout body) {
        pageHost.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        pageHost.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackgroundColor(CARD);
        return c;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        return lp;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 22, true, TEXT);
        t.setPadding(dp(2), dp(4), 0, dp(10));
        return t;
    }

    private TextView subTitle(String s) {
        TextView t = text(s, 17, true, TEXT);
        t.setPadding(dp(2), dp(18), 0, dp(4));
        return t;
    }

    private TextView fieldLabel(String s) {
        TextView t = text(s, 13, true, MUTED);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(Color.rgb(156, 163, 175));
        e.setBackgroundColor(CARD);
        e.setPadding(dp(12), 0, dp(12), 0);
        e.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return e;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        return b;
    }

    private long[] monthRange(Calendar source) {
        Calendar s = (Calendar) source.clone();
        s.set(Calendar.DAY_OF_MONTH, 1);
        zeroTime(s);
        Calendar e = (Calendar) s.clone();
        e.add(Calendar.MONTH, 1);
        return new long[]{s.getTimeInMillis(), e.getTimeInMillis()};
    }

    private long[] weekRange(Calendar source) {
        Calendar s = (Calendar) source.clone();
        int dow = s.get(Calendar.DAY_OF_WEEK);
        int delta = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        s.add(Calendar.DAY_OF_MONTH, delta);
        zeroTime(s);
        Calendar e = (Calendar) s.clone();
        e.add(Calendar.DAY_OF_MONTH, 7);
        return new long[]{s.getTimeInMillis(), e.getTimeInMillis()};
    }

    private void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private String monthKey(Calendar c) {
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    private String displayMonth(Calendar c) {
        return String.format(Locale.CHINA, "%d年%d月", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    private String percent(double rate) {
        return new DecimalFormat("0.0%").format(rate);
    }

    private String stripZero(double d) {
        if (Math.rint(d) == d) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    private double parseNonNegative(String s) {
        String x = s.trim();
        if (x.isEmpty()) return 0;
        try {
            double v = Double.parseDouble(x);
            return v >= 0 ? v : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private void updateDateButton(Button b) {
        b.setText("选择日期：" + dateFmt.format(addDate.getTime()));
    }

    private String typeZh(String type) {
        if (FinanceDb.TYPE_EXPENSE.equals(type)) return "消费";
        if (FinanceDb.TYPE_INCOME.equals(type)) return "收入";
        return "负债";
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
