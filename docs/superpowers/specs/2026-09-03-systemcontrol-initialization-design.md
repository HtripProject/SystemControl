# SystemControl 初始化工程设计

## 目标

创建包名为 `com.htrip.systemcontrol` 的 Android Studio 初始工程。工程必须能作为后续系统权限控制服务的基础，但本次不实现任何 Kotlin、Java 或 AIDL 代码，也不暴露任何权限敏感能力。

## 已确认范围

- 仅创建一个 Android 应用模块：`app`。
- 使用 Kotlin DSL、Version Catalog、Gradle Wrapper 8.6 和 JDK 17。
- 使用 Android Gradle Plugin 8.4.2 与 Kotlin Android Plugin 1.9.24。
- `namespace` 与 `applicationId` 均为 `com.htrip.systemcontrol`。
- `minSdk` 为 17；`compileSdk` 与 `targetSdk` 均为 34。
- 不引入 Hilt、AndroidX、网络、序列化、测试或其他第三方依赖。
- 不初始化 Git 仓库。

## 目录结构

```text
SystemControl/
├── .gitignore
├── README.md
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── androidTest/.gitkeep
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── aidl/com/htrip/systemcontrol/.gitkeep
        │   ├── kotlin/com/htrip/systemcontrol/
        │   │   ├── data/.gitkeep
        │   │   ├── di/.gitkeep
        │   │   ├── domain/.gitkeep
        │   │   ├── presentation/.gitkeep
        │   │   └── service/.gitkeep
        │   └── res/values/strings.xml
        └── test/.gitkeep
```

`.gitkeep` 仅用于保留空目录，不是实现代码。

## 架构与安全边界

当前阶段只预留 `presentation`、`domain`、`data`、`di`、`service` 和 AIDL 源集目录，以便后续按 MVVM + Clean Architecture 实现。工程不会在本次创建任何 Activity、Service、Application、Binder 接口、UseCase、Repository 或 UI。

Manifest 仅包含一个无组件的 `application` 与应用名称资源。本次不声明 Android 权限，不创建导出组件，不配置系统签名或 keystore，也不设置 Binder 调用方访问规则。静默安装、热点控制及客户端策略回调均明确属于后续功能实现范围。

## 验收

创建后在 Windows 环境执行：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lint
.\gradlew.bat test
```

预期三条命令均成功。由于本次没有测试代码，`test` 的成功只表示测试任务可运行，而非存在业务测试。

## 非目标

- 不创建任何 `.kt`、`.java` 或 `.aidl` 文件。
- 不新增模块。
- 不引入任何业务依赖或第三方库。
- 不申请系统权限、不暴露 AIDL Service、不实现系统能力调用。
- 不初始化 Git 仓库、不创建提交。
