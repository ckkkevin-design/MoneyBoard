# Moeny Board — 项目上下文（供 GitHub / AI / 后续开发读取）

> **此文件是项目需求与工程约束的权威上下文。后续迭代应优先读取本文件，避免功能回退。**

## 1. 项目身份

- 产品显示名：**Moeny Board**（注意：产品当前明确要求使用 `Moeny` 这个拼写，不要擅自改成 Money）。
- Android applicationId：`com.kevin.moneyboard`
- 当前版本：`1.5.0`
- versionCode：`9`
- 开发者署名：**陈开开**
- 技术栈：原生 Java + Android View + SQLite。
- 最低 Android：API 24 / Android 7.0。
- 数据策略：完全离线、本地存储；不要新增联网权限，除非产品明确要求。

## 2. 产品不可回退需求

### 2.1 账目类型

必须保留三类：

1. 消费 `EXPENSE`
2. 收入 `INCOME`
3. 负债 `DEBT`

净结余：

```text
收入 - 消费 - 负债
```

### 2.2 用途

- 用途/分类名称允许用户自由输入。
- 可以提供快捷分类，但不能限制用户只能选固定分类。

### 2.3 首页看板

首页必须维持 v1.2 风格的信息结构：

1. Moeny Board 品牌标题。
2. 当前记账周期。
3. 紫色总预算卡。
4. 收入 / 消费 / 负债 / 结余四项卡片。
5. 每日消费趋势曲线图。
6. 消费分类占比。
7. 净结余目标。
8. 最近记录。

交互规则：

- **紫色预算卡 → 直接弹出总预算/目标设置。**
- **每日消费趋势图 → 消费明细页。**
- 消费明细页先展示“钱花在哪里”（分类汇总），再展示“具体怎么花”（逐笔账目）。
- 点击某个消费分类可继续下钻到该分类的逐笔记录。

不要把预算卡改回“先跳预算页再找按钮”的多层交互。

### 2.4 记账周期

两种模式必须共存：

#### A. 按月循环

- 用户可设置每月 1~28 日为周期开始日。
- 默认：每月 15 日 00:00 → 次月 15 日 00:00。
- 周期结束时间是 exclusive boundary，即结束日 00:00 属于下一周期。

#### B. 自定义日期

- 用户可手动设置任意开始日期和结束日期。
- 必须完整显示日期，避免字号过大被裁切。

所有以下模块必须使用同一个“当前周期”：

- 首页看板
- 总预算
- 分类预算
- 本期分析
- 用途排行
- 消费明细
- 净结余目标

## 3. v1.5 预算一致性设计

### 3.1 问题背景

v1.4.x 出现过“代码看似已修改，但手机前端仍显示未设置预算”的反馈。

其中一个已确认的风险是：用户可能从 GitHub Actions 下载了旧 run 的 Artifact。另一个工程风险是历史版本使用多种预算 key 规则。

### 3.2 Canonical cycle key

从 v1.5 起，总预算/目标/分类预算统一使用精确周期边界生成 key：

```text
cycle-<start yyyyMMdd>-<end yyyyMMdd>
```

示例：

```text
cycle-20260815-20260915
```

代码入口：

- `cycleStorageKey(Calendar)`
- `getPlanForCycle(Calendar)`
- `savePlanForCycle(Calendar, double, double)`
- `getCategoryBudgetsForCycle(Calendar)`
- `saveCategoryBudgetForCycle(...)`

不要在业务页面直接调用 `db.getPlan(legacyCycleKey(...))`。

### 3.3 兼容旧数据

- `legacyCycleKey(Calendar)` 仅用于 v1.4.x 兼容。
- 新版本读取不到 canonical key 时，会读取 legacy key 并迁移。
- 保存时同时写 canonical + legacy，提供降级兼容。

### 3.4 写入校验

保存总预算后必须：

1. SQLite 写入返回成功。
2. 立即重新读取。
3. 比较 budget / target 与输入值。
4. 校验成功后关闭 dialog。
5. 立即重绘当前页面。
6. Toast 显示保存成功金额。

