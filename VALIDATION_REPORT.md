# Moeny Board v1.6.0 — 静态核验报告

## 已检查

- [x] `versionName` = `1.6.0`
- [x] `versionCode` = `10`
- [x] 首页趋势图不再绑定整卡跳转。
- [x] 明细跳转改为显式“查看明细 ›”按钮。
- [x] 趋势图构造参数包含真实周期 start/end 时间。
- [x] Y 轴金额刻度逻辑存在。
- [x] X 轴最多 5 个日期标签逻辑存在。
- [x] tooltip 含日期、金额、垂直指示线、高亮数据点。
- [x] 长按延迟 = 260ms。
- [x] 长按触发 haptic feedback。
- [x] 长按后水平拖动会更新 nearest index。
- [x] tooltip 松手后 1800ms 自动清除。
- [x] 趋势线 reveal animator = 720ms。
- [x] 页面 mount 动画 = 220ms。
- [x] v1.5 预算 canonical key 代码未移除。
- [x] 数据库文件和表结构未改动。

## 构建说明

当前本地容器没有 Android SDK，因此无法在本地完成 Android 编译。
GitHub Actions workflow 已更新为先校验 v1.6 版本号，再执行 `gradle :app:assembleDebug --stacktrace`。
最终以 GitHub Actions 绿色通过为编译验收。
