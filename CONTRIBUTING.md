# Contributing Guide

> [!CAUTION]
> ChaoxingSignFaker（随地大小签）是一个开源应用，使用AGPLv3.0许可证发布。开源代码本源为让源代码对所有人开发，保持代码的开放性并欢迎任何人参与到项目的开发中来，但**不欢迎**任何形式的修改代码、名称等进行二次分发、换皮和商业化等行为。

## 本地构建

本版本已移除更新服务和统计 SDK，不再需要配置原更新服务的 GitHub Packages 仓库或访问令牌。

1. 安装 JDK 21 和 Android SDK 37，在 `local.properties` 设置 SDK 路径。
2. Windows 下运行 `./build-local-release.ps1`，首次构建时生成本地 Release 签名。
3. 运行 `./gradlew.bat :app:testDebugUnitTest :app:lintRelease`。
4. 运行 `python tools/verify_privacy.py` 检查最终 APK。

签名保存、保留的联网功能和验证限制见 [PRIVACY_CHANGES.md](./PRIVACY_CHANGES.md)。