## 4. 版本识别与 Artifact 防混淆

设置页“关于我们”必须从系统安装包信息动态读取实际版本（`PackageManager.getPackageInfo(...)`），不要在界面手写固定版本。

禁止手写固定版本文案，例如 `v1.4.1`，否则升级后 UI 会显示旧版本。

v1.5 GitHub Actions Artifact：

```text
MoenyBoard-v1.5.0-apk
```

APK：

```text
MoenyBoard-v1.5.0.apk
```

用户验收时第一步必须确认手机“关于我们”显示 `1.5.0 (9)`。

## 5. UI 方向

- 首页结构以 v1.2 为基准。
- 风格：浅色、清爽、轻量玻璃拟态 / 毛玻璃感。
- 卡片：半透明浅色层 + 柔和描边 + 圆角 + 轻阴影。
- 紫色预算卡保持视觉主焦点。
- 深色模式必须继续可用。
- 不追求堆砌功能，优先保证信息层级清晰、按钮入口明确。

## 6. 数据库

数据库：`money_board.db`

主要表：

### transactions

- id
- type
- purpose
- amount
- note
- occurred_at

### month_plans

历史表名保留，不要因为名字叫 month 就删除或重建；v1.5 的 key 已经支持自定义周期。

- month_key
- budget
- target

### category_budgets

- cycle_key
- category
- budget

不要无故清库或提高 DB_VERSION 后 drop table。

## 7. 关键源文件

```text
app/src/main/java/com/kevin/moneyboard/MainActivity.java
app/src/main/java/com/kevin/moneyboard/FinanceDb.java
app/src/main/AndroidManifest.xml
app/build.gradle
.github/workflows/build-apk.yml
```

当前工程仍是单 Activity + 程序化 View UI。后续若重构为多 Activity/Fragment/Compose，必须先确保数据兼容及全部现有功能迁移完成。

## 8. 发布前回归清单

每个版本至少检查：

- [ ] `app/build.gradle` 的 versionName/versionCode 已升级。
- [ ] “关于我们”显示系统安装包的实际 versionName/versionCode。
- [ ] 新增一笔消费后：首页、趋势、分类占比同步变化。
- [ ] 紫色预算卡可直接打开预算设置。
- [ ] 输入 3000 保存后：首页预算上限立即变为 ¥3,000.00。
- [ ] 预算管理页立即显示 ¥3,000.00。
- [ ] 杀掉 App 重开后预算仍为 ¥3,000.00。
- [ ] 切换周期后，新周期预算独立。
- [ ] 切回旧周期时旧预算仍存在。
- [ ] 分类预算保存后刷新仍存在。
- [ ] 默认 15 日周期边界正确。
- [ ] 自定义周期边界正确。
- [ ] 趋势图点击进入消费明细，而不是预算页。
- [ ] JSON 备份/恢复仍可用。
- [ ] CSV 导出仍可用。
- [ ] 浅色/深色主题可切换。
- [ ] GitHub Actions 最新 run 构建成功。
- [ ] 下载的是最新 run 的版本化 Artifact，而不是历史 run。

## 9. 不要擅自做的事情

- 不要把产品名自动纠正为 `Money Board`。
- 不要把周期重新固定死为自然月。
- 不要删除负债字段。
- 不要把用途改成只能选固定列表。
- 不要让首页预算卡只跳页面但不能直接设置。
- 不要把每日消费趋势图改成预算入口。
- 不要新增云同步/联网/账号体系，除非产品明确要求。
- 不要为了 UI 重构清除现有 SQLite 数据。

## 10. 当前下一步建议

优先继续完善：

1. 在真机验证 v1.5 预算写入/重启持久化。
2. 若要进一步增强“毛玻璃”，优先做兼容性好的透明/渐变/blur fallback，不牺牲 API 24 支持。
3. 后续可以增加预算预警（80% / 100%）和周期剩余日均可花额度，但应作为可选增强，不改变现有记账主路径。
