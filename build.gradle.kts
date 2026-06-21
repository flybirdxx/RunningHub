plugins {
    id("runninghub.long-term-governance")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
kotlin {
    jvmToolchain(17)
}

/**
 * 校验当前迁移阶段必须保持的模块依赖边界。
 *
 * 该任务故意放在根工程中，便于本地和 CI 使用同一个入口。检查范围只覆盖已经进入
 * L1 收口的硬门禁：已迁移 Feature 不得重新依赖 shared，Domain 不得导入平台或数据层框架，
 * Presentation 不得反向依赖 Data/网络/存储实现，composeApp commonMain 只能依赖 Domain
 * 或 Presentation 入口，Data 实现只能由平台启动层装配；composeApp 对 shared 的遗留使用必须在
 * docs/migration/shared-allowlist.txt 中显式登记。同时禁止 Git 索引中出现构建产物、
 * 敏感凭据或未登记的 shared 新文件。
 */
tasks.register("checkArchitectureBoundaries") {
    group = "verification"
    description = "Checks L1 migration dependency boundaries and shared allowlist."

    doLast {
        val root = rootDir.toPath()
        val violations = mutableListOf<String>()
        val allowlistFile = root.resolve("docs/migration/shared-allowlist.txt").toFile()
        val sharedBaselineFile = root.resolve("docs/migration/shared-baseline.txt").toFile()
        val allowedSharedFiles = allowlistFile
            .takeIf { it.exists() }
            ?.readLines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.toSet()
            ?: emptySet()
        val sharedBaselineFiles = sharedBaselineFile
            .takeIf { it.exists() }
            ?.readLines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.toSet()
            ?: emptySet()

        fun java.nio.file.Path.relativePath(): String =
            root.relativize(this).toString().replace('\\', '/')

        fun java.io.File.kotlinAndGradleFiles(): Sequence<java.io.File> =
            walkTopDown()
                .filter { it.isFile }
                .filter { it.extension == "kt" || it.extension == "kts" }

        fun java.io.File.importLines(): Sequence<Pair<Int, String>> =
            readLines().asSequence()
                .mapIndexed { index, line -> index + 1 to line.trim() }
                .filter { (_, line) -> line.startsWith("import ") }

        fun commonMainKotlinFiles(moduleRoot: java.io.File): Sequence<java.io.File> =
            if (!moduleRoot.exists()) {
                emptySequence()
            } else {
                moduleRoot.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filter { it.toPath().relativePath().contains("/src/commonMain/kotlin/") }
            }

        fun productionKotlinFiles(moduleRoot: java.io.File): Sequence<java.io.File> =
            if (!moduleRoot.exists()) {
                emptySequence()
            } else {
                moduleRoot.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filter { file ->
                        val relative = file.toPath().relativePath()
                        listOf(
                            "/src/commonMain/kotlin/",
                            "/src/androidMain/kotlin/",
                            "/src/iosMain/kotlin/",
                        ).any { it in relative }
                    }
            }

        fun java.io.File.hasSharedDependencyInCommonMain(): Boolean {
            var inCommonMainDependencies = false
            var braceDepth = 0

            // 迁移期允许 Android Application 入口继续装配 sharedModule，但 commonMain 不能再直接依赖 shared；
            // 这里仅扫描 commonMain.dependencies 块，避免把平台启动层的临时依赖误判为通用 UI 层回退。
            readLines().forEach { line ->
                if (!inCommonMainDependencies && "commonMain.dependencies" in line) {
                    inCommonMainDependencies = true
                    braceDepth = line.count { it == '{' } - line.count { it == '}' }
                } else if (inCommonMainDependencies) {
                    braceDepth += line.count { it == '{' } - line.count { it == '}' }
                }

                if (inCommonMainDependencies && ("project(\":shared\")" in line || "projects.shared" in line)) {
                    return true
                }

                if (inCommonMainDependencies && braceDepth <= 0) {
                    inCommonMainDependencies = false
                }
            }

            return false
        }

        fun java.io.File.hasAnyDependencyInCommonMain(snippets: List<String>): Boolean {
            var inCommonMainDependencies = false
            var braceDepth = 0

            // composeApp 是应用壳，commonMain 只能看到可跨平台复用的领域契约和 Presentation 入口。
            // Data 模块涉及平台 HTTP 引擎、存储和启动装配，必须放在 androidMain/iosMain 等平台层。
            readLines().forEach { line ->
                if (!inCommonMainDependencies && "commonMain.dependencies" in line) {
                    inCommonMainDependencies = true
                    braceDepth = line.count { it == '{' } - line.count { it == '}' }
                } else if (inCommonMainDependencies) {
                    braceDepth += line.count { it == '{' } - line.count { it == '}' }
                }

                if (inCommonMainDependencies && snippets.any { it in line }) {
                    return true
                }

                if (inCommonMainDependencies && braceDepth <= 0) {
                    inCommonMainDependencies = false
                }
            }

            return false
        }

        fun addViolation(file: java.io.File, line: Int?, message: String) {
            val location = if (line == null) file.toPath().relativePath() else "${file.toPath().relativePath()}:$line"
            violations += "$location $message"
        }

        // 这里同时覆盖字符串形式和类型安全 project accessor，避免不同 Gradle 写法绕过
        // Domain/Presentation 到 Feature Data 实现层的单向依赖门禁。
        val featureDataDependencySnippets = listOf(
            "project(\":feature:auth:data\")",
            "project(\":feature:community:data\")",
            "project(\":feature:discovery:data\")",
            "project(\":feature:task:data\")",
            "project(\":feature:quickcreate:data\")",
            "projects.feature.auth.data",
            "projects.feature.community.data",
            "projects.feature.discovery.data",
            "projects.feature.task.data",
            "projects.feature.quickcreate.data",
        )
        // import 前缀只匹配实现包，不拦截 Domain 暴露的 repository/model 契约。
        // 这样可以保持 Presentation -> Domain 的合法依赖，同时阻止直接触达 Data 实现。
        val featureDataImportPrefixes = listOf(
            "com.runninghub.feature.auth.data.",
            "com.runninghub.feature.community.data.",
            "com.runninghub.feature.discovery.data.",
            "com.runninghub.feature.task.data.",
            "com.runninghub.feature.quickcreate.data.",
        )

        fun gitTrackedFiles(): List<String> {
            val process = ProcessBuilder("git", "ls-files")
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw GradleException("Unable to inspect tracked files with git ls-files:\n$output")
            }
            return output.lineSequence()
                .map { it.trim().replace('\\', '/') }
                .filter { it.isNotEmpty() }
                .toList()
        }

        val trackedFiles = gitTrackedFiles()

        // L1 封板要求 CI 阻止构建产物和本地配置进入 Git 索引；使用 git ls-files
        // 可以避免本地执行 Gradle 后生成的 build/ 目录影响门禁结果。
        val forbiddenTrackedFilePatterns = listOf(
            Regex("""(^|/)(build|\.gradle|\.kotlin)(/|$)"""),
            Regex("""(^|/)local\.properties$"""),
            Regex(""".*\.(apk|aab|dex|jks|keystore)$""", RegexOption.IGNORE_CASE),
        )
        trackedFiles
            .filter { file -> forbiddenTrackedFilePatterns.any { it.containsMatchIn(file) } }
            .forEach { file -> violations += "$file must not be tracked because it is a build artifact or local secret file." }

        // shared 是迁移期兼容模块，不允许在未更新基线和归属文档的情况下继续增长。
        trackedFiles
            .filter { it.startsWith("shared/") }
            .filter { it !in sharedBaselineFiles }
            .forEach { file -> violations += "$file is a new shared file. Move it to feature/core or update shared migration ownership explicitly." }

        val textFileExtensions = setOf(
            "gradle",
            "kts",
            "kt",
            "java",
            "xml",
            "json",
            "yaml",
            "yml",
            "md",
            "txt",
            "properties",
            "sq",
        )
        val secretPatterns = listOf(
            "OpenAI API key" to Regex("""sk-[A-Za-z0-9_-]{20,}"""),
            "GitHub token" to Regex("""ghp_[A-Za-z0-9_]{20,}"""),
            "Google API key" to Regex("""AIza[0-9A-Za-z_-]{20,}"""),
            "AWS access key" to Regex("""AKIA[0-9A-Z]{16}"""),
            "private key block" to Regex("""-----BEGIN (RSA |OPENSSH |EC |DSA )?PRIVATE KEY-----"""),
            "hardcoded bearer token" to Regex(
                """Authorization:\s*Bearer\s+(?!YOUR_API_KEY|<token>|TOKEN)[A-Za-z0-9._-]{20,}""",
                RegexOption.IGNORE_CASE,
            ),
            "hardcoded api key assignment" to Regex(
                """\b(api[_-]?key|apiKey)\s*=\s*["'][A-Za-z0-9._-]{20,}["']""",
                RegexOption.IGNORE_CASE,
            ),
        )
        trackedFiles
            .asSequence()
            .filterNot { it.startsWith("docs/migration/") }
            .filterNot { it.startsWith(".github/") }
            // skills/ 存放本地协作技能和示例模板，包含文档化的假 token/私钥片段；
            // L1 安全门禁只扫描本应用源码、构建配置和交付文档，避免示例材料造成误报。
            .filterNot { it.startsWith("skills/") }
            .filter { file -> file.substringAfterLast('.', missingDelimiterValue = "") in textFileExtensions }
            .forEach { relative ->
                val file = root.resolve(relative).toFile()
                if (file.exists()) {
                    file.readLines().forEachIndexed { index, line ->
                        secretPatterns
                            .firstOrNull { (_, pattern) -> pattern.containsMatchIn(line) }
                            ?.let { (label, _) -> violations += "$relative:${index + 1} contains possible $label." }
                    }
                }
            }

        val settingsText = root.resolve("settings.gradle.kts").toFile().readText()
        if ("include(\":core:designsystem\")" in settingsText) {
            violations += "settings.gradle.kts still includes removed empty module :core:designsystem."
        }
        val composeBuildFile = root.resolve("composeApp/build.gradle.kts").toFile()
        val composeBuildText = composeBuildFile.readText()
        if ("isMinifyEnabled = true" !in composeBuildText) {
            // Release 包必须经过 R8 收缩；否则生产构建会继续携带未使用代码和更大的逆向分析面。
            addViolation(composeBuildFile, null, "release build must keep R8 minify enabled.")
        }
        if ("isShrinkResources = true" !in composeBuildText) {
            // 资源压缩依赖 R8 结果，关闭后容易把调试和未使用资源一起带入发布包。
            addViolation(composeBuildFile, null, "release build must keep Android resource shrinking enabled.")
        }
        if ("proguard-rules.pro" !in composeBuildText) {
            addViolation(composeBuildFile, null, "release build must include project ProGuard rules.")
        }
        val apiEnvironmentFile = root.resolve("core/network/src/commonMain/kotlin/com/runninghub/core/network/RunningHubApiEnvironment.kt").toFile()
        val apiEnvironmentText = apiEnvironmentFile.readText()
        if ("data class ApiEnvironment(" !in apiEnvironmentText) {
            // 网络环境必须成为可注入模型，避免 Data 层长期绑定单一生产地址。
            addViolation(apiEnvironmentFile, null, "core network must expose ApiEnvironment for platform startup injection.")
        }
        if ("fun configure(environment: ApiEnvironment)" !in apiEnvironmentText) {
            addViolation(apiEnvironmentFile, null, "RunningHubApiEnvironment must remain configurable by platform startup.")
        }
        if ("const val WEB_ORIGIN" in apiEnvironmentText || "const val TOKEN_REFRESH_URL" in apiEnvironmentText) {
            addViolation(apiEnvironmentFile, null, "API environment URLs must not be compile-time constants after injection support.")
        }
        listOf(
            "composeApp/src/androidMain/kotlin/com/runninghub/app/di/AndroidRuntimeModule.kt",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt",
        ).forEach { relative ->
            val runtimeModuleFile = root.resolve(relative).toFile()
            val runtimeModuleText = runtimeModuleFile.readText()
            if ("single<ApiEnvironment>(createdAtStart = true)" !in runtimeModuleText) {
                // 平台启动层负责选择 debug/staging/release 环境；Data 层不得自行决定远端根地址。
                addViolation(runtimeModuleFile, null, "runtime module must bind ApiEnvironment at startup.")
            }
            if ("RunningHubApiEnvironment::configure" !in runtimeModuleText) {
                addViolation(runtimeModuleFile, null, "runtime module must apply ApiEnvironment before network clients are used.")
            }
        }

        val commonMainRoots = listOf("composeApp", "core", "feature", "shared")
            .map { root.resolve(it).toFile() }
        val forbiddenCommonMainImportPrefixes = listOf(
            "android.",
            "java.awt.",
            "platform.Foundation.",
            "platform.UIKit.",
        )
        val forbiddenCommonMainQualifiedTypes = listOf(
            "android.content.Context",
            "android.net.Uri",
            "android.app.Application",
            "platform.Foundation.",
            "platform.UIKit.",
        )
        commonMainRoots
            .asSequence()
            .flatMap { commonMainKotlinFiles(it) }
            .forEach { file ->
                // Gate I 要求 commonMain 只能使用跨平台 API；平台类型必须隔离到 androidMain/iosMain，
                // 否则 Android/iOS 共享编译虽可能局部通过，业务模型和 Presentation 仍会被平台实现绑死。
                file.importLines().forEach { (lineNumber, line) ->
                    val imported = line.removePrefix("import ")
                    if (forbiddenCommonMainImportPrefixes.any { imported.startsWith(it) }) {
                        addViolation(file, lineNumber, "imports platform-only API from commonMain.")
                    }
                }
                file.readLines().forEachIndexed { index, line ->
                    if (forbiddenCommonMainQualifiedTypes.any { it in line }) {
                        addViolation(file, index + 1, "references platform-only type from commonMain.")
                    }
                }
            }

        val productionRoots = listOf("composeApp", "core", "feature", "shared")
            .map { root.resolve(it).toFile() }
        productionRoots
            .asSequence()
            .flatMap { productionKotlinFiles(it) }
            .forEach { file ->
                val text = file.readText()
                // 生产业务流程必须使用结构化协程和显式错误处理；这些模式会绕过生命周期、
                // 阻塞线程或吞掉异常，是 L1 后继续迭代时最容易重新引入的架构债。
                if (Regex("""\brunBlocking\s*\(""").containsMatchIn(text)) {
                    addViolation(file, null, "uses runBlocking in production source.")
                }
                if (Regex("""\bGlobalScope\s*\.""").containsMatchIn(text) || "import kotlinx.coroutines.GlobalScope" in text) {
                    addViolation(file, null, "uses GlobalScope in production source.")
                }
                if (Regex("""catch\s*\([^)]*\)\s*\{\s*(?://[^\r\n]*(?:\r?\n)?\s*)*\}""").containsMatchIn(text)) {
                    addViolation(file, null, "contains an empty catch block in production source.")
                }
                // SessionManager 在生产 DI 中必须带恢复仓库，否则进程重启后会话恢复会退化为未登录，
                // 重新引入登录页或根 App 之外的第二套会话事实来源。
                if (Regex("""\bSessionManager\s*\(\s*\)""").containsMatchIn(text)) {
                    addViolation(file, null, "constructs SessionManager without SessionRestoreRepository in production source.")
                }
                val relative = file.toPath().relativePath()
                if (
                    relative.startsWith("composeApp/src/commonMain/kotlin/") &&
                    relative != "composeApp/src/commonMain/kotlin/com/runninghub/app/App.kt" &&
                    Regex("""\b(MainVoyagerScreen|LoginVoyagerScreen)\s*\(""").containsMatchIn(text)
                ) {
                    addViolation(file, null, "constructs root Voyager screen outside App root navigation.")
                }
                if (
                    relative.startsWith("composeApp/src/commonMain/kotlin/") &&
                    Regex("""\b(CreateVoyagerScreen|CreateScreenModel)\b""").containsMatchIn(text)
                ) {
                    // 旧创作页状态机已经退役；生产源码中重新出现这些类型名，通常表示第二套创作入口被接回。
                    addViolation(file, null, "references retired legacy Create screen or state machine.")
                }
                if (
                    relative == "core/network/src/commonMain/kotlin/com/runninghub/core/network/auth/TokenRefresher.kt" &&
                    (Regex("""\bRegex\s*\(""").containsMatchIn(text) || ".toRegex()" in text)
                ) {
                    // 刷新响应包含敏感 token，必须按 JSON DTO 解码；正则会破坏 escape 语义并容易误采字段。
                    addViolation(file, null, "parses token refresh JSON with regex instead of kotlinx.serialization DTO.")
                }
                if (
                    relative in setOf(
                        "composeApp/src/androidMain/kotlin/com/runninghub/app/di/AndroidRuntimeModule.kt",
                        "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt",
                    ) &&
                    (
                        "single<CredentialStore> { get<PreferencesSettingsStore>() }" in text ||
                            "primary = createSecureCredentialStore()" !in text ||
                            "MigratingCredentialStore(" !in text
                        )
                ) {
                    // 生产组合根必须把敏感凭据写入 Android Keystore / iOS Keychain；Preferences 只允许作为旧数据迁移源。
                    addViolation(file, null, "binds CredentialStore without platform secure storage migration.")
                }
            }

        val featureDir = root.resolve("feature").toFile()
        if (featureDir.exists()) {
            featureDir.kotlinAndGradleFiles().forEach { file ->
                val relative = file.toPath().relativePath()
                val text = file.readText()
                if ("project(\":shared\")" in text || "projects.shared" in text || "com.runninghub.shared" in text) {
                    addViolation(file, null, "must not depend on shared from migrated feature modules.")
                }

                if (
                    relative.contains("/data/") &&
                    (
                        "project(\":composeApp\")" in text ||
                            "projects.composeApp" in text ||
                            "com.runninghub.app." in text
                        )
                ) {
                    addViolation(file, null, "must not depend on composeApp from Feature Data modules.")
                }

                if (relative.contains("/domain/src/commonMain/") && file.extension == "kt") {
                    file.importLines().forEach { (lineNumber, line) ->
                        val forbidden = listOf(
                            "android.",
                            "androidx.compose.",
                            "androidx.datastore.",
                            "app.cash.sqldelight.",
                            "io.ktor.",
                            "platform.Foundation.",
                            "platform.UIKit.",
                        )
                        if (forbidden.any { line.removePrefix("import ").startsWith(it) }) {
                            addViolation(file, lineNumber, "imports platform, UI, network, or storage API in Domain commonMain.")
                        }
                        if (featureDataImportPrefixes.any { line.removePrefix("import ").startsWith(it) }) {
                            addViolation(file, lineNumber, "imports Feature Data implementation in Domain commonMain.")
                        }
                    }
                }

                if (relative.endsWith("/domain/build.gradle.kts")) {
                    if (featureDataDependencySnippets.any { it in text }) {
                        addViolation(file, null, "declares Feature Data implementation dependency from Domain module.")
                    }
                }

                if (relative.contains("/presentation/src/commonMain/") && file.extension == "kt") {
                    file.importLines().forEach { (lineNumber, line) ->
                        val imported = line.removePrefix("import ")
                        val forbidden = listOf(
                            "androidx.datastore.",
                            "app.cash.sqldelight.",
                            "io.ktor.",
                            "com.runninghub.shared.data.",
                        )
                        if (forbidden.any { imported.startsWith(it) }) {
                            addViolation(file, lineNumber, "imports Data, network, or storage implementation in Presentation commonMain.")
                        }
                        if (featureDataImportPrefixes.any { imported.startsWith(it) }) {
                            addViolation(file, lineNumber, "imports Feature Data implementation in Presentation commonMain.")
                        }
                    }
                }

                if (relative.endsWith("/presentation/build.gradle.kts")) {
                    val forbiddenPresentationDependencies = listOf(
                        "project(\":shared\")",
                        "projects.shared",
                        "libs.ktor.",
                        "libs.datastore",
                        "libs.sqldelight",
                    ) + featureDataDependencySnippets
                    if (forbiddenPresentationDependencies.any { it in text }) {
                        addViolation(file, null, "declares forbidden Presentation dependency.")
                    }
                }
            }
        }

        val composeTargets = listOf(
            root.resolve("composeApp/build.gradle.kts").toFile(),
            root.resolve("composeApp/src").toFile(),
        )
        composeTargets
            .asSequence()
            .flatMap { target ->
                when {
                    target.isFile -> sequenceOf(target)
                    target.exists() -> target.kotlinAndGradleFiles()
                    else -> emptySequence()
                }
            }
            .forEach { file ->
                val text = file.readText()
                val relative = file.toPath().relativePath()
                if (relative == "composeApp/build.gradle.kts") {
                    if (file.hasSharedDependencyInCommonMain()) {
                        addViolation(file, null, "declares shared in commonMain.dependencies; shared may only remain in platform startup during migration.")
                    }
                    if (file.hasAnyDependencyInCommonMain(featureDataDependencySnippets)) {
                        addViolation(file, null, "declares Feature Data implementation in commonMain.dependencies; platform startup source sets must assemble Data modules.")
                    }
                    return@forEach
                }

                val usesShared = "project(\":shared\")" in text || "projects.shared" in text || "com.runninghub.shared" in text
                if (usesShared && relative !in allowedSharedFiles) {
                    addViolation(file, null, "uses shared but is not listed in docs/migration/shared-allowlist.txt.")
                }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Architecture boundary check failed:")
                    violations.forEach { appendLine("- $it") }
                }
            )
        }
    }
}

