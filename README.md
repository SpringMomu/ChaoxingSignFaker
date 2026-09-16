# ChaoxingSignFaker

本地隐私清理版本：构建、改动及验证范围见 [PRIVACY_CHANGES.md](./PRIVACY_CHANGES.md)。

> [!CAUTION]
> ChaoxingSignFaker（随地大小签）是一个开源应用，使用AGPLv3.0许可证发布。开源代码本源为让源代码对所有人开发，保持代码的开放性并欢迎任何人参与到项目的开发中来，但**不欢迎**任何形式的修改代码、名称等进行二次分发、换皮和商业化等行为。

> [!IMPORTANT]
> ChaoxingSignFaker（随地大小签）仅作为交流学习使用，通过本项目加深前端设计、接口调用、数据库使用、网络通信安全等方面知识的理解。请勿将此项目用作商业用途。任何人或组织使用项目或项目中的代码进行的任何不正当违规和违法行为与作者无关，作者不承担任何因使用本应用而导致的法律或其他任何责任。

## 使用

> [!NOTE]
> ChaoxingSignFaker（随地大小签）需要Android 8.0+（`minSdk>=26`）版本。  
> 此应用**并不支持**iPhone、iPad以及鸿蒙（仅HarmonyOS NEXT）操作系统，也并没有准备适配的计划，不过可以使用任意安卓手机在代签选项页通过账号密码登录从而为您的账号使用应用的大部分签到功能。

- 在本机运行 `./build-local-release.ps1` 生成签名 Release APK。
- 输出文件：`app/build/outputs/apk/release/app-release.apk`。

## 功能

> [!TIP]
> 目前v1.13.6+版本已支持群聊内发起的签到，并简单的支持了人脸识别签到。  
> v1.14.7+版本支持了新版本（26/05/30学习通更新）的人脸识别签到（[#166](https://github.com/aquamarine5/ChaoxingSignFaker/pull/166) 感谢[@miloce](https://github.com/miloce)提供技术支持）

- 随意指定当前的位置以完成位置签到
- 从图库选择照片以完成拍照签到
- 为自己和其他账号完成二维码签到 **（需要一台设备在现场扫码，理论上可为任意多的用户签到）**
- 使用一台设备登录多账号来签到（通过扫码、链接等方式添加其他账号）
- 完成单击按钮类型的签到
- 完成任意有验证码的签到
- 完成签到之后的发布的签退操作
- 手势签到和验证码签到
- 群聊签到和人脸识别签到

## 开发

- 详见[CONTRIBUTING.md](./CONTRIBUTING.md)

## About Brainspark Project

- Brainspark 项目是 @aquamarine5 日常头脑风暴的一部分, Code Anything Possible。
