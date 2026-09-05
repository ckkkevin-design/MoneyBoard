# MoneyBoard v1.3.1 高还原修复版

这是按用户确认的 MoneyBoard UI 示意图重构的 Android 原生记账 App。完全离线，本地 SQLite 存储，不申请联网权限。

## v1.3.1 修复说明

- 修复 GitHub Actions 编译错误：`settingField()` 返回类型由 `View` 改为 `TextView`，解决 `View cannot be converted to TextView`。
- 其余 v1.3 高还原 UI、15号记账周期、预算与统计逻辑保持不变。

## v1.3 主要升级

- 首页改成参考图的 MoneyBoard 成品风格：本期剩余、绿色环形预算、当前周期、今日记录、快捷操作、四项收支指标。
- 记一笔页：支出 / 收入 / 负债分段切换，大金额输入，8 个快捷用途 + 任意自定义用途，日期和备注。
- 图表分析：本期 / 本周 / 本月 / 自定义日期范围，支出构成环图，每日支出柱状图。
- 预算管理：总预算、净结余目标、分类预算、分类预算进度。
- 账单列表：全部 / 支出 / 收入 / 负债筛选，按日期分组，长按账单删除。
- 数据统计：支出、收入、日均支出、最大单笔、净结余。
- 用途排行：支出 / 收入 / 负债排行。
- 周期设置：默认每月 15 日 00:00 到次月 15 日 00:00，也可在设置里改 1~28 日。
- 浅色 / 深色 / 跟随系统主题。
- JSON 数据备份 / 恢复。
- CSV 账单导出。
- App 图标更新。
- v1.3 起内置固定个人调试签名，之后 GitHub Actions 每次重新构建都可以直接覆盖安装 v1.3+，不会因为云端 Runner 换签名而冲突。

## 计算规则

- 净结余 = 收入 - 支出 - 负债
- 总预算只统计支出
- 默认记账周期 = 每月 15 日 00:00（含）到次月 15 日 00:00（不含）
- 预算与净结余目标按记账周期独立保存

## 从 v1.2 更新

v1.3 继续使用同一个 applicationId `com.kevin.moneyboard` 和同一个 SQLite 数据库名。

**签名提醒：** 你之前 v1.0~v1.2 是由 GitHub 临时 debug 签名生成的；如果手机已经装了旧 APK，第一次装 v1.3 有可能提示“签名不一致 / 与现有应用冲突”。这是 GitHub Runner 每次生成不同 debug 签名造成的。若遇到这个提示，需要先卸载旧版再安装 v1.3。**从 v1.3 开始已经改成固定签名，以后的 v1.4、v1.5 就可以直接覆盖升级。**

如果你的 GitHub 仓库已经配置好 Actions：

1. 解压本项目。
2. 将根目录的 `app`、`build.gradle`、`settings.gradle` 覆盖上传到 GitHub 仓库根目录。
3. `.github/workflows/build-apk.yml` 可以继续沿用；本项目也已经自带。
4. Commit 后 GitHub Actions 自动构建。
5. Actions -> 成功的 Build Android APK -> Summary -> Artifacts -> 下载 `MoneyBoard-debug-apk`。
6. 解压得到 `app-debug.apk`，发到安卓手机覆盖安装即可。

> 注意：不要把整个 `MoneyBoardAndroid_v1.3` 文件夹再套一层上传。GitHub 仓库根目录应该直接看到 `app`、`.github`、`build.gradle`、`settings.gradle`。

## 参考界面

`docs/UI_REFERENCE.png` 保存了本次 v1.3 的目标 UI 参考图。
