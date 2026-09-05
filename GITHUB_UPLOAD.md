# GitHub 更新到 Moeny Board v1.5.0

## 最稳的上传方式

将本包内容覆盖到仓库根目录，至少覆盖：

```text
app/
build.gradle
settings.gradle
README.md
PROJECT_CONTEXT.md
CHANGELOG.md
```

建议同时更新：

```text
.github/workflows/build-apk.yml
```

如果网页拖拽 `.github` 不方便：

1. GitHub 仓库打开 `.github/workflows/build-apk.yml`。
2. 点铅笔 Edit。
3. 用本包 `.github/workflows/build-apk.yml` 的内容整体替换。
4. Commit changes。

## 这次不要下载错 APK

提交 v1.5 后，只进入 **最新一次** Actions run。

如果 workflow 已更新，Artifact 必须叫：

```text
MoenyBoard-v1.5.0-apk
```

如果你暂时没更新 workflow，Artifact 仍可能叫旧的 `MoneyBoard-debug-apk`，这时更要确认它来自最新 commit。

安装后一定进入：

```text
我的 -> 关于我们
```

确认：

```text
Moeny Board v1.5.0
版本：1.5.0 (9)
```

看不到这个版本号，就不要继续判断“新功能是否生效”，因为装的不是 v1.5。
