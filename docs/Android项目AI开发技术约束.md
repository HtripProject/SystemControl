# Android 项目 AI 开发技术约束（通用版）

> 适用对象：所有 Android 新建/维护项目，以及在其中工作的 AI Agent（Claude Code / Codex 等）。
> 目的：约束 AI 在开发环境、依赖选型、架构设计、模块拆分四方面的行为，保证产出可编译、可维护、风格一致。
> 优先级总则：**稳定性 > 可维护性 > 架构完美**。渐进式重构，避免过度设计。

---

## 1. 开发环境约束

### 1.1 语言与工具链

| 项目       | 约束                                   |
| ---------- | -------------------------------------- |
| 语言       | Kotlin 优先；存量 Java 代码保持现状，禁止无意义 Java→Kotlin 转换 |
| JDK        | 与所用 AGP 版本要求的 JDK 保持一致（AGP 8.x 要求 JDK 17+） |
| 构建       | Gradle 8.6（gradle-8.6-bin），构建脚本一律使用 **Kotlin DSL**（`.gradle.kts`） |
| 依赖管理   | 必须使用 **Version Catalog**（`gradle/libs.versions.toml`）统一管理版本，禁止在模块中硬编码版本号 |
| IDE        | Android Studio（最新稳定版）           |
| CI/CD      | 以项目实际配置为准（Jenkins / GitHub Actions 等） |

### 1.2 SDK 版本

| 项目         | 约束（新项目默认值，可按项目需求调整，但必须显式声明） |
| ------------ | ------------------------------------------------------ |
| minSdk       | API 17 |
| targetSdk    | 优先使用API 34(可视项目而定) |
| compileSdk   | 与 targetSdk 同级                                 |

### 1.3 AI 开发环境行为约束

1. **修改前必须**：阅读模块结构、理解依赖关系、输出修改计划。
2. **修改中必须**：小步提交；不改无关文件；不做大规模格式化；保持行为一致。
3. **禁止**：
   - 未经确认升级任何依赖版本
   - 未经任务要求修改 Gradle 配置（AGP、Kotlin、SDK 版本、仓库源）
   - 一次性重构整个项目
   - 无理由的 XML↔Compose 互转
4. **修改后必须**：
   - 通过提交前验证：`./gradlew assembleDebug`、`./gradlew lint`、`./gradlew test`
   - 若验证失败，说明原因、已尝试的修复、剩余问题
   - 输出修改摘要：修改文件列表、修改原因、架构影响、风险说明、验证方式

---

## 2. 第三方库选用与版本约束

### 2.1 默认技术栈选型

新增能力时优先使用下表中的库，不在表内的需求须先说明候选方案及理由：

| 类别         | 首选                               | 备注                                 |
| ------------ | ---------------------------------- | ------------------------------------ |
| UI | 优选 xml + viewBinding | 可视项目而定选中Jetpack Compose |
| 异步         | Kotlin Coroutines + Flow           | 禁止 RxJava 用于新代码               |
| DI           | Hilt                               | 存量 Koin 项目可继续用 Koin，不混用  |
| 网络         | Retrofit + OkHttp                  | 序列化用 Gson 或 Kotlinx Serialization（全项目统一一种） |
| 本地数据库   | Room                               | 禁止直接 SQLite                      |
| K/V 存储     | DataStore（新代码）/ SharedPreferences（存量兼容） | 新增键值存储一律 DataStore |
| 图片加载     | Coil（Compose 优先）/ Glide（XML 优先） | 全项目统一一种              |
| JSON         | Gson / Kotlinx Serialization       | 二选一，全项目统一                   |
| 测试         | JUnit（4 或 5，全项目统一）+ MockK |                                     |

### 2.2 选型原则

1. **官方优先**：AndroidX / Jetpack 系列优先于第三方实现。
2. **一职一库**：同一职责只允许一个库（如网络、图片、JSON 序列化、DI 各只一个），禁止引入功能重复的库。
3. **稳定版优先**：不使用 alpha/beta 版本，除非官方明确推荐或有强制需求，且需在文档中记录理由。
4. **新增依赖须谨慎**：引入任何新第三方库前，AI 必须说明用途、体积影响、维护状态，并征得确认。
5. **版本升级须谨慎**：禁止顺手升级依赖；升级须单独提交，先查 Release Note 中的 breaking change。
6. 所有版本号集中在 `gradle/libs.versions.toml`，模块 build 文件只引用 catalog 别名。

### 2.3 禁止项

- 引入已停止维护的库
- 为省几行代码引入重量级框架（如引入 Guava 全量包只为一个工具方法）
- 通过 `implementation` 之外的方式（`api`）泄露依赖，除非确实需要对上层暴露

---

## 3. 项目架构约束

### 3.1 总体架构

- 架构模式：**MVVM + Clean Architecture**，Feature 内聚式三层结构。
- 每个 Feature 模块内部分为三层，层间职责与依赖方向固定。

### 3.2 分层与依赖方向

```text
presentation -> domain <- data
```

