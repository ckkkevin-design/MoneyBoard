package com.kevin.moneyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 247, 251);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(24, 28, 40);
    private static final int MUTED = Color.rgb(118, 124, 140);
    private static final int LINE = Color.rgb(229, 232, 239);
    private static final int PRIMARY = Color.rgb(91, 82, 240);
    private static final int PRIMARY_SOFT = Color.rgb(239, 238, 255);
    private static final int GREEN = Color.rgb(9, 153, 111);
    private static final int GREEN_SOFT = Color.rgb(235, 250, 245);
    private static final int RED = Color.rgb(225, 67, 67);
    private static final int RED_SOFT = Color.rgb(255, 241, 241);
    private static final int ORANGE = Color.rgb(220, 132, 24);
    private static final int ORANGE_SOFT = Color.rgb(255, 247, 235);
    private static final int BLUE = Color.rgb(53, 105, 220);
    private static final int BLUE_SOFT = Color.rgb(239, 245, 255);
    private static final int HERO_1 = Color.rgb(35, 37, 72);
    private static final int HERO_2 = Color.rgb(78, 70, 177);
    private static final int HERO_TEXT_SOFT = Color.rgb(219, 220, 242);

    private FinanceDb db;
    private LinearLayout pageHost;
    private TextView[] navItems;
    private int selectedNav = 0;

    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private Calendar addDate = Calendar.getInstance();
    private Calendar planCycle = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new FinanceDb(this);
        planCycle = cycleStart(Calendar.getInstance());
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(CARD);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildRoot());
        showDashboard();
    }

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(15), dp(20), dp(10));
        TextView title = text("钱迹看板", 25, true, TEXT);
        TextView sub = text("每月 15 日切换新周期", 12, false, MUTED);
        sub.setPadding(0, dp(3), 0, 0);
        header.addView(title);
        header.addView(sub);
        root.addView(header);

        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackgroundColor(CARD);
        nav.setElevation(dp(10));

        String[] labels = {"看板", "记一笔", "分析", "明细", "我的"};
        Runnable[] pages = {this::showDashboard, this::showAdd, this::showAnalysis, this::showRecords, this::showSettings};
        navItems = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 12, false, MUTED);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> {
                selectedNav = index;
                updateNav();
                pages[index].run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            nav.addView(item, lp);
            navItems[i] = item;
        }
        root.addView(nav);
        updateNav();
        return root;
    }

    private void updateNav() {
        if (navItems == null) return;
        for (int i = 0; i < navItems.length; i++) {
            boolean on = i == selectedNav;
            navItems[i].setTextColor(on ? PRIMARY : MUTED);
            navItems[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            navItems[i].setBackground(on ? rounded(PRIMARY_SOFT, 0, 16) : transparentRounded());
        }
    }

    private void showDashboard() {
        selectNav(0);
        LinearLayout body = pageBody();
        Calendar now = Calendar.getInstance();
        long[] cycle = cycleRange(now);
        FinanceDb.Summary s = db.getSummary(cycle[0], cycle[1]);
        FinanceDb.Plan p = db.getPlan(cycleKey(now));

        body.addView(cycleHeadline(now));
        body.addView(heroBudgetCard(now, s, p), cardParams());
        body.addView(compactMetrics(s));

        body.addView(chartCard("每日消费趋势", "观察这个周期每天花了多少", new LineChartView(dailyExpenseSeries(now), PRIMARY)), cardParams());
        body.addView(categoryCard(now), cardParams());

        double targetRate = p.target > 0 ? s.net() / p.target : 0;
        body.addView(progressCard("本周期净结余目标",
                p.target > 0 ? "当前 ¥" + money.format(s.net()) + " / 目标 ¥" + money.format(p.target)
                        : "还没有设置目标",
                targetRate,
                p.target > 0 ? "已完成 " + percent(targetRate) : "去「我的」里设置本周期目标",
                GREEN), cardParams());

        body.addView(recentCard(), cardParams());
        mount(body);
    }

    private View cycleHeadline(Calendar now) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("本周期", 23, true, TEXT);
        TextView pill = text(compactCycle(now), 12, true, PRIMARY);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(11), dp(6), dp(11), dp(6));
        pill.setBackground(rounded(PRIMARY_SOFT, 0, 14));
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(pill);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(2), dp(2), dp(2), dp(9));
        row.setLayoutParams(lp);
        return row;
    }

    private View heroBudgetCard(Calendar now, FinanceDb.Summary s, FinanceDb.Plan p) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        hero.setBackground(gradientRounded(HERO_1, HERO_2, 24));
        hero.setElevation(dp(4));

        TextView eyebrow = text("本周期可用预算", 13, true, HERO_TEXT_SOFT);
        hero.addView(eyebrow);

        double remaining = p.budget - s.expense;
        String big = p.budget > 0 ? (remaining >= 0 ? "¥" + money.format(remaining) : "-¥" + money.format(-remaining)) : "未设置额度";
        TextView amount = text(big, p.budget > 0 ? 31 : 23, true, Color.WHITE);
        amount.setPadding(0, dp(7), 0, dp(14));
        hero.addView(amount);

        LinearLayout lower = new LinearLayout(this);
        lower.setOrientation(LinearLayout.HORIZONTAL);
        lower.setGravity(Gravity.CENTER_VERTICAL);

        double rate = p.budget > 0 ? s.expense / p.budget : 0;
        BudgetRingView ring = new BudgetRingView(rate);
        lower.addView(ring, new LinearLayout.LayoutParams(dp(104), dp(104)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(18), 0, 0, 0);
        info.addView(heroStat("预算已用", "¥" + money.format(s.expense)));
        info.addView(heroStat("预算上限", p.budget > 0 ? "¥" + money.format(p.budget) : "待设置"));
        info.addView(heroStat("周期", compactCycle(now)));
        lower.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hero.addView(lower);
        return hero;
    }

    private View heroStat(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView l = text(label, 12, false, HERO_TEXT_SOFT);
        TextView v = text(value, 13, true, Color.WHITE);
        v.setGravity(Gravity.END);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);
        return row;
    }

    private View compactMetrics(FinanceDb.Summary s) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setPadding(0, dp(2), 0, dp(2));
        outer.addView(metricPill("收入", s.income, GREEN, GREEN_SOFT), weightParamsWithMargins());
        outer.addView(metricPill("消费", s.expense, RED, RED_SOFT), weightParamsWithMargins());
        outer.addView(metricPill("负债", s.debt, ORANGE, ORANGE_SOFT), weightParamsWithMargins());
        outer.addView(metricPill("结余", s.net(), BLUE, BLUE_SOFT), weightParamsWithMargins());
        return outer;
    }

    private View metricPill(String label, double value, int accent, int soft) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(7), dp(11), dp(7), dp(11));
        box.setBackground(rounded(CARD, LINE, 18));
        TextView l = text(label, 11, true, accent);
        TextView v = text(shortMoney(value), 13, true, TEXT);
        v.setPadding(0, dp(4), 0, 0);
        box.addView(l);
        box.addView(v);
        return box;
    }

    private String shortMoney(double v) {
        if (Math.abs(v) >= 10000) return "¥" + new DecimalFormat("0.0w").format(v / 10000.0);
        if (Math.rint(v) == v) return "¥" + new DecimalFormat("#,##0").format(v);
        return "¥" + new DecimalFormat("#,##0.0").format(v);
    }

    private View chartCard(String title, String subtitle, View chart) {
        LinearLayout c = card();
        c.addView(text(title, 17, true, TEXT));
        TextView sub = text(subtitle, 12, false, MUTED);
        sub.setPadding(0, dp(4), 0, dp(8));
        c.addView(sub);
        c.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(155)));
        return c;
    }

    private View categoryCard(Calendar now) {
        long[] r = cycleRange(now);
        Map<String, Double> map = db.getPurposeTotals(FinanceDb.TYPE_EXPENSE, r[0], r[1]);
        LinearLayout c = card();
        c.addView(text("消费分类占比", 17, true, TEXT));
        TextView sub = text("看看钱主要花在哪些地方", 12, false, MUTED);
        sub.setPadding(0, dp(4), 0, dp(12));
        c.addView(sub);

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        int take = 0;
        for (Map.Entry<String, Double> e : map.entrySet()) {
            if (take++ >= 5) break;
            labels.add(e.getKey());
            values.add(e.getValue());
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        DonutChartView donut = new DonutChartView(values);
        row.addView(donut, new LinearLayout.LayoutParams(dp(128), dp(128)));

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        legend.setPadding(dp(12), 0, 0, 0);
        if (labels.isEmpty()) {
            TextView empty = text("暂无消费记录", 13, false, MUTED);
            legend.addView(empty);
        } else {
            double total = 0;
            for (double v : values) total += v;
            for (int i = 0; i < labels.size(); i++) {
                legend.addView(legendRow(labels.get(i), values.get(i), total, chartColors()[i % chartColors().length]));
            }
        }
        row.addView(legend, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(row);
        return c;
    }

    private View legendRow(String label, double value, double total, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(rounded(color, 0, 99));
        row.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));
        TextView name = text("  " + label, 12, false, TEXT);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String pct = total > 0 ? new DecimalFormat("0%").format(value / total) : "0%";
        TextView val = text(pct, 12, true, MUTED);
        row.addView(val);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);
        return row;
    }

    private View recentCard() {
        LinearLayout c = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("最近记录", 17, true, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = text("查看全部 ›", 12, true, PRIMARY);
        more.setOnClickListener(v -> showRecords());
        head.addView(more);
        c.addView(head);
        List<FinanceDb.Record> rows = db.getRecent(null, 4);
        if (rows.isEmpty()) {
            TextView empty = text("还没有账目，去记第一笔吧", 13, false, MUTED);
            empty.setPadding(0, dp(18), 0, dp(4));
            c.addView(empty);
        } else {
            for (FinanceDb.Record r : rows) c.addView(recentRow(r));
        }
        return c;
    }

    private View recentRow(FinanceDb.Record r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));
        int accent = FinanceDb.TYPE_EXPENSE.equals(r.type) ? RED : FinanceDb.TYPE_INCOME.equals(r.type) ? GREEN : ORANGE;
        TextView badge = text(typeZh(r.type), 11, true, accent);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(typeSoft(r.type), 0, 11));
        row.addView(badge, new LinearLayout.LayoutParams(dp(42), dp(28)));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(10), 0, 0, 0);
        mid.addView(text(r.purpose, 14, true, TEXT));
        mid.addView(text(dateFmt.format(new Date(r.occurredAt)), 11, false, MUTED));
        row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView amount = text((FinanceDb.TYPE_INCOME.equals(r.type) ? "+" : "-") + "¥" + money.format(r.amount), 14, true, accent);
        row.addView(amount);
        return row;
    }

    private List<Double> dailyExpenseSeries(Calendar now) {
        List<Double> out = new ArrayList<>();
        Calendar d = cycleStart(now);
        Calendar end = (Calendar) d.clone();
        end.add(Calendar.MONTH, 1);
        Calendar cursor = (Calendar) d.clone();
        while (cursor.before(end)) {
            Calendar next = (Calendar) cursor.clone();
            next.add(Calendar.DAY_OF_MONTH, 1);
            out.add(db.getSummary(cursor.getTimeInMillis(), next.getTimeInMillis()).expense);
            cursor = next;
        }
        return out;
    }

    private int[] chartColors() {
        return new int[]{PRIMARY, BLUE, ORANGE, GREEN, RED};
    }

    private void showAdd() {
        selectNav(1);
        LinearLayout body = pageBody();
        body.addView(sectionTitle("记一笔"));
        body.addView(text("当前周期 " + compactCycle(Calendar.getInstance()), 13, false, MUTED));

        final int[] selectedType = {0};
        body.addView(fieldLabel("类型"));
        body.addView(segmented(new String[]{"消费", "收入", "负债"}, selectedType));

        EditText amount = edit("0.00");
        amount.setTextSize(28);
        amount.setGravity(Gravity.CENTER_VERTICAL);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(fieldLabel("金额"));
        body.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        EditText purpose = edit("例如：午餐、工资、信用卡");
        body.addView(fieldLabel("用途 / 来源名称"));
        body.addView(purpose);

        body.addView(quickPurposeChips(purpose));

        EditText note = edit("备注（可选）");
        body.addView(fieldLabel("备注"));
        body.addView(note);

        TextView date = outlineButton("");
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
        body.addView(date, fullButtonParams());

        TextView save = primaryButton("保存记录");
        save.setOnClickListener(v -> {
            String p = purpose.getText().toString().trim();
            String a = amount.getText().toString().trim();
            if (p.isEmpty()) { toast("请填写用途 / 来源名称"); return; }
            double val;
            try { val = Double.parseDouble(a); }
            catch (Exception e) { toast("请输入正确金额"); return; }
            if (val <= 0) { toast("金额必须大于 0"); return; }
            String dbType = selectedType[0] == 0 ? FinanceDb.TYPE_EXPENSE : selectedType[0] == 1 ? FinanceDb.TYPE_INCOME : FinanceDb.TYPE_DEBT;
            Calendar t = (Calendar) addDate.clone();
            Calendar n = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, n.get(Calendar.HOUR_OF_DAY));
            t.set(Calendar.MINUTE, n.get(Calendar.MINUTE));
            t.set(Calendar.SECOND, n.get(Calendar.SECOND));
            db.addRecord(dbType, p, val, note.getText().toString(), t.getTimeInMillis());
            purpose.setText(""); amount.setText(""); note.setText("");
            toast("已保存");
            showDashboard();
        });
        LinearLayout.LayoutParams saveLp = fullButtonParams();
        saveLp.setMargins(0, dp(22), 0, dp(12));
        body.addView(save, saveLp);
        mount(body);
    }

    private View quickPurposeChips(EditText purpose) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(9), 0, 0);
        String[] presets = {"餐饮", "交通", "购物", "娱乐", "房租"};
        for (String p : presets) {
            TextView chip = text(p, 11, true, MUTED);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(ripple(Color.WHITE, LINE, 13));
            chip.setOnClickListener(v -> purpose.setText(((TextView) v).getText().toString()));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(chip, lp);
        }
        return row;
    }

    private LinearLayout segmented(String[] labels, int[] selected) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setPadding(dp(4), dp(4), dp(4), dp(4));
        wrap.setBackground(rounded(Color.rgb(236, 238, 244), 0, 18));
        TextView[] items = new TextView[labels.length];
        Runnable paint = () -> {
            for (int i = 0; i < items.length; i++) {
                boolean on = i == selected[0];
                items[i].setTextColor(on ? TEXT : MUTED);
                items[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
                items[i].setBackground(on ? rounded(CARD, 0, 15) : transparentRounded());
                items[i].setElevation(on ? dp(2) : 0);
            }
        };
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 14, false, MUTED);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> { selected[0] = index; paint.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(1), 0, dp(1), 0);
            wrap.addView(item, lp);
            items[i] = item;
        }
        paint.run();
        return wrap;
    }

    private void showAnalysis() {
        selectNav(2);
        LinearLayout body = pageBody();
        Calendar now = Calendar.getInstance();
        body.addView(sectionTitle("分析"));
        body.addView(cyclePill(now));

        long[] wr = weekRange(now);
        long[] cr = cycleRange(now);
        FinanceDb.Summary ws = db.getSummary(wr[0], wr[1]);
        FinanceDb.Summary cs = db.getSummary(cr[0], cr[1]);
        body.addView(summaryCard("本周速览", ws), cardParams());
        body.addView(summaryCard("本周期速览", cs), cardParams());
        body.addView(chartCard("本周期每日消费", "从 15 日开始计算", new LineChartView(dailyExpenseSeries(now), PRIMARY)), cardParams());
        body.addView(categoryCard(now), cardParams());

        body.addView(subTitle("本周期消费排行"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_EXPENSE, cr[0], cr[1]), RED);
        body.addView(subTitle("本周期收入来源"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_INCOME, cr[0], cr[1]), GREEN);
        body.addView(subTitle("本周期负债项目"));
        addPurposeBars(body, db.getPurposeTotals(FinanceDb.TYPE_DEBT, cr[0], cr[1]), ORANGE);
        mount(body);
    }

    private View summaryCard(String title, FinanceDb.Summary s) {
        LinearLayout c = card();
        c.addView(text(title, 16, true, TEXT));
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, dp(12), 0, dp(6));
        row1.addView(summaryValue("收入", s.income, GREEN), weightParams());
        row1.addView(summaryValue("消费", s.expense, RED), weightParams());
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(summaryValue("负债", s.debt, ORANGE), weightParams());
        row2.addView(summaryValue("净结余", s.net(), BLUE), weightParams());
        c.addView(row1); c.addView(row2);
        return c;
    }

    private View summaryValue(String label, double value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label, 11, true, color);
        TextView v = text("¥" + money.format(value), 15, true, TEXT);
        v.setPadding(0, dp(3), 0, 0);
        box.addView(l); box.addView(v);
        return box;
    }

    private void addPurposeBars(LinearLayout body, Map<String, Double> map, int color) {
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
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(e.getKey(), 14, true, TEXT);
            TextView val = text("¥" + money.format(e.getValue()), 14, true, color);
            line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(val);
            c.addView(line);
            c.addView(progressLine(max == 0 ? 0 : e.getValue() / max, color), progressParams());
            body.addView(c, cardParams());
        }
    }

    private void showRecords() {
        selectNav(3);
        LinearLayout body = pageBody();
        body.addView(sectionTitle("账目明细"));
        final int[] selectedFilter = {0};
        String[] labels = {"全部", "消费", "收入", "负债"};
        LinearLayout filter = new LinearLayout(this);
        filter.setOrientation(LinearLayout.HORIZONTAL);
        TextView[] chips = new TextView[labels.length];
        LinearLayout listHost = new LinearLayout(this);
        listHost.setOrientation(LinearLayout.VERTICAL);

        Runnable load = () -> {
            for (int i = 0; i < chips.length; i++) {
                boolean on = i == selectedFilter[0];
                chips[i].setTextColor(on ? PRIMARY : MUTED);
                chips[i].setBackground(rounded(on ? PRIMARY_SOFT : CARD, on ? 0 : LINE, 15));
                chips[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            }
            listHost.removeAllViews();
            String type = null;
            if (selectedFilter[0] == 1) type = FinanceDb.TYPE_EXPENSE;
            else if (selectedFilter[0] == 2) type = FinanceDb.TYPE_INCOME;
            else if (selectedFilter[0] == 3) type = FinanceDb.TYPE_DEBT;
            List<FinanceDb.Record> rows = db.getRecent(type, 200);
            if (rows.isEmpty()) {
                TextView empty = text("还没有账目", 14, false, MUTED);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(35), 0, dp(20));
                listHost.addView(empty);
            }
            for (FinanceDb.Record r : rows) listHost.addView(recordRow(r), cardParams());
        };

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView chip = text(labels[i], 12, false, MUTED);
            chip.setGravity(Gravity.CENTER);
            chip.setOnClickListener(v -> { selectedFilter[0] = index; load.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            filter.addView(chip, lp);
            chips[i] = chip;
        }
        body.addView(filter);
        body.addView(listHost);
        load.run();
        mount(body);
    }

    private View recordRow(FinanceDb.Record r) {
        LinearLayout c = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView left = text(r.purpose, 15, true, TEXT);
        int accent = FinanceDb.TYPE_EXPENSE.equals(r.type) ? RED : FinanceDb.TYPE_INCOME.equals(r.type) ? GREEN : ORANGE;
        TextView right = text((FinanceDb.TYPE_INCOME.equals(r.type) ? "+" : "-") + "¥" + money.format(r.amount), 16, true, accent);
        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(right);
        c.addView(top);

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView type = text(typeZh(r.type), 11, true, accent);
        type.setPadding(dp(8), dp(3), dp(8), dp(3));
        type.setBackground(rounded(typeSoft(r.type), 0, 10));
        metaRow.addView(type);
        String meta = "  " + dateFmt.format(new Date(r.occurredAt));
        if (r.note != null && !r.note.trim().isEmpty()) meta += "  ·  " + r.note;
        metaRow.addView(text(meta, 12, false, MUTED), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView del = tinyDangerButton("删除");
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("删除这笔账？")
                .setMessage(typeZh(r.type) + " · " + r.purpose + " · ¥" + money.format(r.amount))
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, which) -> { db.deleteRecord(r.id); showRecords(); }).show());
        metaRow.addView(del, new LinearLayout.LayoutParams(dp(54), dp(32)));
        metaRow.setPadding(0, dp(10), 0, 0);
        c.addView(metaRow);
        return c;
    }

    private int typeSoft(String type) {
        if (FinanceDb.TYPE_EXPENSE.equals(type)) return RED_SOFT;
        if (FinanceDb.TYPE_INCOME.equals(type)) return GREEN_SOFT;
        return ORANGE_SOFT;
    }

    private void showSettings() {
        selectNav(4);
        LinearLayout body = pageBody();
        body.addView(sectionTitle("预算与目标"));
        body.addView(text("预算和目标按 15 日到次月 15 日的周期分别保存", 13, false, MUTED));

        LinearLayout cycleNav = new LinearLayout(this);
        cycleNav.setOrientation(LinearLayout.HORIZONTAL);
        cycleNav.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = squareButton("‹");
        TextView next = squareButton("›");
        TextView cycleLabel = text(displayCycle(planCycle), 16, true, TEXT);
        cycleLabel.setGravity(Gravity.CENTER);
        cycleNav.addView(prev, new LinearLayout.LayoutParams(dp(46), dp(46)));
        cycleNav.addView(cycleLabel, new LinearLayout.LayoutParams(0, dp(46), 1f));
        cycleNav.addView(next, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        navLp.setMargins(0, dp(14), 0, dp(8));
        body.addView(cycleNav, navLp);

        EditText budget = edit("例如 3000");
        EditText target = edit("例如 2000");
        budget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(fieldLabel("这个周期最多能花多少钱")); body.addView(budget);
        body.addView(fieldLabel("这个周期希望净结余多少")); body.addView(target);

        TextView help = text("净结余 = 收入 - 消费 - 负债。预算只统计消费。每月 15 日 00:00 自动进入下一周期。", 13, false, MUTED);
        help.setLineSpacing(dp(3), 1f);
        help.setPadding(0, dp(12), 0, dp(8));
        body.addView(help);

        Runnable reload = () -> {
            cycleLabel.setText(displayCycle(planCycle));
            FinanceDb.Plan p = db.getPlan(cycleKey(planCycle));
            budget.setText(p.budget == 0 ? "" : stripZero(p.budget));
            target.setText(p.target == 0 ? "" : stripZero(p.target));
        };
        prev.setOnClickListener(v -> { planCycle.add(Calendar.MONTH, -1); reload.run(); });
        next.setOnClickListener(v -> { planCycle.add(Calendar.MONTH, 1); reload.run(); });

        TextView save = primaryButton("保存这个周期");
        save.setOnClickListener(v -> {
            double b = parseNonNegative(budget.getText().toString());
            double t = parseNonNegative(target.getText().toString());
            if (b < 0 || t < 0) { toast("请输入 0 或正数"); return; }
            db.savePlan(cycleKey(planCycle), b, t);
            toast("已保存 " + displayCycle(planCycle));
            reload.run();
        });
        LinearLayout.LayoutParams lp = fullButtonParams();
        lp.setMargins(0, dp(16), 0, dp(10));
        body.addView(save, lp);
        reload.run();
        mount(body);
    }

    private View cyclePill(Calendar source) {
        TextView pill = text("记账周期  " + displayCycle(source), 13, true, PRIMARY);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(13), 0, dp(13), 0);
        pill.setBackground(rounded(PRIMARY_SOFT, 0, 15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(dp(2), 0, 0, dp(10));
        pill.setLayoutParams(lp);
        return pill;
    }

    private View progressCard(String title, String detail, double rate, String foot, int color) {
        LinearLayout c = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(title, 16, true, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(text(percent(Math.max(0, rate)), 13, true, color));
        c.addView(head);
        TextView d = text(detail, 14, false, MUTED);
        d.setPadding(0, dp(7), 0, 0);
        c.addView(d);
        c.addView(progressLine(rate, color), progressParams());
        TextView f = text(foot, 13, false, MUTED);
        f.setPadding(0, dp(8), 0, 0);
        c.addView(f);
        return c;
    }

    private View progressLine(double rate, int color) {
        double safe = Math.max(0, Math.min(1, rate));
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(rounded(Color.rgb(235, 237, 242), 0, 99));
        if (safe > 0) {
            View fill = new View(this);
            fill.setBackground(rounded(color, 0, 99));
            track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) safe));
        }
        if (safe < 1) {
            View rest = new View(this);
            track.addView(rest, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) (1 - safe)));
        }
        return track;
    }

    private LinearLayout.LayoutParams progressParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        lp.setMargins(0, dp(11), 0, 0);
        return lp;
    }

    private LinearLayout pageBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(15), dp(8), dp(15), dp(30));
        return body;
    }

    private void mount(LinearLayout body) {
        pageHost.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.addView(body);
        pageHost.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(rounded(CARD, 0, 20));
        c.setElevation(dp(2));
        return c;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(7), 0, dp(7));
        return lp;
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams weightParamsWithMargins() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(5), dp(3), dp(5));
        return lp;
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 23, true, TEXT);
        t.setPadding(dp(2), dp(4), 0, dp(10));
        return t;
    }

    private TextView subTitle(String s) {
        TextView t = text(s, 17, true, TEXT);
        t.setPadding(dp(2), dp(20), 0, dp(5));
        return t;
    }

    private TextView fieldLabel(String s) {
        TextView t = text(s, 13, true, MUTED);
        t.setPadding(dp(2), dp(15), 0, dp(7));
        return t;
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(TEXT);
        e.setHintTextColor(Color.rgb(161, 166, 178));
        e.setBackground(rounded(CARD, LINE, 16));
        e.setPadding(dp(15), 0, dp(15), 0);
        e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        return e;
    }

    private TextView primaryButton(String label) {
        TextView b = text(label, 15, true, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setBackground(ripple(PRIMARY, PRIMARY, 17));
        b.setElevation(dp(3));
        return b;
    }

    private TextView outlineButton(String label) {
        TextView b = text(label, 14, true, TEXT);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(15), 0, dp(15), 0);
        b.setBackground(ripple(CARD, LINE, 16));
        return b;
    }

    private TextView squareButton(String label) {
        TextView b = text(label, 25, false, PRIMARY);
        b.setGravity(Gravity.CENTER);
        b.setBackground(ripple(PRIMARY_SOFT, PRIMARY_SOFT, 15));
        return b;
    }

    private TextView tinyDangerButton(String label) {
        TextView b = text(label, 12, true, RED);
        b.setGravity(Gravity.CENTER);
        b.setBackground(ripple(RED_SOFT, RED_SOFT, 12));
        return b;
    }

    private Drawable ripple(int fill, int stroke, int radiusDp) {
        GradientDrawable content = rounded(fill, stroke == fill ? 0 : stroke, radiusDp);
        GradientDrawable mask = rounded(Color.WHITE, 0, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(32, 0, 0, 0)), content, mask);
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable gradientRounded(int start, int end, int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable transparentRounded() {
        return rounded(Color.TRANSPARENT, 0, 16);
    }

    private long[] cycleRange(Calendar source) {
        Calendar s = cycleStart(source);
        Calendar e = (Calendar) s.clone();
        e.add(Calendar.MONTH, 1);
        return new long[]{s.getTimeInMillis(), e.getTimeInMillis()};
    }

    private Calendar cycleStart(Calendar source) {
        Calendar s = (Calendar) source.clone();
        zeroTime(s);
        if (s.get(Calendar.DAY_OF_MONTH) < 15) s.add(Calendar.MONTH, -1);
        s.set(Calendar.DAY_OF_MONTH, 15);
        zeroTime(s);
        return s;
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

    private String cycleKey(Calendar source) {
        Calendar s = cycleStart(source);
        return String.format(Locale.US, "%04d-%02d", s.get(Calendar.YEAR), s.get(Calendar.MONTH) + 1);
    }

    private String compactCycle(Calendar source) {
        Calendar s = cycleStart(source);
        Calendar e = (Calendar) s.clone();
        e.add(Calendar.MONTH, 1);
        return String.format(Locale.CHINA, "%02d/15 - %02d/15", s.get(Calendar.MONTH) + 1, e.get(Calendar.MONTH) + 1);
    }

    private String displayCycle(Calendar source) {
        Calendar s = cycleStart(source);
        Calendar e = (Calendar) s.clone();
        e.add(Calendar.MONTH, 1);
        if (s.get(Calendar.YEAR) == e.get(Calendar.YEAR)) {
            return String.format(Locale.CHINA, "%d年%d月15日 — %d月15日", s.get(Calendar.YEAR), s.get(Calendar.MONTH) + 1, e.get(Calendar.MONTH) + 1);
        }
        return String.format(Locale.CHINA, "%d年%d月15日 — %d年%d月15日", s.get(Calendar.YEAR), s.get(Calendar.MONTH) + 1, e.get(Calendar.YEAR), e.get(Calendar.MONTH) + 1);
    }

    private String percent(double rate) {
        if (Double.isNaN(rate) || Double.isInfinite(rate)) return "0.0%";
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

    private void updateDateButton(TextView b) {
        b.setText("选择日期   " + dateFmt.format(addDate.getTime()));
    }

    private String typeZh(String type) {
        if (FinanceDb.TYPE_EXPENSE.equals(type)) return "消费";
        if (FinanceDb.TYPE_INCOME.equals(type)) return "收入";
        return "负债";
    }

    private void selectNav(int index) {
        selectedNav = index;
        updateNav();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private class BudgetRingView extends View {
        private final double rate;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        BudgetRingView(double rate) { super(MainActivity.this); this.rate = Math.max(0, rate); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float stroke = dp(10);
            float pad = stroke / 2f + dp(4);
            RectF oval = new RectF(pad, pad, w - pad, h - pad);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.argb(55, 255, 255, 255));
            canvas.drawArc(oval, -90, 360, false, p);
            p.setColor(Color.WHITE);
            canvas.drawArc(oval, -90, (float) (360 * Math.min(1, rate)), false, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(dp(16));
            String pct = new DecimalFormat("0%").format(Math.min(1, rate));
            canvas.drawText(pct, w / 2f, h / 2f + dp(5), p);
        }
    }

    private class LineChartView extends View {
        private final List<Double> values;
        private final int color;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        LineChartView(List<Double> values, int color) { super(MainActivity.this); this.values = values; this.color = color; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float left = dp(8), top = dp(14), right = getWidth() - dp(8), bottom = getHeight() - dp(18);
            p.setStrokeWidth(dp(1));
            p.setColor(Color.rgb(236, 238, 244));
            for (int i = 0; i < 4; i++) {
                float y = top + (bottom - top) * i / 3f;
                canvas.drawLine(left, y, right, y, p);
            }
            double max = 0;
            for (double v : values) max = Math.max(max, v);
            if (values.isEmpty() || max <= 0) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(MUTED);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(dp(12));
                canvas.drawText("暂无消费数据", getWidth() / 2f, getHeight() / 2f, p);
                return;
            }
            Path path = new Path();
            for (int i = 0; i < values.size(); i++) {
                float x = left + (right - left) * i / Math.max(1, values.size() - 1);
                float y = bottom - (float) ((values.get(i) / max) * (bottom - top));
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(color);
            canvas.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            for (int i = 0; i < values.size(); i += Math.max(1, values.size() / 6)) {
                float x = left + (right - left) * i / Math.max(1, values.size() - 1);
                float y = bottom - (float) ((values.get(i) / max) * (bottom - top));
                canvas.drawCircle(x, y, dp(3), p);
            }
            p.setColor(MUTED);
            p.setTextSize(dp(10));
            p.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("15日", left, getHeight() - dp(2), p);
            p.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("14日", right, getHeight() - dp(2), p);
        }
    }

    private class DonutChartView extends View {
        private final List<Double> values;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        DonutChartView(List<Double> values) { super(MainActivity.this); this.values = values; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float stroke = dp(18);
            float pad = stroke / 2f + dp(5);
            RectF oval = new RectF(pad, pad, getWidth() - pad, getHeight() - pad);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(Paint.Cap.BUTT);
            double total = 0;
            for (double v : values) total += v;
            if (total <= 0) {
                p.setColor(Color.rgb(232, 234, 240));
                canvas.drawArc(oval, -90, 360, false, p);
            } else {
                float start = -90;
                int[] colors = chartColors();
                for (int i = 0; i < values.size(); i++) {
                    float sweep = (float) (360 * values.get(i) / total);
                    p.setColor(colors[i % colors.length]);
                    canvas.drawArc(oval, start, sweep, false, p);
                    start += sweep;
                }
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(TEXT);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(13));
            canvas.drawText(total > 0 ? "消费" : "暂无", getWidth() / 2f, getHeight() / 2f + dp(4), p);
        }
    }
}