/**
 * 校验 L1 GitHub Actions workflow 与本地 Gradle 门禁保持一致。
 *
 * Gate J 要求 CI 覆盖 Android、iOS、架构边界、测试和构建入口；仅在文档中记录 workflow
 * 不足以防止后续误删或把 CI 命令改回空跑任务。该任务直接读取 `.github/workflows`
 * 中的 YAML 文本，检查当前仓库约定的关键字段，作为本地和 CI 共用的轻量防线。
 */
tasks.register("checkL1CiWorkflows") {
    group = "verification"
    description = "Checks that L1 GitHub Actions workflows invoke the expected Gradle gates."

    doLast {
        val workflowChecks = listOf(
            "Android CI" to rootDir.resolve(".github/workflows/android-ci.yml") to listOf(
                "runs-on: ubuntu-latest",
                "concurrency:",
                "cancel-in-progress: true",
                "schedule:",
                "cron: \"0 18 * * 0\"",
                "java-version: \"17\"",
                "chmod +x gradlew",
                "./gradlew verifyL1Android",
                "Upload Android verification reports",
                "android-verification-reports",
                "**/build/reports/**",
                "**/build/test-results/**",
                "if-no-files-found: ignore",
            ),
            "iOS CI" to rootDir.resolve(".github/workflows/ios-ci.yml") to listOf(
                "runs-on: macos-15",
                "concurrency:",
                "cancel-in-progress: true",
                "schedule:",
                "cron: \"30 18 * * 0\"",
                "java-version: \"17\"",
                "chmod +x gradlew",
                "Validate iOS migration scripts",
                "bash -n docs/migration/collect-ios-macos-evidence.sh",
                "bash -n docs/migration/run-ios-simulator-smoke.sh",
                "bash docs/migration/collect-ios-macos-evidence.sh --self-test",
                "bash docs/migration/run-ios-simulator-smoke.sh --self-test",
                "./gradlew verifyL1Ios",
                "Run iOS Native unit tests",
                ":core:network:iosSimulatorArm64Test",
                ":feature:auth:domain:iosSimulatorArm64Test",
                ":feature:auth:data:iosSimulatorArm64Test",
                ":feature:quickcreate:presentation:iosSimulatorArm64Test",
                "Run Xcode Debug build",
                "xcodebuild",
                "-project iosApp/iosApp.xcodeproj",
                "-scheme RunningHub",
                "-derivedDataPath build/xcode/DerivedData",
                "-resultBundlePath build/xcode/RunningHub.xcresult",
                "CODE_SIGNING_ALLOWED=NO",
                "Run iOS Simulator launch smoke",
                "docs/migration/run-ios-simulator-smoke.sh",
                "--app-path build/xcode/DerivedData/Build/Products/Debug-iphonesimulator/RunningHub.app",
                "--bundle-id com.runninghub.app.ios",
                "--output build/xcode/simulator-launch-smoke.txt",
                "Upload iOS verification reports",
                "ios-verification-reports",
                "build/xcode/**",
                "workflow_dispatch:",
                "simulator_smoke_pass:",
                "smoke_notes:",
                "docs/migration/collect-ios-macos-evidence.sh",
                "actions/upload-artifact@v4",
                "ios-macos-link-and-simulator.md",
            ),
        )
        val dependencySubmissionWorkflow = rootDir.resolve(".github/workflows/dependency-submission.yml")
        val dependencySubmissionRequiredSnippets = listOf(
            "name: Dependency Submission",
            "push:",
            "schedule:",
            "workflow_dispatch:",
            "permissions:",
            "contents: write",
            "concurrency:",
            "cancel-in-progress: true",
            "runs-on: ubuntu-latest",
            "actions/checkout@v6",
            "actions/setup-java@v5",
            "java-version: \"17\"",
            "chmod +x gradlew",
            "gradle/actions/dependency-submission@v6",
            "dependency-graph: generate-and-submit",
        )
        val violations = mutableListOf<String>()
        val trackedWorkflowFiles = ProcessBuilder("git", "ls-files", ".github/workflows")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect tracked workflow files with git ls-files:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val dirtyWorkflowFiles = ProcessBuilder("git", "diff", "--name-only", "--", ".github/workflows")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect unstaged workflow changes with git diff:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val dependabotFile = rootDir.resolve(".github/dependabot.yml")
        val trackedDependabotFiles = ProcessBuilder("git", "ls-files", ".github/dependabot.yml")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect tracked Dependabot file with git ls-files:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val dirtyDependabotFiles = ProcessBuilder("git", "diff", "--name-only", "--", ".github/dependabot.yml")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect unstaged Dependabot changes with git diff:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val prTemplateFile = rootDir.resolve(".github/pull_request_template.md")
        val trackedPrTemplateFiles = ProcessBuilder("git", "ls-files", ".github/pull_request_template.md")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect tracked pull request template with git ls-files:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val dirtyPrTemplateFiles = ProcessBuilder("git", "diff", "--name-only", "--", ".github/pull_request_template.md")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect unstaged pull request template changes with git diff:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }

        workflowChecks.forEach { workflow ->
            val (nameAndFile, expectedSnippets) = workflow
            val (name, file) = nameAndFile
            val relativePath = file.toRelativeString(rootDir).replace('\\', '/')
            if (!file.exists()) {
                violations += "$name workflow is missing at $relativePath."
                return@forEach
            }

            // 远端 GitHub Actions 只会运行已进入 Git 索引并随提交推送的 workflow。
            // 这里显式检查跟踪状态，避免本地存在 workflow 文件但远端完全没有 CI 记录。
            if (relativePath !in trackedWorkflowFiles) {
                violations += "$name workflow exists locally but is not tracked by Git at $relativePath."
            }
            if (relativePath in dirtyWorkflowFiles) {
                violations += "$name workflow has unstaged changes at $relativePath; stage it before using it as Gate J evidence."
            }

            val text = file.readText()

            // workflow 必须在 push 和 PR 都运行，避免只在本地验证通过却没有 PR 状态。
            listOf("pull_request:", "push:").forEach { trigger ->
                if (trigger !in text) {
                    violations += "$name workflow must include trigger `$trigger`."
                }
            }

            expectedSnippets.forEach { snippet ->
                if (snippet !in text) {
                    violations += "$name workflow must contain `$snippet`."
                }
            }

            // iOS link 和 Simulator 证据依赖 GitHub 托管 macOS 镜像的 Xcode/SDK 组合；
            // 使用浮动 latest 标签会让同一提交在不同日期落到不同系统镜像，削弱 Gate J 的可复现性。
            if (name == "iOS CI" && "runs-on: macos-latest" in text) {
                violations += "$name workflow must pin a concrete macOS runner label instead of macos-latest."
            }
        }

        val dependencySubmissionRelativePath = dependencySubmissionWorkflow.toRelativeString(rootDir).replace('\\', '/')
        if (!dependencySubmissionWorkflow.exists()) {
            violations += "Dependency Submission workflow is missing at $dependencySubmissionRelativePath."
        } else {
            if (dependencySubmissionRelativePath !in trackedWorkflowFiles) {
                violations += "Dependency Submission workflow exists locally but is not tracked by Git at $dependencySubmissionRelativePath."
            }
            if (dependencySubmissionRelativePath in dirtyWorkflowFiles) {
                violations += "Dependency Submission workflow has unstaged changes at $dependencySubmissionRelativePath."
            }

            val dependencySubmissionText = dependencySubmissionWorkflow.readText()
            dependencySubmissionRequiredSnippets.forEach { snippet ->
                if (snippet !in dependencySubmissionText) {
                    violations += "Dependency Submission workflow must contain `$snippet`."
                }
            }
            if ("pull_request:" in dependencySubmissionText) {
                // Dependency Submission 需要 contents: write 写入 GitHub Dependency Graph；
                // 不在 PR 事件执行可以避免给 fork/未信任上下文暴露写权限路径。
                violations += "Dependency Submission workflow must not run on pull_request."
            }
        }

        val dependabotRelativePath = dependabotFile.toRelativeString(rootDir).replace('\\', '/')
        if (!dependabotFile.exists()) {
            violations += "Dependabot configuration is missing at $dependabotRelativePath."
        } else {
            // 依赖版本巡检本身也是 CI 治理的一部分；把 Gradle 与 GitHub Actions 更新入口纳入门禁，
            // 避免后续只保留一次性修复而没有持续发现过期依赖的机制。
            if (dependabotRelativePath !in trackedDependabotFiles) {
                violations += "Dependabot configuration exists locally but is not tracked by Git at $dependabotRelativePath."
            }
            if (dependabotRelativePath in dirtyDependabotFiles) {
                violations += "Dependabot configuration has unstaged changes at $dependabotRelativePath."
            }

            val dependabotText = dependabotFile.readText()
            listOf(
                "version: 2",
                "package-ecosystem: \"gradle\"",
                "package-ecosystem: \"github-actions\"",
                "directory: \"/\"",
                "interval: \"weekly\"",
                "timezone: \"UTC\"",
            ).forEach { snippet ->
                if (snippet !in dependabotText) {
                    violations += "Dependabot configuration must contain `$snippet`."
                }
            }
        }

        val prTemplateRelativePath = prTemplateFile.toRelativeString(rootDir).replace('\\', '/')
        if (!prTemplateFile.exists()) {
            violations += "Pull request template is missing at $prTemplateRelativePath."
        } else {
            // 复核文档要求 PR 明确说明目标、验证、风险和回滚；模板进入门禁后，
            // 后续补丁不会绕过这些交付信息直接进入代码审查。
            if (prTemplateRelativePath !in trackedPrTemplateFiles) {
                violations += "Pull request template exists locally but is not tracked by Git at $prTemplateRelativePath."
            }
            if (prTemplateRelativePath in dirtyPrTemplateFiles) {
                violations += "Pull request template has unstaged changes at $prTemplateRelativePath."
            }

            val prTemplateText = prTemplateFile.readText()
            listOf(
                "变更目标",
                "影响模块与平台",
                "架构边界说明",
                "实际执行命令",
                "测试结果",
                "CI 与保护分支",
                "Android CI 通过",
                "iOS CI 通过",
                "checkL1SealEvidence",
                "未验证项与剩余风险",
                "截图或录屏",
                "兼容性、数据迁移与安全影响",
                "依赖升级与重大版本人工回归",
                "本 PR 不包含依赖或构建工具升级",
                "major 级依赖、Gradle、AGP、Kotlin、Xcode 或运行时 SDK 升级",
                "本 PR 不启用自动合并",
                "回滚方案",
            ).forEach { snippet ->
                if (snippet !in prTemplateText) {
                    violations += "Pull request template must contain `$snippet`."
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("L1 CI workflow check failed:")
                    violations.forEach { appendLine("- $it") }
                }
            )
        }
    }
}

