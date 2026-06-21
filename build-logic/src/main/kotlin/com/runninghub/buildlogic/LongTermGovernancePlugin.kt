package com.runninghub.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 注册 RunningHub 长期治理门禁。
 *
 * 该插件承接架构复核中的长期维护建议，只检查能够在仓库内稳定验证的治理入口：
 * 当前架构文档、日常开发规范、ADR、迁移归档边界、日志/契约测试/文案/性能/发布规范，
 * 以及 `composeApp` 巨型文件的新增风险。外部 CI 证据、GitHub Branch Protection、
 * App Store/TestFlight 发布等需要远端权限或真实设备的动作不在这里假装完成。
 */
class LongTermGovernancePlugin : Plugin<Project> {

    /**
     * 在根工程注册 `checkLongTermGovernance` 任务。
     *
     * 该任务必须只读取仓库文本文件，不能触发网络、设备、签名或外部服务访问；
     * 因此它可以被 Android、iOS 和本地聚合验证安全复用。
     */
    override fun apply(target: Project) {
        with(target) {
            if (this != rootProject) {
                throw GradleException("runninghub.long-term-governance must be applied to the root project only.")
            }

            tasks.register("checkLongTermGovernance") {
                group = "verification"
                description = "Checks long-term architecture governance documents and composeApp growth guardrails."

                doLast {
                    val violations = mutableListOf<String>()

                    val requiredDocuments = listOf(
                        "ARCHITECTURE.md",
                        "DEVELOPMENT.md",
                        "docs/adr/0001-feature-first-kmp-boundaries.md",
                        "docs/adr/0002-runtime-environment-and-secure-storage.md",
                        "docs/archive/migration-2026/README.md",
                        "docs/governance/ai-task-template.md",
                        "docs/governance/build-logic-migration.md",
                        "docs/governance/build-script-baseline.txt",
                        "docs/governance/chinese-commenting.md",
                        "docs/governance/long-term-priorities.md",
                        "docs/governance/feature-presentation-thresholds.txt",
                        "docs/governance/logging-and-observability.md",
                        "docs/governance/api-and-database-contract-tests.md",
                        "docs/governance/api-contract-test-baseline.txt",
                        "docs/governance/database-contract-test-baseline.txt",
                        "docs/governance/ui-copy-resources.md",
                        "docs/governance/ui-copy-hardcoded-baseline.txt",
                        "docs/governance/performance-baselines.md",
                        "docs/governance/performance-baseline-targets.txt",
                        "docs/governance/release-automation.md",
                        "docs/governance/release-readiness-checklist.md",
                        "docs/governance/composeapp-file-size-allowlist.txt",
                        ".github/dependabot.yml",
                        ".github/pull_request_template.md",
                        ".github/workflows/dependency-submission.yml",
                    )

                    requiredDocuments.forEach { relativePath ->
                        val file = rootDir.resolve(relativePath)
                        if (!file.isFile) {
                            violations += "Long-term governance document is missing: $relativePath."
                        } else if (file.readText().isBlank()) {
                            violations += "Long-term governance document must not be blank: $relativePath."
                        }
                    }

                    requireDocumentSnippets(
                        relativePath = "ARCHITECTURE.md",
                        snippets = listOf("Presentation -> Domain <- Data", "checkLongTermGovernance", "composeApp", "shared"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "DEVELOPMENT.md",
                        snippets = listOf("checkLongTermGovernance", "verifyL1Android", "未验证项"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/archive/migration-2026/README.md",
                        snippets = listOf("checkL1SealEvidence", "docs/migration/", "归档条件", "当前 L1 尚未完成当前 HEAD 外部证据封板"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/long-term-priorities.md",
                        snippets = listOf(
                            "P1",
                            "P2",
                            "P3",
                            "P4",
                            "P5",
                            "P6",
                            "P7",
                            "P8",
                            "P9",
                            "P10",
                            "本地门禁映射",
                            "checkContextEntryGuard",
                            "checkRootBuildScriptSize",
                            "checkLegacyAndroidAppGuard",
                            "checkProductionTodoGuard",
                            "checkComposeAppFileSize",
                            "checkFeaturePresentationThresholds",
                            "checkTrustedAuthHostGuard",
                            "checkAuthReplayGuard",
                            "checkAuthJsonParsingGuard",
                            "checkRuntimeEnvironmentOverrideGuard",
                            "checkSecureCredentialStorageGuard",
                            "checkMigratedDataErrorMessageGuard",
                            "checkIosPermissionAndMediaGuard",
                            "checkSmsCaptchaGuard",
                            "checkSensitiveLogging",
                            "checkMediaUploadErrorPrivacyGuard",
                            "checkApiContractBaseline",
                            "checkDatabaseContractBaseline",
                            "checkUiCopyBaseline",
                            "checkPerformanceBaselineTargets",
                            "checkAndroidReleaseBuildGuard",
                            "checkReleaseReadinessChecklist",
                            "checkDependencyMaintenanceGuard",
                            "AGENTS.md",
                            "150-250",
                            "ai-task-template",
                            "PrivacyInfo.xcprivacy",
                            "Dependabot",
                            "Dependency Submission",
                            "PR 模板",
                            "重大版本升级人工回归",
                            "完成口径",
                            "不可本地完成",
                        ),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/ai-task-template.md",
                        snippets = listOf("任务 ID", "唯一目标", "允许修改目录", "禁止修改目录", "验收命令", "最大重试次数", "完成后更新的文档"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/build-logic-migration.md",
                        snippets = listOf("root-build.gradle.kts", "checkArchitectureBoundaries", "checkL1SealEvidence", "迁出顺序", "不再新增根脚本验收逻辑"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/build-script-baseline.txt",
                        snippets = listOf("filePath|maxLines|reason", "build.gradle.kts|1800"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/chinese-commenting.md",
                        snippets = listOf("中文注释", "KDoc", "字段级注释", "TODO", "checkLongTermGovernance", "修改完成检查"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/feature-presentation-thresholds.txt",
                        snippets = listOf("feature|maxLines|reason", "history|", "login|", "plaza|", "profile|"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/logging-and-observability.md",
                        snippets = listOf("Token", "Cookie", "脱敏", "崩溃监控", "debug(", "URL"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/api-and-database-contract-tests.md",
                        snippets = listOf("DTO-to-Domain", "SQLDelight", "migration", "契约测试"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/api-contract-test-baseline.txt",
                        snippets = listOf("dtoPath|testPath|scope", "QuickCreationV2Dto.kt", "WebAppCatalogDto.kt"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/database-contract-test-baseline.txt",
                        snippets = listOf("schemaPath|testPath|scope", "DiscoveryCache.sq"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/ui-copy-resources.md",
                        snippets = listOf("Compose Resources", "硬编码", "Data 层"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/ui-copy-hardcoded-baseline.txt",
                        snippets = listOf("filePath|maxMatches|reason", "LoginScreen.kt", "QuickCreateProjectContent.kt"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/performance-baselines.md",
                        snippets = listOf("启动", "内存", "长轮询", "Android", "iOS"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/performance-baseline-targets.txt",
                        snippets = listOf("metric|platform|evidence|owner", "cold-start", "long-polling-network-idle"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/release-automation.md",
                        snippets = listOf("Android Release", "TestFlight", "签名", "人工确认", "iOS 人审证据要求", "validToken", "重大版本依赖升级回归", "Android/iOS 人工回归矩阵"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/release-readiness-checklist.md",
                        snippets = listOf("Android Release", "iOS TestFlight", "iOS 权限与媒体回归矩阵", "验证码 Web 容器回归矩阵", "依赖升级与重大版本回归", "人工确认", "不得自动执行"),
                        violations = violations,
                    )

                    checkComposeAppFileSize(violations)
                    checkContextEntryGuard(violations)
                    checkRootBuildScriptSize(violations)
                    checkLegacyAndroidAppGuard(violations)
                    checkProductionTodoGuard(violations)
                    checkFeaturePresentationThresholds(violations)
                    checkTrustedAuthHostGuard(violations)
                    checkAuthReplayGuard(violations)
                    checkAuthJsonParsingGuard(violations)
                    checkRuntimeEnvironmentOverrideGuard(violations)
                    checkSecureCredentialStorageGuard(violations)
                    checkMigratedDataErrorMessageGuard(violations)
                    checkIosPermissionAndMediaGuard(violations)
                    checkSmsCaptchaGuard(violations)
                    checkSensitiveLogging(violations)
                    checkMediaUploadErrorPrivacyGuard(violations)
                    checkApiContractBaseline(violations)
                    checkCoreAuthContractSamples(violations)
                    checkDatabaseContractBaseline(violations)
                    checkUiCopyBaseline(violations)
                    checkPerformanceBaselineTargets(violations)
                    checkAndroidReleaseBuildGuard(violations)
                    checkReleaseReadinessChecklist(violations)
                    checkDependencyMaintenanceGuard(violations)

                    if (violations.isNotEmpty()) {
                        throw GradleException(
                            buildString {
                                appendLine("Long-term governance check failed:")
                                violations.forEach { appendLine("- $it") }
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * 校验关键治理文档保留必要主题词。
     *
     * 这里使用轻量文本片段而不是解析 Markdown AST，是为了让任务在没有额外依赖的
     * Gradle 配置阶段保持稳定；片段只覆盖不可删除的治理结论，不约束具体措辞。
     */
    private fun Project.requireDocumentSnippets(
        relativePath: String,
        snippets: List<String>,
        violations: MutableList<String>,
    ) {
        val file = rootDir.resolve(relativePath)
        if (!file.isFile) {
            return
        }
        val text = file.readText()
        snippets
            .filterNot { it in text }
            .forEach { snippet -> violations += "$relativePath must contain `$snippet`." }
    }

    /**
     * 校验指定仓库文件保留关键实现片段。
     *
     * 与 [requireDocumentSnippets] 的区别是调用方已经解析出具体文件，适合平台实现、Info.plist
     * 或其他不以治理文档路径组织的防退化检查。
     */
    private fun Project.requireFileSnippets(
        file: java.io.File,
        snippets: List<String>,
        violations: MutableList<String>,
    ) {
        if (!file.isFile) {
            return
        }
        val text = file.readText()
        val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
        snippets
            .filterNot { it in text }
            .forEach { snippet -> violations += "$relativePath must contain `$snippet`." }
    }

    /**
     * 阻止 `composeApp` 继续新增超大 commonMain Kotlin 文件。
     *
     * 迁移期已有若干历史页面超过阈值，暂时通过 allowlist 登记；后续新增或改名后的
     * 超大文件必须先拆分职责，否则会让 `composeApp` 继续膨胀为新的 UI 单体。
     */
    private fun Project.checkComposeAppFileSize(violations: MutableList<String>) {
        val maxLines = 800
        val allowlistFile = rootDir.resolve("docs/governance/composeapp-file-size-allowlist.txt")
        val allowed = allowlistFile
            .takeIf { it.isFile }
            ?.readLines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.toSet()
            .orEmpty()

        val commonMain = rootDir.resolve("composeApp/src/commonMain/kotlin")
        if (!commonMain.isDirectory) {
            violations += "composeApp commonMain source directory is missing."
            return
        }

        commonMain.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                val lineCount = file.readLines().size
                if (lineCount > maxLines && relativePath !in allowed) {
                    violations += "$relativePath has $lineCount lines; split it or add a documented temporary exception."
                }
            }
    }

    /**
     * 校验 AI 协作入口保持短规则形态。
     *
     * 复核建议要求 `AGENTS.md` 只保留 150-250 行强规则，把迁移过程、详细示例和可按需读取的
     * 专项规范拆到当前态文档或 `docs/governance`。该检查防止后续协作者再次把迁移历史、
     * 大段示例或长期背景塞回入口文件，导致 AI 每次任务都加载无关上下文。
     */
    private fun Project.checkContextEntryGuard(violations: MutableList<String>) {
        val agentsFile = rootDir.resolve("AGENTS.md")
        if (!agentsFile.isFile) {
            violations += "AGENTS.md is missing."
            return
        }

        val lines = agentsFile.readLines()
        val lineCount = lines.size
        if (lineCount !in 150..250) {
            violations += "AGENTS.md must stay between 150 and 250 lines; found $lineCount lines."
        }

        val text = lines.joinToString("\n")
        val requiredSnippets = listOf(
            "ARCHITECTURE.md",
            "DEVELOPMENT.md",
            "docs/governance/long-term-priorities.md",
            "docs/governance/ai-task-template.md",
            "docs/governance/chinese-commenting.md",
            "docs/migration/",
        )
        requiredSnippets
            .filterNot { it in text }
            .forEach { snippet -> violations += "AGENTS.md must contain `$snippet`." }
    }

    /**
     * 校验根构建脚本不会继续膨胀。
     *
     * 复核建议要求把根 `build.gradle.kts` 中超过千行的验收逻辑迁入 `build-logic` 或独立脚本。
     * 当前补丁已经把新增的长期治理放入插件；此检查用基线文件锁住根脚本行数，避免后续协作者
     * 继续把新 Gate 堆回根脚本。真正迁出旧 Gate 时，应同步下调基线。
     */
    private fun Project.checkRootBuildScriptSize(violations: MutableList<String>) {
        val baselineFile = rootDir.resolve("docs/governance/build-script-baseline.txt")
        if (!baselineFile.isFile) {
            return
        }

        baselineFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it != "filePath|maxLines|reason" }
            .forEach { line ->
                val columns = line.split('|')
                if (columns.size < 2) {
                    violations += "build-script-baseline.txt entry must use filePath|maxLines|reason format: $line"
                    return@forEach
                }

                val relativePath = columns[0].trim()
                val maxLines = columns[1].trim().toIntOrNull()
                if (maxLines == null || maxLines <= 0) {
                    violations += "build-script-baseline.txt entry for $relativePath must define a positive maxLines value."
                    return@forEach
                }

                val file = rootDir.resolve(relativePath)
                if (!file.isFile) {
                    violations += "build script baseline points to missing file: $relativePath."
                    return@forEach
                }

                val actualLines = file.readLines().size
                if (actualLines > maxLines) {
                    violations += "$relativePath has $actualLines lines, exceeding baseline maxLines=$maxLines. Move new verification logic into build-logic or lower the baseline after migration."
                }
            }
    }

    /**
     * 校验历史 `androidApp` 模块不会重新进入当前模块图。
     *
     * 复核建议中期要求删除历史 `androidApp`。源码入口删除后，本地仍可能残留被
     * `.gitignore` 忽略的 `androidApp/build` 目录；该目录不代表 Gradle 模块，也不得重新
     * 通过 `settings.gradle.kts` 或旧 `sharedModule` 成为生产启动旁路。
     */
    private fun Project.checkLegacyAndroidAppGuard(violations: MutableList<String>) {
        val legacyAndroidApp = rootDir.resolve("androidApp")
        if (!legacyAndroidApp.isDirectory) {
            return
        }
        val legacyBuildScript = legacyAndroidApp.resolve("build.gradle.kts")
        val legacySourceRoot = legacyAndroidApp.resolve("src")
        if (!legacyBuildScript.exists() && !legacySourceRoot.exists()) {
            return
        }

        val settingsFile = rootDir.resolve("settings.gradle.kts")
        if (!settingsFile.isFile) {
            violations += "settings.gradle.kts is missing, cannot verify legacy androidApp module boundary."
            return
        }
        val settingsText = settingsFile.readText()
        if ("include(\":androidApp\")" in settingsText || "include(\":androidApp\"" in settingsText) {
            violations += "androidApp is a historical directory and must not be included in the current Gradle module graph without a dedicated migration AC."
        }

        val ownershipFile = rootDir.resolve("docs/migration/shared-ownership.md")
        if (!ownershipFile.isFile) {
            violations += "docs/migration/shared-ownership.md must document the historical androidApp boundary."
            return
        }
        requireFileSnippets(
            file = ownershipFile,
            snippets = listOf(
                "androidApp/",
                "settings.gradle.kts",
                "已删除",
                "本地 `androidApp/build`",
            ),
            violations = violations,
        )
    }

    /**
     * 校验生产源码中的 TODO/FIXME 带有可追踪编号。
     *
     * 中文注释规范要求临时方案说明产生原因、当前风险、移除条件和任务编号。静态检查无法完整理解
     * 每段注释语义，因此这里先锁住最低门槛：生产 Kotlin 中的 `TODO` 或 `FIXME` 必须采用
     * `TODO(RH-123)`、`FIXME(RH-123)` 这类可追踪格式；测试和治理文档中的示例不参与检查。
     */
    private fun Project.checkProductionTodoGuard(violations: MutableList<String>) {
        val productionRoots = listOf("composeApp", "core", "feature", "shared")
            .map { rootDir.resolve(it) }
            .filter { it.isDirectory }
        val allowedMarker = Regex("""\b(?:TODO|FIXME)\([A-Z]+-\d+\)""")

        productionRoots
            .asSequence()
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" } }
            .filter { file ->
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                listOf(
                    "/src/commonMain/kotlin/",
                    "/src/androidMain/kotlin/",
                    "/src/iosMain/kotlin/",
                ).any { it in relativePath }
            }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    if ((line.contains("TODO") || line.contains("FIXME")) && !allowedMarker.containsMatchIn(line)) {
                        val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                        violations += "$relativePath:${index + 1} contains TODO/FIXME without a task id such as TODO(RH-123)."
                    }
                }
            }
    }

    /**
     * 约束大体量 UI Feature 的 Presentation 模块拆分计划。
     *
     * 复核建议要求 Feature 达到规模阈值后建立独立 Presentation 模块。既有历史 Feature
     * 不能在一次治理中强行迁移，因此用基线记录当前行数；后续如果这些 Feature 继续增长，
     * 门禁会要求先拆分模块或显式更新治理文档说明原因。
     */
    private fun Project.checkFeaturePresentationThresholds(violations: MutableList<String>) {
        val thresholdLines = 800
        val baselineFile = rootDir.resolve("docs/governance/feature-presentation-thresholds.txt")
        val baseline = baselineFile
            .takeIf { it.isFile }
            ?.readLines()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.associate { line ->
                val parts = line.split('|')
                val featureName = parts.getOrNull(0).orEmpty()
                val maxLines = parts.getOrNull(1)?.toIntOrNull() ?: 0
                featureName to maxLines
            }
            .orEmpty()

        val featureUiRoot = rootDir.resolve("composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature")
        if (!featureUiRoot.isDirectory) {
            violations += "composeApp feature UI directory is missing."
            return
        }

        featureUiRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .forEach { featureDir ->
                val featureName = featureDir.name
                val totalLines = featureDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .sumOf { it.readLines().size }
                val hasPresentationModule = rootDir.resolve("feature/$featureName/presentation").isDirectory

                if (totalLines > thresholdLines && !hasPresentationModule) {
                    val allowedLines = baseline[featureName]
                    when {
                        allowedLines == null -> {
                            violations += "$featureName UI has $totalLines lines and no presentation module; split it or register a threshold baseline."
                        }
                        totalLines > allowedLines -> {
                            violations += "$featureName UI grew from allowed $allowedLines lines to $totalLines without a presentation module."
                        }
                    }
                }
            }
    }

    /**
     * 防止认证头主机判断退回宽泛字符串匹配。
     *
     * 复核指出 `host.contains("runninghub.cn")` 会把 `evilrunninghub.cn` 或
     * `runninghub.cn.example.com` 误判为可信主机。该检查要求 core/network 保留精确白名单入口，
     * 并在认证拦截器中统一通过 [com.runninghub.core.network.isTrustedRunningHubHost] 判断请求主机。
     */
    private fun Project.checkTrustedAuthHostGuard(violations: MutableList<String>) {
        val environmentFile = rootDir.resolve(
            "core/network/src/commonMain/kotlin/com/runninghub/core/network/RunningHubApiEnvironment.kt"
        )
        val interceptorFile = rootDir.resolve(
            "core/network/src/commonMain/kotlin/com/runninghub/core/network/auth/RunningHubAuthInterceptors.kt"
        )

        if (!environmentFile.isFile) {
            violations += "Trusted auth host guard source is missing: ${environmentFile.relativeTo(rootDir).invariantSeparatorsPath}."
            return
        }
        if (!interceptorFile.isFile) {
            violations += "Auth interceptor source is missing: ${interceptorFile.relativeTo(rootDir).invariantSeparatorsPath}."
            return
        }

        val environmentText = environmentFile.readText()
        val interceptorText = interceptorFile.readText()
        val requiredEnvironmentSnippets = listOf(
            "val trustedAuthHosts: Set<String>",
            "trustedAuthHosts = setOf(\"www.runninghub.cn\")",
            "internal fun isTrustedRunningHubHost(host: String): Boolean",
            "host.lowercase() in RunningHubApiEnvironment.TRUSTED_AUTH_HOSTS",
        )
        requiredEnvironmentSnippets
            .filterNot { it in environmentText }
            .forEach { snippet -> violations += "RunningHubApiEnvironment.kt must contain `$snippet`." }

        val requiredInterceptorSnippets = listOf(
            "isTrustedRunningHubHost(context.url.host)",
            "isTrustedRunningHubHost(call.request.url.host)",
        )
        requiredInterceptorSnippets
            .filterNot { it in interceptorText }
            .forEach { snippet -> violations += "RunningHubAuthInterceptors.kt must contain `$snippet`." }

        val forbiddenHostMatchingPatterns = listOf(
            Regex("""\.contains\s*\(\s*"www\.runninghub\.cn"\s*\)"""),
            Regex("""\.contains\s*\(\s*"runninghub\.cn"\s*\)"""),
            Regex("""\.endsWith\s*\(\s*"runninghub\.cn"\s*\)"""),
            Regex("""\.endsWith\s*\(\s*"\.runninghub\.cn"\s*\)"""),
        )
        listOf(environmentFile, interceptorFile).forEach { file ->
            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
            file.readLines().forEachIndexed { index, line ->
                val codeOnly = line.substringBefore("//").trim()
                if (codeOnly.isEmpty()) {
                    return@forEachIndexed
                }
                if (forbiddenHostMatchingPatterns.any { it.containsMatchIn(codeOnly) }) {
                    violations += "$relativePath:${index + 1} must use exact trustedAuthHosts matching, not suffix or contains host matching."
                }
            }
        }
    }

    /**
     * 防止 401 刷新后无条件重放非幂等请求。
     *
     * POST、上传、计费和任务提交可能已经在服务端产生部分副作用。认证拦截器可以刷新凭据，
     * 但只有安全方法或调用方显式标记可重放的请求才能自动执行第二次请求；对应测试必须保留，
     * 防止后续维护把该策略退回“所有 401 都重试一次”。
     */
    private fun Project.checkAuthReplayGuard(violations: MutableList<String>) {
        val interceptorFile = rootDir.resolve(
            "core/network/src/commonMain/kotlin/com/runninghub/core/network/auth/RunningHubAuthInterceptors.kt"
        )
        val testFile = rootDir.resolve(
            "core/network/src/commonTest/kotlin/com/runninghub/core/network/auth/RunningHubAuthInterceptorsTest.kt"
        )
        val requiredFiles = listOf(interceptorFile, testFile)
        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "Auth replay guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = interceptorFile,
            snippets = listOf(
                "markRunningHubAuthRetryAllowed",
                "canReplayAfterTokenRefresh",
                "HttpMethod.Get",
                "HttpMethod.Head",
                "HttpMethod.Options",
                "return@intercept call",
                "RunningHubExplicitAuthorizationKey",
                "hadExplicitAuthorization",
                "RunningHubExplicitCookieKey",
                "preserveExplicitCookie",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = testFile,
            snippets = listOf(
                "post unauthorized response refreshes token but does not retry without replay marker",
                "post unauthorized response retries once when request marks body replayable",
                "explicit authorization is not replaced or retried after unauthorized response",
                "explicit cookie is preserved when stored authorization is refreshed and retried",
            ),
            violations = violations,
        )
    }

    /**
     * 防止认证协议 JSON 重新退回正则解析。
     *
     * 复核要求 token 刷新响应按 JSON DTO 解码；同样，JWT payload 也是 JSON，不能用正则截取
     * `sub` 或 `exp`。该检查锁定 AuthRepository 中的 JSON 解析入口和 unicode escape 回归测试，
     * 避免认证 ID、过期时间或刷新判断在字段转义、空白和字段顺序变化时失真。
     */
    private fun Project.checkAuthJsonParsingGuard(violations: MutableList<String>) {
        val repositoryFile = rootDir.resolve(
            "feature/auth/data/src/commonMain/kotlin/com/runninghub/feature/auth/data/repository/AuthRepositoryImpl.kt"
        )
        val testFile = rootDir.resolve(
            "feature/auth/data/src/commonTest/kotlin/com/runninghub/feature/auth/data/repository/AuthRepositoryImplTest.kt"
        )
        val requiredFiles = listOf(repositoryFile, testFile)
        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "Auth JSON parsing guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = repositoryFile,
            snippets = listOf(
                "JWT_PAYLOAD_JSON.parseToJsonElement(decoded).jsonObject",
                "payload[\"sub\"]?.jsonPrimitive?.contentOrNull",
                "payload[\"exp\"]?.jsonPrimitive?.longOrNull",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = testFile,
            snippets = listOf(
                "getCurrentUserId decodes escaped JWT subject with JSON semantics",
                """{"sub":"user\u002D123","exp":4102444800}""",
            ),
            violations = violations,
        )

        val executableText = repositoryFile.readLines()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
            }
            .joinToString("\n")
        if (Regex("""\bRegex\s*\(""").containsMatchIn(executableText) || ".toRegex()" in executableText) {
            violations += "AuthRepositoryImpl.kt must parse auth JSON/JWT payload with kotlinx.serialization, not Regex/toRegex."
        }
    }

    /**
     * 防止运行环境注入退回固定生产地址。
     *
     * 复核建议要求 debug/staging/release 环境由平台启动层注入，Data 层不能长期绑定单一生产地址。
     * 仓库当前没有登记真实 staging/dev 地址，因此默认值可以回退 production；但 Android 必须保留
     * build type 的 `RUNNINGHUB_*` BuildConfig 字段，iOS 必须保留同名进程环境变量入口，
     * 这样后续只改构建参数或 Xcode scheme 就能切换环境。
     */
    private fun Project.checkRuntimeEnvironmentOverrideGuard(violations: MutableList<String>) {
        val composeBuildFile = rootDir.resolve("composeApp/build.gradle.kts")
        val androidRuntimeModule = rootDir.resolve(
            "composeApp/src/androidMain/kotlin/com/runninghub/app/di/AndroidRuntimeModule.kt"
        )
        val iosRuntimeModule = rootDir.resolve(
            "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt"
        )

        val requiredFiles = listOf(composeBuildFile, androidRuntimeModule, iosRuntimeModule)
        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "Runtime environment guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = composeBuildFile,
            snippets = listOf(
                "buildFeatures",
                "buildConfig = true",
                "runninghub.debug.webBaseUrl",
                "runninghub.release.webBaseUrl",
                "RUNNINGHUB_WEB_BASE_URL",
                "RUNNINGHUB_TRUSTED_AUTH_HOSTS",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = androidRuntimeModule,
            snippets = listOf(
                "androidApiEnvironment().also(RunningHubApiEnvironment::configure)",
                "BuildConfig.RUNNINGHUB_WEB_BASE_URL",
                "BuildConfig.RUNNINGHUB_API_BASE_URL",
                "BuildConfig.RUNNINGHUB_USER_CENTER_BASE_URL",
                "BuildConfig.RUNNINGHUB_TASK_BASE_URL",
                "BuildConfig.RUNNINGHUB_OPEN_API_V2_BASE_URL",
                "BuildConfig.RUNNINGHUB_TRUSTED_AUTH_HOSTS",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = iosRuntimeModule,
            snippets = listOf(
                "iosApiEnvironment().also(RunningHubApiEnvironment::configure)",
                "NSProcessInfo.processInfo.environment",
                "RUNNINGHUB_WEB_BASE_URL",
                "RUNNINGHUB_API_BASE_URL",
                "RUNNINGHUB_USER_CENTER_BASE_URL",
                "RUNNINGHUB_TASK_BASE_URL",
                "RUNNINGHUB_OPEN_API_V2_BASE_URL",
                "RUNNINGHUB_TRUSTED_AUTH_HOSTS",
            ),
            violations = violations,
        )
    }

    /**
     * 防止敏感凭据存储退回普通 Preferences。
     *
     * 复核建议要求 access token、refresh token、Cookie 和 API Key 不再长期保存到普通
     * DataStore/Preferences。该检查锁定平台安全存储的最小证据：commonMain 只暴露
     * [CredentialStore] 创建入口，Android actual 使用 Keystore 加密后落盘，iOS actual 使用
     * Keychain，双端运行期组合根必须通过 [MigratingCredentialStore] 从旧 Preferences 懒迁移，
     * 不能把 [CredentialStore] 直接绑定回 [PreferencesSettingsStore]。
     */
    private fun Project.checkSecureCredentialStorageGuard(violations: MutableList<String>) {
        val commonStore = rootDir.resolve(
            "core/storage/src/commonMain/kotlin/com/runninghub/core/storage/SecureCredentialStore.kt"
        )
        val androidStore = rootDir.resolve(
            "core/storage/src/androidMain/kotlin/com/runninghub/core/storage/AndroidSecureCredentialStore.kt"
        )
        val iosStore = rootDir.resolve(
            "core/storage/src/iosMain/kotlin/com/runninghub/core/storage/IosKeychainCredentialStore.kt"
        )
        val androidRuntimeModule = rootDir.resolve(
            "composeApp/src/androidMain/kotlin/com/runninghub/app/di/AndroidRuntimeModule.kt"
        )
        val iosRuntimeModule = rootDir.resolve(
            "composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt"
        )
        val migrationTest = rootDir.resolve(
            "core/storage/src/commonTest/kotlin/com/runninghub/core/storage/MigratingCredentialStoreTest.kt"
        )

        val requiredFiles = listOf(
            commonStore,
            androidStore,
            iosStore,
            androidRuntimeModule,
            iosRuntimeModule,
            migrationTest,
        )
        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "Secure credential storage guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = commonStore,
            snippets = listOf(
                "expect fun createSecureCredentialStore(): CredentialStore",
                "class MigratingCredentialStore",
                "private val migrationMutex = Mutex()",
                "legacy?.clearAuthToken()",
                "legacy?.clearRefreshToken()",
                "legacy?.clearCookie()",
                "legacy?.clearApiKey()",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = androidStore,
            snippets = listOf(
                "actual fun createSecureCredentialStore(): CredentialStore",
                "AndroidSecureCredentialStore",
                "AndroidKeyStore",
                "KeyGenParameterSpec.Builder",
                "AES/GCM/NoPadding",
                "GCMParameterSpec",
                "SharedPreferences 只用于保存密文载荷",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = iosStore,
            snippets = listOf(
                "actual fun createSecureCredentialStore(): CredentialStore",
                "IosKeychainCredentialStore",
                "SecItemAdd",
                "SecItemCopyMatching",
                "SecItemDelete",
                "kSecClassGenericPassword",
                "kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly",
            ),
            violations = violations,
        )
        listOf(androidRuntimeModule, iosRuntimeModule).forEach { runtimeModule ->
            requireFileSnippets(
                file = runtimeModule,
                snippets = listOf(
                    "single<CredentialStore>",
                    "MigratingCredentialStore(",
                    "primary = createSecureCredentialStore()",
                    "legacy = get<PreferencesSettingsStore>()",
                    "single<BalanceCache> { get<PreferencesSettingsStore>() }",
                    "single<QuickCreateDraftStore> { get<PreferencesSettingsStore>() }",
                ),
                violations = violations,
            )

            val executableText = runtimeModule.readText()
                .lines()
                .filterNot { line ->
                    val trimmed = line.trim()
                    trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
                }
                .joinToString("\n")
            if (Regex("""single\s*<\s*CredentialStore\s*>\s*\{\s*get\s*<\s*PreferencesSettingsStore\s*>\s*\(\s*\)\s*\}""")
                    .containsMatchIn(executableText)
            ) {
                violations += "${runtimeModule.relativeTo(rootDir).invariantSeparatorsPath} must bind CredentialStore through MigratingCredentialStore and createSecureCredentialStore()."
            }
        }
        requireFileSnippets(
            file = migrationTest,
            snippets = listOf(
                "getAuthToken migrates legacy token into primary store",
                "setCookie writes primary store and clears legacy value",
                "clearAll clears credentials without invoking legacy destructive clear",
                "assertFalse(legacy.clearAllCalled)",
            ),
            violations = violations,
        )
    }

    /**
     * 校验已迁移 Data 模块不会把服务端 msg 作为异常或任务状态消息外泄。
     *
     * 复核要求服务端 `msg` 不能直接成为最终 UI 文案。已迁移 Feature Data 可以把 `.msg`
     * 用于错误分类或结构化诊断字段，但不能直接放入 `check`、`throw`、Unknown 错误构造，
     * 也不能作为 QuickCreate 任务状态错误消息向 Presentation 传播。Presentation 应只接收
     * 稳定错误码或由 Domain 明确定义的结构化错误。
     */
    private fun Project.checkMigratedDataErrorMessageGuard(violations: MutableList<String>) {
        val featureRoot = rootDir.resolve("feature")
        val roots = featureRoot
            .takeIf { it.isDirectory }
            ?.listFiles()
            ?.map { it.resolve("data/src/commonMain/kotlin") }
            ?.filter { it.isDirectory }
            .orEmpty()
        val forbiddenPatterns = listOf(
            "check failure message uses remote msg" to Regex(
                """check\s*\([^)]*\.code\s*==\s*0[^)]*\)\s*\{[^}]*\.msg""",
                RegexOption.DOT_MATCHES_ALL,
            ),
            "throw uses remote msg" to Regex("""throw\s+(?!mapSmsError\s*\()[^\r\n]*\.msg\b"""),
            "Unknown error stores raw msg" to Regex("""(?:AuthError|SmsError)\.Unknown\s*\(\s*msg\s*\)"""),
            "task status exposes remote message" to Regex(
                """QuickCreateTaskStatus\.(?:Error|Failed)\s*\([^)\r\n]*(?:\.msg\b|\.message\b|errorMessage\b|e\.message\b)"""
            ),
        )

        roots
            .asSequence()
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" } }
            .forEach { file ->
                val text = file.readText()
                forbiddenPatterns
                    .filter { (_, pattern) -> pattern.containsMatchIn(text) }
                    .forEach { (label, _) ->
                        violations += "${file.relativeTo(rootDir).invariantSeparatorsPath} must not expose service response msg directly: $label."
                    }
            }
    }

    /**
     * 防止 iOS 权限和媒体选择重新退回占位实现。
     *
     * 复核要求 iOS 不得用固定 `GRANTED`、空 picker 或空设置跳转替代真实平台能力。该检查只锁定
     * 可静态证明的关键实现点：PhotoKit 授权回调、图片/视频选择器、音频文件选择器、设置页跳转、
     * iOS 权限轨迹持久化、Info.plist 权限说明和安全作用域 URL 读取。
     * 真实 Simulator/真机上传回归仍需外部证据。
     */
    private fun Project.checkIosPermissionAndMediaGuard(violations: MutableList<String>) {
        val permissionController = rootDir.resolve(
            "composeApp/src/iosMain/kotlin/com/runninghub/app/platform/PermissionController.ios.kt"
        )
        val permissionStore = rootDir.resolve(
            "core/storage/src/iosMain/kotlin/com/runninghub/core/storage/PermissionDataStoreImpl.ios.kt"
        )
        val mediaResolver = rootDir.resolve(
            "composeApp/src/iosMain/kotlin/com/runninghub/app/platform/MediaResolver.ios.kt"
        )
        val infoPlist = rootDir.resolve("iosApp/iosApp/Info.plist")
        val privacyManifest = rootDir.resolve("iosApp/iosApp/PrivacyInfo.xcprivacy")
        val xcodeProject = rootDir.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")

        val requiredFiles = listOf(permissionController, permissionStore, mediaResolver, infoPlist, privacyManifest, xcodeProject)
        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "iOS permission/media guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = permissionController,
            snippets = listOf(
                "PHPhotoLibrary.authorizationStatus()",
                "PHPhotoLibrary.requestAuthorization",
                "PHAuthorizationStatusLimited",
                "UIImagePickerController()",
                "UIDocumentPickerViewController",
                "UIApplicationOpenSettingsURLString",
                "scope.launch { permissionStateStore.markGranted",
                "scope.launch { permissionStateStore.markPermanentlyDenied",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = permissionStore,
            snippets = listOf(
                "NSUserDefaults.standardUserDefaults",
                "readPermissionSet",
                "persistState()",
                "permissionDataStoreInstance",
                "PermissionStatus.UNKNOWN",
                "markPermanentlyDenied",
                "mutex.withLock",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = mediaResolver,
            snippets = listOf(
                "startAccessingSecurityScopedResource()",
                "stopAccessingSecurityScopedResource()",
                "NSData.dataWithContentsOfURL",
                "NSFileManager.defaultManager.attributesOfItemAtPath",
            ),
            violations = violations,
        )
        val mediaResolverText = mediaResolver.readText()
        listOf("Invalid URI: \$uri", "Cannot read data from \$uri")
            .filter { it in mediaResolverText }
            .forEach { snippet ->
                violations += "MediaResolver.ios.kt must not include local media URI in exception message: `$snippet`."
            }
        requireFileSnippets(
            file = infoPlist,
            snippets = listOf(
                "NSPhotoLibraryUsageDescription",
                "NSCameraUsageDescription",
                "NSMicrophoneUsageDescription",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = privacyManifest,
            snippets = listOf(
                "NSPrivacyAccessedAPITypes",
                "NSPrivacyAccessedAPICategoryUserDefaults",
                "CA92.1",
                "NSPrivacyAccessedAPICategoryFileTimestamp",
                "C617.1",
                "NSPrivacyCollectedDataTypes",
                "NSPrivacyTracking",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = xcodeProject,
            snippets = listOf(
                "PrivacyInfo.xcprivacy",
                "PrivacyInfo.xcprivacy in Resources",
            ),
            violations = violations,
        )

        val forbiddenAlwaysGrantedPatterns = listOf(
            Regex("""checkAndRequest\s*\([^)]*\)\s*\{[^}]*onGranted\s*\(\s*\)""", RegexOption.DOT_MATCHES_ALL),
            Regex("""getCurrentStatus\s*\([^)]*\)[^{=]*=\s*PermissionStatus\.GRANTED"""),
            Regex("""override\s+suspend\s+fun\s+markGranted\s*\([^)]*\)\s*\{\s*\}"""),
            Regex("""override\s+fun\s+pickMedia\s*\([^)]*\)\s*\{\s*\}""", RegexOption.DOT_MATCHES_ALL),
            Regex("""override\s+fun\s+openAppSettings\s*\(\s*\)\s*\{\s*\}"""),
        )
        listOf(permissionController, permissionStore).forEach { file ->
            val text = file.readText()
            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
            forbiddenAlwaysGrantedPatterns
                .filter { it.containsMatchIn(text) }
                .forEach { violations += "$relativePath must not contain placeholder iOS permission behavior matched by `${it.pattern}`." }
        }
    }

    /**
     * 防止短信图形验证码 Web 容器退回未验证路径。
     *
     * 复核要求 iOS WKWebView 验证码单独验证 token 回传、关闭回调、handler 清理、失败降级和
     * Android/iOS 行为一致性。真实 TAC 资源加载仍需运行环境验证；这里锁定可本地静态证明的
     * 包装层协议，避免后续删除 bridge、scheme 兜底或销毁清理逻辑。
     */
    private fun Project.checkSmsCaptchaGuard(violations: MutableList<String>) {
        val commonHtml = rootDir.resolve(
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaHtml.kt"
        )
        val callbackParser = rootDir.resolve(
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaCallback.kt"
        )
        val androidDialog = rootDir.resolve(
            "composeApp/src/androidMain/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaDialog.android.kt"
        )
        val iosDialog = rootDir.resolve(
            "composeApp/src/iosMain/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaDialog.ios.kt"
        )
        val htmlTest = rootDir.resolve(
            "composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaHtmlTest.kt"
        )
        val callbackTest = rootDir.resolve(
            "composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaCallbackTest.kt"
        )

        val requiredFiles = listOf(commonHtml, callbackParser, androidDialog, iosDialog, htmlTest, callbackTest)
        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "SMS captcha guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = commonHtml,
            snippets = listOf(
                "window.webkit.messageHandlers[bridgeName].postMessage('token:' + callbackToken)",
                "window.webkit.messageHandlers[bridgeName].postMessage('close')",
                "token = callbackToken;",
                "\$tokenCallbackExpression;",
                "\$closeCallbackExpression;",
                "window.__captchaScriptTimer = window.setTimeout",
                "window.__captchaWatchdog = window.setTimeout",
                "oldScript.parentNode.removeChild(oldScript)",
                "function extractValidToken(res)",
                "图形验证脚本加载失败，请点击重试",
                "图形验证图片加载失败，请点击重试",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = callbackParser,
            snippets = listOf(
                "internal sealed interface SmsCaptchaCallback",
                "data class Token(val value: String?)",
                "data object Close",
                "decodePercentEncoded",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = androidDialog,
            snippets = listOf(
                "settings.javaScriptEnabled = true",
                "webViewClient = SmsCaptchaWebViewClient(bridge)",
                "webView.destroy()",
                "handleSmsCaptchaCallbackUrl",
            ),
            violations = violations,
        )
        val androidExecutableText = androidDialog.readText()
            .lines()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
            }
            .joinToString("\n")
        if ("addJavascriptInterface" in androidExecutableText) {
            violations += "Android SMS captcha WebView must not expose addJavascriptInterface to TAC HTML."
        }
        requireFileSnippets(
            file = iosDialog,
            snippets = listOf(
                "WKWebViewConfiguration",
                "addScriptMessageHandler",
                "removeScriptMessageHandlerForName(CAPTCHA_BRIDGE_NAME)",
                "webView.navigationDelegate = null",
                "WKNavigationActionPolicy.WKNavigationActionPolicyCancel",
                "dispatch_async(dispatch_get_main_queue())",
                "message.startsWith(\"token:\")",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = htmlTest,
            snippets = listOf(
                "captcha html delegates challenge loading to tac",
                "captcha success extracts token before native callback",
                "captcha html reports script load failure and timeout",
                "captcha html clears stale script and timers before retry",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = callbackTest,
            snippets = listOf(
                "token callback url extracts encoded token",
                "token callback url maps blank value to null token",
                "close callback url maps to close event",
                "unknown callback url is ignored",
            ),
            violations = violations,
        )
    }

    /**
     * 拦截明显会泄露敏感凭据的日志语句。
     *
     * 该检查只扫描生产 Kotlin 源码中的日志调用行，避免把 KDoc、测试 fixture 或正常字段命名误判为泄露。
     * 如果确实需要排查认证问题，应记录脱敏后的状态码、请求类别或哈希摘要，而不是原始凭据。
     */
    private fun Project.checkSensitiveLogging(violations: MutableList<String>) {
        val sourceRoots = listOf("composeApp", "core", "feature", "shared")
            .map { rootDir.resolve(it) }
            .filter { it.isDirectory }
        val productionSegments = listOf(
            "/src/commonMain/kotlin/",
            "/src/androidMain/kotlin/",
            "/src/iosMain/kotlin/",
        )
        val loggingMarkers = listOf("Log.", "println(", "Napier.", "Logger.", "debug(")
        val sensitiveTerms = listOf(
            "token",
            "cookie",
            "authorization",
            "apikey",
            "api key",
            "password",
            "密码",
            "url",
            "filename",
        )

        sourceRoots
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .filter { file ->
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                productionSegments.any { it in "/$relativePath" }
            }
            .forEach { file ->
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                        return@forEachIndexed
                    }
                    val normalized = trimmed.lowercase()
                    val logs = loggingMarkers.any { it in trimmed }
                    val containsSensitiveTerm = sensitiveTerms.any { it in normalized }
                    if (logs && containsSensitiveTerm) {
                        violations += "$relativePath:${index + 1} logs a sensitive credential term; use redacted structured logging."
                    }
                }
            }
    }

    /**
     * 防止快捷创作上传等待错误重新暴露本地媒体文件名。
     *
     * 媒体卡片可以展示用户选择的 displayName，但生成提交前的上传失败/超时错误可能进入
     * 页面全局错误、崩溃上报或支持截图。该检查禁止 Coordinator 把 displayName 拼进异常消息，
     * 与 iOS MediaResolver 的 URI 脱敏策略保持一致。
     */
    private fun Project.checkMediaUploadErrorPrivacyGuard(violations: MutableList<String>) {
        val coordinator = rootDir.resolve(
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateMediaUploadCoordinator.kt"
        )
        if (!coordinator.isFile) {
            violations += "QuickCreate media upload coordinator is missing."
            return
        }

        val text = coordinator.readText()
        val forbiddenSnippets = listOf(
            "素材上传失败: \${",
            "素材上传超时: \${",
            "joinToString { it.displayName }",
            "素材上传失败: \$",
            "素材上传超时: \$",
        )
        forbiddenSnippets
            .filter { it in text }
            .forEach { snippet -> violations += "QuickCreateMediaUploadCoordinator.kt must not expose media displayName in upload error message: `$snippet`." }

        listOf("MEDIA_UPLOAD_FAILED_MESSAGE", "MEDIA_UPLOAD_TIMEOUT_MESSAGE")
            .filterNot { it in text }
            .forEach { snippet -> violations += "QuickCreateMediaUploadCoordinator.kt must use `$snippet` for sanitized upload errors." }
    }

    /**
     * 校验 API DTO 已进入契约测试基线。
     *
     * 新增 DTO、Request 或 Response 时，必须同时登记覆盖它的契约测试入口。门禁不强制
     * 一文件一测试，但基线中的测试文件必须存在且包含真实测试标记，从而避免协议模型只增加实现、
     * 或只登记空壳测试文件而没有可执行验证。
     */
    private fun Project.checkApiContractBaseline(violations: MutableList<String>) {
        val baselineFile = rootDir.resolve("docs/governance/api-contract-test-baseline.txt")
        val entries = readPipeSeparatedBaseline(
            file = baselineFile,
            expectedColumns = 3,
            violations = violations,
        )
        val trackedDtos = entries.map { it[0] }.toSet()

        val dtoFiles = listOf("core", "feature", "shared", "composeApp")
            .map { rootDir.resolve(it) }
            .filter { it.isDirectory }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filter { file ->
                        val name = file.name
                        name.endsWith("Dto.kt") || name.endsWith("Request.kt") || name.endsWith("Response.kt")
                    }
                    .filter { file ->
                        val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                        "/src/commonMain/kotlin/" in "/$relativePath"
                    }
                    .toList()
            }
            .map { it.relativeTo(rootDir).invariantSeparatorsPath }
            .toSet()

        dtoFiles
            .filterNot { it in trackedDtos }
            .forEach { dtoPath -> violations += "$dtoPath must be listed in api-contract-test-baseline.txt." }

        entries.forEach { entry ->
            val dtoPath = entry[0]
            val testPath = entry[1]
            val scope = entry[2]
            if (!rootDir.resolve(dtoPath).isFile) {
                violations += "API contract baseline points to a missing DTO file: $dtoPath."
            }
            val testFile = rootDir.resolve(testPath)
            if (scope.isBlank()) {
                violations += "API contract baseline for $dtoPath must explain the covered contract scope."
            }
            if (!testPath.contains("/src/") || !testPath.contains("Test/")) {
                violations += "API contract baseline for $dtoPath must point to a test source set: $testPath."
            }
            if (!testFile.isFile) {
                violations += "API contract baseline for $dtoPath points to a missing test file: $testPath."
            } else if ("@Test" !in testFile.readText()) {
                // 只检查最低可执行入口，不把测试命名和断言风格绑死在治理插件里。
                violations += "API contract baseline for $dtoPath points to $testPath without any @Test entry."
            }
        }
    }

    /**
     * 校验核心登录 DTO 保留真实字段样本契约。
     *
     * 复核要求核心登录、计费、提交和历史等协议模型不能只靠宽松 JSON 配置“能解就行”。
     * Auth 登录响应包含敏感 token 字段，必须用样本测试锁定 `access_token`、`refresh_token`
     * 和 JSON escape 解码语义，避免字段名或解析策略漂移后只在运行时暴露。
     */
    private fun Project.checkCoreAuthContractSamples(violations: MutableList<String>) {
        val authMapperTest = rootDir.resolve(
            "feature/auth/data/src/commonTest/kotlin/com/runninghub/feature/auth/data/remote/dto/AuthMappersTest.kt"
        )
        if (!authMapperTest.isFile) {
            violations += "Auth contract sample test is missing: ${authMapperTest.relativeTo(rootDir).invariantSeparatorsPath}."
            return
        }

        requireFileSnippets(
            file = authMapperTest,
            snippets = listOf(
                "login token response decodes captured server field names",
                "\"access_token\": \"access\\u002Dtoken\"",
                "\"refresh_token\": \"refresh\\u002Dtoken\"",
                "traceId",
            ),
            violations = violations,
        )
    }

    /**
     * 校验 SQLDelight schema 已进入数据库契约基线。
     *
     * 当前迁移期只允许已登记的历史 Discovery cache schema 以 `legacy-debt` 标注缺口；
     * 所有新增 `.sq` / `.sqm` 文件都必须登记真实测试入口，防止数据库结构变化只停留在实现层。
     */
    private fun Project.checkDatabaseContractBaseline(violations: MutableList<String>) {
        val allowedLegacyDebtSchemas = setOf(
            "shared/src/commonMain/sqldelight/com/runninghub/shared/db/DiscoveryCache.sq",
        )
        val baselineFile = rootDir.resolve("docs/governance/database-contract-test-baseline.txt")
        val entries = readPipeSeparatedBaseline(
            file = baselineFile,
            expectedColumns = 3,
            violations = violations,
        )
        val trackedSchemas = entries.map { it[0] }.toSet()

        val schemaFiles = listOf("core", "feature", "shared", "composeApp")
            .map { rootDir.resolve(it) }
            .filter { it.isDirectory }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && (it.extension == "sq" || it.extension == "sqm") }
                    .toList()
            }
            .map { it.relativeTo(rootDir).invariantSeparatorsPath }
            .toSet()

        schemaFiles
            .filterNot { it in trackedSchemas }
            .forEach { schemaPath -> violations += "$schemaPath must be listed in database-contract-test-baseline.txt." }

        entries.forEach { entry ->
            val schemaPath = entry[0]
            val testPath = entry[1]
            if (!rootDir.resolve(schemaPath).isFile) {
                violations += "Database contract baseline points to a missing schema file: $schemaPath."
            }
            if (testPath == "legacy-debt" && schemaPath !in allowedLegacyDebtSchemas) {
                violations += "$schemaPath must use a real database contract or migration test; legacy-debt is only allowed for existing shared DiscoveryCache."
            }
            if (testPath != "legacy-debt" && !rootDir.resolve(testPath).isFile) {
                violations += "Database contract baseline for $schemaPath points to a missing test file: $testPath."
            }
        }
    }

    /**
     * 校验 UI 与 Presentation 硬编码文案没有继续增长。
     *
     * 复核建议要求用户可见文案迁移到 Compose Resources。既有页面存在大量历史硬编码，
     * 因此这里采用文件级基线：新增硬编码文案文件或超过登记数量都会失败；减少硬编码数量不会失败。
     * 独立 Presentation 模块虽然不含 Compose 节点，但会集中生成错误、按钮和状态文案，也必须纳入基线。
     */
    private fun Project.checkUiCopyBaseline(violations: MutableList<String>) {
        val baselineFile = rootDir.resolve("docs/governance/ui-copy-hardcoded-baseline.txt")
        val entries = readPipeSeparatedBaseline(
            file = baselineFile,
            expectedColumns = 3,
            violations = violations,
        )
        val baseline = entries.associate { entry ->
            entry[0] to (entry[1].toIntOrNull() ?: 0)
        }
        entries
            .filter { it[1].toIntOrNull() == null }
            .forEach { entry -> violations += "UI copy baseline for ${entry[0]} must use a numeric maxMatches value." }

        val composeUiRoot = rootDir.resolve("composeApp/src/commonMain/kotlin")
        if (!composeUiRoot.isDirectory) {
            violations += "composeApp commonMain source directory is missing for UI copy scan."
            return
        }

        val hardcodedCopyPatterns = listOf(
            "Text\\(\"".toRegex(),
            "text\\s*=\\s*\"".toRegex(),
            "errorMessage\\s*=\\s*\"".toRegex(),
            "placeholder\\s*=\\s*\\{\\s*Text\\(\"".toRegex(),
            "label\\s*=\\s*\\{\\s*Text\\(\"".toRegex(),
            "confirmText\\s*=\\s*\"".toRegex(),
            "title\\s*=\\s*\\{\\s*Text\\(\"".toRegex(),
        )
        val presentationCopyPatterns = listOf(
            "\"[^\"]*\\p{IsHan}[^\"]*\"".toRegex(),
        )

        checkUiCopyRoot(
            root = composeUiRoot,
            patterns = hardcodedCopyPatterns,
            baseline = baseline,
            violations = violations,
        )

        val featureRoot = rootDir.resolve("feature")
        if (featureRoot.isDirectory) {
            featureRoot.walkTopDown()
                .filter { file ->
                    file.isDirectory &&
                        file.relativeTo(rootDir).invariantSeparatorsPath.endsWith("presentation/src/commonMain/kotlin")
                }
                .forEach { presentationRoot ->
                    checkUiCopyRoot(
                        root = presentationRoot,
                        patterns = presentationCopyPatterns,
                        baseline = baseline,
                        violations = violations,
                    )
                }
        }

        baseline.keys
            .filterNot { rootDir.resolve(it).isFile }
            .forEach { missingPath -> violations += "UI copy baseline points to a missing file: $missingPath." }
    }

    /**
     * 按文件级基线扫描一个 UI 文案目录。
     *
     * Compose 页面使用结构化模式，Presentation 模块使用中文字符串字面量模式；两者共享同一个
     * baseline 文件，便于后续 Compose Resources 迁移时逐步下调具体文件的允许数量。
     */
    private fun Project.checkUiCopyRoot(
        root: java.io.File,
        patterns: List<Regex>,
        baseline: Map<String, Int>,
        violations: MutableList<String>,
    ) {
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                val matchCount = file.readLines().count { line ->
                    patterns.any { pattern -> pattern.containsMatchIn(line) }
                }
                if (matchCount == 0) {
                    return@forEach
                }

                val allowedCount = baseline[relativePath]
                when {
                    allowedCount == null -> {
                        violations += "$relativePath has $matchCount hardcoded UI copy matches; use Compose Resources or register a baseline."
                    }
                    matchCount > allowedCount -> {
                        violations += "$relativePath hardcoded UI copy matches grew from allowed $allowedCount to $matchCount."
                    }
                }
            }
    }

    /**
     * 校验性能基线目标覆盖长期维护关注点。
     *
     * 这里不伪造性能数据，只要求仓库保留可审查的指标、平台、证据产物和负责人字段；
     * 真机或 macOS 数据仍需要在具备设备/runner 后按该表补充。
     */
    private fun Project.checkPerformanceBaselineTargets(violations: MutableList<String>) {
        val baselineFile = rootDir.resolve("docs/governance/performance-baseline-targets.txt")
        val entries = readPipeSeparatedBaseline(
            file = baselineFile,
            expectedColumns = 4,
            violations = violations,
        )
        val requiredMetrics = setOf(
            "cold-start",
            "first-render",
            "steady-memory",
            "image-cache",
            "video-preview",
            "long-polling-network-idle",
        )
        val metrics = entries.map { it[0] }.toSet()
        requiredMetrics
            .filterNot { it in metrics }
            .forEach { metric -> violations += "performance-baseline-targets.txt must define metric `$metric`." }

        entries.forEach { entry ->
            val metric = entry[0]
            val platform = entry[1]
            val evidence = entry[2]
            val owner = entry[3]
            if (platform !in setOf("android", "ios", "android-ios")) {
                violations += "Performance metric $metric uses unsupported platform `$platform`."
            }
            if (evidence == "none") {
                violations += "Performance metric $metric must define an evidence artifact or collection command."
            } else {
                validatePerformanceEvidence(
                    metric = metric,
                    evidence = evidence,
                    violations = violations,
                )
            }
            if (owner == "none") {
                violations += "Performance metric $metric must define an owner or responsible area."
            }
        }
    }

    /**
     * 校验性能目标表中的证据路径可追溯。
     *
     * 性能数据本身需要设备或 CI 采集，本地门禁不能伪造通过；但目标表必须指向仓库内真实文档、
     * 脚本或采集产物。带 `#anchor` 的 Markdown 证据还必须保留对应章节，避免指标目标漂移成死链接。
     */
    private fun Project.validatePerformanceEvidence(
        metric: String,
        evidence: String,
        violations: MutableList<String>,
    ) {
        val path = evidence.substringBefore('#')
        val anchor = evidence.substringAfter('#', missingDelimiterValue = "")
        val evidenceFile = rootDir.resolve(path)
        if (!evidenceFile.isFile) {
            violations += "Performance metric $metric points to missing evidence path: $path."
            return
        }

        if (anchor.isNotBlank()) {
            val heading = Regex("""^#{1,6}\s+$anchor\s*$""", RegexOption.MULTILINE)
            val explicitAnchor = Regex("""<a\s+id=["']$anchor["']""")
            val text = evidenceFile.readText()
            if (!heading.containsMatchIn(text) && !explicitAnchor.containsMatchIn(text)) {
                violations += "Performance metric $metric points to missing Markdown anchor `$anchor` in $path."
            }
        }
    }

    /**
     * 校验 Android Release 构建治理没有退化。
     *
     * 复核建议要求发布前开启 R8/minify、资源压缩并维护 keep 规则。该检查只锁定仓库内可静态
     * 证明的发布配置：release build type 必须启用压缩并引用项目 ProGuard 文件，L1 Android
     * 聚合任务必须继续执行 assembleRelease，ProGuard 规则必须覆盖 Ktor、kotlinx.serialization、
     * Koin 和常见日志 facade。真实签名、安装回归和商店上传仍由发布清单的人审步骤确认。
     */
    private fun Project.checkAndroidReleaseBuildGuard(violations: MutableList<String>) {
        val composeBuildFile = rootDir.resolve("composeApp/build.gradle.kts")
        val rootBuildFile = rootDir.resolve("build.gradle.kts")
        val proguardFile = rootDir.resolve("composeApp/proguard-rules.pro")
        val requiredFiles = listOf(composeBuildFile, rootBuildFile, proguardFile)

        requiredFiles
            .filterNot { it.isFile }
            .forEach { file -> violations += "Android release governance source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }
        if (requiredFiles.any { !it.isFile }) {
            return
        }

        requireFileSnippets(
            file = composeBuildFile,
            snippets = listOf(
                "release {",
                "isMinifyEnabled = true",
                "isShrinkResources = true",
                "getDefaultProguardFile(\"proguard-android-optimize.txt\")",
                "\"proguard-rules.pro\"",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = rootBuildFile,
            snippets = listOf(
                "tasks.register(\"verifyL1Android\")",
                "\":composeApp:assembleRelease\"",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = proguardFile,
            snippets = listOf(
                "-keep class io.ktor.** { *; }",
                "-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod",
                "-keep,includedescriptorclasses class com.runninghub.**\$\$serializer { *; }",
                "-keep class kotlin.Metadata { *; }",
                "-keep class org.koin.** { *; }",
                "-dontwarn org.slf4j.**",
            ),
            violations = violations,
        )
    }

    /**
     * 校验发布清单保留人工确认边界。
     *
     * 复核建议要求建立 App Store/TestFlight 与 Android Release 自动化流水线，但商店上传、
     * 生产签名和敏感配置不能由本地门禁擅自执行；该检查确保清单持续声明这些停止条件。
     */
    private fun Project.checkReleaseReadinessChecklist(violations: MutableList<String>) {
        val checklist = rootDir.resolve("docs/governance/release-readiness-checklist.md")
        if (!checklist.isFile) {
            return
        }
        val text = checklist.readText()
        val requiredItems = listOf(
            "- [ ] `:composeApp:assembleRelease` 已通过",
            "- [ ] Android Release 安装回归已完成",
            "- [ ] macOS Xcode 构建已通过",
            "- [ ] 图片、视频和音频上传使用真实 iOS 权限/选择器路径回归通过",
            "- [ ] 短信图形验证码在 Android WebView 和 iOS WKWebView 上完成 token/关闭/失败重试冒烟",
            "- [ ] 首次请求照片权限：允许、拒绝、有限照片权限三种路径均记录结果。",
            "- [ ] 已拒绝照片权限：再次选择图片或视频时不进入成功回调，并能跳转系统设置页。",
            "- [ ] 图片选择：系统选择器返回的 URI 可被上传链路读取，取消选择不会污染页面状态。",
            "- [ ] 音频选择：文档选择器导入的文件可读取字节和文件名，取消选择不会污染页面状态。",
            "- [ ] 成功验证后能回传 `validToken`，且 token 不进入日志、崩溃报告或截图说明。",
            "- [ ] 连续打开两次验证码时，第二次不会收到第一次的回调。",
            "- [ ] ATS、Cookie、同源策略和跨域请求行为已在真实环境记录结论。",
            "- [ ] `PrivacyInfo.xcprivacy` 已随 iOS target 打包，并声明 UserDefaults 与文件元数据访问原因",
            "- [ ] iOS TestFlight 上传前人工确认账号、证书和隐私表单",
            "- [ ] major 级依赖、Gradle、AGP、Kotlin、Xcode 或运行时 SDK 升级已列出 Android/iOS 人工回归矩阵和负责人。",
            "- [ ] 重大版本升级不得自动合并，必须由负责人确认双端回归结果后再发布。",
            "- [ ] 生产签名、商店上传和敏感配置变更不得自动执行",
        )

        requiredItems
            .filterNot { it in text }
            .forEach { item -> violations += "release-readiness-checklist.md must contain `$item`." }
    }

    /**
     * 校验依赖升级治理没有从长期门禁中脱落。
     *
     * 复核建议要求使用 Dependabot/Renovate 定期升级依赖，并用 Dependency Submission
     * 支撑 GitHub Dependency Graph 与 Dependabot Alerts。这里锁定仓库已选择的 Dependabot
     * 方案：Gradle 与 GitHub Actions 依赖必须每周巡检；Dependency Submission workflow
     * 只在 push、schedule 或人工触发时写入依赖图，不能在不可信 PR 上申请 `contents: write`。
     * 重大版本升级是否合入仍由 PR 模板和人工回归确认，本检查不自动批准依赖变更。
     */
    private fun Project.checkDependencyMaintenanceGuard(violations: MutableList<String>) {
        val dependabotFile = rootDir.resolve(".github/dependabot.yml")
        val pullRequestTemplate = rootDir.resolve(".github/pull_request_template.md")
        val dependencySubmissionWorkflow = rootDir.resolve(".github/workflows/dependency-submission.yml")

        if (!dependabotFile.isFile) {
            violations += "Dependabot configuration is missing at .github/dependabot.yml."
        } else {
            requireFileSnippets(
                file = dependabotFile,
                snippets = listOf(
                    "version: 2",
                    "package-ecosystem: \"gradle\"",
                    "package-ecosystem: \"github-actions\"",
                    "interval: \"weekly\"",
                    "open-pull-requests-limit: 5",
                ),
                violations = violations,
            )
        }

        if (!pullRequestTemplate.isFile) {
            violations += "Pull request template is missing at .github/pull_request_template.md."
        } else {
            requireFileSnippets(
                file = pullRequestTemplate,
                snippets = listOf(
                    "依赖升级与重大版本人工回归",
                    "本 PR 不包含依赖或构建工具升级",
                    "major 级依赖、Gradle、AGP、Kotlin、Xcode 或运行时 SDK 升级",
                    "本 PR 不启用自动合并",
                    "重大版本升级必须人工确认 Android/iOS 回归结果",
                ),
                violations = violations,
            )
        }

        if (!dependencySubmissionWorkflow.isFile) {
            violations += "Dependency Submission workflow is missing at .github/workflows/dependency-submission.yml."
            return
        }

        requireFileSnippets(
            file = dependencySubmissionWorkflow,
            snippets = listOf(
                "name: Dependency Submission",
                "contents: write",
                "schedule:",
                "workflow_dispatch:",
                "actions/setup-java@v5",
                "java-version: \"17\"",
                "gradle/actions/dependency-submission@v6",
                "dependency-graph: generate-and-submit",
            ),
            violations = violations,
        )

        val workflowText = dependencySubmissionWorkflow.readText()
        if (Regex("""(?m)^\s+pull_request\s*:""").containsMatchIn(workflowText)) {
            violations += "Dependency Submission workflow must not run on pull_request because it needs contents: write."
        }
    }

    /**
     * 读取 `|` 分隔的治理基线文件。
     *
     * 基线文件允许注释和空行；非注释行必须满足固定列数，避免后续协作者写入无法被机器检查的自由文本。
     */
    private fun Project.readPipeSeparatedBaseline(
        file: java.io.File,
        expectedColumns: Int,
        violations: MutableList<String>,
    ): List<List<String>> {
        if (!file.isFile) {
            return emptyList()
        }

        return file.readLines()
            .mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    null
                } else {
                    val parts = trimmed.split('|').map { it.trim() }
                    if (parts.size != expectedColumns || parts.any { it.isEmpty() }) {
                        violations += "${file.relativeTo(rootDir).invariantSeparatorsPath}:${index + 1} must have $expectedColumns non-empty pipe-separated columns."
                        null
                    } else {
                        parts
                    }
                }
            }
    }
}
