# Moeny Board v1.6.0

一个完全离线、本地存储的 Android 个人记账 App。

当前显示名仍按产品要求使用 **Moeny Board**。

## v1.6 重点更新

- 重做首页“每日消费趋势”交互。
- 趋势线增加约 720ms 左→右绘制动画。
- 页面切换增加轻量淡入/上移动画。
- 增加 Y 轴金额刻度与 X 轴日期刻度。
- X 轴最多显示 5 个 `MM/dd` 日期标签。
- Y 轴自动使用易读的 nice scale。
- 短按趋势图：显示该日金额 tooltip，不跳页。
- 长按趋势图：震动反馈 + 数据悬停。
- 长按后左右拖动：连续查看每天日期与金额。
- tooltip 显示“日期 + 金额”，并带垂直辅助线。
- 取消整张趋势图卡点击即跳明细。
- 新增独立 **“查看明细 ›”** 按钮进入消费明细页。

## 版本

- versionName: `1.6.0`
- versionCode: `10`
- applicationId: `com.kevin.moneyboard`
- 最低 Android: 7.0 / API 24
- 开发者署名：陈开开

## GitHub Actions

构建成功后 Artifact 名：

```text
MoenyBoard-v1.6.0-apk
```

其中包含：

```text
MoenyBoard-v1.6.0.apk
MoenyBoard-v1.6.0.sha256.txt
```

## 后续开发

请先阅读 `PROJECT_CONTEXT.md`。它记录了不可回退的产品交互和数据兼容要求。
