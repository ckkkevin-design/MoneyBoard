# Moeny Board v1.5.0

> 一个完全离线、本地存储的 Android 个人记账 App。软件显示名称按产品约定保留为 **Moeny Board**。

当前版本：**1.5.0**  
Android `versionCode`：**9**  
开发者：**陈开开**

## 核心功能

- 三类账目：消费 / 收入 / 负债。
- 用途名称可完全自定义，并提供常用快捷分类。
- 自动计算：净结余 = 收入 - 消费 - 负债。
- 首页看板沿用 v1.2 结构：紫色预算卡、四项汇总、每日消费趋势、消费分类占比、目标和最近记录。
- 点击首页 **紫色预算卡**：直接设置当前周期总预算和净结余目标。
- 点击首页 **每日消费趋势图**：查看本周期消费分类及逐笔明细。
- 周期支持：
  - 按月循环：每月 1~28 日中任意一天作为起始日，默认 15 日。
  - 自定义日期：任意开始日 00:00 → 任意结束日 00:00。
- 分类预算、用途排行、周/月/本期分析。
- 浅色 / 深色 / 跟随系统主题。
- 毛玻璃 / 玻璃拟态卡片背景。
- JSON 备份恢复、CSV 导出。
- 数据完全保存在手机本地，无联网权限。

## v1.5.0 重点修复：总预算“保存了但前端不刷新”

v1.5 不再让首页和预算页各自直接拼接旧的 `month_key`。

现在所有预算读取/写入都使用同一个 **canonical cycle key**：

```text
cycle-YYYYMMDD-YYYYMMDD
```

例如：

```text
cycle-20260815-20260915
```

并且：

1. 保存预算后立即从 SQLite 再读一次做写入校验。
2. 校验通过后才关闭对话框，并立即重绘当前页面。
3. 首页预算卡与预算管理页统一使用同一套读取方法。
4. 自动读取并迁移 v1.4.x 旧预算 key，不丢旧预算。
5. 同时向旧 key 双写，避免意外降级后预算消失。
6. 设置页显示实际安装版本，避免误装旧 APK 却以为新代码没生效。

## 如何确认手机里真的是 v1.5

打开：

**我的 → 关于我们**

应看到：

```text
Moeny Board v1.5.0
版本：1.5.0 (9)
```

如果仍看到 1.4.x，说明下载/安装的是旧的 GitHub Actions Artifact，而不是 v1.5 APK。

## GitHub Actions 构建

仓库包含：

```text
.github/workflows/build-apk.yml
```

v1.5 workflow 会在构建前主动校验：

```text
versionName '1.5.0'
versionCode 9
```

构建成功后 Artifact 名称为：

```text
MoenyBoard-v1.5.0-apk
```

里面包含：

```text
MoenyBoard-v1.5.0.apk
MoenyBoard-v1.5.0.sha256.txt
```

这样可以避免和 v1.4.x 的 `MoneyBoard-debug-apk` 混淆。

## 更新仓库

如果当前 GitHub 仓库已经可以正常自动构建：

1. 上传并覆盖 `app/`。
2. 覆盖根目录 `build.gradle`、`settings.gradle`。
3. 建议同时覆盖 `.github/workflows/build-apk.yml`，以启用带版本号的 Artifact。
4. 将 `README.md`、`PROJECT_CONTEXT.md`、`CHANGELOG.md` 上传到仓库根目录。
5. Commit 后等待最新一次 Actions 运行成功。
6. **只进入这一次最新运行** 下载 `MoenyBoard-v1.5.0-apk`。

## 数据兼容

- applicationId 保持 `com.kevin.moneyboard`。
- 沿用 v1.3 以后固定签名。
- SQLite 数据库名称仍为 `money_board.db`。
- v1.3 / v1.4.x 正常情况下可覆盖安装到 v1.5，原账目不会因升级清空。

更多工程约束和未来迭代说明见 [`PROJECT_CONTEXT.md`](PROJECT_CONTEXT.md)。
