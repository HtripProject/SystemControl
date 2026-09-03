# SystemControl Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a buildable, code-free Android Studio project template for the `com.htrip.systemcontrol` application.

**Architecture:** Use one Android application module and pre-create empty clean-architecture, service, and AIDL source directories only. The manifest intentionally exposes no components and requests no permissions, so this template does not provide any hardware-control capability until a separately approved implementation adds one.

**Tech Stack:** Gradle Wrapper 8.6, Android Gradle Plugin 8.4.2, Kotlin Android Plugin 1.9.24, JDK 17+, Kotlin DSL, Version Catalog, Android API 34.

**Spec:** `docs/superpowers/specs/2026-09-03-systemcontrol-initialization-design.md`

## Global Constraints

- `namespace` and `applicationId` must both be `com.htrip.systemcontrol`.
- Use Gradle 8.6, Android Gradle Plugin 8.4.2, Kotlin Android Plugin 1.9.24, and JDK 17 or higher.
- Set `minSdk = 17`, `compileSdk = 34`, and `targetSdk = 34`.
- Use Kotlin DSL and manage every version in `gradle/libs.versions.toml`.
- Create only the `app` module; do not create `core` or `feature` modules.
- Do not add Hilt, AndroidX, networking, serialization, testing, or other dependencies.
- Do not create `.kt`, `.java`, or `.aidl` files.
- Do not declare Android permissions, exported components, Services, signing configuration, or keystore paths.
- Do not initialize Git or create a commit.

---

### Task 1: Prepare the required local build toolchain and Wrapper

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`

**Interfaces:**
- Consumes: JDK 17+ available to the Gradle process.
- Produces: `gradlew.bat`, the Windows entry point used by all later build commands.

- [ ] **Step 1: Verify that the Gradle process uses JDK 17 or higher**

Run:

```powershell
java -version
```

Expected: the reported major Java version is 17 or higher. The current environment reports Java 11, so select a JDK 17+ in Android Studio or set `JAVA_HOME` only for the current terminal session before continuing. Do not add a machine-specific JDK path to project files.

- [x] **Step 2: Download the Gradle 8.6 distribution to a temporary directory**

Run:

```powershell
$gradleArchive = Join-Path $env:TEMP 'gradle-8.6-bin.zip'
$gradleExtractRoot = Join-Path $env:TEMP 'systemcontrol-gradle-8.6'
Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.6-bin.zip' -OutFile $gradleArchive
Expand-Archive -LiteralPath $gradleArchive -DestinationPath $gradleExtractRoot -Force
```

Expected: `%TEMP%\systemcontrol-gradle-8.6\gradle-8.6\bin\gradle.bat` exists.

- [x] **Step 3: Generate the Wrapper from the downloaded distribution**

Run:

```powershell
& (Join-Path $env:TEMP 'systemcontrol-gradle-8.6\gradle-8.6\bin\gradle.bat') wrapper --gradle-version 8.6 --distribution-type bin
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` are created; `gradle-wrapper.properties` uses the `gradle-8.6-bin.zip` distribution.

- [ ] **Step 4: Verify the generated Wrapper version**

Run:

```powershell
.\gradlew.bat --version
```

Expected: Gradle 8.6 and JDK 17 or higher are reported.

- [ ] **Step 5: Do not commit**

The approved scope explicitly prohibits Git initialization and commits. Leave the generated Wrapper uncommitted.

### Task 2: Add the root and app Gradle configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

**Interfaces:**
- Consumes: the Gradle 8.6 Wrapper from Task 1.
- Produces: a single `:app` Android application module with API 34 compilation and no runtime dependencies.

- [x] **Step 1: Create `settings.gradle.kts` with repositories and the single application module**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SystemControl"
include(":app")
```

