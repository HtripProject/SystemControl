# SystemControl

`SystemControl` 是一个面向 Android 机顶盒/酒店电视系统能力的控制框架。普通客户端 App 通过 Binder/AIDL 调用系统服务 App；系统服务 App 再执行厂商系统 API、Android Framework API 或系统服务提供的能力。

当前项目以 **Toast** 为首个完整示例，已经打通客户端调用、跨进程 Binder、服务端分发与服务端执行的全流程。后续可在同一模式下扩展静默安装、背光、截屏、热点、音量、电源和信号源等能力。

## 架构

```text
普通客户端 App
    ↓
control-client
    ↓
control-api（AIDL）
    ↓ Binder IPC
service App 进程
    ↓
control-server
    ↓
Android Framework / 厂商系统 API / system_server
```

模块依赖方向：

```text
control-api
    ↑
    ├── control-client ──→ client-sample
    └── control-server ──→ service
```

`control-client` 与 `control-server` 不直接依赖。双方只通过 `control-api` 中定义的 AIDL 协议通信。

## 模块职责

| 模块 | 职责 |
| --- | --- |
| `control-api` | AIDL 接口、服务组件常量、后续公共数据结构和能力声明。 |
| `control-client` | 提供客户端 SDK；负责显式绑定 `PlatformService`、将 `IBinder` 转为 AIDL Proxy，并发起远程调用。 |
| `control-server` | 服务端业务实现；包含 `PlatformApiBinder`、Provider、Handler 等具体能力。 |
| `service` | 服务端 App；声明并创建 `PlatformService`，将 `control-server` 中的实现运行在服务端进程。 |
| `client-sample` | 普通客户端示例 App；演示 SDK 集成和调用流程。 |

## 进程与打包边界

客户端与服务端位于不同进程：

```text
client-sample.apk 进程
    └── ControlClient / PlatformClient / IPlatformApi.Proxy

service.apk 进程
    └── PlatformService / PlatformApiBinder / ToastProvider
```

具体执行代码必须被打包进 `service.apk`，才能在服务端进程运行。即使某个执行类的源码由共享模块维护，只要服务端依赖它，服务端执行的始终是 `service.apk` 中的那份代码。

客户端升级不会自动升级服务端逻辑；服务端执行逻辑变更后必须重新构建、安装或预置新的 `service.apk`。

## 当前示例：Toast

公共 AIDL 定义：

```aidl
boolean showToast(String message);
```

完整调用链：

```text
client-sample/MainActivity
  → ControlClient.showToast(message)
  → PlatformClient
  → IPlatformApi.Proxy.showToast(message)
  → Binder.transact(...)

service 进程
  → IPlatformApi.Stub.onTransact(...)
  → PlatformApiBinder.showToast(message)
  → ToastProvider.show(message)
  → Toast.makeText(...).show()
```

其中 `Toast.makeText()` 只在服务端进程执行；客户端不执行实际 Toast 业务。

相关实现：

```text
control-api/.../IPlatformApi.aidl
control-client/.../ControlClient.kt
control-client/.../PlatformClient.kt
service/.../PlatformService.kt
control-server/.../PlatformApiBinder.kt
control-server/.../ToastProvider.kt
```

## 客户端集成方式

客户端只需要依赖 `control-client`：

```kotlin
private val controlClient = ControlClient(this)

controlClient.connect(
    onConnected = {
        controlClient.showToast("Toast 请求已由服务端执行")
    },
    onError = { error ->
        // 服务不存在、Binder 断开等错误
    },
)
```

使用完成后，在合适的生命周期节点释放连接：

```kotlin
controlClient.disconnect()
```

`connect()` 返回 `true` 只表示绑定请求已被系统接受；必须等待 `onConnected` 回调后再发起远程调用。

服务端组件由 `ServiceContract` 统一定义：

```text
包名：com.htrip.systemcontrol.service
服务：com.htrip.systemcontrol.service.PlatformService
Action：com.htrip.systemcontrol.action.BIND_PLATFORM
```

