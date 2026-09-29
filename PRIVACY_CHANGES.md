# 隐私清理版本

## 已移除

- 排行榜页面、Supabase 查询和上传、账号排名标识及上报调度。
- 友盟账号信息、手机号、姓名及使用事件上报，以及友盟 SDK。
- Sentry 异常上报、截图和界面层级附件、自动初始化及构建上传插件。
- Stackbricks 更新检查、强制更新、更新提示、下载和安装入口，以及相关构建和运行时依赖。
- GitHub、联系作者、赞助入口及签到后的赞助弹窗。
- 向作者 CDN 请求远程封禁名单的功能。
- ML Kit 扫码依赖，改用 ZXing 在本机识别相机画面和图库图片。
- 本地 ONNX AAR 中的遥测初始化 ContentProvider、Java 遥测上传实现和保留规则；创建模型会话前显式调用 `setTelemetry(false)`。
- 自动读取剪贴板的隐藏设置入口；用户主动点击粘贴按钮的功能仍可用。
- 账号、Cookie、密码、人脸照片的系统云备份及设备迁移备份。
- 不再需要的电话状态、修改 Wi-Fi 状态权限。
- 上游 1.19.1 新增的通过友盟 SDK 读取 OAID 生成本机统一设备码的逻辑。本机账号只使用已保存的设备码，未保存时沿用上游在无法获取 OAID 时的处理方式（使用随机设备码）；代签用户通过分享链接 `dc` 参数绑定设备码的功能保留。

排行榜旧数据的 Protobuf 字段编号及名称已保留为 `reserved`，避免后续新增字段误读旧数据。

## 保留的核心联网功能

超星账号登录、课程及群聊读取、签到提交、验证码、照片及人脸图片上传与下载仍使用原有业务接口。
设备信息中用于超星业务接口认证的逻辑保留。验证码模型推理和二维码图像识别在本机执行。

百度地图、地址搜索和定位 SDK 保留，以支持地图选点及收藏位置；已显式关闭其地图日志、原生日志分析和调试模式。
地图和定位本身仍会与百度服务通信。闭源地图 SDK 内部全部网络行为并未通过动态抓包验证，不能将此版本描述为“完全无第三方联网”。

代签分享改为 `cxsignfaker://import?...`，不再生成携带手机号和加密密码的作者服务器 HTTP 链接。
扫码和粘贴导入兼容旧 HTTP/HTTPS 链接，解析过程在本机完成，不访问链接页面。
接收人仍能获得分享中的账号数据；这是主动代签分享功能的一部分。

源码中的版权、许可证及出处注释保留，不属于应用内跳转入口。

## 构建与签名

要求 JDK 21、Android SDK 37 和项目 Gradle Wrapper。SDK 路径在本机 `local.properties` 中配置。

```powershell
.\build-local-release.ps1
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease --console=plain
python tools/verify_privacy.py
```

APK：`app/build/outputs/apk/release/app-release.apk`。

`build-local-release.ps1` 首次运行会创建本地签名，之后复用 `signing/local-release.jks` 和
`signing/local-release.properties`。两个文件已被 Git 忽略，应一起妥善保存，避免以后无法升级此本地版本。
如果已配置环境变量 `keystorePassword`，构建优先使用原有 `chaoxingsignfaker.jks`。
直接运行 `assembleRelease` 时若两种签名都未配置，则输出 unsigned APK。

新建本地签名与原版签名不同，通常不能覆盖安装原版。原项目百度地图 API Key 的签名鉴权需在实机确认。
上游已在代码中内置群聊解密密钥，构建时不再需要配置 `imEncryptedKey`。

## 验证范围

- 2026-09-15 最终构建通过：`assembleRelease`、`testDebugUnitTest`、`lintRelease`。共 10 项单元测试通过，Lint 为 0 errors / 36 warnings。
- 最终 APK 已通过隐私静态检查、APK v2 签名校验和 16 KiB ZIP 对齐检查。
- 单元测试覆盖：相册二维码、相机亮度数据、四个旋转方向、反色二维码、空白画面，及新旧代签链接和非法链接。
- `verify_privacy.py` 检查源码、ONNX AAR、合并清单和 Release DEX 中已移除的依赖及上传端点，并确认本地模型仍存在。
- 没有连接 Android 设备，不包含安装启动、账号登录、地图鉴权、真实签到和全流程网络抓包验证。

如果以后替换 ONNX AAR，需重新运行 `python tools/remove_onnx_telemetry.py`，再构建和执行静态检查。