/**
 * 校验 L1 迁移辅助脚本仍可作为可追溯验收工具使用。
 *
 * Gate G 的登录态 Tab 网络观察依赖 `observe-tab-network.ps1`。CI 不直接执行 adb 观察，
 * 但应阻止脚本被误删、移除 `-SelfTest` 或重新写入会让 Windows PowerShell 解析失败的中文运行时字符串。
 * 这里使用纯文本检查，避免给 Linux/macOS runner 增加 PowerShell 运行时前提。
 */
tasks.register("checkMigrationScripts") {
    group = "verification"
    description = "Checks migration helper scripts required by L1 evidence collection."

    doLast {
        val tabNetworkScript = rootDir.resolve("docs/migration/observe-tab-network.ps1")
        val githubActionsScript = rootDir.resolve("docs/migration/collect-github-actions-evidence.ps1")
        val iosMacosScript = rootDir.resolve("docs/migration/collect-ios-macos-evidence.sh")
        val iosSimulatorSmokeScript = rootDir.resolve("docs/migration/run-ios-simulator-smoke.sh")
        val iosEvidenceDownloadScript = rootDir.resolve("docs/migration/download-ios-macos-evidence.ps1")
        val iosEvidenceRequestScript = rootDir.resolve("docs/migration/request-ios-macos-evidence.ps1")
        val l1EvidenceFinalizeScript = rootDir.resolve("docs/migration/finalize-l1-external-evidence.ps1")
        val androidNetworkObserver = rootDir.resolve("composeApp/src/androidMain/kotlin/com/runninghub/app/di/AndroidNetworkActivityLogObserver.kt")
        val iosKoinEntry = rootDir.resolve("composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt")
        val iosMainViewController = rootDir.resolve("composeApp/src/iosMain/kotlin/com/runninghub/app/MainViewController.kt")
        val iosXcodeProject = rootDir.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
        val iosSwiftApp = rootDir.resolve("iosApp/iosApp/iOSApp.swift")
        val iosContentView = rootDir.resolve("iosApp/iosApp/ContentView.swift")
        val iosPrivacyManifest = rootDir.resolve("iosApp/iosApp/PrivacyInfo.xcprivacy")
        val violations = mutableListOf<String>()

        if (!tabNetworkScript.exists()) {
            throw GradleException("Migration script is missing: ${tabNetworkScript.toRelativeString(rootDir)}")
        }
        if (!githubActionsScript.exists()) {
            throw GradleException("Migration script is missing: ${githubActionsScript.toRelativeString(rootDir)}")
        }
        if (!iosMacosScript.exists()) {
            throw GradleException("Migration script is missing: ${iosMacosScript.toRelativeString(rootDir)}")
        }
        if (!iosSimulatorSmokeScript.exists()) {
            throw GradleException("Migration script is missing: ${iosSimulatorSmokeScript.toRelativeString(rootDir)}")
        }
        if (!androidNetworkObserver.exists()) {
            throw GradleException("Android network observer is missing: ${androidNetworkObserver.toRelativeString(rootDir)}")
        }
        listOf(iosKoinEntry, iosMainViewController, iosXcodeProject, iosSwiftApp, iosContentView)
            .filterNot { it.exists() }
            .forEach { file -> violations += "iOS wrapper file is missing: ${file.toRelativeString(rootDir)}" }

        val tabNetworkScriptText = tabNetworkScript.readText()
        val githubActionsScriptText = githubActionsScript.readText()
        val iosMacosScriptText = iosMacosScript.readText()
        val iosSimulatorSmokeScriptText = iosSimulatorSmokeScript.readText()
        val androidNetworkObserverText = androidNetworkObserver.readText()
        val iosKoinEntryText = iosKoinEntry.takeIf { it.exists() }?.readText().orEmpty()
        val iosMainViewControllerText = iosMainViewController.takeIf { it.exists() }?.readText().orEmpty()
        val iosXcodeProjectText = iosXcodeProject.takeIf { it.exists() }?.readText().orEmpty()
        val iosSwiftAppText = iosSwiftApp.takeIf { it.exists() }?.readText().orEmpty()
        val iosContentViewText = iosContentView.takeIf { it.exists() }?.readText().orEmpty()
        val rootBuildText = rootProject.buildFile.readText()
        val l1SealEvidenceTaskText = rootBuildText.substringAfter("tasks.register(\"checkL1SealEvidence\")")
        val requiredTrackedFiles = listOf(
            "build.gradle.kts",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/MainViewController.kt",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt",
            "doc/RunningHub-KMP-架构迁移验收标准.md",
            "docs/migration/acceptance.md",
            "docs/migration/collect-github-actions-evidence.ps1",
            "docs/migration/collect-ios-macos-evidence.sh",
            "docs/migration/current-state.yaml",
            "docs/migration/dependency-rules.md",
            "docs/migration/download-ios-macos-evidence.ps1",
            "docs/migration/finalize-l1-external-evidence.ps1",
            "docs/migration/evidence/android-logout-network.json",
            "docs/migration/l1-external-evidence.md",
            "docs/migration/l1-seal-audit.md",
            "docs/migration/observe-tab-network.ps1",
            "docs/migration/request-ios-macos-evidence.ps1",
            "docs/migration/run-ios-simulator-smoke.sh",
            "docs/migration/shared-allowlist.txt",
            "docs/migration/shared-baseline.txt",
            "docs/migration/shared-ownership.md",
            "docs/migration/tab-lifecycle.md",
            "iosApp/README.md",
            "iosApp/iosApp.xcodeproj/project.pbxproj",
            "iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/RunningHub.xcscheme",
            "iosApp/iosApp/ContentView.swift",
            "iosApp/iosApp/Info.plist",
            "iosApp/iosApp/iOSApp.swift",
            "iosApp/iosApp/PrivacyInfo.xcprivacy",
        )
        val trackedMigrationFiles = ProcessBuilder(
            "git",
            "-c",
            "core.quotePath=false",
            "ls-files",
            "build.gradle.kts",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/MainViewController.kt",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt",
            "doc/RunningHub-KMP-架构迁移验收标准.md",
            "docs/migration",
            "iosApp",
        )
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect tracked migration files with git ls-files:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val dirtyMigrationFiles = ProcessBuilder(
            "git",
            "-c",
            "core.quotePath=false",
            "diff",
            "--name-only",
            "--",
            "build.gradle.kts",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/MainViewController.kt",
            "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt",
            "doc/RunningHub-KMP-架构迁移验收标准.md",
            "docs/migration",
            "iosApp",
        )
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect unstaged migration files with git diff:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }

        // 这些文件是 AC-11 的可追溯证据和后续 CI 的输入；如果只存在于本地工作区，
        // 远端 runner 会缺少观察脚本、shared 基线或当前 Gate 状态，从而造成“本地通过、CI 不可复现”。
        requiredTrackedFiles
            .filterNot { it in trackedMigrationFiles }
            .forEach { file -> violations += "$file must be tracked by Git before it can be used as L1 evidence." }
        dirtyMigrationFiles
            .forEach { file -> violations += "$file has unstaged changes; stage it before using it as L1 evidence." }

        val requiredTabNetworkSnippets = listOf(
            "[string] \$OutputPath",
            "[string] \$OperationNotes",
            "[switch] \$SelfTest",
            "[switch] \$ForceStopBeforeLaunch",
            "function New-NetworkSampleFromLine",
            "function Get-NetworkObservationSummary",
            "function New-NetworkObservationEvidence",
            "function Save-NetworkObservationEvidence",
            "function Invoke-SelfTest",
            "durationSeconds",
            "stableWindowSeconds",
            "stableWindowSampleCount",
            "operationNotes",
            "result=pass_candidate",
            "evidencePath=",
            "RunningHubNetwork",
        )
        val requiredGitHubActionsSnippets = listOf(
            "[string] \$OutputDir",
            "[string] \$HeadSha",
            "[string] \$Branch",
            "[switch] \$Wait",
            "[int] \$WaitTimeoutSeconds",
            "[int] \$PollSeconds",
            "[switch] \$SelfTest",
            "function Resolve-GitHeadSha",
            "function Invoke-GhRunList",
            "function Select-SuccessfulWorkflowRun",
            "function Wait-SuccessfulWorkflowRun",
            "function Save-GitHubActionsEvidence",
            "function Assert-NonBlankEvidenceField",
            "function Assert-GitHubActionsEvidence",
            "function Assert-MatchingWorkflowHeadSha",
            "ConvertTo-Json -Compress",
            "UTF8Encoding",
            "Start-Sleep",
            "status=in_progress",
            "databaseId",
            "headSha",
            "targetHeadSha=",
            "same headSha",
            "github-actions-android.json",
            "github-actions-ios.json",
            "Android CI",
            "iOS CI",
            "conclusion -eq \"success\"",
        )
        val requiredIosMacosSnippets = listOf(
            "--simulator-smoke-pass",
            "--self-test",
            "headSha: \$head_sha",
            "git rev-parse HEAD",
            "linkResult: \$link_result",
            "xcodebuildCommand:",
            "xcodebuildResult: \$xcodebuild_result",
            "xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub",
            "** BUILD SUCCEEDED **",
            "simulatorSmokeResult: \$simulator_smoke_result",
            ":composeApp:linkDebugFrameworkIosSimulatorArm64",
            "BUILD SUCCESSFUL",
            "Darwin",
            "Simulator login",
            "Simulator QuickCreate",
        )
        val requiredIosSimulatorSmokeSnippets = listOf(
            "--self-test",
            "xcrun simctl bootstatus",
            "xcrun simctl install",
            "xcrun simctl launch",
            "xcrun simctl terminate",
            "xcrun simctl delete",
            "build/xcode/DerivedData/Build/Products/Debug-iphonesimulator/RunningHub.app",
            "com.runninghub.app.ios",
            "simulatorLaunchResult: pass",
            "simulatorSmokePath=",
        )
        val requiredIosEvidenceDownloadSnippets = listOf(
            "[string] \$RunId",
            "[string] \$ArtifactName",
            "[switch] \$Wait",
            "[int] \$WaitTimeoutSeconds",
            "[int] \$PollSeconds",
            "[switch] \$SelfTest",
            "function Select-IosEvidenceRun",
            "function Wait-IosEvidenceRun",
            "function Invoke-GhRunDownload",
            "function Copy-IosEvidenceArtifact",
            "function Assert-IosEvidenceMarkdown",
            "Start-Sleep",
            "workflow_dispatch='iOS CI' status=in_progress",
            "workflow_dispatch",
            "ios-macos-link-and-simulator.md",
            "gh run download",
            "linkResult: pass",
            "xcodebuildResult: pass",
            "simulatorSmokeResult: pass",
        )
        val requiredIosEvidenceRequestSnippets = listOf(
            "[switch] \$ConfirmSimulatorSmokePass",
            "[string] \$SmokeNotes",
            "[switch] \$Wait",
            "[switch] \$SelfTest",
            "function Assert-SmokeConfirmation",
            "function Invoke-GhWorkflowRun",
            "function Invoke-IosEvidenceDownload",
            "gh workflow run",
            "simulator_smoke_pass=true",
            "smoke_notes=",
            "download-ios-macos-evidence.ps1",
        )
        val requiredL1EvidenceFinalizeSnippets = listOf(
            "[ValidateSet(\"skip\", \"request\", \"download\", \"none\")]",
            "[switch] \$StageEvidence",
            "[switch] \$RunSealCheck",
            "[switch] \$SelfTest",
            "function Assert-CleanPreCollectionState",
            "function Invoke-CiEvidenceCollection",
            "function Write-SkippedIosEvidence",
            "function Invoke-IosEvidenceRequest",
            "function Invoke-IosEvidenceDownload",
            "function Get-L1EvidencePaths",
            "function Add-L1EvidenceFiles",
            "function Invoke-L1SealEvidenceCheck",
            "collect-github-actions-evidence.ps1",
            "request-ios-macos-evidence.ps1",
            "download-ios-macos-evidence.ps1",
            "git add --",
            "git diff --cached --name-only",
            "git ls-files --others --exclude-standard",
            "Requested -HeadSha",
            "preCollectionClean=true",
            "evidenceStaged=true",
            "sealEvidenceCheck=pass",
            "untracked file was accepted",
            "overallResult: skipped",
            "linkResult: skipped",
            "xcodebuildResult: skipped",
            "simulatorSmokeResult: skipped",
            "checkL1SealEvidence",
        )
        val requiredAndroidNetworkObserverSnippets = listOf(
            "NETWORK_ACTIVITY_HEARTBEAT_MILLIS",
            "delay(NETWORK_ACTIVITY_HEARTBEAT_MILLIS)",
            "tracker.snapshots.value",
            "started=\${snapshot.startedCount}",
            "completed=\${snapshot.completedCount}",
            "inFlight=\${snapshot.inFlightCount}",
        )
        val requiredIosKoinEntrySnippets = listOf(
            "val iosRuntimeModule = module",
            "fun startRunningHubKoin()",
            "KoinPlatformTools.defaultContext().getOrNull()",
            "authDataModule",
            "communityDataModule",
            "discoveryDataModule",
            "taskDataModule",
            "quickCreateDataModule",
            "appModule",
        )
        val requiredIosViewControllerSnippets = listOf(
            "fun MainViewController()",
            "ComposeUIViewController",
            "App()",
        )
        val requiredIosXcodeProjectSnippets = listOf(
            "embedAndSignAppleFrameworkForXcode",
            "FRAMEWORK_SEARCH_PATHS",
            "\$(SRCROOT)/../composeApp/build/xcode-frameworks/\$(CONFIGURATION)/\$(SDK_NAME)",
            "OTHER_LDFLAGS",
            "-framework",
            "ComposeApp",
            "PRODUCT_BUNDLE_IDENTIFIER = com.runninghub.app.ios",
            "IPHONEOS_DEPLOYMENT_TARGET = 16.0",
            "SWIFT_VERSION = 5.0",
            "PrivacyInfo.xcprivacy in Resources",
        )
        val iosPrivacyManifestText = iosPrivacyManifest.takeIf { it.exists() }?.readText().orEmpty()
        val requiredIosPrivacyManifestSnippets = listOf(
            "NSPrivacyAccessedAPITypes",
            "NSPrivacyAccessedAPICategoryUserDefaults",
            "CA92.1",
            "NSPrivacyAccessedAPICategoryFileTimestamp",
            "C617.1",
            "NSPrivacyCollectedDataTypes",
            "NSPrivacyTracking",
        )
        val requiredIosSwiftSnippets = listOf(
            "IosRuntimeModuleKt.startRunningHubKoin()",
            "MainViewControllerKt.MainViewController()",
            "UIViewControllerRepresentable",
            "import ComposeApp",
        )
        val iosSharedScheme = rootDir.resolve("iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/RunningHub.xcscheme")
        val iosSharedSchemeText = iosSharedScheme.takeIf { it.exists() }?.readText().orEmpty()
        val requiredIosSchemeSnippets = listOf(
            "BlueprintName = \"RunningHub\"",
            "BuildableName = \"RunningHub.app\"",
            "container:iosApp.xcodeproj",
        )
        val requiredL1SealEvidenceSnippets = listOf(
            "\"diff\", \"--cached\", \"--name-only\"",
            "allowedStagedEvidenceFiles",
            "val stagedFiles",
            "has staged non-evidence changes; commit code/config changes",
            "overallResult",
            "skipped",
            "followUpRequired",
            "must use linkResult=pass when overallResult=pass",
            "must use linkResult=skipped",
        )
        requiredTabNetworkSnippets
            .filterNot { it in tabNetworkScriptText }
            .forEach { snippet -> violations += "observe-tab-network.ps1 must contain `$snippet`." }
        requiredGitHubActionsSnippets
            .filterNot { it in githubActionsScriptText }
            .forEach { snippet -> violations += "collect-github-actions-evidence.ps1 must contain `$snippet`." }
        requiredIosMacosSnippets
            .filterNot { it in iosMacosScriptText }
            .forEach { snippet -> violations += "collect-ios-macos-evidence.sh must contain `$snippet`." }
        requiredIosSimulatorSmokeSnippets
            .filterNot { it in iosSimulatorSmokeScriptText }
            .forEach { snippet -> violations += "run-ios-simulator-smoke.sh must contain `$snippet`." }
        val iosEvidenceDownloadScriptText = iosEvidenceDownloadScript.takeIf { it.exists() }?.readText().orEmpty()
        val iosEvidenceRequestScriptText = iosEvidenceRequestScript.takeIf { it.exists() }?.readText().orEmpty()
        val l1EvidenceFinalizeScriptText = l1EvidenceFinalizeScript.takeIf { it.exists() }?.readText().orEmpty()
        requiredIosEvidenceDownloadSnippets
            .filterNot { it in iosEvidenceDownloadScriptText }
            .forEach { snippet -> violations += "download-ios-macos-evidence.ps1 must contain `$snippet`." }
        requiredIosEvidenceRequestSnippets
            .filterNot { it in iosEvidenceRequestScriptText }
            .forEach { snippet -> violations += "request-ios-macos-evidence.ps1 must contain `$snippet`." }
        requiredL1EvidenceFinalizeSnippets
            .filterNot { it in l1EvidenceFinalizeScriptText }
            .forEach { snippet -> violations += "finalize-l1-external-evidence.ps1 must contain `$snippet`." }
        requiredAndroidNetworkObserverSnippets
            .filterNot { it in androidNetworkObserverText }
            .forEach { snippet -> violations += "AndroidNetworkActivityLogObserver.kt must contain `$snippet`." }
        requiredIosKoinEntrySnippets
            .filterNot { it in iosKoinEntryText }
            .forEach { snippet -> violations += "IosRuntimeModule.kt must contain `$snippet`." }
        requiredIosViewControllerSnippets
            .filterNot { it in iosMainViewControllerText }
            .forEach { snippet -> violations += "MainViewController.kt must contain `$snippet`." }
        requiredIosXcodeProjectSnippets
            .filterNot { it in iosXcodeProjectText }
            .forEach { snippet -> violations += "iosApp.xcodeproj/project.pbxproj must contain `$snippet`." }
        requiredIosPrivacyManifestSnippets
            .filterNot { it in iosPrivacyManifestText }
            .forEach { snippet -> violations += "PrivacyInfo.xcprivacy must contain `$snippet`." }
        requiredIosSchemeSnippets
            .filterNot { it in iosSharedSchemeText }
            .forEach { snippet -> violations += "RunningHub.xcscheme must contain `$snippet`." }
        requiredIosSwiftSnippets
            .filterNot { it in iosSwiftAppText || it in iosContentViewText }
            .forEach { snippet -> violations += "iOS Swift wrapper must contain `$snippet`." }
        requiredL1SealEvidenceSnippets
            .filterNot { it in l1SealEvidenceTaskText }
            .forEach { snippet -> violations += "checkL1SealEvidence must contain `$snippet`." }

        // 该脚本需要能被 Windows PowerShell 5 直接执行。仓库当前没有统一保存 BOM，
        // 同时 shell 脚本需要能在 macOS runner 上直接执行；运行时字符串保持 ASCII，
        // 中文说明放在 Markdown/YAML 文档中记录。
        listOf(tabNetworkScript, githubActionsScript, iosEvidenceDownloadScript, iosEvidenceRequestScript).forEach { script ->
            script.readLines().forEachIndexed { index, line ->
                if (line.any { it.code > 127 }) {
                    violations += "${script.name}:${index + 1} contains non-ASCII runtime text."
                }
            }
        }
        iosMacosScript.readLines().forEachIndexed { index, line ->
            if (line.any { it.code > 127 }) {
                violations += "${iosMacosScript.name}:${index + 1} contains non-ASCII runtime text."
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Migration script check failed:")
                    violations.forEach { appendLine("- $it") }
                }
            )
        }
    }
}

