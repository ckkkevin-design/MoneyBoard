# GitHub 上传说明 — Moeny Board v1.6.0

最稳妥的做法：将本覆盖包内容覆盖到仓库根目录。

必须覆盖：

- `app/`
- `build.gradle`
- `settings.gradle`
- `.github/workflows/build-apk.yml`
- `README.md`
- `PROJECT_CONTEXT.md`
- `CHANGELOG.md`
- `VALIDATION_REPORT.md`

提交后 GitHub Actions 会自动编译。
成功 Artifact 必须叫：`MoenyBoard-v1.6.0-apk`。
安装后在“我的 → 关于我们”确认：`1.6.0 (10)`。
