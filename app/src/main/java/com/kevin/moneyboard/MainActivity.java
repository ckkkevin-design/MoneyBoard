package com.kevin.moneyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQ_BACKUP = 201;
    private static final int REQ_RESTORE = 202;
    private static final int REQ_CSV = 203;

    private FinanceDb db;
    private SharedPreferences prefs;
    private LinearLayout pageHost;
    private TextView[] navItems;
    private int selectedNav = 0;
    private Calendar addDate = Calendar.getInstance();
    private Calendar customStart = null;
    private Calendar customEnd = null;

    private int BG, CARD, CARD_ALT, TEXT, MUTED, LINE, BLUE, BLUE_SOFT, GREEN, GREEN_SOFT,
            RED, RED_SOFT, ORANGE, ORANGE_SOFT, PURPLE, PURPLE_SOFT, NAV_BG, SHADOW, INPUT_BG;
    private boolean dark;

    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("MM月dd日", Locale.CHINA);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new FinanceDb(this);
        prefs = getSharedPreferences("moneyboard_settings", MODE_PRIVATE);
        applyPalette();
        applySystemBars();
        setContentView(buildRoot());
        showHome();
    }

    private void applyPalette() {
        String mode = prefs == null ? "light" : prefs.getString("theme", "light");
        if ("system".equals(mode)) {
            dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        } else dark = "dark".equals(mode);

        if (dark) {
            BG = Color.rgb(14, 20, 27);
            CARD = Color.rgb(24, 32, 42);
            CARD_ALT = Color.rgb(29, 39, 50);
            TEXT = Color.rgb(244, 247, 251);
            MUTED = Color.rgb(155, 166, 180);
            LINE = Color.rgb(48, 60, 74);
            NAV_BG = Color.rgb(17, 24, 32);
            INPUT_BG = Color.rgb(31, 41, 53);
        } else {
            BG = Color.rgb(248, 250, 253);
            CARD = Color.WHITE;
            CARD_ALT = Color.rgb(251, 252, 254);
            TEXT = Color.rgb(17, 24, 39);
            MUTED = Color.rgb(112, 122, 139);
            LINE = Color.rgb(231, 235, 241);
            NAV_BG = Color.WHITE;
            INPUT_BG = Color.rgb(246, 248, 251);
        }
        BLUE = Color.rgb(58, 132, 247);
        BLUE_SOFT = dark ? Color.rgb(25, 52, 79) : Color.rgb(234, 244, 255);
        GREEN = Color.rgb(18, 181, 121);
        GREEN_SOFT = dark ? Color.rgb(22, 74, 57) : Color.rgb(229, 250, 241);
        RED = Color.rgb(255, 73, 91);
        RED_SOFT = dark ? Color.rgb(82, 38, 46) : Color.rgb(255, 237, 240);
        ORANGE = Color.rgb(245, 154, 35);
        ORANGE_SOFT = dark ? Color.rgb(78, 56, 28) : Color.rgb(255, 246, 230);
        PURPLE = Color.rgb(128, 92, 246);
        PURPLE_SOFT = dark ? Color.rgb(56, 44, 92) : Color.rgb(243, 238, 255);
        SHADOW = dark ? Color.TRANSPARENT : Color.argb(18, 15, 23, 42);
    }

    private void applySystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(NAV_BG);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(8), dp(6), dp(8), dp(8));
        nav.setBackgroundColor(NAV_BG);
        nav.setElevation(dp(12));

        String[] labels = {"⌂\n首页", "✎\n记账", "▥\n分析", "⚙\n我的"};
        Runnable[] pages = {this::showHome, this::showAdd, this::showAnalysis, this::showSettings};
        navItems = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 11, false, MUTED);
            item.setGravity(Gravity.CENTER);
            item.setLineSpacing(0, .92f);
            item.setOnClickListener(v -> {
                selectedNav = index;
                updateNav();
                pages[index].run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
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
            navItems[i].setTextColor(on ? (i == 0 ? GREEN : BLUE) : MUTED);
            navItems[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void showHome() {
        selectNav(0);
        Calendar now = Calendar.getInstance();
        long[] cycle = cycleRange(now);
        FinanceDb.Summary s = db.getSummary(cycle[0], cycle[1]);
        FinanceDb.Plan plan = db.getPlan(cycleKey(now));

        LinearLayout body = pageBody();
        body.addView(homeTopBar());
        body.addView(homeBudgetCard(s, plan), cardParams(0));
        body.addView(periodCard(now), cardParams(10));
        body.addView(todaySummary(), cardParams(10));
        body.addView(sectionLabel("快捷操作", null));
        body.addView(quickActions(), cardParams(6));
        body.addView(homeMiniMetrics(s), cardParams(10));
        body.addView(goalCard(s, plan), cardParams(10));
        body.addView(recentCard(), cardParams(10));
        mount(body);
    }

    private View homeTopBar() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("▥", 18, true, Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(gradientRounded(Color.rgb(62, 126, 244), Color.rgb(63, 161, 240), 9));
        row.addView(logo, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView title = text("MoneyBoard", 20, true, TEXT);
        title.setPadding(dp(9), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView gear = text("⚙", 21, false, TEXT);
        gear.setGravity(Gravity.CENTER);
        gear.setOnClickListener(v -> showSettings());
        row.addView(gear, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(2), 0, dp(2), dp(12));
        row.setLayoutParams(lp);
        return row;
    }

    private View homeBudgetCard(FinanceDb.Summary s, FinanceDb.Plan p) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(17), dp(18), dp(16));
        c.setBackground(gradientRounded(dark ? Color.rgb(18, 75, 58) : Color.rgb(222, 250, 239), dark ? Color.rgb(14, 88, 66) : Color.rgb(235, 255, 247), 20));
        c.setElevation(dp(2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text("本期剩余", 12, true, dark ? Color.rgb(148, 224, 193) : Color.rgb(56, 113, 88)));
        double remain = p.budget - s.expense;
        String amount = p.budget <= 0 ? "未设置预算" : (remain >= 0 ? "¥" + money.format(remain) : "-¥" + money.format(-remain));
        TextView big = text(amount, p.budget > 0 ? 29 : 20, true, TEXT);
        big.setPadding(0, dp(4), 0, 0);
        left.addView(big);
        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        double rate = p.budget > 0 ? s.expense / p.budget : 0;
        RingProgressView ring = new RingProgressView(rate, GREEN, dark ? Color.rgb(48, 96, 76) : Color.rgb(187, 235, 216), TEXT);
        top.addView(ring, new LinearLayout.LayoutParams(dp(82), dp(82)));
        c.addView(top);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(0, dp(11), 0, 0);
        bottom.addView(homeBudgetStat("本期已用", "¥" + money.format(s.expense)), weight());
        bottom.addView(homeBudgetStat("本期预算", p.budget > 0 ? "¥" + money.format(p.budget) : "待设置"), weight());
        c.addView(bottom);
        return c;
    }

    private View homeBudgetStat(String label, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(label, 11, false, dark ? Color.rgb(151, 188, 173) : Color.rgb(86, 125, 108)));
        TextView v = text(value, 13, true, TEXT);
        v.setPadding(0, dp(3), 0, 0);
        box.addView(v);
        return box;
    }

    private View periodCard(Calendar now) {
        LinearLayout c = rowCard(BLUE_SOFT);
        TextView icon = iconBubble("▣", BLUE, dark ? Color.rgb(34, 65, 95) : Color.WHITE);
        c.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(11), 0, 0, 0);
        info.addView(text("当前周期", 11, true, BLUE));
        info.addView(text(displayCycle(now), 13, true, TEXT));
        c.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView days = text("剩余 " + daysRemaining(now) + " 天", 11, true, MUTED);
        c.addView(days);
        return c;
    }

    private View todaySummary() {
        Calendar start = Calendar.getInstance();
        zeroTime(start);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 1);
        FinanceDb.Summary s = db.getSummary(start.getTimeInMillis(), end.getTimeInMillis());
        int count = db.getRecords(null, start.getTimeInMillis(), end.getTimeInMillis(), 200).size();
        LinearLayout c = card();
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text("今日记录", 13, true, TEXT));
        TextView v = text("¥" + money.format(s.expense), 19, true, s.expense > 0 ? RED : TEXT);
        v.setPadding(0, dp(3), 0, 0);
        left.addView(v);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(count + " 笔消费", 12, false, MUTED));
        c.addView(row);
        return c;
    }

    private View quickActions() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.addView(actionButton("✎  记一笔", BLUE, dark ? Color.rgb(27, 60, 103) : Color.rgb(233, 244, 255), this::showAdd), weightMargin(3));
        r1.addView(actionButton("▣  设置预算", GREEN, GREEN_SOFT, this::showBudget), weightMargin(3));
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.setPadding(0, dp(8), 0, 0);
        r2.addView(actionButton("☷  账单列表", TEXT, CARD, this::showRecords), weightMargin(3));
        r2.addView(actionButton("▥  图表分析", GREEN, GREEN_SOFT, this::showAnalysis), weightMargin(3));
        outer.addView(r1);
        outer.addView(r2);
        return outer;
    }

    private TextView actionButton(String label, int color, int fill, Runnable action) {
        TextView b = text(label, 13, true, color);
        b.setGravity(Gravity.CENTER);
        b.setBackground(ripple(fill, fill == CARD ? LINE : 0, 15));
        b.setOnClickListener(v -> action.run());
        b.setElevation(dark ? 0 : dp(1));
        b.setMinHeight(dp(48));
        return b;
    }

    private View homeMiniMetrics(FinanceDb.Summary s) {
        LinearLayout c = card();
        c.setPadding(dp(10), dp(13), dp(10), dp(13));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(metricMini("收入", s.income, GREEN), weight());
        row.addView(metricMini("消费", s.expense, RED), weight());
        row.addView(metricMini("负债", s.debt, ORANGE), weight());
        row.addView(metricMini("结余", s.net(), BLUE), weight());
        c.addView(row);
        return c;
    }

    private View metricMini(String label, double value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(text(label, 11, false, MUTED));
        TextView v = text(shortMoney(value), 13, true, color);
        v.setPadding(0, dp(4), 0, 0);
        box.addView(v);
        return box;
    }

    private View goalCard(FinanceDb.Summary s, FinanceDb.Plan p) {
        LinearLayout c = card();
        LinearLayout head = new LinearLayout(this); head.setOrientation(LinearLayout.HORIZONTAL); head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("本期结余目标", 13, true, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        double rate = p.target > 0 ? s.net() / p.target : 0;
        head.addView(text(p.target > 0 ? new DecimalFormat("0%").format(Math.max(0, Math.min(1, rate))) : "未设置", 12, true, GREEN));
        c.addView(head);
        c.addView(progressLine(rate, GREEN), progressParams());
        TextView foot = text(p.target > 0 ? "当前净结余 ¥" + money.format(s.net()) + " / 目标 ¥" + money.format(p.target) : "在预算管理里设置本周期净结余目标", 11, false, MUTED);
        foot.setPadding(0, dp(8), 0, 0); c.addView(foot);
        c.setOnClickListener(v -> showBudget());
        return c;
    }

    private View recentCard() {
        LinearLayout c = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("最近记录", 15, true, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = text("查看全部  ›", 12, true, BLUE);
        more.setOnClickListener(v -> showRecords());
        head.addView(more);
        c.addView(head);
        List<FinanceDb.Record> rows = db.getRecent(null, 4);
        if (rows.isEmpty()) {
            TextView empty = text("还没有记录，点“记一笔”开始吧", 13, false, MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(8));
            c.addView(empty);
        } else {
            for (FinanceDb.Record r : rows) c.addView(transactionCompactRow(r));
        }
        return c;
    }

    private View transactionCompactRow(FinanceDb.Record r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(8));
        TextView icon = categoryIcon(r.purpose, 34);
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, 0, 0);
        info.addView(text(r.purpose, 13, true, TEXT));
        info.addView(text(dayFmt.format(new Date(r.occurredAt)) + "  " + timeFmt.format(new Date(r.occurredAt)), 10, false, MUTED));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int color = typeColor(r.type);
        String sign = FinanceDb.TYPE_INCOME.equals(r.type) ? "+" : "-";
        row.addView(text(sign + "¥" + money.format(r.amount), 13, true, color));
        return row;
    }

    private void showAdd() {
        selectNav(1);
        LinearLayout body = pageBody();
        body.addView(simpleTopBar("记一笔", null));
        final int[] selectedType = {0};
        body.addView(typeSegmented(selectedType));

        EditText amount = edit("0.00");
        amount.setTextSize(32);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setGravity(Gravity.CENTER_VERTICAL);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setPadding(dp(8), 0, dp(8), 0);
        amount.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout amountWrap = card();
        amountWrap.setPadding(dp(16), dp(10), dp(16), dp(10));
        TextView yuan = text("¥", 22, true, TEXT);
        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setOrientation(LinearLayout.HORIZONTAL);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.addView(yuan);
        amountRow.addView(amount, new LinearLayout.LayoutParams(0, dp(62), 1f));
        amountWrap.addView(amountRow);
        body.addView(amountWrap, cardParams(12));

        TextView catTitle = text("选择用途", 13, true, TEXT);
        catTitle.setPadding(dp(2), dp(2), 0, dp(8));
        body.addView(catTitle);
        EditText purpose = edit("自定义用途名称");
        body.addView(categoryGrid(purpose));
        LinearLayout.LayoutParams purposeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        purposeLp.setMargins(0, dp(10), 0, 0);
        body.addView(purpose, purposeLp);

        TextView date = settingField("日期", dateFmt.format(addDate.getTime()));
        date.setOnClickListener(v -> new DatePickerDialog(this, (view, y, m, d) -> {
            addDate.set(y, m, d);
            ((TextView) v).setText("日期                                      " + dateFmt.format(addDate.getTime()));
        }, addDate.get(Calendar.YEAR), addDate.get(Calendar.MONTH), addDate.get(Calendar.DAY_OF_MONTH)).show());
        body.addView(date, fieldParams());

        EditText note = edit("备注（可选）");
        body.addView(note, fieldParams());

        TextView save = filledButton("保存", RED);
        save.setOnClickListener(v -> {
            String p = purpose.getText().toString().trim();
            if (p.isEmpty()) { toast("请选择或填写用途"); return; }
            double val;
            try { val = Double.parseDouble(amount.getText().toString().trim()); }
            catch (Exception e) { toast("请输入正确金额"); return; }
            if (val <= 0) { toast("金额需要大于 0"); return; }
            String type = selectedType[0] == 0 ? FinanceDb.TYPE_EXPENSE : selectedType[0] == 1 ? FinanceDb.TYPE_INCOME : FinanceDb.TYPE_DEBT;
            Calendar t = (Calendar) addDate.clone();
            Calendar now = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
            t.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
            t.set(Calendar.SECOND, now.get(Calendar.SECOND));
            db.addRecord(type, p, val, note.getText().toString(), t.getTimeInMillis());
            toast("已保存");
            amount.setText(""); purpose.setText(""); note.setText("");
            showHome();
        });
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        saveLp.setMargins(0, dp(18), 0, dp(16));
        body.addView(save, saveLp);
        mount(body);
    }

    private View typeSegmented(int[] selected) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setPadding(dp(4), dp(4), dp(4), dp(4));
        wrap.setBackground(rounded(INPUT_BG, 0, 18));
        String[] labels = {"支出", "收入", "负债"};
        int[] colors = {RED, GREEN, ORANGE};
        TextView[] items = new TextView[3];
        Runnable repaint = () -> {
            for (int i = 0; i < 3; i++) {
                boolean on = selected[0] == i;
                items[i].setTextColor(on ? Color.WHITE : MUTED);
                items[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
                items[i].setBackground(on ? rounded(colors[i], 0, 15) : rounded(Color.TRANSPARENT, 0, 15));
            }
        };
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView item = text(labels[i], 13, false, MUTED);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> { selected[0] = idx; repaint.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            wrap.addView(item, lp);
            items[i] = item;
        }
        repaint.run();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        wrap.setLayoutParams(lp);
        return wrap;
    }

    private View categoryGrid(EditText purpose) {
        String[] names = {"餐饮", "购物", "交通", "日用", "娱乐", "医疗", "学习", "其他"};
        String[] icons = {"🍴", "▣", "▰", "⌂", "♬", "+", "▤", "•••"};
        int[] colors = {RED, BLUE, Color.rgb(61, 151, 221), PURPLE, ORANGE, GREEN, Color.rgb(79, 137, 213), PURPLE};
        int[] soft = {RED_SOFT, BLUE_SOFT, dark ? Color.rgb(28, 61, 82) : Color.rgb(233, 247, 255), PURPLE_SOFT, ORANGE_SOFT, GREEN_SOFT, BLUE_SOFT, PURPLE_SOFT};
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 4; c++) {
                int idx = r * 4 + c;
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                TextView icon = text(icons[idx], 17, true, colors[idx]);
                icon.setGravity(Gravity.CENTER);
                icon.setBackground(rounded(soft[idx], 0, 16));
                item.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
                TextView name = text(names[idx], 11, idx == 0, idx == 0 ? RED : TEXT);
                name.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                nlp.setMargins(0, dp(5), 0, 0);
                item.addView(name, nlp);
                item.setOnClickListener(v -> purpose.setText(names[idx]));
                row.addView(item, new LinearLayout.LayoutParams(0, dp(80), 1f));
            }
            outer.addView(row);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outer.setBackground(rounded(CARD, LINE, 18));
        outer.setPadding(dp(6), dp(9), dp(6), dp(8));
        outer.setLayoutParams(lp);
        return outer;
    }

    private void showAnalysis() {
        selectNav(2);
        LinearLayout body = pageBody();
        body.addView(simpleTopBar("图表分析", null));
        final int[] period = {0};
        LinearLayout chartHost = new LinearLayout(this);
        chartHost.setOrientation(LinearLayout.VERTICAL);
        body.addView(periodTabs(period, chartHost));
        body.addView(chartHost);
        loadAnalysis(chartHost, period[0]);
        mount(body);
    }

    private View periodTabs(int[] selected, LinearLayout host) {
        String[] labels = {"本期", "本周", "本月", "自定义"};
        TextView[] items = new TextView[labels.length];
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Runnable repaint = () -> {
            for (int i = 0; i < items.length; i++) {
                boolean on = selected[0] == i;
                items[i].setTextColor(on ? Color.WHITE : MUTED);
                items[i].setBackground(rounded(on ? BLUE : INPUT_BG, 0, 14));
                items[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            }
        };
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView item = text(labels[i], 11, false, MUTED);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> {
                if (idx == 3) {
                    chooseCustomRange(() -> {
                        selected[0] = 3; repaint.run(); loadAnalysis(host, 3);
                    });
                } else {
                    selected[0] = idx; repaint.run(); loadAnalysis(host, idx);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(item, lp);
            items[i] = item;
        }
        repaint.run();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(lp);
        return row;
    }

    private void chooseCustomRange(Runnable done) {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -30);
        new DatePickerDialog(this, (v, y, m, d) -> {
            customStart = Calendar.getInstance(); customStart.set(y, m, d); zeroTime(customStart);
            Calendar endDefault = Calendar.getInstance();
            new DatePickerDialog(this, (v2, y2, m2, d2) -> {
                customEnd = Calendar.getInstance(); customEnd.set(y2, m2, d2); zeroTime(customEnd); customEnd.add(Calendar.DAY_OF_MONTH, 1);
                if (customEnd.before(customStart)) { toast("结束日期不能早于开始日期"); return; }
                done.run();
            }, endDefault.get(Calendar.YEAR), endDefault.get(Calendar.MONTH), endDefault.get(Calendar.DAY_OF_MONTH)).show();
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadAnalysis(LinearLayout host, int period) {
        host.removeAllViews();
        long[] range = rangeFor(period);
        FinanceDb.Summary s = db.getSummary(range[0], range[1]);
        Map<String, Double> map = db.getPurposeTotals(FinanceDb.TYPE_EXPENSE, range[0], range[1]);

        LinearLayout donutCard = card();
        donutCard.addView(text("支出构成", 15, true, TEXT));
        LinearLayout donutRow = new LinearLayout(this);
        donutRow.setOrientation(LinearLayout.HORIZONTAL);
        donutRow.setGravity(Gravity.CENTER_VERTICAL);
        List<Double> values = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int count = 0;
        double other = 0;
        for (Map.Entry<String, Double> e : map.entrySet()) {
            if (count < 5) { names.add(e.getKey()); values.add(e.getValue()); count++; }
            else other += e.getValue();
        }
        if (other > 0) { names.add("其他"); values.add(other); }
        DonutChartView donut = new DonutChartView(values, s.expense);
        donutRow.addView(donut, new LinearLayout.LayoutParams(dp(150), dp(150)));
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        legend.setPadding(dp(10), 0, 0, 0);
        double total = s.expense;
        int[] colors = chartColors();
        for (int i = 0; i < names.size(); i++) legend.addView(legendRow(names.get(i), values.get(i), total, colors[i % colors.length]));
        if (names.isEmpty()) legend.addView(text("暂无支出数据", 12, false, MUTED));
        donutRow.addView(legend, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        donutCard.addView(donutRow);
        host.addView(donutCard, cardParams(0));

        LinearLayout trend = card();
        trend.addView(text("每日支出趋势", 15, true, TEXT));
        TextView sub = text(periodName(period) + "  ·  支出 ¥" + money.format(s.expense), 11, false, MUTED);
        sub.setPadding(0, dp(3), 0, dp(8));
        trend.addView(sub);
        trend.addView(new BarChartView(dailySeries(range[0], range[1])), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190)));
        host.addView(trend, cardParams(10));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(actionButton("数据统计", BLUE, BLUE_SOFT, this::showStats), weightMargin(4));
        actions.addView(actionButton("用途排行", GREEN, GREEN_SOFT, this::showRanking), weightMargin(4));
        host.addView(actions, cardParams(10));
    }

    private View legendRow(String name, double value, double total, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this); dot.setBackground(rounded(color, 0, 99));
        row.addView(dot, new LinearLayout.LayoutParams(dp(7), dp(7)));
        TextView n = text("  " + name, 11, false, TEXT);
        row.addView(n, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(total > 0 ? new DecimalFormat("0%").format(value / total) : "0%", 11, true, MUTED));
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private void showBudget() {
        LinearLayout body = pageBody();
        body.addView(backTopBar("预算管理", this::showHome));
        Calendar now = Calendar.getInstance();
        long[] cycle = cycleRange(now);
        FinanceDb.Summary s = db.getSummary(cycle[0], cycle[1]);
        FinanceDb.Plan p = db.getPlan(cycleKey(now));

        LinearLayout period = rowCard(BLUE_SOFT);
        period.addView(iconBubble("▣", BLUE, dark ? Color.rgb(34, 65, 95) : Color.WHITE), new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout pi = new LinearLayout(this); pi.setOrientation(LinearLayout.VERTICAL); pi.setPadding(dp(10),0,0,0);
        pi.addView(text("当前周期", 11, true, BLUE)); pi.addView(text(displayCycle(now), 13, true, TEXT));
        period.addView(pi, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView modify = text("修改", 11, true, BLUE); modify.setPadding(dp(10), dp(6), dp(10), dp(6)); modify.setBackground(rounded(dark ? Color.rgb(35,65,92) : Color.WHITE,0,12)); modify.setOnClickListener(v->editTotalPlan(now));
        period.addView(modify);
        body.addView(period, cardParams(0));

        LinearLayout totalCard = card();
        LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(text("总预算", 13, true, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(text(p.budget > 0 ? new DecimalFormat("0%").format(Math.min(1, s.expense / p.budget)) : "0%", 13, true, TEXT));
        totalCard.addView(line);
        TextView big = text(p.budget > 0 ? "¥" + money.format(p.budget) : "未设置", 25, true, TEXT); big.setPadding(0, dp(8),0,0); totalCard.addView(big);
        totalCard.addView(progressLine(p.budget > 0 ? s.expense / p.budget : 0, GREEN), progressParams());
        TextView foot = text("已用 ¥" + money.format(s.expense) + (p.budget > 0 ? "       剩余 ¥" + money.format(Math.max(0, p.budget - s.expense)) : ""), 11, false, MUTED); foot.setPadding(0,dp(8),0,0); totalCard.addView(foot);
        body.addView(totalCard, cardParams(10));

        body.addView(sectionLabel("分类预算", "+ 添加分类"));
        Map<String, Double> saved = db.getCategoryBudgets(cycleKey(now));
        String[] defaults = {"餐饮", "购物", "交通", "日用"};
        if (saved.isEmpty()) {
            double base = p.budget > 0 ? p.budget : 0;
            saved = new LinkedHashMap<>();
            saved.put("餐饮", base * .30); saved.put("购物", base * .20); saved.put("交通", base * .12); saved.put("日用", base * .12);
        }
        Map<String, Double> spent = db.getPurposeTotals(FinanceDb.TYPE_EXPENSE, cycle[0], cycle[1]);
        for (Map.Entry<String, Double> e : saved.entrySet()) {
            double used = spent.containsKey(e.getKey()) ? spent.get(e.getKey()) : 0;
            body.addView(categoryBudgetRow(now, e.getKey(), e.getValue(), used), cardParams(7));
        }
        TextView add = text("＋ 添加分类预算", 13, true, BLUE); add.setGravity(Gravity.CENTER); add.setBackground(ripple(BLUE_SOFT,0,16)); add.setOnClickListener(v->editCategoryBudget(now, null, 0));
        body.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        mount(body);
    }

    private View categoryBudgetRow(Calendar now, String category, double budget, double used) {
        LinearLayout c = card();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = categoryIcon(category, 38); top.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(10),0,0,0);
        info.addView(text(category, 13, true, TEXT));
        info.addView(text("¥" + money.format(used) + " 已用", 10, false, MUTED));
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView right = text(budget > 0 ? "¥" + money.format(budget) : "未设置", 13, true, TEXT); right.setOnClickListener(v->editCategoryBudget(now, category, budget)); top.addView(right);
        c.addView(top);
        c.addView(progressLine(budget > 0 ? used / budget : 0, used > budget && budget > 0 ? RED : GREEN), progressParams());
        return c;
    }

    private void editTotalPlan(Calendar cycle) {
        FinanceDb.Plan p = db.getPlan(cycleKey(cycle));
        LinearLayout box = dialogForm();
        EditText budget = edit("总预算"); budget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); if (p.budget > 0) budget.setText(stripZero(p.budget));
        EditText target = edit("净结余目标"); target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); if (p.target > 0) target.setText(stripZero(p.target));
        box.addView(budget); box.addView(target, fieldParams());
        new AlertDialog.Builder(this).setTitle("本周期预算与目标").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            double b=parseNonNegative(budget.getText().toString()), t=parseNonNegative(target.getText().toString());
            if(b<0||t<0){toast("金额格式不正确");return;} db.savePlan(cycleKey(cycle),b,t); showBudget();
        }).show();
    }

    private void editCategoryBudget(Calendar cycle, String category, double current) {
        LinearLayout box = dialogForm();
        EditText name = edit("分类名称"); if(category!=null) name.setText(category);
        EditText amount = edit("预算金额"); amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); if(current>0) amount.setText(stripZero(current));
        box.addView(name); box.addView(amount, fieldParams());
        new AlertDialog.Builder(this).setTitle(category==null?"添加分类预算":"修改分类预算").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            double v=parseNonNegative(amount.getText().toString()); if(name.getText().toString().trim().isEmpty()||v<0){toast("请填写正确的分类和金额");return;}
            db.saveCategoryBudget(cycleKey(cycle),name.getText().toString(),v); showBudget();
        }).show();
    }

    private void showRecords() {
        LinearLayout body = pageBody();
        body.addView(backTopBar("账单列表", this::showHome));
        final int[] selected = {0};
        LinearLayout listHost = new LinearLayout(this); listHost.setOrientation(LinearLayout.VERTICAL);
        body.addView(recordFilters(selected, listHost));
        body.addView(listHost);
        loadRecords(listHost, selected[0]);
        mount(body);
    }

    private View recordFilters(int[] selected, LinearLayout host) {
        String[] labels={"全部","支出","收入","负债"}; TextView[] items=new TextView[4];
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Runnable repaint=()->{for(int i=0;i<4;i++){boolean on=selected[0]==i;items[i].setTextColor(on?Color.WHITE:MUTED);items[i].setBackground(rounded(on?BLUE:INPUT_BG,0,14));items[i].setTypeface(Typeface.DEFAULT,on?Typeface.BOLD:Typeface.NORMAL);}};
        for(int i=0;i<4;i++){final int idx=i;TextView t=text(labels[i],11,false,MUTED);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->{selected[0]=idx;repaint.run();loadRecords(host,idx);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(36),1f);lp.setMargins(dp(2),0,dp(2),0);row.addView(t,lp);items[i]=t;} repaint.run();
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,0,0,dp(10));row.setLayoutParams(lp);return row;
    }

    private void loadRecords(LinearLayout host,int filter){
        host.removeAllViews();String type=filter==1?FinanceDb.TYPE_EXPENSE:filter==2?FinanceDb.TYPE_INCOME:filter==3?FinanceDb.TYPE_DEBT:null;
        List<FinanceDb.Record> rows=db.getRecent(type,400); if(rows.isEmpty()){TextView e=text("暂无账单",13,false,MUTED);e.setGravity(Gravity.CENTER);e.setPadding(0,dp(40),0,0);host.addView(e);return;}
        String last=""; for(FinanceDb.Record r:rows){String day=dateFmt.format(new Date(r.occurredAt)); if(!day.equals(last)){Calendar c=Calendar.getInstance();c.setTimeInMillis(r.occurredAt);TextView h=text(dayFmt.format(new Date(r.occurredAt))+"  (周"+weekZh(c)+")",11,true,MUTED);h.setPadding(dp(2),dp(10),0,dp(5));host.addView(h);last=day;} host.addView(recordListRow(r));}
    }

    private View recordListRow(FinanceDb.Record r){
        LinearLayout c=card();c.setPadding(dp(14),dp(11),dp(14),dp(11));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(categoryIcon(r.purpose,38),new LinearLayout.LayoutParams(dp(38),dp(38)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);info.addView(text(r.purpose,13,true,TEXT));info.addView(text(timeFmt.format(new Date(r.occurredAt))+(r.note==null||r.note.isEmpty()?"":"  ·  "+r.note),10,false,MUTED));row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        String sign=FinanceDb.TYPE_INCOME.equals(r.type)?"+":"-";TextView amount=text(sign+"¥"+money.format(r.amount),13,true,typeColor(r.type));row.addView(amount);c.addView(row);
        c.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("删除这笔账？").setMessage(r.purpose+"  ¥"+money.format(r.amount)).setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{db.deleteRecord(r.id);showRecords();}).show();return true;});
        return c;
    }

    private void showStats(){
        LinearLayout body=pageBody();body.addView(backTopBar("数据统计",this::showAnalysis));final int[] period={0};LinearLayout host=new LinearLayout(this);host.setOrientation(LinearLayout.VERTICAL);body.addView(statsPeriodTabs(period,host));body.addView(host);loadStats(host,0);mount(body);
    }

    private View statsPeriodTabs(int[] selected, LinearLayout host) {
        String[] labels = {"本期", "本周", "本月", "自定义"};
        TextView[] items = new TextView[labels.length];
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Runnable repaint = () -> {
            for (int i = 0; i < items.length; i++) {
                boolean on = selected[0] == i;
                items[i].setTextColor(on ? Color.WHITE : MUTED);
                items[i].setBackground(rounded(on ? BLUE : INPUT_BG, 0, 14));
                items[i].setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            }
        };
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView item = text(labels[i], 11, false, MUTED); item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> {
                if (idx == 3) {
                    chooseCustomRange(() -> { selected[0] = 3; repaint.run(); loadStats(host, 3); });
                } else { selected[0] = idx; repaint.run(); loadStats(host, idx); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f); lp.setMargins(dp(2),0,dp(2),0);
            row.addView(item, lp); items[i] = item;
        }
        repaint.run();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,0,dp(10)); row.setLayoutParams(lp);
        return row;
    }

    private void loadStats(LinearLayout host,int period){
        host.removeAllViews();long[] range=rangeFor(period);FinanceDb.Summary s=db.getSummary(range[0],range[1]);long days=Math.max(1,(range[1]-range[0])/86400000L);FinanceDb.Record max=db.getLargestRecord(FinanceDb.TYPE_EXPENSE,range[0],range[1]);
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);r1.addView(statTile("支出","¥"+money.format(s.expense),RED,"本期累计"),weightMargin(4));r1.addView(statTile("收入","¥"+money.format(s.income),GREEN,"本期累计"),weightMargin(4));
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);r2.setPadding(0,dp(8),0,0);r2.addView(statTile("日均支出","¥"+money.format(s.expense/days),ORANGE,"按自然日计算"),weightMargin(4));r2.addView(statTile("最大单笔",max==null?"¥0.00":"¥"+money.format(max.amount),BLUE,max==null?"暂无记录":max.purpose),weightMargin(4));grid.addView(r1);grid.addView(r2);host.addView(grid);
        LinearLayout net=card();net.addView(text("净结余",13,true,TEXT));TextView big=text("¥"+money.format(s.net()),27,true,s.net()>=0?GREEN:RED);big.setPadding(0,dp(6),0,0);net.addView(big);net.addView(text("收入 - 支出 - 负债",11,false,MUTED));host.addView(net,cardParams(10));
    }

    private View statTile(String title,String value,int color,String foot){
        LinearLayout c=card();c.setPadding(dp(14),dp(13),dp(14),dp(13));c.addView(text(title,11,true,MUTED));TextView v=text(value,19,true,color);v.setPadding(0,dp(5),0,dp(4));c.addView(v);c.addView(text(foot,10,false,MUTED));return c;
    }

    private void showRanking(){
        LinearLayout body=pageBody();body.addView(backTopBar("用途排行",this::showAnalysis));final int[] type={0};LinearLayout host=new LinearLayout(this);host.setOrientation(LinearLayout.VERTICAL);
        String[] labels={"支出排行","收入排行","负债排行"};TextView[] items=new TextView[3];LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);Runnable repaint=()->{for(int i=0;i<3;i++){boolean on=type[0]==i;items[i].setTextColor(on?Color.WHITE:MUTED);items[i].setBackground(rounded(on?(i==0?BLUE:i==1?GREEN:ORANGE):INPUT_BG,0,14));}};for(int i=0;i<3;i++){final int idx=i;TextView t=text(labels[i],11,false,MUTED);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->{type[0]=idx;repaint.run();loadRanking(host,idx);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(38),1f);lp.setMargins(dp(2),0,dp(2),0);tabs.addView(t,lp);items[i]=t;}repaint.run();body.addView(tabs);body.addView(host);loadRanking(host,0);mount(body);
    }

    private void loadRanking(LinearLayout host,int typeIdx){
        host.removeAllViews();long[] range=cycleRange(Calendar.getInstance());String type=typeIdx==0?FinanceDb.TYPE_EXPENSE:typeIdx==1?FinanceDb.TYPE_INCOME:FinanceDb.TYPE_DEBT;int color=typeIdx==0?BLUE:typeIdx==1?GREEN:ORANGE;Map<String,Double> map=db.getPurposeTotals(type,range[0],range[1]);if(map.isEmpty()){TextView e=text("本周期暂无数据",13,false,MUTED);e.setGravity(Gravity.CENTER);e.setPadding(0,dp(40),0,0);host.addView(e);return;}double max=0;for(double v:map.values())max=Math.max(max,v);int rank=1;for(Map.Entry<String,Double> e:map.entrySet()){LinearLayout c=card();c.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);TextView no=text(String.valueOf(rank),11,true,rank<=3?Color.WHITE:MUTED);no.setGravity(Gravity.CENTER);no.setBackground(rounded(rank==1?RED:rank==2?ORANGE:rank==3?BLUE:INPUT_BG,0,99));line.addView(no,new LinearLayout.LayoutParams(dp(28),dp(28)));TextView name=text(e.getKey(),13,true,TEXT);name.setPadding(dp(10),0,0,0);line.addView(name,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));line.addView(text("¥"+money.format(e.getValue()),13,true,TEXT));c.addView(line);c.addView(progressLine(max==0?0:e.getValue()/max,color),progressParams());host.addView(c,cardParams(7));rank++;if(rank>20)break;}
    }

    private void showSettings(){
        selectNav(3);LinearLayout body=pageBody();body.addView(simpleTopBar("设置",null));
        body.addView(settingsRow("▣","记账周期","每月 "+cycleDay()+" 日",this::showCycleSettings),cardParams(0));
        body.addView(settingsRow("☷","分类预算","设置各用途消费额度",this::showBudget),cardParams(7));
        body.addView(settingsRow("↥","数据备份","导出 MoneyBoard 备份文件",this::backupData),cardParams(7));
        body.addView(settingsRow("↧","数据恢复","从备份文件恢复",this::restoreData),cardParams(7));
        body.addView(settingsRow("⇩","导出数据","导出 CSV 账单",this::exportCsv),cardParams(7));
        body.addView(settingsRow("◐","主题设置",themeLabel(),this::showThemeDialog),cardParams(7));
        body.addView(settingsRow("ⓘ","关于我们","MoneyBoard v1.3.0",()->new AlertDialog.Builder(this).setTitle("MoneyBoard").setMessage("一个完全离线、本地存储的个人记账 App。\n\n记账周期默认每月 15 日到次月 15 日。\n数据只保存在你的手机。 ").setPositiveButton("知道了",null).show()),cardParams(7));
        mount(body);
    }

    private View settingsRow(String icon,String title,String sub,Runnable action){
        LinearLayout c=card();c.setPadding(dp(14),dp(12),dp(14),dp(12));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView ico=text(icon,17,true,BLUE);ico.setGravity(Gravity.CENTER);ico.setBackground(rounded(BLUE_SOFT,0,13));row.addView(ico,new LinearLayout.LayoutParams(dp(38),dp(38)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(11),0,0,0);info.addView(text(title,13,true,TEXT));info.addView(text(sub,10,false,MUTED));row.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));row.addView(text("›",22,false,MUTED));c.addView(row);c.setOnClickListener(v->action.run());return c;
    }

    private void showCycleSettings(){
        LinearLayout body=pageBody();body.addView(backTopBar("周期设置",this::showSettings));
        LinearLayout c=card();c.addView(text("记账周期起始日",13,true,TEXT));final int[] day={cycleDay()};TextView number=text(day[0]+" 日",27,true,TEXT);number.setGravity(Gravity.CENTER);number.setPadding(0,dp(12),0,dp(12));
        LinearLayout adjust=new LinearLayout(this);adjust.setOrientation(LinearLayout.HORIZONTAL);TextView minus=outlineButton("−");TextView plus=outlineButton("＋");adjust.addView(minus,new LinearLayout.LayoutParams(dp(48),dp(44)));adjust.addView(number,new LinearLayout.LayoutParams(0,dp(44),1f));adjust.addView(plus,new LinearLayout.LayoutParams(dp(48),dp(44)));minus.setOnClickListener(v->{day[0]=Math.max(1,day[0]-1);number.setText(day[0]+" 日");});plus.setOnClickListener(v->{day[0]=Math.min(28,day[0]+1);number.setText(day[0]+" 日");});c.addView(adjust);
        TextView hint=text("每月 "+day[0]+" 日 00:00 开始新的记账周期。默认是 15 日。",11,false,MUTED);hint.setPadding(0,dp(12),0,0);c.addView(hint);body.addView(c,cardParams(0));
        LinearLayout current=rowCard(BLUE_SOFT);current.addView(iconBubble("ⓘ",BLUE,dark?Color.rgb(34,65,95):Color.WHITE),new LinearLayout.LayoutParams(dp(38),dp(38)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);info.addView(text("当前周期",11,true,BLUE));info.addView(text(displayCycle(Calendar.getInstance()),13,true,TEXT));current.addView(info);body.addView(current,cardParams(10));
        TextView save=filledButton("保存周期设置",BLUE);save.setOnClickListener(v->{prefs.edit().putInt("cycle_start_day",day[0]).apply();toast("周期起始日已保存");showCycleSettings();});body.addView(save,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));mount(body);
    }

    private void backupData(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"MoneyBoard_backup_"+dateFmt.format(new Date())+".json");startActivityForResult(i,REQ_BACKUP);
    }
    private void restoreData(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");startActivityForResult(i,REQ_RESTORE);}
    private void exportCsv(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"MoneyBoard_账单_"+dateFmt.format(new Date())+".csv");startActivityForResult(i,REQ_CSV);}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{if(requestCode==REQ_BACKUP){writeUri(uri,db.exportJson());toast("备份完成");}else if(requestCode==REQ_CSV){writeUri(uri,"\uFEFF"+db.exportCsv());toast("CSV 已导出");}else if(requestCode==REQ_RESTORE){String json=readUri(uri);new AlertDialog.Builder(this).setTitle("恢复备份？").setMessage("恢复会覆盖当前账目、预算和分类预算。").setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{try{db.importJson(json);toast("恢复完成");showHome();}catch(Exception e){toast("恢复失败："+e.getMessage());}}).show();}}catch(Exception e){toast("操作失败："+e.getMessage());}}
    private void writeUri(Uri uri,String content)throws Exception{try(OutputStream os=getContentResolver().openOutputStream(uri)){if(os==null)throw new Exception("无法打开文件");os.write(content.getBytes(StandardCharsets.UTF_8));}}
    private String readUri(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new Exception("无法打开文件");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString("UTF-8");}}

    private void showThemeDialog(){
        String[] labels={"浅色","深色","跟随系统"};String[] values={"light","dark","system"};String current=prefs.getString("theme","light");int checked=0;for(int i=0;i<values.length;i++)if(values[i].equals(current))checked=i;final int[] pick={checked};new AlertDialog.Builder(this).setTitle("主题设置").setSingleChoiceItems(labels,checked,(d,w)->pick[0]=w).setNegativeButton("取消",null).setPositiveButton("应用",(d,w)->{prefs.edit().putString("theme",values[pick[0]]).apply();applyPalette();applySystemBars();setContentView(buildRoot());showSettings();}).show();
    }

    private View sectionLabel(String title,String action){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=text(title,14,true,TEXT);row.addView(t,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));if(action!=null){TextView a=text(action,11,true,BLUE);row.addView(a);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(dp(2),dp(9),dp(2),dp(7));row.setLayoutParams(lp);return row;}

    private View simpleTopBar(String title,Runnable trailing){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView left=text(title,20,true,TEXT);row.addView(left,new LinearLayout.LayoutParams(0,dp(42),1f));if(trailing!=null){TextView b=text("⚙",20,false,TEXT);b.setGravity(Gravity.CENTER);b.setOnClickListener(v->trailing.run());row.addView(b,new LinearLayout.LayoutParams(dp(42),dp(42)));}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,0,0,dp(12));row.setLayoutParams(lp);return row;}
    private View backTopBar(String title,Runnable back){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView b=text("‹",29,false,TEXT);b.setGravity(Gravity.CENTER_VERTICAL);b.setOnClickListener(v->back.run());row.addView(b,new LinearLayout.LayoutParams(dp(36),dp(44)));TextView t=text(title,18,true,TEXT);t.setGravity(Gravity.CENTER);row.addView(t,new LinearLayout.LayoutParams(0,dp(44),1f));row.addView(new View(this),new LinearLayout.LayoutParams(dp(36),dp(44)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,0,0,dp(12));row.setLayoutParams(lp);return row;}

    private View settingField(String label,String value){TextView t=text(label+"                                      "+value,12,true,TEXT);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(14),0,dp(14),0);t.setBackground(ripple(INPUT_BG,0,14));return t;}
    private LinearLayout dialogForm(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(8),dp(22),0);return box;}

    private long[] rangeFor(int period){Calendar now=Calendar.getInstance();if(period==0)return cycleRange(now);if(period==1)return weekRange(now);if(period==2)return monthRange(now);if(customStart!=null&&customEnd!=null)return new long[]{customStart.getTimeInMillis(),customEnd.getTimeInMillis()};return cycleRange(now);}
    private String periodName(int p){return p==0?"本期":p==1?"本周":p==2?"本月":"自定义";}

    private List<Double> dailySeries(long startMs,long endMs){List<Double> out=new ArrayList<>();Calendar c=Calendar.getInstance();c.setTimeInMillis(startMs);while(c.getTimeInMillis()<endMs&&out.size()<62){Calendar n=(Calendar)c.clone();n.add(Calendar.DAY_OF_MONTH,1);out.add(db.getSummary(c.getTimeInMillis(),Math.min(n.getTimeInMillis(),endMs)).expense);c=n;}return out;}

    private long[] cycleRange(Calendar source){Calendar s=cycleStart(source);Calendar e=(Calendar)s.clone();e.add(Calendar.MONTH,1);return new long[]{s.getTimeInMillis(),e.getTimeInMillis()};}
    private Calendar cycleStart(Calendar source){Calendar s=(Calendar)source.clone();zeroTime(s);int day=cycleDay();if(s.get(Calendar.DAY_OF_MONTH)<day)s.add(Calendar.MONTH,-1);int max=s.getActualMaximum(Calendar.DAY_OF_MONTH);s.set(Calendar.DAY_OF_MONTH,Math.min(day,max));zeroTime(s);return s;}
    private long[] weekRange(Calendar source){Calendar s=(Calendar)source.clone();int dow=s.get(Calendar.DAY_OF_WEEK);int delta=dow==Calendar.SUNDAY?-6:Calendar.MONDAY-dow;s.add(Calendar.DAY_OF_MONTH,delta);zeroTime(s);Calendar e=(Calendar)s.clone();e.add(Calendar.DAY_OF_MONTH,7);return new long[]{s.getTimeInMillis(),e.getTimeInMillis()};}
    private long[] monthRange(Calendar source){Calendar s=(Calendar)source.clone();s.set(Calendar.DAY_OF_MONTH,1);zeroTime(s);Calendar e=(Calendar)s.clone();e.add(Calendar.MONTH,1);return new long[]{s.getTimeInMillis(),e.getTimeInMillis()};}
    private int cycleDay(){return prefs==null?15:Math.max(1,Math.min(28,prefs.getInt("cycle_start_day",15)));}
    private String cycleKey(Calendar source){Calendar s=cycleStart(source);if(cycleDay()==15)return String.format(Locale.US,"%04d-%02d",s.get(Calendar.YEAR),s.get(Calendar.MONTH)+1);return String.format(Locale.US,"%04d-%02d-%02d",s.get(Calendar.YEAR),s.get(Calendar.MONTH)+1,s.get(Calendar.DAY_OF_MONTH));}
    private String displayCycle(Calendar source){Calendar s=cycleStart(source);Calendar e=(Calendar)s.clone();e.add(Calendar.MONTH,1);return String.format(Locale.CHINA,"%d月%d日 - %d月%d日",s.get(Calendar.MONTH)+1,s.get(Calendar.DAY_OF_MONTH),e.get(Calendar.MONTH)+1,e.get(Calendar.DAY_OF_MONTH));}
    private int daysRemaining(Calendar source){long[] r=cycleRange(source);long diff=r[1]-source.getTimeInMillis();return Math.max(0,(int)Math.ceil(diff/86400000.0));}
    private void zeroTime(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}

    private int[] chartColors(){return new int[]{Color.rgb(255,79,66),PURPLE,BLUE,ORANGE,GREEN,Color.rgb(111,133,185)};}
    private int typeColor(String type){if(FinanceDb.TYPE_EXPENSE.equals(type))return RED;if(FinanceDb.TYPE_INCOME.equals(type))return GREEN;return ORANGE;}
    private int typeSoft(String type){if(FinanceDb.TYPE_EXPENSE.equals(type))return RED_SOFT;if(FinanceDb.TYPE_INCOME.equals(type))return GREEN_SOFT;return ORANGE_SOFT;}
    private String shortMoney(double v){double a=Math.abs(v);if(a>=10000)return (v<0?"-":"")+"¥"+new DecimalFormat("0.0").format(a/10000)+"w";return (v<0?"-":"")+"¥"+new DecimalFormat("#,##0").format(a);}
    private String stripZero(double d){return Math.rint(d)==d?String.valueOf((long)d):String.valueOf(d);}
    private double parseNonNegative(String s){String x=s.trim();if(x.isEmpty())return 0;try{double v=Double.parseDouble(x);return v>=0?v:-1;}catch(Exception e){return -1;}}
    private String weekZh(Calendar c){String[] names={"日","一","二","三","四","五","六"};return names[c.get(Calendar.DAY_OF_WEEK)-1];}
    private String themeLabel(){String v=prefs.getString("theme","light");return "dark".equals(v)?"深色":"system".equals(v)?"跟随系统":"浅色";}

    private TextView categoryIcon(String purpose,int size){String icon="•";int color=BLUE,soft=BLUE_SOFT;String p=purpose==null?"":purpose;if(p.contains("餐")||p.contains("吃")||p.contains("咖啡")){icon="🍴";color=RED;soft=RED_SOFT;}else if(p.contains("购")||p.contains("买")){icon="▣";color=PURPLE;soft=PURPLE_SOFT;}else if(p.contains("交通")||p.contains("地铁")||p.contains("车")){icon="▰";color=BLUE;soft=BLUE_SOFT;}else if(p.contains("日用")||p.contains("房")){icon="⌂";color=GREEN;soft=GREEN_SOFT;}else if(p.contains("医")){icon="+";color=GREEN;soft=GREEN_SOFT;}else if(p.contains("娱乐")){icon="♬";color=ORANGE;soft=ORANGE_SOFT;}TextView t=text(icon,size<=34?13:15,true,color);t.setGravity(Gravity.CENTER);t.setBackground(rounded(soft,0,99));return t;}
    private TextView iconBubble(String icon,int color,int fill){TextView t=text(icon,15,true,color);t.setGravity(Gravity.CENTER);t.setBackground(rounded(fill,0,12));return t;}

    private LinearLayout rowCard(int fill){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(rounded(fill,0,17));return c;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(rounded(CARD,LINE,18));c.setElevation(dark?0:dp(1));return c;}
    private LinearLayout pageBody(){LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(22));return body;}
    private void mount(LinearLayout body){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.addView(body,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));pageHost.removeAllViews();pageHost.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));}

    private TextView text(String s,int sp,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    private EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(13);e.setSingleLine(true);e.setPadding(dp(14),0,dp(14),0);e.setBackground(ripple(INPUT_BG,0,14));return e;}
    private TextView filledButton(String label,int color){TextView t=text(label,14,true,Color.WHITE);t.setGravity(Gravity.CENTER);t.setBackground(ripple(color,0,16));return t;}
    private TextView outlineButton(String label){TextView t=text(label,14,true,TEXT);t.setGravity(Gravity.CENTER);t.setBackground(ripple(CARD,LINE,13));return t;}

    private View progressLine(double rate,int color){double safe=Math.max(0,Math.min(1,rate));LinearLayout track=new LinearLayout(this);track.setOrientation(LinearLayout.HORIZONTAL);track.setBackground(rounded(dark?Color.rgb(49,60,72):Color.rgb(234,237,242),0,99));if(safe>0){View fill=new View(this);fill.setBackground(rounded(color,0,99));track.addView(fill,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,(float)safe));}if(safe<1){View rest=new View(this);track.addView(rest,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,(float)(1-safe)));}return track;}
    private LinearLayout.LayoutParams progressParams(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(7));lp.setMargins(0,dp(9),0,0);return lp;}
    private LinearLayout.LayoutParams fieldParams(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));lp.setMargins(0,dp(10),0,0);return lp;}
    private LinearLayout.LayoutParams cardParams(int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(top),0,0);return lp;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);}
    private LinearLayout.LayoutParams weightMargin(int m){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);lp.setMargins(dp(m),0,dp(m),0);return lp;}

    private Drawable ripple(int fill,int stroke,int radius){GradientDrawable content=rounded(fill,stroke,radius);GradientDrawable mask=rounded(Color.WHITE,0,radius);return new RippleDrawable(ColorStateList.valueOf(Color.argb(25,0,0,0)),content,mask);}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradientRounded(int start,int end,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(dp(radius));return d;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void selectNav(int i){selectedNav=i;updateNav();}

    private class RingProgressView extends View {
        private final double rate; private final int active,track,textColor; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        RingProgressView(double rate,int active,int track,int textColor){super(MainActivity.this);this.rate=Math.max(0,rate);this.active=active;this.track=track;this.textColor=textColor;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float stroke=dp(8),pad=stroke/2f+dp(4);RectF oval=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(track);canvas.drawArc(oval,-90,360,false,p);p.setColor(active);canvas.drawArc(oval,-90,(float)(360*Math.min(1,rate)),false,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(13));p.setColor(textColor);canvas.drawText(new DecimalFormat("0%").format(Math.min(1,rate)),getWidth()/2f,getHeight()/2f+dp(5),p);}
    }

    private class DonutChartView extends View {
        private final List<Double> values; private final double total; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        DonutChartView(List<Double> values,double total){super(MainActivity.this);this.values=values;this.total=total;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float stroke=dp(20),pad=stroke/2f+dp(6);RectF oval=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.BUTT);double sum=0;for(double v:values)sum+=v;if(sum<=0){p.setColor(LINE);canvas.drawArc(oval,-90,360,false,p);}else{float start=-90;int[] colors=chartColors();for(int i=0;i<values.size();i++){float sweep=(float)(360*values.get(i)/sum);p.setColor(colors[i%colors.length]);canvas.drawArc(oval,start,sweep,false,p);start+=sweep;}}p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(TEXT);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(13));canvas.drawText("¥"+new DecimalFormat("#,##0").format(total),getWidth()/2f,getHeight()/2f,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(9));p.setColor(MUTED);canvas.drawText("本期支出",getWidth()/2f,getHeight()/2f+dp(14),p);}
    }

    private class BarChartView extends View {
        private final List<Double> values; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        BarChartView(List<Double> values){super(MainActivity.this);this.values=values;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float left=dp(12),right=getWidth()-dp(8),top=dp(16),bottom=getHeight()-dp(26);p.setStrokeWidth(dp(1));p.setColor(LINE);for(int i=0;i<4;i++){float y=top+(bottom-top)*i/3f;canvas.drawLine(left,y,right,y,p);}double max=0;for(double v:values)max=Math.max(max,v);if(max<=0){p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(11));p.setColor(MUTED);canvas.drawText("暂无支出数据",getWidth()/2f,getHeight()/2f,p);return;}float width=(right-left)/Math.max(1,values.size());float barW=Math.max(dp(3),width*.48f);for(int i=0;i<values.size();i++){float x=left+width*i+width/2f;float h=(float)((values.get(i)/max)*(bottom-top));RectF r=new RectF(x-barW/2,bottom-h,x+barW/2,bottom);p.setColor(BLUE);canvas.drawRoundRect(r,barW/2,barW/2,p);}p.setTextSize(dp(9));p.setColor(MUTED);p.setTextAlign(Paint.Align.LEFT);canvas.drawText("开始",left,getHeight()-dp(5),p);p.setTextAlign(Paint.Align.RIGHT);canvas.drawText("现在",right,getHeight()-dp(5),p);}
    }
}
