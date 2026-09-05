# 钱迹看板 MoneyBoard

一个完全离线、本地存储的 Android 记账 App。

## 已实现

- 三类账目：消费 / 收入 / 负债
- 用途/来源名称完全自定义
- 金额自动汇总
- 本周、本月分析
- 消费用途排行
- 收入来源排行
- 负债项目排行
- 每月单独设置“最多能花多少钱”
- 每月单独设置“净结余目标”
- 首页看板：收入、消费、负债、净结余、预算使用率、预算剩余、目标完成率
- 明细筛选与删除
- SQLite 本地保存，无账号、无网络权限

## 计算口径

- 净结余 = 收入 - 消费 - 负债
- 月消费额度只统计“消费”
- 周区间按周一到周日
- 月区间按自然月

## 最省事的 APK 构建方式：GitHub Actions

1. 新建一个 GitHub 仓库。
2. 把本压缩包解压后的全部文件上传到仓库根目录。
3. 打开 GitHub 仓库的 `Actions` 页面。
4. 选择 `Build Android APK`。
5. 点击 `Run workflow`。
6. 构建完成后，在该次运行页面底部下载 `MoneyBoard-debug-apk`。
7. 解压后得到 `app-debug.apk`，传到安卓手机安装即可。

> 第一次安装外部 APK 时，Android 可能要求你允许当前浏览器/文件管理器“安装未知应用”。

## Android Studio 构建

如果你电脑装有 Android Studio，可直接打开项目目录，并使用 Android Studio 配置的 Android SDK / Gradle 进行构建：

`Build > Build App Bundle(s) / APK(s) > Build APK(s)`

## 数据隐私

App 没有声明 INTERNET 权限，账目只保存在手机本机 SQLite 数据库中。
