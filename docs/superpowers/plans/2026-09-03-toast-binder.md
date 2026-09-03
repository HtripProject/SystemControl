# Toast Binder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a complete Toast capability across a shared AIDL module, client SDK, server implementation library, service App, and sample client App.

**Architecture:** `control-api` owns the single AIDL contract and service constants. `control-client` binds to the exported `service` App and invokes the generated Binder proxy. `control-server` owns `PlatformApiBinder` and `ToastProvider`; `service` wires them into `PlatformService`; `client-sample` demonstrates connection and invocation.

**Tech Stack:** Kotlin, Kotlin DSL, Gradle 8.6, AGP 8.4.2, Kotlin 1.9.24, Android API 34, platform Android APIs only, JUnit 4 for JVM unit tests.

**Spec:** `docs/superpowers/specs/2026-09-03-toast-binder-design.md`

## Global Constraints

- Keep Gradle 8.6, AGP 8.4.2, Kotlin 1.9.24, minSdk 17, compileSdk 34, and targetSdk 34.
- Use Kotlin DSL and keep dependency versions in `gradle/libs.versions.toml`.
- Keep runtime dependencies limited to Android platform APIs; add only JUnit 4 as a test dependency.
- Use one `IPlatformApi.aidl` source in `control-api`; do not duplicate the contract.
- Keep the dependency direction `control-api ← control-client ← client-sample` and `control-api ← control-server ← service`.
- Do not add system signing files, keystores, private credentials, or unrelated refactors.

---

### Task 1: Restructure the Gradle modules

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Create: `control-api/build.gradle.kts`
- Create: `control-client/build.gradle.kts`
- Create: `control-server/build.gradle.kts`
- Create: `service/build.gradle.kts`
- Create: `client-sample/build.gradle.kts`
- Delete: `app/build.gradle.kts`
- Delete: `app/proguard-rules.pro`
- Delete: `app/src/main/AndroidManifest.xml`
- Delete: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces the five requested modules and their dependency graph.
- Keeps the existing Android toolchain versions unchanged.

- [ ] Update `settings.gradle.kts` to include exactly `control-api`, `control-client`, `control-server`, `service`, and `client-sample`.
- [ ] Add Android library and application plugin aliases to the version catalog and root build file.
- [ ] Configure library modules with namespace, compileSdk, minSdk, and Kotlin/JVM 17 compatibility.
- [ ] Configure both application modules with unique application IDs and the same SDK/toolchain values.
- [ ] Run `gradlew.bat projects` and verify all five modules are discovered.

### Task 2: Define the shared AIDL contract

**Files:**
- Create: `control-api/src/main/aidl/com/htrip/systemcontrol/api/IPlatformApi.aidl`
- Create: `control-api/src/main/kotlin/com/htrip/systemcontrol/api/ServiceContract.kt`

**Interfaces:**
- `IPlatformApi.boolean showToast(String message)`.
- `ServiceContract.SERVICE_PACKAGE`, `SERVICE_CLASS`, and `SERVICE_ACTION`.

- [ ] Write a JVM contract test that references the intended service constants and fails because `ServiceContract` is not yet defined.
- [ ] Run the focused test and confirm the failure is caused by the missing contract.
- [ ] Add the AIDL method and service constants.
- [ ] Run the focused test and confirm it passes.

### Task 3: Implement and test the server-side Toast provider

**Files:**
- Create: `control-server/src/main/kotlin/com/htrip/systemcontrol/server/ToastProvider.kt`
- Create: `control-server/src/main/kotlin/com/htrip/systemcontrol/server/PlatformApiBinder.kt`
- Create: `control-server/src/test/kotlin/com/htrip/systemcontrol/server/ToastProviderTest.kt`
- Modify: `control-server/build.gradle.kts`

**Interfaces:**
- `ToastProvider(context: Context).show(message: String?): Boolean`.
- `PlatformApiBinder(toastProvider: ToastProvider)` overrides `IPlatformApi.Stub.showToast`.

- [ ] Write tests for non-blank message forwarding and null/blank rejection.
- [ ] Run `:control-server:test` and confirm the tests fail because the provider is missing.
- [ ] Implement the provider with an injectable toast executor and main-thread Android Toast dispatch.
- [ ] Implement the Binder stub as a thin delegation layer.
- [ ] Run `:control-server:test` and confirm all provider tests pass.

### Task 4: Implement the service App

**Files:**
- Create: `service/src/main/AndroidManifest.xml`
- Create: `service/src/main/res/values/strings.xml`
- Create: `service/src/main/kotlin/com/htrip/systemcontrol/service/PlatformService.kt`

**Interfaces:**
- Exported `PlatformService` returns a `PlatformApiBinder` from `onBind`.
- The service is exported without a binding permission so ordinary-signed clients can connect.

- [ ] Add the signature permission and exported service declaration.
- [ ] Construct `ToastProvider` and `PlatformApiBinder` in `PlatformService.onCreate`.
- [ ] Return the binder from `onBind` and clear it in `onDestroy`.
- [ ] Do not add a no-op `Application` class; the service has no process-wide initialization yet.
- [ ] Build `:service:assembleDebug` and inspect the merged manifest for the service and permission.

### Task 5: Implement the client SDK and sample App

**Files:**
- Create: `control-client/src/main/kotlin/com/htrip/systemcontrol/client/PlatformClient.kt`
- Create: `control-client/src/main/kotlin/com/htrip/systemcontrol/client/ControlClient.kt`
- Create: `client-sample/src/main/AndroidManifest.xml`
- Create: `client-sample/src/main/res/values/strings.xml`
- Create: `client-sample/src/main/res/layout/activity_main.xml`
- Create: `client-sample/src/main/kotlin/com/htrip/systemcontrol/sample/MainActivity.kt`

**Interfaces:**
- `ControlClient(context: Context)`.
- `connect(onConnected: () -> Unit, onError: (Throwable) -> Unit): Boolean`.
- `showToast(message: String): Boolean`.
- `disconnect()`.

- [ ] Add the client manifest permission and launcher Activity.
- [ ] Implement explicit service binding with `ComponentName` and `BIND_AUTO_CREATE`.
- [ ] Convert the connected `IBinder` with `IPlatformApi.Stub.asInterface`.
- [ ] Catch binding and remote call failures at the SDK boundary.
- [ ] Add a simple XML screen with connection status and a Toast button.
- [ ] Build both applications and verify the sample depends only on `control-client`.

### Task 6: Verify the complete project

**Files:**
- Modify: `README.md`

- [ ] Document the two-App debug flow and the Binder call chain.
- [ ] Run `gradlew.bat assembleDebug`.
- [ ] Run `gradlew.bat lint`.
- [ ] Run `gradlew.bat test`.
- [ ] Report any JDK 17 limitation if the environment cannot provide it.