/**
 * 校验 L1 封板所需的外部证据已经落盘。
 *
 * `verifyL1Local` 只能证明当前机器上的本地自动化门禁通过，不能替代 GitHub Actions、
 * macOS iOS link、登录态 Android Tab 网络观察或退出登录后的 Android 运行观察。该任务故意不接入 `verifyL1Local`，
 * 只在准备把 AC-11 标记为封板时手动执行，避免把缺失的外部证据伪装成本地绿灯。
 */
tasks.register("checkL1SealEvidence") {
    group = "verification"
    description = "Checks external evidence required before claiming L1 migration seal."

    doLast {
        data class EvidenceFile(
            val label: String,
            val relativePath: String,
            val requiredSnippets: List<String>,
        )

        val evidenceFiles = listOf(
            EvidenceFile(
                label = "Android GitHub Actions",
                relativePath = "docs/migration/evidence/github-actions-android.json",
                requiredSnippets = listOf("workflowName", "conclusion", "success", "url", "Android CI"),
            ),
            EvidenceFile(
                label = "iOS GitHub Actions",
                relativePath = "docs/migration/evidence/github-actions-ios.json",
                requiredSnippets = listOf("workflowName", "conclusion", "success", "url", "iOS CI"),
            ),
            EvidenceFile(
                label = "Android login-state Tab network observation",
                relativePath = "docs/migration/evidence/android-tab-network.json",
                requiredSnippets = listOf(
                    "\"schemaVersion\"",
                    "\"result\"",
                    "pass_candidate",
                    "\"stableWindowStartedDelta\"",
                    "\"stableWindowMaxInFlight\"",
                    "\"stableWindowSampleCount\"",
                ),
            ),
            EvidenceFile(
                label = "Android logout network observation",
                relativePath = "docs/migration/evidence/android-logout-network.json",
                requiredSnippets = listOf(
                    "\"schemaVersion\"",
                    "\"result\"",
                    "pass_candidate",
                    "\"stableWindowStartedDelta\"",
                    "\"stableWindowMaxInFlight\"",
                    "\"stableWindowSampleCount\"",
                    "logout",
                ),
            ),
            EvidenceFile(
                label = "macOS iOS link and Simulator smoke",
                relativePath = "docs/migration/evidence/ios-macos-link-and-simulator.md",
                requiredSnippets = listOf(
                    "headSha:",
                    "overallResult:",
                    "skipReason:",
                    "followUpRequired:",
                ),
            ),
        )

        val trackedEvidenceFiles = ProcessBuilder("git", "ls-files", "docs/migration/evidence")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect tracked L1 evidence files with git ls-files:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val unstagedFiles = ProcessBuilder("git", "-c", "core.quotePath=false", "diff", "--name-only")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect unstaged files with git diff:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        val stagedFiles = ProcessBuilder("git", "-c", "core.quotePath=false", "diff", "--cached", "--name-only")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException("Unable to inspect staged files with git diff --cached:\n$output")
                }
                output.lineSequence()
                    .map { it.trim().replace('\\', '/') }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }

        val violations = mutableListOf<String>()
        val allowedStagedEvidenceFiles = evidenceFiles.map { it.relativePath }.toSet()

        // 外部证据必须绑定到已经提交的代码 HEAD。证据文件本身通常在 CI/macOS
        // 运行结束后才落盘，因此允许只暂存证据文件；其他 staged 变更仍说明代码未被当前 HEAD 覆盖。
        unstagedFiles.forEach { file ->
            violations += "$file has unstaged changes; stage it before using L1 seal evidence."
        }
        stagedFiles
            .filterNot { it in allowedStagedEvidenceFiles }
            .forEach { file ->
                violations += "$file has staged non-evidence changes; commit code/config changes and recollect CI evidence for that HEAD before using L1 seal evidence."
            }

        val evidenceSensitivePatterns = listOf(
            "OpenAI API key" to Regex("""sk-[A-Za-z0-9_-]{20,}"""),
            "GitHub token" to Regex("""ghp_[A-Za-z0-9_]{20,}"""),
            "Google API key" to Regex("""AIza[0-9A-Za-z_-]{20,}"""),
            "AWS access key" to Regex("""AKIA[0-9A-Z]{16}"""),
            "private key block" to Regex("""-----BEGIN (RSA |OPENSSH |EC |DSA )?PRIVATE KEY-----"""),
            "Authorization header" to Regex(
                """\bAuthorization\s*:\s*(Bearer|Basic)?\s*[A-Za-z0-9._~+/=-]{8,}""",
                RegexOption.IGNORE_CASE,
            ),
            "Cookie header" to Regex(
                """\b(Set-Cookie|Cookie)\s*:\s*[^;\r\n=]+=[^;\r\n]{4,}""",
                RegexOption.IGNORE_CASE,
            ),
            "credential field" to Regex(
                """"?(accessToken|refreshToken|idToken|apiKey|cookie)"?\s*[:=]\s*"[^"\r\n]{12,}"""",
                RegexOption.IGNORE_CASE,
            ),
            "request body" to Regex(
                """\b(requestBody|request_body)\s*[:=]\s*(\{|\[|").{20,}""",
                RegexOption.IGNORE_CASE,
            ),
        )

        fun textField(json: Map<*, *>, name: String): String =
            json[name]?.toString()?.trim().orEmpty()

        fun numberField(json: Map<*, *>, name: String): Double? =
            when (val value = json[name]) {
                is Number -> value.toDouble()
                is String -> value.trim().toDoubleOrNull()
                else -> null
            }

        fun markdownField(text: String, name: String): String =
            text.lineSequence()
                .firstOrNull { it.startsWith("$name:") }
                ?.substringAfter(':')
                ?.trim()
                .orEmpty()

        fun validateEvidenceContainsNoSensitiveData(evidence: EvidenceFile, text: String) {
            // L1 外部证据会被加入 Git 索引并随补丁交付；即使运行验证通过，也不能把认证头、
            // Cookie、API Key 或请求 Body 带入仓库。这里只拦截明确的凭据形态，避免普通说明文本误报。
            text.lineSequence().forEachIndexed { index, line ->
                evidenceSensitivePatterns
                    .firstOrNull { (_, pattern) -> pattern.containsMatchIn(line) }
                    ?.let { (label, _) ->
                        violations += "${evidence.label} evidence at ${evidence.relativePath}:${index + 1} contains possible $label."
                    }
            }
        }

        fun parseJsonEvidence(evidence: EvidenceFile): Any? {
            val file = rootDir.resolve(evidence.relativePath)
            if (!file.exists()) {
                return null
            }
            return try {
                groovy.json.JsonSlurper().parse(file)
            } catch (error: Exception) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must be valid JSON: ${error.message}"
                null
            }
        }

        fun jsonObjects(value: Any?): List<Map<*, *>> =
            when (value) {
                is Map<*, *> -> listOf(value)
                is List<*> -> value.mapNotNull { it as? Map<*, *> }
                else -> emptyList()
            }

        val currentHeadSha = ProcessBuilder("git", "rev-parse", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .let { process ->
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                val exitCode = process.waitFor()
                if (exitCode != 0 || output.isBlank()) {
                    throw GradleException("Unable to resolve current git HEAD for L1 evidence validation:\n$output")
                }
                output
            }

        fun validateGitHubActionsEvidence(evidence: EvidenceFile, workflowName: String): Map<*, *>? {
            if (!rootDir.resolve(evidence.relativePath).exists()) {
                return null
            }
            val runs = jsonObjects(parseJsonEvidence(evidence))
            if (runs.isEmpty()) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must be a JSON object or array of workflow runs."
                return null
            }

            val matchingRuns = runs.filter { textField(it, "workflowName") == workflowName }
            if (matchingRuns.isEmpty()) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include workflowName=$workflowName."
                return null
            }

            val successfulRun = matchingRuns.firstOrNull {
                textField(it, "status") == "completed" && textField(it, "conclusion") == "success"
            }
            if (successfulRun == null) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must describe a completed successful workflow run."
                return null
            }

            listOf("databaseId", "headSha", "url").forEach { field ->
                if (textField(successfulRun, field).isBlank()) {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must include non-blank `$field`."
                }
            }
            val expectedUrlPrefix = "https://github.com/flybirdxx/RunningHub/actions/runs/"
            if (!textField(successfulRun, "url").startsWith(expectedUrlPrefix)) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must use a RunningHub GitHub Actions run URL."
            }
            // 远端 CI 证据必须绑定当前待封板提交；只要求 Android/iOS headSha 一致仍可能拿到旧提交的绿灯。
            val actualHeadSha = textField(successfulRun, "headSha")
            if (actualHeadSha.isNotBlank() && actualHeadSha != currentHeadSha) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must use current HEAD $currentHeadSha; found $actualHeadSha."
            }
            return successfulRun
        }

        fun validateAndroidNetworkEvidence(evidence: EvidenceFile) {
            if (!rootDir.resolve(evidence.relativePath).exists()) {
                return
            }
            val json = parseJsonEvidence(evidence) as? Map<*, *>
            if (json == null) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must be a JSON object."
                return
            }

            // 运行期网络观察证据必须按字段验证，避免仅包含 pass_candidate 文本的手工 JSON 被误判为 Tab 生命周期已验收。
            if (numberField(json, "schemaVersion") == null) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include numeric schemaVersion."
            }
            if (textField(json, "packageName") != "com.runninghub.app.debug") {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must target packageName=com.runninghub.app.debug."
            }
            if (textField(json, "operationNotes").isBlank()) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include non-blank operationNotes describing the logged-in tab path."
            }
            // 登录态 Tab 观察必须覆盖完整操作和静置窗口；短窗口冷启动样本只能证明采集通道可用，
            // 不能证明 History/QuickCreate 等不可见页面已经停止后台请求。
            if ((numberField(json, "durationSeconds") ?: 0.0) < 120.0) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must use durationSeconds>=120."
            }
            if ((numberField(json, "stableWindowSeconds") ?: 0.0) < 30.0) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must use stableWindowSeconds>=30."
            }
            if (textField(json, "result") != "pass_candidate") {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must have result=pass_candidate."
            }
            if ((numberField(json, "sampleCount") ?: 0.0) <= 0.0) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must contain at least one RunningHubNetwork sample."
            }
            val samples = json["samples"] as? List<*>
            val stableWindowSeconds = numberField(json, "stableWindowSeconds") ?: 0.0
            val sampleCount = numberField(json, "sampleCount") ?: 0.0
            val stableWindowSampleCount = numberField(json, "stableWindowSampleCount") ?: 0.0
            if (samples == null) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include a samples array."
            } else if (samples.size.toDouble() != sampleCount) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must have sampleCount matching samples.size."
            }
            // Android debug 观察器会按秒输出心跳样本，但 adb 轮询和 logcat 清理存在秒级调度抖动。
            // 因此最终证据允许 1 个样本误差，同时仍要求稳定窗口时长、请求增量和 in-flight 均满足条件，
            // 避免把某一瞬间 inFlight 为 0 误判为后台任务已释放。
            if (stableWindowSampleCount < stableWindowSeconds - 1.0) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must contain stable-window samples with at most one scheduling jitter sample missing."
            }
            if (numberField(json, "stableWindowStartedDelta") != 0.0) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must have stableWindowStartedDelta=0."
            }
            if (numberField(json, "stableWindowMaxInFlight") != 0.0) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must have stableWindowMaxInFlight=0."
            }
        }

        fun validateIosMacosEvidence(evidence: EvidenceFile) {
            val file = rootDir.resolve(evidence.relativePath)
            if (!file.exists()) {
                return
            }
            val text = file.readText()
            val capturedAt = markdownField(text, "capturedAt")
            val evidenceHeadSha = markdownField(text, "headSha")
            val host = markdownField(text, "host")
            val overallResult = markdownField(text, "overallResult")
            val skipReason = markdownField(text, "skipReason")
            val followUpRequired = markdownField(text, "followUpRequired")
            val linkCommand = markdownField(text, "linkCommand")
            val linkResult = markdownField(text, "linkResult")
            val xcodebuildCommand = markdownField(text, "xcodebuildCommand")
            val xcodebuildResult = markdownField(text, "xcodebuildResult")
            val simulatorSmokeResult = markdownField(text, "simulatorSmokeResult")
            if (capturedAt.isBlank()) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include non-blank capturedAt."
            }
            if (evidenceHeadSha.isBlank()) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include non-blank headSha."
            } else if (evidenceHeadSha != currentHeadSha) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must use current HEAD $currentHeadSha; found $evidenceHeadSha."
            }
            if (overallResult !in setOf("pass", "skipped")) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must set overallResult to pass or skipped."
            }

            val notes = text.substringAfter("## Notes", missingDelimiterValue = "").substringBefore("## Gradle output tail")
            if (notes.isBlank()) {
                violations += "${evidence.label} evidence at ${evidence.relativePath} must include non-blank notes describing the pass evidence or skip decision."
            }

            if (overallResult == "pass") {
                if (!host.contains("Darwin")) {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must be captured on macOS with host containing Darwin."
                }
                val expectedLinkCommand = "./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64"
                if (linkCommand != expectedLinkCommand) {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must use linkCommand=$expectedLinkCommand."
                }
                if (linkResult != "pass") {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must use linkResult=pass when overallResult=pass."
                }
                val expectedXcodebuildCommand = "xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO"
                if (xcodebuildCommand != expectedXcodebuildCommand) {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must use xcodebuildCommand=$expectedXcodebuildCommand."
                }
                if (xcodebuildResult != "pass") {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must use xcodebuildResult=pass when overallResult=pass."
                }
                if (simulatorSmokeResult != "pass") {
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must use simulatorSmokeResult=pass when overallResult=pass."
                }
            }

            // 用户已明确当前无法测试 macOS 环境，因此允许把 iOS link/Simulator 作为有记录的 skip。
            // skip 仍必须绑定当前 HEAD、说明风险和后续补验条件，避免被误读为真实 macOS 通过证据。
            if (overallResult == "skipped") {
                if (skipReason.isBlank()) {
                    violations += "${evidence.label} skip evidence at ${evidence.relativePath} must include non-blank skipReason."
                }
                if (followUpRequired.isBlank() || followUpRequired.equals("false", ignoreCase = true)) {
                    violations += "${evidence.label} skip evidence at ${evidence.relativePath} must keep followUpRequired non-blank and not false."
                }
                if (linkResult != "skipped") {
                    violations += "${evidence.label} skip evidence at ${evidence.relativePath} must use linkResult=skipped; found $linkResult."
                }
                if (xcodebuildResult != "skipped") {
                    violations += "${evidence.label} skip evidence at ${evidence.relativePath} must use xcodebuildResult=skipped; found $xcodebuildResult."
                }
                if (simulatorSmokeResult != "skipped") {
                    violations += "${evidence.label} skip evidence at ${evidence.relativePath} must use simulatorSmokeResult=skipped; found $simulatorSmokeResult."
                }
            }
        }

        evidenceFiles.forEach { evidence ->
            val file = rootDir.resolve(evidence.relativePath)
            if (!file.exists()) {
                violations += "${evidence.label} evidence is missing at ${evidence.relativePath}."
                return@forEach
            }
            if (evidence.relativePath !in trackedEvidenceFiles) {
                violations += "${evidence.label} evidence exists but is not tracked by Git at ${evidence.relativePath}."
            }

            val text = file.readText()
            validateEvidenceContainsNoSensitiveData(evidence, text)
            evidence.requiredSnippets
                .filterNot { it in text }
                .forEach { snippet ->
                    violations += "${evidence.label} evidence at ${evidence.relativePath} must contain `$snippet`."
                }
        }
        val androidCiEvidence = validateGitHubActionsEvidence(evidenceFiles[0], "Android CI")
        val iosCiEvidence = validateGitHubActionsEvidence(evidenceFiles[1], "iOS CI")
        if (androidCiEvidence != null && iosCiEvidence != null) {
            val androidHeadSha = textField(androidCiEvidence, "headSha")
            val iosHeadSha = textField(iosCiEvidence, "headSha")
            // 双端 CI 必须证明同一份待封板提交通过，不能用两个不同提交上的成功运行拼接出 L1 证据。
            if (androidHeadSha.isNotBlank() && iosHeadSha.isNotBlank() && androidHeadSha != iosHeadSha) {
                violations += "Android CI and iOS CI evidence must use the same headSha; found Android=$androidHeadSha and iOS=$iosHeadSha."
            }
        }
        validateAndroidNetworkEvidence(evidenceFiles[2])
        validateAndroidNetworkEvidence(evidenceFiles[3])
        validateIosMacosEvidence(evidenceFiles[4])

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("L1 seal evidence check failed:")
                    violations.forEach { appendLine("- $it") }
                }
            )
        }
    }
}