- `presentation`：Activity / Fragment / Compose Screen / ViewModel / UiState / UiEvent / Adapter
- `domain`：UseCase / Repository 接口 / 业务模型（Business Model）
- `data`：Repository 实现 / Remote & Local DataSource / Mapper / DTO / Entity
- `di`：Hilt Module，只做依赖绑定

**必须遵守：**

- `domain` 层为**纯 Kotlin**，不依赖 Android SDK、Retrofit、Room 等任何框架
- `presentation` 只能通过 UseCase/Repository 接口获取数据，**禁止直接访问 data 层**
- UI 层禁止出现：Retrofit 调用、DAO/SQL、SharedPreferences/DataStore 直读、复杂业务逻辑

### 3.3 数据流规范

- ViewModel：必须使用 `viewModelScope` + `StateFlow` + **immutable UiState**（data class，字段带默认值）
- Repository 模式：接口定义在 domain，实现在 data，通过 DI 绑定
- 网络请求：统一 `suspend` API + 统一 Result 封装 + 统一错误处理
- DTO ≠ 业务模型：data 层 Mapper 负责转换，domain 只见业务模型

### 3.4 Coroutine 规范

必须：

- IO 操作使用 `Dispatchers.IO`（通过注入 Dispatcher 便于测试）
- 数据流用 Flow / suspend 函数

禁止：

- `GlobalScope`
- 生产代码中使用 `runBlocking`

### 3.5 命名规范

```text
LoginActivity / LoginFragment / LoginViewModel / LoginUiState
GetUserUseCase
UserRepository（接口）/ UserRepositoryImpl（实现）
UserDto（网络）/ UserEntity（数据库）/ User（业务模型）
```

### 3.6 反模式（禁止事项）

- God Activity / God Fragment / God ViewModel
- Utils 垃圾桶类（无节制往里塞静态方法）
- BaseXXX 无限继承链
- 静态单例持有 Context
- 硬编码字符串（UI 文案必须进 `strings.xml`）
- UI 层直接操作数据库 / 网络

### 3.7 UI 技术约束

- 项目 UI 技术以项目既有选择为准（XML View 或 Compose），**新增页面遵循所在模块现有风格**
- 禁止无理由的 XML → Compose（或反向）全量迁移
- 两种技术并存时，公共 UI 组件收敛到 `core/ui`

---

## 4. 模块拆分原则

### 4.1 顶层结构

```text
ProjectRoot/
├── app/                  # 壳工程
├── core/                 # 技术基础设施（与业务无关）
│   ├── common/           # 扩展函数、Result 封装、Base、Logger、Dispatcher、错误定义
│   ├── network/          # Retrofit、OkHttp、Interceptor、网络配置
│   ├── database/         # Room Database、DAO、Entity
│   ├── datastore/        # DataStore / SharedPreferences 兼容层
│   └── ui/               # 通用 UI 组件、Dialog、Adapter、Compose 通用组件
├── feature/              # 业务模块，一个业务域一个模块
│   └── home/
│       ├── presentation/
│       ├── domain/
│       ├── data/
│       └── di/
├── gradle/libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

### 4.2 模块分类与职责

| 模块类型 | 职责                                   | 禁止                                     |
| -------- | -------------------------------------- | ---------------------------------------- |
| `app`    | Application、MainActivity、NavHost、App 初始化、DI Root、全局配置 | 任何业务逻辑、Repository 实现、网络请求、数据库操作 |
| `core/*` | 单一技术职责的基础设施                 | 业务逻辑、Feature 相关代码、页面代码      |
| `feature/*` | 一个业务域的完整三层实现             | 跨 Feature 直接依赖（必须经 domain 接口或公共层） |

### 4.3 拆分判断准则

1. **按业务域拆，不按技术层拆**：`feature/home`、`feature/login`，而不是 `feature/ui`、`feature/data`。
2. **core 只放与业务无关的东西**：一段代码若只在某个业务使用，放 feature 内；若两个以上业务共用且不含业务语义，才可下沉 core。
3. **Feature 之间禁止直接依赖**：需要复用时，把共享部分下沉到 `core`，或通过接口 + DI 解耦。
4. **单一职责命名**：模块名 = 业务域名，小写、单词、见名知义。
5. **避免过早拆分**：项目初期可先单模块 + 包结构分层，等出现明确复用/编译速度需求时再拆模块；拆分本身也是"重构"，须走小步流程。

### 4.4 依赖关系图

```text
app ──> feature/* ──> core/*（按需）
禁止：core ──> feature
禁止：feature/A ──> feature/B
禁止：domain（各层内部）反向依赖
```

---

## 5. AI 执行流程模板

每次任务遵循：

```text
需求
-> 分析当前模块结构、依赖关系
-> 输出修改计划（文件清单 + 架构影响）
-> 小步实现（不改无关代码）
-> 单测 / lint / assembleDebug 验证
-> 输出修改摘要（文件列表、原因、架构影响、风险、验证方式）
-> 提交
```

若计划中的修改与本约束冲突（如需升级依赖、引入新库、调整 Gradle 配置、跨层调用），**必须先停下来说明并获得确认，再继续**。
