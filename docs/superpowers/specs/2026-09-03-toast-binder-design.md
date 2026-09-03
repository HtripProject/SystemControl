# Toast Binder 功能设计

## 目标

在 SystemControl 工程中实现一个可扩展的 Android Binder/AIDL 示例，以 Toast 为首个能力，完整演示客户端 SDK、服务端系统 App 和公共 AIDL 协议之间的调用关系。

## 模块结构

```text
SystemControl/
├── control-api/       # AIDL、接口常量、公共数据结构
├── control-client/    # 客户端 SDK
├── control-server/    # 服务端业务实现
├── service/           # 服务端系统 App
└── client-sample/     # 客户端示例 App
```

依赖方向固定为：

```text
control-api
    ↑
    ├── control-client ← client-sample
    └── control-server ← service
```

客户端和服务端不直接互相依赖，AIDL 是双方唯一的 Binder 协议边界。

## Binder 接口

公共 AIDL 包名为 `com.htrip.systemcontrol.api`，首个方法为：

```aidl
boolean showToast(String message);
```

服务组件为 `com.htrip.systemcontrol.service.PlatformService`。本示例暂不配置绑定权限，普通签名客户端通过显式 ComponentName 绑定；后续接入敏感能力时必须补充调用方授权机制。

## 调用流程

```text
client-sample
  → control-client/ControlClient
  → PlatformClient.bindService()
  → IPlatformApi.Proxy.showToast()
  → Binder.transact()
  → service/PlatformService.onBind()
  → control-server/PlatformApiBinder.showToast()
  → ToastProvider.show()
  → Android Toast
```

Toast 的创建和显示只发生在服务端进程；客户端只负责绑定、发起 AIDL 调用和展示连接状态。

## 错误处理

- 空消息或纯空白消息由服务端返回 `false`，不显示 Toast。
- 服务未安装、绑定失败或 Binder 断开时，客户端返回失败并通过回调通知示例页面。
- `RemoteException` 转换为客户端的失败结果，不向示例页面暴露 Binder 细节。
- 服务端通过主线程 Handler 创建 Toast，避免从 Binder 线程直接更新 UI。

## 验证

- `ToastProvider` 使用可注入执行器进行 JVM 单元测试，覆盖有效消息转发和空消息拒绝。
- 使用 `assembleDebug` 验证两个 App 及三个 Library 模块可编译。
- 使用 `lint` 和 `test` 验证工程质量。

## 非目标

- 本次不实现静默安装、截屏、热点等其他能力。
- 本次不添加系统签名、keystore 或厂商私有权限。
- `service` 可以在普通调试设备安装运行；部署为系统预置服务时仍需平台签名和系统镜像配置。