/**
 * 执行 L1 架构封板前所有 Gradle Test 类型任务。
 *
 * KMP 与 Android 子模块会生成各自的 `testDebugUnitTest` 等任务，根项目自带的 `test`
 * 任务无法代表整仓单元测试矩阵。单独提供该聚合入口，是为了让本地验证和 GitHub Actions
 * 都不会因为根测试任务空跑而误判 Gate J 已满足。
 */
tasks.register("verifyL1UnitTests") {
    group = "verification"
    description = "Runs all Gradle Test tasks required by the L1 migration gate."
}

/**
 * 执行 Android 侧 L1 自动化门禁。
 *
 * 该任务用于 Linux CI 和本地 Android 回归，覆盖架构边界、所有可发现的 JVM/Android
 * 单元测试、Android lint、debug 构建和启用 R8/资源压缩后的 release 构建。iOS 编译和 framework link 不放在这里，
 * 避免 Ubuntu runner 因平台能力不匹配而给出不可执行的检查项。
 */
tasks.register("verifyL1Android") {
    group = "verification"
    description = "Runs Android-side L1 migration checks for CI and local verification."

    dependsOn(
        "checkL1CiWorkflows",
        "checkMigrationScripts",
        "checkLongTermGovernance",
        "checkArchitectureBoundaries",
        "verifyL1UnitTests",
        ":composeApp:lintDebug",
        ":composeApp:assembleDebug",
        ":composeApp:assembleRelease",
    )
}