`PlatformClient` 使用显式 `ComponentName` 和 `BIND_AUTO_CREATE` 绑定服务。因此，即使服务端进程尚未启动，首次成功绑定时 Android 也会自动创建服务端进程并执行 `PlatformService.onCreate()` / `onBind()`。

## 构建与运行

环境要求：

- JDK 17+
- Android SDK API 34
- Gradle 8.6
- Android Gradle Plugin 8.4.2

在 Windows PowerShell 中构建：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lint
.\gradlew.bat test
```

安装顺序：

```powershell
adb install -r service\build\outputs\apk\debug\service-debug.apk
adb install -r client-sample\build\outputs\apk\debug\client-sample-debug.apk
```

然后打开 `client-sample`，点击“连接服务”，状态变为“已连接”后再点击“调用服务端 Toast”。服务端没有界面，客户端绑定时会自动拉起它。

主要产物：

```text
service/build/outputs/apk/debug/service-debug.apk
client-sample/build/outputs/apk/debug/client-sample-debug.apk
control-api/build/outputs/aar/control-api-debug.aar
control-client/build/outputs/aar/control-client-debug.aar
control-server/build/outputs/aar/control-server-debug.aar
```

## 服务端部署与安全

当前 Toast 示例为开放绑定：`PlatformService` 已导出，普通签名客户端可以直接绑定。这是为了便于完成普通客户端到系统服务的联调。

后续加入静默安装、恢复出厂、热点控制等敏感能力前，必须补充调用方授权机制。可选方案包括厂商权限、签名权限、白名单校验，或基于受控回调/令牌的授权设计。不能直接沿用当前开放绑定策略暴露敏感接口。

部署到目标机顶盒时，`service` 通常需要作为系统预置 App 使用平台签名或厂商要求的权限；最终能力是否可用仍取决于目标 ROM 的系统权限、SELinux 策略和厂商私有 API。

## AIDL 演进规则

客户端与服务端可以独立升级，但两端 AIDL 必须保持兼容：

- 只在接口末尾追加新方法；不要修改、删除或重排既有方法。
- 不要修改既有方法的参数类型、返回类型或 Parcelable 字段语义。
- 新增能力前建议增加 `getApiVersion()`、`getCapabilities()` 等版本/能力查询接口。
- 客户端调用新能力前，应先确认服务端版本支持该能力。

服务端可同时被多个客户端绑定。不同客户端的调用会进入同一个 `PlatformApiBinder`，因此后续涉及共享状态、安装任务或硬件资源的实现需要考虑线程安全、任务排队和调用方隔离。

## 性能建议

10 秒一次的 AIDL 调用属于低频调用，Binder 本身的开销通常可以忽略。实际耗时通常来自服务端业务，例如截图编码、网络操作或 APK 安装。

- 保持连接，不要每次轮询都重复 `bindService()` / `unbindService()`。
- 不要在客户端主线程执行可能耗时的同步远程调用。
- 大数据不要直接作为 AIDL 参数传输；优先使用文件路径、`ParcelFileDescriptor` 或服务端存储。
- 不需要返回结果的高频通知可以评估 `oneway` AIDL；需要确认结果的调用保持同步接口。

## 已知限制

Android 12 及更高版本可能限制后台普通 App 显示 Toast。当前示例可以验证 Binder 调用和服务端 Provider 的执行，但最终 Toast 是否显示取决于目标系统策略。将 `service` 预置为平台签名系统服务后，应在目标 ROM 上再次验证显示行为。

## 后续扩展方式

以新增“静默安装”为例：

1. 在 `control-api` 的 AIDL 中追加安装接口。
2. 在 `control-server` 中增加 `AppProvider` / `AppHandler` 的安装实现。
3. 由 `PlatformApiBinder` 将 AIDL 方法分发给对应 Provider。
4. 在 `control-client` 中添加公开 SDK 方法。
5. 在 `client-sample` 中增加调用示例和失败处理。
6. 为服务端实现补充单元测试，并在目标设备验证系统权限与实际安装结果。

该模式同样适用于背光、截屏、热点、音量、电源和信号源等能力。