- [x] **Step 2: Create the root `build.gradle.kts` and Version Catalog**

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.4.2"
kotlin = "1.9.24"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [x] **Step 3: Create `gradle.properties` without machine-specific paths**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
kotlin.code.style=official
```

- [x] **Step 4: Create the minimal `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.htrip.systemcontrol"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.htrip.systemcontrol"
        minSdk = 17
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}
```

This file must not add dependencies, build features, signing configuration, or permissions.

- [x] **Step 5: Create an empty `app/proguard-rules.pro`**

Create the file with only this comment:

```proguard
# Reserved for future, separately approved shrinking rules.
```

- [ ] **Step 6: Verify Gradle project discovery**

Run:

```powershell
.\gradlew.bat projects
```

Expected: the output lists exactly one subproject, `Project ':app'`.

- [ ] **Step 7: Do not commit**

The approved scope explicitly prohibits Git initialization and commits.

### Task 3: Create the code-free Android template surface and validate it

**Files:**
- Create: `.gitignore`
- Create: `README.md`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/kotlin/com/htrip/systemcontrol/presentation/.gitkeep`
- Create: `app/src/main/kotlin/com/htrip/systemcontrol/domain/.gitkeep`
- Create: `app/src/main/kotlin/com/htrip/systemcontrol/data/.gitkeep`
- Create: `app/src/main/kotlin/com/htrip/systemcontrol/di/.gitkeep`
- Create: `app/src/main/kotlin/com/htrip/systemcontrol/service/.gitkeep`
- Create: `app/src/main/aidl/com/htrip/systemcontrol/.gitkeep`
- Create: `app/src/test/.gitkeep`
- Create: `app/src/androidTest/.gitkeep`

**Interfaces:**
- Consumes: the `:app` module configured in Task 2.
- Produces: an installable Android application package with no components, source implementations, or sensitive capabilities.

- [x] **Step 1: Create a Manifest containing no permissions or components**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="@string/app_name" />
</manifest>
```

- [x] **Step 2: Create the only Android resource**

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SystemControl</string>
</resources>
```

- [x] **Step 3: Create the agreed empty source directories with `.gitkeep` files**

Run:

```powershell
$emptyFiles = @(
    'app/src/main/kotlin/com/htrip/systemcontrol/presentation/.gitkeep',
    'app/src/main/kotlin/com/htrip/systemcontrol/domain/.gitkeep',
    'app/src/main/kotlin/com/htrip/systemcontrol/data/.gitkeep',
    'app/src/main/kotlin/com/htrip/systemcontrol/di/.gitkeep',
    'app/src/main/kotlin/com/htrip/systemcontrol/service/.gitkeep',
    'app/src/main/aidl/com/htrip/systemcontrol/.gitkeep',
    'app/src/test/.gitkeep',
    'app/src/androidTest/.gitkeep'
)
$emptyFiles | ForEach-Object {
    New-Item -ItemType Directory -Force -Path (Split-Path $_ -Parent) | Out-Null
    New-Item -ItemType File -Force -Path $_ | Out-Null
}
```

- [x] **Step 4: Create the project metadata files**

`.gitignore`:

```gitignore
.gradle/
.idea/
local.properties
build/
app/build/
*.iml
```

`README.md`:

```markdown
# SystemControl

Android system-control application template for `com.htrip.systemcontrol`.

This initial project intentionally contains no implementation code, Android permissions, exported components, AIDL interfaces, or signing configuration. Sensitive hardware-control capabilities require separate approval and implementation.
```

- [x] **Step 5: Verify that no implementation sources or sensitive Manifest declarations exist**

Run:

```powershell
$sourceFiles = Get-ChildItem -Recurse -File app -Include *.kt,*.java,*.aidl
if ($sourceFiles) { throw "No Kotlin, Java, or AIDL files are permitted in this template." }
$manifest = Get-Content -Raw -LiteralPath 'app/src/main/AndroidManifest.xml'
if ($manifest -match '<uses-permission|<service|<activity|<receiver|<provider') {
    throw "The initial Manifest must not declare permissions or components."
}
```

Expected: the command returns without output or error.

- [ ] **Step 6: Run the required Gradle verification commands**

Run:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lint
.\gradlew.bat test
```

Expected: all three commands succeed. No tests are expected to execute because this template intentionally contains no test code.

- [ ] **Step 7: Do not commit**

The approved scope explicitly prohibits Git initialization and commits.
