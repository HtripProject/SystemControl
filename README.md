# SystemControl

一个基于 Android Binder/AIDL 的系统控制示例工程。当前只实现 Toast，用于演示客户端 SDK 调用服务端系统 App 的完整链路。

## 模块

```text
control-api       公共 AIDL 和服务组件常量
control-client    客户端 SDK
control-server    服务端 Binder 分发和 Provider
service           服务端 App
client-sample     客户端示例 App
```

## 调试运行

1. 构建并安装 `service` 和 `client-sample`，二者不要求使用相同签名。
2. 先安装并启动服务端 App（它主要提供 `PlatformService`，没有界面）。
3. 打开客户端示例，点击“连接服务”，再点击“调用服务端 Toast”。

在 Android 12 及更高版本上，普通后台 App 可能被系统限制显示自定义 Toast；这不影响 Binder 调用和服务端 Provider 执行。将 `service` 作为平台签名的系统服务预置后，应按目标系统的 Toast 策略验证最终显示效果。

调用路径为：

```text
client-sample → control-client → AIDL Proxy → Binder
→ service/PlatformService → PlatformApiBinder
→ ToastProvider → Android Toast
```

当前示例的 Service 为开放绑定，普通签名客户端可以直接调用。`service` 部署到机顶盒系统分区时，还需要平台签名和对应系统权限；后续接入静默安装等敏感能力前，应重新增加调用方授权控制。