/**
 * 执行 iOS 侧 L1 自动化门禁。
 *
 * 该任务用于 macOS CI 或 macOS 开发机，覆盖共享模块与应用模块的 iOS Simulator Kotlin
 * 编译以及 Compose App debug framework link。Windows 本地可能跳过 link，不能替代
 * macOS runner 的真实执行记录。
 */
tasks.register("verifyL1Ios") {
    group = "verification"
    description = "Runs iOS-side L1 migration checks for macOS CI."

    dependsOn(
        "checkL1CiWorkflows",
        "checkMigrationScripts",
        "checkLongTermGovernance",
        "checkArchitectureBoundaries",
        ":shared:compileKotlinIosSimulatorArm64",
        ":composeApp:compileKotlinIosSimulatorArm64",
        ":composeApp:linkDebugFrameworkIosSimulatorArm64",
    )
}

/**
 * 执行 L1 架构封板前的本地聚合验证。
 *
 * AC-11 需要把本地可自动化证据收敛到稳定入口，避免后续协作者在多个文档和聊天记录中
 * 手动拼装命令。本地入口组合 Android 与 iOS 两侧门禁；其中 iOS framework link 的真实
 * 封板证据仍以 macOS CI 或 macOS 开发机输出为准。
 */
tasks.register("verifyL1Local") {
    group = "verification"
    description = "Runs local checks required before L1 migration seal audit."

    dependsOn(
        "verifyL1Android",
        "verifyL1Ios",
    )
}

gradle.projectsEvaluated {
    tasks.named("verifyL1UnitTests").configure {
        dependsOn(
            allprojects.flatMap { project ->
                project.tasks.withType(org.gradle.api.tasks.testing.Test::class.java).map { task -> task.path }
            }
        )
    }
}
