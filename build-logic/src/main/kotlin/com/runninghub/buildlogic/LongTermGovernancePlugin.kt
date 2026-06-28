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
                            "checkMojibakeTextGuard",
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
                            "checkRetiredCreateGuard",
                            "checkHistoryCompatibilityBridgeGuard",
                            "checkQuickCreateLegacyImplementationGuard",
                            "checkQuickCreateScreenModelFacadeGuard",
                            "checkQuickCreateTaskStatusTextGuard",
                            "checkTaskHistoryPresentationTextGuard",
                            "checkUiCopyBaseline",
                            "checkQuickCreateDomainModelTextGuard",
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
                        snippets = listOf("filePath|maxLines|reason", "build.gradle.kts|1777"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/chinese-commenting.md",
                        snippets = listOf("中文注释", "KDoc", "字段级注释", "TODO", "checkLongTermGovernance", "修改完成检查"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/feature-presentation-thresholds.txt",
                        snippets = listOf(
                            "feature|maxLines|ownerPresentation|reason",
                            "history|",
                            "feature:task:presentation",
                            "feature:auth:presentation",
                            "feature:community:presentation",
                        ),
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
                        snippets = listOf("schemaPath|testPath|scope", "legacy-debt"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/ui-copy-resources.md",
                        snippets = listOf("Compose Resources", "硬编码", "Data 层"),
                        violations = violations,
                    )
                    requireDocumentSnippets(
                        relativePath = "docs/governance/ui-copy-hardcoded-baseline.txt",
                        snippets = listOf("filePath|maxMatches|reason", "当前无正数基线"),
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
                    checkRetiredSharedCurrentDocsGuard(violations)
                    checkRootBuildScriptSize(violations)
                    checkLegacyAndroidAppGuard(violations)
                    checkProductionTodoGuard(violations)
                    checkMojibakeTextGuard(violations)
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
                    checkRetiredCreateGuard(violations)
                    checkHistoryCompatibilityBridgeGuard(violations)
                    checkQuickCreateLegacyImplementationGuard(violations)
                    checkQuickCreateScreenModelFacadeGuard(violations)
                    checkQuickCreateTaskStatusTextGuard(violations)
                    checkTaskHistoryPresentationTextGuard(violations)
                    checkUiCopyBaseline(violations)
                    checkQuickCreateDomainModelTextGuard(violations)
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
     * 校验当前态事实源不再指向已退役的 `:shared` 治理入口。
     *
     * 迁移归档和历史执行流水可以保留旧命令，方便追溯迁移过程；但验收标准、当前状态和
     * 当前门禁描述必须反映已经退役的模块图，避免后续任务继续调用不存在的 `:shared` 编译任务，
     * 或要求维护已经删除的 allowlist/baseline 文件。
     */
    private fun Project.checkRetiredSharedCurrentDocsGuard(violations: MutableList<String>) {
        val currentDocuments = mapOf(
            "doc/RunningHub-KMP-架构迁移验收标准.md" to listOf(
                "docs/migration/shared-allowlist.txt",
                "docs/migration/shared-baseline.txt",
                ":shared:compile",
                "shared -> 仅作为尚未迁移功能的临时兼容模块",
                "L1 不要求一次性删除整个 `shared`",
                "[x] L1 已通过",
            ),
            "docs/migration/acceptance.md" to listOf(
                "docs/migration/shared-allowlist.txt",
                ":shared:compileKotlinIosSimulatorArm64",
                "当前工作区仍有已暂存的非证据",
                "证据仍绑定旧 HEAD",
            ),
            "docs/migration/current-state.yaml" to listOf(
                "docs/migration/shared-allowlist.txt",
                "docs/migration/shared-baseline.txt",
                ":shared:compile",
            ),
            "build.gradle.kts" to listOf(
                "shared allowlist",
            ),
        )

        currentDocuments.forEach { (relativePath, forbiddenSnippets) ->
            val file = rootDir.resolve(relativePath)
            if (!file.isFile) {
                return@forEach
            }
            val text = if (relativePath == "docs/migration/current-state.yaml") {
                // current-state.yaml 后半部分包含滚动历史流水，旧命令可以保留作追溯；
                // 这里仅检查顶部当前态和最近验证切片，防止后续任务继续使用已删除入口。
                file.readLines().take(360).joinToString("\n")
            } else {
                file.readText()
            }
            forbiddenSnippets
                .filter { it in text }
                .forEach { snippet ->
                    violations += "$relativePath must not describe retired :shared current-state governance with `$snippet`."
                }
        }
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
     * 拦截生产 Kotlin 源码中的常见中文乱码片段。
     *
     * 中文注释是交付内容，出现 mojibake 会让后续协作者误读业务约束。这里不判断注释语义，
     * 只扫描迁移过程中已经出现过的 UTF-8/GBK 解码错位特征，防止乱码再次进入生产源码或构建脚本。
     */
    private fun Project.checkMojibakeTextGuard(violations: MutableList<String>) {
        val sourceRoots = listOf("composeApp", "core", "feature", "build-logic")
            .map { rootDir.resolve(it) }
            .filter { it.isDirectory }
        val mojibakeCharacters = setOf(
            '\u9225',
            '\u940F',
            '\u9483',
            '\u95C8',
            '\u9479',
            '\u6A01',
            '\u00E4',
            '\u00E5',
            '\u00E6',
            '\uFFFD',
        )

        sourceRoots
            .asSequence()
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
            }
            .filterNot { file ->
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                listOf(
                    "/src/commonTest/",
                    "/src/androidUnitTest/",
                    "/src/iosTest/",
                    "/build/",
                ).any { it in relativePath }
            }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    if (line.any { it in mojibakeCharacters }) {
                        val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                        violations += "$relativePath:${index + 1} contains mojibake text; rewrite the comment or string with readable Chinese/ASCII."
                    }
                }
            }
    }

    /**
     * 约束大体量 UI Feature 的 Presentation 模块拆分计划。
     *
     * 复核建议要求 Feature 达到规模阈值后建立独立 Presentation 模块。既有历史 Feature
     * 不能在一次治理中强行迁移，因此用基线记录当前行数和真实 Presentation owner；
     * 后续如果这些 Feature 超过基线缓冲继续增长，门禁会要求先拆分模块或显式更新治理文档说明原因。
     * 缓冲只用于 UI 壳、资源映射和注释，不能让本地可变状态流、协程编排或轮询逻辑回流到 composeApp。
     */
    private fun Project.checkFeaturePresentationThresholds(violations: MutableList<String>) {
        val thresholdLines = 800
        val baselineGrowthBufferLines = 80
        val baselineFile = rootDir.resolve("docs/governance/feature-presentation-thresholds.txt")
        val baselineEntries = baselineFile
            .takeIf { it.isFile }
            ?.readLines()
            ?.mapIndexedNotNull { index, rawLine ->
                parseFeaturePresentationBaseline(
                    line = rawLine.trim(),
                    lineNumber = index + 1,
                    violations = violations,
                )
            }
            .orEmpty()
        baselineEntries
            .groupingBy { it.featureName }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { featureName ->
                violations += "docs/governance/feature-presentation-thresholds.txt contains duplicate baseline for $featureName."
            }
        baselineEntries.forEach { entry ->
            val ownerDir = ownerPresentationDirectory(entry.ownerPresentation)
            if (ownerDir == null) {
                violations += "${entry.featureName} baseline has invalid ownerPresentation '${entry.ownerPresentation}'; use feature:<name>:presentation."
            } else if (!ownerDir.isDirectory) {
                violations += "${entry.featureName} baseline ownerPresentation '${entry.ownerPresentation}' does not exist."
            }

            val featureDir = rootDir.resolve("composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/${entry.featureName}")
            if (!featureDir.isDirectory) {
                violations += "${entry.featureName} baseline is stale; composeApp feature UI directory is missing."
            }
            if (rootDir.resolve("feature/${entry.featureName}/presentation").isDirectory) {
                violations += "${entry.featureName} has a same-name Presentation module; remove its migration threshold baseline."
            }
            if (featureDir.isDirectory) {
                checkFeaturePresentationStateMachineGuard(
                    featureDir = featureDir,
                    entry = entry,
                    violations = violations,
                )
            }
        }
        val baseline = baselineEntries.associateBy { it.featureName }

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
                    val entry = baseline[featureName]
                    when {
                        entry == null -> {
                            violations += "$featureName UI has $totalLines lines and no same-name presentation module; split it or register a threshold baseline with ownerPresentation."
                        }
                        totalLines > entry.maxLines + baselineGrowthBufferLines -> {
                            violations += "$featureName UI grew from allowed ${entry.maxLines} lines plus $baselineGrowthBufferLines buffer lines to $totalLines without a same-name presentation module; owner is ${entry.ownerPresentation}."
                        }
                    }
                }
            }
    }

    /**
     * 阻止 Feature 行数缓冲被用来重新承载状态机或业务编排。
     *
     * 登记在 `feature-presentation-thresholds.txt` 的 composeApp 目录只允许保留 Voyager
     * ScreenModel 门面、Compose UI、资源映射和明确登记的兼容桥。真实状态流、协程启动、
     * 轮询、组合 Flow 和互斥控制必须继续留在 [FeaturePresentationBaseline.ownerPresentation]
     * 指向的 Presentation 模块。
     */
    private fun Project.checkFeaturePresentationStateMachineGuard(
        featureDir: java.io.File,
        entry: FeaturePresentationBaseline,
        violations: MutableList<String>,
    ) {
        val forbiddenPatterns = listOf(
            Regex("""\bMutableStateFlow\b""") to "local mutable state flow",
            Regex("""\bMutableSharedFlow\b""") to "local mutable shared flow",
            Regex("""\bkotlinx\.coroutines\.channels\.Channel\b""") to "local coroutine channel",
            Regex("""\bkotlinx\.coroutines\.sync\.Mutex\b""") to "local concurrency lock",
            Regex("""\bscreenModelScope\.launch\s*\{""") to "ScreenModel coroutine orchestration",
            Regex("""\bcoroutineScope\.launch\s*\{""") to "local coroutine orchestration",
            Regex("""(?<![A-Za-z0-9_.])launch\s*\{""") to "local coroutine orchestration",
            Regex("""\bstateIn\s*\(""") to "local Flow state ownership",
            Regex("""\bshareIn\s*\(""") to "local shared Flow ownership",
            Regex("""\blaunchIn\s*\(""") to "local Flow collection ownership",
            Regex("""\bflatMapLatest\s*\(""") to "local reactive state transition",
            Regex("""\bcombine\s*\(""") to "local reactive state composition",
            Regex("""\bwhile\s*\(\s*isActive\s*\)""") to "local polling loop",
            Regex("""\bdelay\s*\(""") to "local polling or retry timer",
            Regex("""\bwithContext\s*\(""") to "local async orchestration",
        )

        featureDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                forbiddenPatterns
                    .firstOrNull { (pattern, _) -> pattern.containsMatchIn(text) }
                    ?.let { (_, reason) ->
                        val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                        violations += "$relativePath contains $reason; ${entry.featureName} is over the UI feature threshold and must keep state machine or business orchestration in ${entry.ownerPresentation}."
                    }
            }
    }

    /**
     * 记录无同名 Presentation 模块的大体量 composeApp Feature 临时基线。
     *
     * @property featureName composeApp `ui/feature/<name>` 目录名。
     * @property maxLines 当前登记的 Kotlin 行数基线，单位为行；小幅增长由门禁缓冲吸收，明显膨胀仍需拆分或更新治理说明。
     * @property ownerPresentation 实际持有状态机的 Presentation 模块，格式为 `feature:<name>:presentation`。
     * @property reason 保留该 UI 壳的迁移期原因。
     */
    private data class FeaturePresentationBaseline(
        val featureName: String,
        val maxLines: Int,
        val ownerPresentation: String,
        val reason: String,
    )

    /**
     * 解析 `feature-presentation-thresholds.txt` 的一行基线。
     *
     * 空行和注释行返回 `null`；非法行会写入 [violations]，由统一治理任务汇总失败。
     */
    private fun Project.parseFeaturePresentationBaseline(
        line: String,
        lineNumber: Int,
        violations: MutableList<String>,
    ): FeaturePresentationBaseline? {
        if (line.isEmpty() || line.startsWith("#")) {
            return null
        }

        val parts = line.split('|', limit = 4)
        if (parts.size != 4) {
            violations += "docs/governance/feature-presentation-thresholds.txt:$lineNumber must use feature|maxLines|ownerPresentation|reason."
            return null
        }

        val featureName = parts[0].trim()
        val maxLines = parts[1].trim().toIntOrNull()
        val ownerPresentation = parts[2].trim()
        val reason = parts[3].trim()
        if (featureName.isBlank()) {
            violations += "docs/governance/feature-presentation-thresholds.txt:$lineNumber has blank feature name."
        }
        if (maxLines == null || maxLines <= 0) {
            violations += "docs/governance/feature-presentation-thresholds.txt:$lineNumber has invalid maxLines '${parts[1]}'."
        }
        if (ownerPresentation.isBlank()) {
            violations += "docs/governance/feature-presentation-thresholds.txt:$lineNumber has blank ownerPresentation."
        }
        if (reason.isBlank()) {
            violations += "docs/governance/feature-presentation-thresholds.txt:$lineNumber has blank reason."
        }
        if (featureName.isBlank() || maxLines == null || maxLines <= 0 || ownerPresentation.isBlank() || reason.isBlank()) {
            return null
        }

        return FeaturePresentationBaseline(
            featureName = featureName,
            maxLines = maxLines,
            ownerPresentation = ownerPresentation,
            reason = reason,
        )
    }

    /**
     * 将 `feature:<name>:presentation` 模块坐标转换为仓库目录。
     *
     * 返回 `null` 表示坐标格式非法，调用方负责生成治理错误。
     */
    private fun Project.ownerPresentationDirectory(ownerPresentation: String): java.io.File? {
        val match = Regex("""feature:([a-z0-9_-]+):presentation""").matchEntire(ownerPresentation) ?: return null
        return rootDir.resolve("feature/${match.groupValues[1]}/presentation")
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

        val replayableReadPostSources = mapOf(
            "feature/discovery/data/src/commonMain/kotlin/com/runninghub/feature/discovery/data/remote/api/WebAppCatalogApi.kt" to
                listOf("webapp/list", "webapp/carefullyChosenList", "webapp/customMadeWebappList", "webapp/user/list", "portal/tag/tree", "webapp/detail"),
            "feature/community/data/src/commonMain/kotlin/com/runninghub/feature/community/data/remote/api/PlazaApi.kt" to
                listOf("portal/tag/tree", "portal/creation/list", "canvas/community/category/list", "canvas/community/composition/list"),
            "feature/model/data/src/commonMain/kotlin/com/runninghub/feature/model/data/remote/api/ModelCatalogApi.kt" to
                listOf("sku/list", "sku/detail"),
            "feature/task/data/src/commonMain/kotlin/com/runninghub/feature/task/data/remote/api/WebAppTaskApi.kt" to
                listOf("webapp/apiCallDemo", "outputs", "output/v2/history"),
            "feature/auth/data/src/commonMain/kotlin/com/runninghub/feature/auth/data/remote/api/AuthApi.kt" to
                listOf("openapi/accountStatus", "fun getUserInfo(", "fun getUserDetail(", "fun isFollow("),
            "feature/quickcreate/data/src/commonMain/kotlin/com/runninghub/feature/quickcreate/data/remote/api/QuickCreateApi.kt" to
                listOf(
                    "qc/v2/categories",
                    "qc/v2/models",
                    "qc/v2/creation-modes",
                    "QC_FEE_PREVIEW",
                    "QC_TASK_LIST",
                    "QC_TASK_DETAIL",
                    "QC_PROJECT_LIST",
                    "QC_PROJECT_TASKS",
                    "QC_PROJECT_DETAIL",
                    "QC_INSPIRATION_TAGS",
                    "QC_INSPIRATION_TEMPLATES",
                    "QC_INSPIRATION_TEMPLATE_DETAIL",
                ),
        )
        replayableReadPostSources.forEach { (relativePath, readEndpointSnippets) ->
            val source = rootDir.resolve(relativePath)
            if (!source.isFile) {
                violations += "Replayable read POST guard source is missing: $relativePath."
                return@forEach
            }
            val text = source.readText()
            val replayMarkerCalls = Regex("""\bmarkRunningHubAuthRetryAllowed\s*\(""")
                .findAll(text)
                .count()
            val presentReadEndpoints = readEndpointSnippets.count { it in text }
            if (replayMarkerCalls < presentReadEndpoints) {
                violations += "$relativePath must mark each idempotent read POST request as replayable after token refresh; found $replayMarkerCalls markers for $presentReadEndpoints registered read endpoints."
            }
        }
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

        val authRepositoryPath =
            "feature/auth/data/src/commonMain/kotlin/com/runninghub/feature/auth/data/repository/AuthRepositoryImpl.kt"
        val authRepositoryTestPath =
            "feature/auth/data/src/commonTest/kotlin/com/runninghub/feature/auth/data/repository/AuthRepositoryImplTest.kt"
        val authDomainPath =
            "feature/auth/domain/src/commonMain/kotlin/com/runninghub/feature/auth/domain/AuthRepository.kt"
        val userRepositoryPath =
            "feature/auth/data/src/commonMain/kotlin/com/runninghub/feature/auth/data/repository/UserRepositoryImpl.kt"
        val userRepositoryTestPath =
            "feature/auth/data/src/commonTest/kotlin/com/runninghub/feature/auth/data/repository/UserRepositoryImplTest.kt"
        val userDomainPath =
            "feature/auth/domain/src/commonMain/kotlin/com/runninghub/feature/auth/domain/UserRepository.kt"
        val taskRepositoryPath =
            "feature/task/data/src/commonMain/kotlin/com/runninghub/feature/task/data/repository/WebAppTaskRepositoryImpl.kt"
        val taskRepositoryTestPath =
            "feature/task/data/src/commonTest/kotlin/com/runninghub/feature/task/data/repository/WebAppTaskRepositoryImplTest.kt"
        val taskDomainPath =
            "feature/task/domain/src/commonMain/kotlin/com/runninghub/feature/task/domain/WebAppTaskRepository.kt"
        val modelDataBuildPath = "feature/model/data/build.gradle.kts"
        val modelInvocationPath =
            "feature/model/data/src/commonMain/kotlin/com/runninghub/feature/model/data/repository/ModelInvocationRepositoryImpl.kt"
        val modelCatalogPath =
            "feature/model/data/src/commonMain/kotlin/com/runninghub/feature/model/data/repository/ModelCatalogRepositoryImpl.kt"
        val modelInvocationTestPath =
            "feature/model/data/src/commonTest/kotlin/com/runninghub/feature/model/data/repository/ModelInvocationRepositoryImplTest.kt"
        val modelCatalogTestPath =
            "feature/model/data/src/commonTest/kotlin/com/runninghub/feature/model/data/repository/ModelCatalogRepositoryImplTest.kt"
        val modelDomainPath =
            "feature/model/domain/src/commonMain/kotlin/com/runninghub/feature/model/domain/ModelCatalogRepository.kt"
        val modelInvocationDomainPath =
            "feature/model/domain/src/commonMain/kotlin/com/runninghub/feature/model/domain/ModelInvocation.kt"
        val plazaRepositoryPath =
            "feature/community/data/src/commonMain/kotlin/com/runninghub/feature/community/data/repository/PlazaRepositoryImpl.kt"
        val plazaRepositoryTestPath =
            "feature/community/data/src/commonTest/kotlin/com/runninghub/feature/community/data/repository/PlazaRepositoryImplTest.kt"
        val plazaDomainPath =
            "feature/community/domain/src/commonMain/kotlin/com/runninghub/feature/community/domain/PlazaRepository.kt"

        val authRepository = rootDir.resolve(authRepositoryPath)
        val authRepositoryTest = rootDir.resolve(authRepositoryTestPath)
        val authDomain = rootDir.resolve(authDomainPath)
        val userRepository = rootDir.resolve(userRepositoryPath)
        val userRepositoryTest = rootDir.resolve(userRepositoryTestPath)
        val userDomain = rootDir.resolve(userDomainPath)
        val taskRepository = rootDir.resolve(taskRepositoryPath)
        val taskRepositoryTest = rootDir.resolve(taskRepositoryTestPath)
        val taskDomain = rootDir.resolve(taskDomainPath)
        val modelDataBuild = rootDir.resolve(modelDataBuildPath)
        val modelInvocation = rootDir.resolve(modelInvocationPath)
        val modelCatalog = rootDir.resolve(modelCatalogPath)
        val modelInvocationTest = rootDir.resolve(modelInvocationTestPath)
        val modelCatalogTest = rootDir.resolve(modelCatalogTestPath)
        val modelDomain = rootDir.resolve(modelDomainPath)
        val modelInvocationDomain = rootDir.resolve(modelInvocationDomainPath)
        val plazaRepository = rootDir.resolve(plazaRepositoryPath)
        val plazaRepositoryTest = rootDir.resolve(plazaRepositoryTestPath)
        val plazaDomain = rootDir.resolve(plazaDomainPath)

        if (authRepository.isFile) {
            val text = authRepository.readText()
            if (text.contains("Empty login response") ||
                text.contains("No access token received") ||
                text.contains("Token refresh failed") ||
                text.contains("Empty user response")
            ) {
                violations += "$authRepositoryPath must not encode auth response structure failures through exception message."
            }
            if (!text.contains("AuthError.EmptyLoginResponse") ||
                !text.contains("AuthError.MissingAccessToken") ||
                !text.contains("AuthError.TokenRefreshFailed") ||
                !text.contains("AuthError.EmptyUserResponse")
            ) {
                violations += "$authRepositoryPath must map auth response structure failures to typed AuthError subclasses."
            }
        }
        if (authRepositoryTest.isFile) {
            val text = authRepositoryTest.readText()
            if (!text.contains("assertIs<AuthError.EmptyLoginResponse>") ||
                !text.contains("assertIs<AuthError.MissingAccessToken>") ||
                !text.contains("assertIs<AuthError.TokenRefreshFailed>") ||
                !text.contains("assertIs<AuthError.EmptyUserResponse>")
            ) {
                violations += "$authRepositoryTestPath must assert typed AuthError subclasses for auth response structure failures."
            }
        }
        if (authDomain.isFile) {
            val text = authDomain.readText()
            if (!text.contains("class EmptyLoginResponse") ||
                !text.contains("class MissingAccessToken") ||
                !text.contains("class TokenRefreshFailed") ||
                !text.contains("class EmptyUserResponse")
            ) {
                violations += "$authDomainPath must expose typed AuthError subclasses for auth response structure failures."
            }
        }
        if (userRepository.isFile) {
            val text = userRepository.readText()
            if (text.contains("Empty response data") ||
                text.contains("IllegalStateException(\"") ||
                text.contains("\${fallbackCode}_CODE_\$code")
            ) {
                violations += "$userRepositoryPath must not encode user repository failures through exception message."
            }
            if (!text.contains("UserRepositoryException(UserRepositoryIssue.AccountStatusMissing") ||
                !text.contains("UserRepositoryException(UserRepositoryIssue.UserInfoMissing") ||
                !text.contains("UserRepositoryException(UserRepositoryIssue.UserDetailMissing") ||
                !text.contains("throw UserRepositoryException(issue, code)")
            ) {
                violations += "$userRepositoryPath must map user repository response failures to typed UserRepositoryException issues."
            }
        }
        if (userRepositoryTest.isFile) {
            val text = userRepositoryTest.readText()
            if (!text.contains("assertIs<UserRepositoryException>") ||
                !text.contains("UserRepositoryIssue.AccountStatusMissing") ||
                !text.contains("UserRepositoryIssue.UserInfoFailed") ||
                !text.contains("UserRepositoryIssue.FollowStatusFailed") ||
                !text.contains("UserRepositoryIssue.FollowUserFailed") ||
                !text.contains("UserRepositoryIssue.UnfollowUserFailed")
            ) {
                violations += "$userRepositoryTestPath must assert typed UserRepositoryException issues for user repository failures."
            }
        }
        if (userDomain.isFile) {
            val text = userDomain.readText()
            if (!text.contains("class UserRepositoryException") ||
                !text.contains("enum class UserRepositoryIssue")
            ) {
                violations += "$userDomainPath must expose typed UserRepositoryException and UserRepositoryIssue instead of message-only user repository errors."
            }
        }
        if (taskRepository.isFile) {
            val text = taskRepository.readText()
            if (text.contains("TASK_\${operation.uppercase()}_FAILED_CODE_\$code") ||
                text.contains("Empty response data") ||
                text.contains("IllegalStateException(\"")
            ) {
                violations += "$taskRepositoryPath must not encode task response failures through exception message."
            }
            if (!text.contains("WebAppTaskException(failedIssue, code)") ||
                !text.contains("WebAppTaskException(missingIssue)") ||
                !text.contains("WebAppTaskIssue.RunTaskFailed") ||
                !text.contains("WebAppTaskIssue.TaskHistoryMissing")
            ) {
                violations += "$taskRepositoryPath must map task response failures to typed WebAppTaskException issues."
            }
        }
        if (taskRepositoryTest.isFile) {
            val text = taskRepositoryTest.readText()
            if (!text.contains("assertIs<WebAppTaskException>") ||
                !text.contains("WebAppTaskIssue.RunTaskFailed") ||
                !text.contains("WebAppTaskIssue.RunTaskMissing") ||
                !text.contains("WebAppTaskIssue.ApiCallDemoFailed") ||
                !text.contains("WebAppTaskIssue.TaskHistoryFailed")
            ) {
                violations += "$taskRepositoryTestPath must assert typed WebAppTaskException issues for task response failures."
            }
        }
        if (taskDomain.isFile) {
            val text = taskDomain.readText()
            if (!text.contains("class WebAppTaskException") ||
                !text.contains("enum class WebAppTaskIssue")
            ) {
                violations += "$taskDomainPath must expose typed WebAppTaskException and WebAppTaskIssue instead of message-only task errors."
            }
        }

        if (modelDataBuild.isFile && !modelDataBuild.readText().contains("projects.core.common")) {
            violations += "$modelDataBuildPath must depend on core:common for shared credential error semantics."
        }
        if (modelInvocation.isFile) {
            val text = modelInvocation.readText()
            if (!text.contains("MissingCredentialException(MissingCredential.ApiKey)")) {
                violations += "$modelInvocationPath must use MissingCredentialException(MissingCredential.ApiKey) when local API Key is missing."
            }
            if (text.contains("error(ModelInvocationIssue")) {
                violations += "$modelInvocationPath must not encode model invocation issues through exception message; use ModelInvocationException(issue)."
            }
            if (!text.contains("ModelInvocationException(ModelInvocationIssue.MediaUploadEmptyUrl)")) {
                violations += "$modelInvocationPath must use ModelInvocationException(ModelInvocationIssue.MediaUploadEmptyUrl) when media upload returns no URL."
            }
            if (text.contains("Model detail load failed") ||
                text.contains("Model detail missing") ||
                text.contains("Model endpoint missing")
            ) {
                violations += "$modelInvocationPath must not encode model catalog endpoint fallback failures through exception message."
            }
            if (!text.contains("ModelCatalogException(ModelCatalogIssue.StandardDetailLoadFailed") ||
                !text.contains("ModelCatalogException(ModelCatalogIssue.StandardDetailMissing") ||
                !text.contains("ModelCatalogException(ModelCatalogIssue.StandardEndpointMissing")
            ) {
                violations += "$modelInvocationPath must map endpoint fallback failures to typed ModelCatalogException issues."
            }
        }
        if (modelCatalog.isFile) {
            val text = modelCatalog.readText()
            if (text.contains("Model list load failed") ||
                text.contains("Model detail load failed") ||
                text.contains("Model detail missing") ||
                text.contains("LLM model list load failed")
            ) {
                violations += "$modelCatalogPath must not encode catalog response failures through exception message."
            }
            if (!text.contains("ModelCatalogException(ModelCatalogIssue.StandardListLoadFailed") ||
                !text.contains("ModelCatalogException(ModelCatalogIssue.StandardDetailLoadFailed") ||
                !text.contains("ModelCatalogException(ModelCatalogIssue.StandardDetailMissing") ||
                !text.contains("ModelCatalogException(ModelCatalogIssue.LlmListLoadFailed")
            ) {
                violations += "$modelCatalogPath must map catalog response failures to typed ModelCatalogException issues."
            }
        }
        if (modelInvocationTest.isFile &&
            !modelInvocationTest.readText().contains("assertIs<MissingCredentialException>")
        ) {
            violations += "$modelInvocationTestPath must assert the stable MissingCredentialException type for missing API Key."
        }
        if (modelInvocationTest.isFile &&
            !modelInvocationTest.readText().contains("assertIs<ModelInvocationException>")
        ) {
            violations += "$modelInvocationTestPath must assert the stable ModelInvocationException type for model invocation issues."
        }
        if (modelInvocationTest.isFile &&
            !modelInvocationTest.readText().contains("ModelCatalogIssue.StandardEndpointMissing")
        ) {
            violations += "$modelInvocationTestPath must assert typed ModelCatalogIssue for endpoint fallback failures."
        }
        if (modelCatalogTest.isFile) {
            val text = modelCatalogTest.readText()
            if (!text.contains("assertIs<ModelCatalogException>") ||
                !text.contains("ModelCatalogIssue.StandardListLoadFailed") ||
                !text.contains("ModelCatalogIssue.StandardDetailMissing") ||
                !text.contains("ModelCatalogIssue.LlmListLoadFailed")
            ) {
                violations += "$modelCatalogTestPath must assert typed ModelCatalogException issues for catalog response failures."
            }
        } else {
            violations += "$modelCatalogTestPath must cover typed ModelCatalogException issues for catalog response failures."
        }
        if (modelDomain.isFile) {
            val text = modelDomain.readText()
            if (!text.contains("class ModelCatalogException") ||
                !text.contains("enum class ModelCatalogIssue")
            ) {
                violations += "$modelDomainPath must expose typed ModelCatalogException and ModelCatalogIssue instead of message-only catalog errors."
            }
        }
        if (modelInvocationDomain.isFile) {
            val text = modelInvocationDomain.readText()
            if (text.contains("API_KEY_MISSING")) {
                violations += "$modelInvocationDomainPath must not keep a duplicate model-specific API Key missing issue code; use core:common MissingCredential."
            }
            if (!text.contains("class ModelInvocationException") ||
                !text.contains("enum class ModelInvocationIssue")
            ) {
                violations += "$modelInvocationDomainPath must expose typed ModelInvocationException and ModelInvocationIssue instead of message-only errors."
            }
        }
        if (plazaRepository.isFile) {
            val text = plazaRepository.readText()
            if (text.contains("IllegalStateException(\"") ||
                text.contains("\${fallbackCode}_CODE_\$code") ||
                text.contains("PLAZA_CREATIONS_LOAD_FAILED\"")
            ) {
                violations += "$plazaRepositoryPath must not encode Plaza response failures through exception message."
            }
            if (!text.contains("PlazaRepositoryException(issue, code)") ||
                !text.contains("PlazaRepositoryIssue.TagsLoadFailed") ||
                !text.contains("PlazaRepositoryIssue.CreationsLoadFailed") ||
                !text.contains("PlazaRepositoryIssue.ShortCategoriesLoadFailed") ||
                !text.contains("PlazaRepositoryIssue.ShortListLoadFailed")
            ) {
                violations += "$plazaRepositoryPath must map Plaza response failures to typed PlazaRepositoryException issues."
            }
        }
        if (plazaRepositoryTest.isFile) {
            val text = plazaRepositoryTest.readText()
            if (!text.contains("assertIs<PlazaRepositoryException>") ||
                !text.contains("PlazaRepositoryIssue.TagsLoadFailed") ||
                !text.contains("PlazaRepositoryIssue.CreationsLoadFailed") ||
                !text.contains("PlazaRepositoryIssue.ShortCategoriesLoadFailed") ||
                !text.contains("PlazaRepositoryIssue.ShortListLoadFailed")
            ) {
                violations += "$plazaRepositoryTestPath must assert typed PlazaRepositoryException issues for Plaza response failures."
            }
        }
        if (plazaDomain.isFile) {
            val text = plazaDomain.readText()
            if (!text.contains("class PlazaRepositoryException") ||
                !text.contains("enum class PlazaRepositoryIssue")
            ) {
                violations += "$plazaDomainPath must expose typed PlazaRepositoryException and PlazaRepositoryIssue instead of message-only Plaza errors."
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
        val composeResources = rootDir.resolve(
            "composeApp/src/commonMain/composeResources/values/strings.xml"
        )
        val callbackTest = rootDir.resolve(
            "composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaCallbackTest.kt"
        )
        val environmentTest = rootDir.resolve(
            "composeApp/src/androidUnitTest/kotlin/com/runninghub/app/ui/feature/login/SmsCaptchaEnvironmentTest.kt"
        )

        val requiredFiles = listOf(
            commonHtml,
            callbackParser,
            androidDialog,
            iosDialog,
            htmlTest,
            composeResources,
            callbackTest,
            environmentTest,
        )
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
                "copy.scriptLoadFailedRetry.escapeJavaScriptString()",
                "copy.imageLoadFailedRetry.escapeJavaScriptString()",
                "copy.retryAction.escapeJavaScriptString()",
                "internal expect fun smsCaptchaBaseUrl(): String",
            ),
            violations = violations,
        )
        requireFileSnippets(
            file = composeResources,
            snippets = listOf(
                "login_sms_captcha_preparing",
                "login_sms_captcha_retry_action",
                "login_sms_captcha_image_load_failed_retry",
                "图形验证图片加载失败，请点击重试",
                "login_sms_captcha_script_load_failed_retry",
                "图形验证脚本加载失败，请点击重试",
                "login_sms_captcha_script_timeout_retry",
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
                "RunningHubApiEnvironment.WEB_BASE_URL",
                "smsCaptchaBaseUrl()",
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
                "RunningHubApiEnvironment.WEB_BASE_URL",
                "smsCaptchaBaseUrl()",
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
        requireFileSnippets(
            file = environmentTest,
            snippets = listOf(
                "captcha base url follows configured web environment",
                "RunningHubApiEnvironment.configure",
                "smsCaptchaBaseUrl()",
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
            "feature/quickcreate/presentation/src/commonMain/kotlin/com/runninghub/feature/quickcreate/presentation/upload/QuickCreateMediaUploadCoordinator.kt"
        )
        if (!coordinator.isFile) {
            violations += "QuickCreate media upload coordinator must live in feature:quickcreate:presentation upload boundary."
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
            .forEach { snippet -> violations += "feature quickcreate presentation upload coordinator must not expose media displayName in upload error message: `$snippet`." }

        listOf("MEDIA_UPLOAD_FAILED_MESSAGE", "MEDIA_UPLOAD_TIMEOUT_MESSAGE")
            .filterNot { it in text }
            .forEach { snippet -> violations += "feature quickcreate presentation upload coordinator must use `$snippet` for sanitized upload errors." }
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
     * 当前仓库已经移除未使用的历史 Discovery cache schema，不再允许任何 schema 使用
     * `legacy-debt` 标注缺口。所有新增 `.sq` / `.sqm` 文件都必须登记真实测试入口，
     * 防止数据库结构变化只停留在实现层。
     */
    private fun Project.checkDatabaseContractBaseline(violations: MutableList<String>) {
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
            if (testPath == "legacy-debt") {
                violations += "$schemaPath must use a real database contract or migration test; legacy-debt is no longer allowed."
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
     * 防止已经退役的旧 Create 页面或状态机重新进入仓库。
     *
     * 新架构的快捷创作入口是 composeApp 的 `QuickCreateVoyagerScreen` 壳层和
     * `feature:quickcreate:*` 模块；旧 `feature/create` 源目录、`CreateVoyagerScreen`
     * 和 `CreateScreenModel` 不应再作为第二套创作实现存在。
     */
    private fun Project.checkRetiredCreateGuard(violations: MutableList<String>) {
        listOf(
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create",
            "composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/create",
        ).forEach { relativePath ->
            val dir = rootDir.resolve(relativePath)
            if (dir.isDirectory && dir.walkTopDown().any { it.isFile && it.extension == "kt" }) {
                violations += "$relativePath contains retired legacy Create source; keep creation on QuickCreate feature-first modules."
            }
        }

        val roots = listOf(
            rootDir.resolve("composeApp/src/commonMain/kotlin"),
            rootDir.resolve("composeApp/src/commonTest/kotlin"),
        )
        val retiredTypePattern = Regex("""\b(CreateVoyagerScreen|CreateScreenModel)\b""")
        roots
            .filter { it.isDirectory }
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .forEach { file ->
                if (retiredTypePattern.containsMatchIn(file.readText())) {
                    val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                    violations += "$relativePath references retired legacy Create screen or state machine."
                }
            }
    }

    /**
     * 约束 History 迁移期兼容桥，避免它在中期治理阶段扩散成新的长期架构事实。
     *
     * 当前 History 页仍需要 QuickCreate 历史详情、取消和参数复用能力，现有 Task Data 的 WebApp
     * 历史接口还不能替代它。因此兼容桥可以暂时留在 composeApp 组合层，但只能有一个明确文件；
     * 未来 Task Data 正式实现 GenerationHistoryRepository 后，应删除该桥和 AppModule 绑定。
     */
    private fun Project.checkHistoryCompatibilityBridgeGuard(violations: MutableList<String>) {
        val adapterPath =
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/QuickCreateGenerationHistoryRepositoryAdapter.kt"
        val appModulePath = "composeApp/src/commonMain/kotlin/com/runninghub/app/di/AppModule.kt"
        val taskDataModulePath = "feature/task/data/src/commonMain/kotlin/com/runninghub/feature/task/data/di/TaskDataModule.kt"
        val taskDataBuildPath = "feature/task/data/build.gradle.kts"

        val adapterFile = rootDir.resolve(adapterPath)
        val appModuleFile = rootDir.resolve(appModulePath)
        val taskDataModuleFile = rootDir.resolve(taskDataModulePath)
        val taskDataBuildFile = rootDir.resolve(taskDataBuildPath)

        if (!appModuleFile.isFile) {
            violations += "AppModule source is missing: $appModulePath."
            return
        }
        if (!taskDataModuleFile.isFile) {
            violations += "Task data module source is missing: $taskDataModulePath."
            return
        }

        val appModuleText = appModuleFile.readText()
        val taskDataModuleText = taskDataModuleFile.readText()
        val taskDataProvidesUnifiedHistory = taskDataModuleText.contains("single<GenerationHistoryRepository>")

        if (taskDataBuildFile.isFile && taskDataBuildFile.readText().contains("projects.feature.quickcreate.domain")) {
            violations += "$taskDataBuildPath must not depend on feature:quickcreate:domain just to move the History compatibility bridge; add a real Task Data unified history implementation instead."
        }

        val appRoot = rootDir.resolve("composeApp/src/commonMain/kotlin/com/runninghub/app")
        if (appRoot.isDirectory) {
            appRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filterNot { it.relativeTo(rootDir).invariantSeparatorsPath == adapterPath }
                .forEach { file ->
                    val text = file.readText()
                    if (text.contains("QuickCreationHistoryItem") ||
                        text.contains("QuickCreationHistoryOutput") ||
                        text.contains("QuickCreationHistoryPage") ||
                        text.contains("QuickCreationTaskHistoryRepository")
                    ) {
                        violations += "${file.relativeTo(rootDir).invariantSeparatorsPath} must not map QuickCreate history models directly; keep the migration bridge isolated or replace it with Task Data."
                    }
                }
        }

        if (adapterFile.isFile) {
            val adapterText = adapterFile.readText()
            if (!adapterText.contains("internal class QuickCreateGenerationHistoryRepositoryAdapter")) {
                violations += "$adapterPath must keep the QuickCreate history bridge internal to composeApp until it is deleted."
            }
            if (!adapterText.contains("QuickCreationTaskHistoryRepository") ||
                !adapterText.contains("GenerationHistoryRepository")
            ) {
                violations += "$adapterPath must remain an explicit QuickCreate-to-GenerationHistory adapter; do not hide the bridge behind broader app code."
            }
            if (!appModuleText.contains("QuickCreateGenerationHistoryRepositoryAdapter") ||
                !appModuleText.contains("single<GenerationHistoryRepository> { QuickCreateGenerationHistoryRepositoryAdapter(get()) }")
            ) {
                violations += "$appModulePath must bind the temporary History bridge explicitly, or delete the bridge after Task Data provides GenerationHistoryRepository."
            }
            if (taskDataProvidesUnifiedHistory) {
                violations += "$adapterPath must be deleted once TaskDataModule binds GenerationHistoryRepository."
            }
        } else {
            if (appModuleText.contains("QuickCreateGenerationHistoryRepositoryAdapter")) {
                violations += "$appModulePath still references deleted QuickCreateGenerationHistoryRepositoryAdapter."
            }
            if (!taskDataProvidesUnifiedHistory) {
                violations += "History compatibility bridge is missing, but TaskDataModule does not bind GenerationHistoryRepository yet."
            }
        }
    }

    /**
     * 防止旧 QuickCreate 实现职责回流到 composeApp。
     *
     * 当前允许 composeApp 保留 Voyager Screen、ScreenModel 门面、Compose UI 叶子组件和资源映射；
     * 仓库聚合、页面级状态所有权、Coordinator/Interactor/StateHolder 装配必须留在
     * `feature:quickcreate:presentation`。该检查把“旧实现”的代码特征显式列出，避免后续协作者
     * 在应用壳中重新创建第二套快捷创作状态机。
     */
    private fun Project.checkQuickCreateLegacyImplementationGuard(violations: MutableList<String>) {
        val rootPath = "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate"
        val quickCreateRoot = rootDir.resolve(rootPath)
        if (!quickCreateRoot.isDirectory) {
            violations += "QuickCreate composeApp UI shell is missing: $rootPath."
            return
        }

        val forbiddenSnippets = listOf(
            "import kotlinx.coroutines.flow.MutableStateFlow",
            "QuickCreationTaskHistoryRepository",
            "QuickCreationModelCatalogRepository",
            "QuickCreationGenerationRepository",
            "QuickCreationFeePreviewRepository",
            "QuickCreationInspirationRepository",
            "QuickCreationMediaUploadRepository",
            "QuickCreationProjectRepository",
            "QuickCreateDraftRepository",
            "QuickCreateCoordinator(",
            "QuickCreateGenerationInteractor(",
            "QuickCreateFeePreviewInteractor(",
            "QuickCreateMediaUploadCoordinator(",
            "QuickCreateTaskPollingController(",
            "QuickCreateDraftStateHolder(",
            "QuickCreateEditorStateHolder(",
            "QuickCreateHistoryStateHolder(",
            "QuickCreateInspirationStateHolder(",
            "QuickCreateProjectStateHolder(",
            "QuickCreateModelCatalogInteractor(",
            "QuickCreateGenerationRequestFactory(",
        )

        quickCreateRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                forbiddenSnippets
                    .filter { forbidden -> text.contains(forbidden) }
                    .forEach { forbidden ->
                        val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                        violations += "$relativePath contains legacy QuickCreate implementation snippet '$forbidden'; keep composeApp as UI shell and move state/coordination to feature:quickcreate:presentation."
                    }
            }

        val testRootPath = "composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate"
        val quickCreateTestRoot = rootDir.resolve(testRootPath)
        val forbiddenTestSnippets = forbiddenSnippets + listOf(
            "QuickCreatePresentationStateHolderFactory(",
            "FakeQuickCreateRepository",
            "RecordingQuickCreateRepository",
        )
        if (quickCreateTestRoot.isDirectory) {
            quickCreateTestRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val text = file.readText()
                    forbiddenTestSnippets
                        .filter { forbidden -> text.contains(forbidden) }
                        .forEach { forbidden ->
                            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                            violations += "$relativePath contains legacy QuickCreate test fixture snippet '$forbidden'; keep behavior tests in feature:quickcreate:presentation and limit composeApp tests to UI shell wiring."
                        }
                }
        }
    }

    /**
     * 防止 QuickCreate 的应用壳 ScreenModel 重新承载仓库聚合或页面状态所有权。
     *
     * QuickCreate 仍由 composeApp 提供 Voyager ScreenModel 生命周期，但页面级状态容器、
     * Coordinator 装配和领域仓库聚合必须位于 `feature:quickcreate:presentation`。
     */
    private fun Project.checkQuickCreateScreenModelFacadeGuard(violations: MutableList<String>) {
        val relativePath = "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt"
        val file = rootDir.resolve(relativePath)
        if (!file.isFile) {
            violations += "QuickCreate ScreenModel facade is missing: $relativePath."
            return
        }

        val text = file.readText()
        val forbiddenSnippets = listOf(
            "import kotlinx.coroutines.flow.MutableStateFlow",
            "QuickCreateCoordinator(",
            "QuickCreationTaskHistoryRepository",
            "QuickCreationModelCatalogRepository",
            "QuickCreationGenerationRepository",
            "QuickCreationFeePreviewRepository",
            "QuickCreationInspirationRepository",
            "QuickCreationMediaUploadRepository",
            "QuickCreationProjectRepository",
            "QuickCreateDraftRepository",
        )
        forbiddenSnippets.forEach { forbidden ->
            if (text.contains(forbidden)) {
                violations += "$relativePath must remain a Voyager facade; move QuickCreate state ownership and repository aggregation to feature:quickcreate:presentation."
            }
        }

        if (!text.contains("QuickCreatePresentationStateHolderFactory")) {
            violations += "$relativePath must create its presentation session through QuickCreatePresentationStateHolderFactory."
        }
    }

    /**
     * 防止 QuickCreate 任务状态区重新出现原文透传文案通道。
     *
     * 任务状态区只能展示稳定状态键或 [QuickCreatePresentationError] 映射后的资源文案；
     * 服务端未知摘要需要先降级为稳定错误语义，不能通过 `Custom(value)` 直接进入 UI。
     */
    private fun Project.checkQuickCreateTaskStatusTextGuard(violations: MutableList<String>) {
        val statusTextPath =
            "feature/quickcreate/presentation/src/commonMain/kotlin/com/runninghub/feature/quickcreate/presentation/result/QuickCreateTaskStatusUi.kt"
        val resultContentPath =
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/presentation/result/QuickCreateResultContent.kt"
        val statusTextFile = rootDir.resolve(statusTextPath)
        val resultContentFile = rootDir.resolve(resultContentPath)

        listOf(statusTextFile, resultContentFile)
            .filterNot { it.isFile }
            .forEach { file -> violations += "QuickCreate task status text guard source is missing: ${file.relativeTo(rootDir).invariantSeparatorsPath}." }

        if (statusTextFile.isFile) {
            val text = statusTextFile.readText()
            if (text.contains("data class Custom(") || text.contains("QuickCreateTaskStatusText.Custom")) {
                violations += "$statusTextPath must not define or document QuickCreateTaskStatusText.Custom; use stable status or error semantics."
            }
        }
        if (resultContentFile.isFile) {
            val text = resultContentFile.readText()
            if (text.contains("is QuickCreateTaskStatusText.Custom")) {
                violations += "$resultContentPath must not render QuickCreateTaskStatusText.Custom directly."
            }
        }
    }

    /**
     * 防止 History 页面重新按运行时任务标题或固定资源推断费用、进度和输出数量。
     *
     * 费用和输出数量必须来自 Task Domain 的服务端字段；当服务端没有返回时，UI 应隐藏或展示
     * 中性空态，不能用固定金额、固定进度或固定输出数量伪造数据。
     */
    private fun Project.checkTaskHistoryPresentationTextGuard(violations: MutableList<String>) {
        val historyScreenPath =
            "composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt"
        val stateHolderPath =
            "feature/task/presentation/src/commonMain/kotlin/com/runninghub/feature/task/presentation/TaskHistoryStateHolder.kt"
        val historyStringsPath = "composeApp/src/commonMain/composeResources/values/strings.xml"

        val historyScreenFile = rootDir.resolve(historyScreenPath)
        if (historyScreenFile.isFile) {
            val text = historyScreenFile.readText()
            if (text.contains("title.contains(\"\\u89c6\\u9891\")") ||
                text.contains("title.contains(\"\\u89d2\\u8272\")")
            ) {
                violations += "$historyScreenPath must not infer task cost or output count from runtime title text; use server cost/output fields."
            }
            if (text.contains("TaskHistoryCostText") || text.contains("TaskHistoryOutputCountText")) {
                violations += "$historyScreenPath must not map fixed TaskHistory cost/output semantics; use server cost/output fields."
            }
        } else {
            violations += "Task history screen guard target is missing: $historyScreenPath."
        }

        val stateHolderFile = rootDir.resolve(stateHolderPath)
        if (stateHolderFile.isFile) {
            val text = stateHolderFile.readText()
            if (text.contains("enum class TaskHistoryCostText") ||
                text.contains("enum class TaskHistoryOutputCountText") ||
                text.contains("toHistoryCostText") ||
                text.contains("toHistoryOutputCountText")
            ) {
                violations += "$stateHolderPath must preserve server cost/output values instead of fixed cost/output text semantics."
            }
        } else {
            violations += "Task history presentation guard target is missing: $stateHolderPath."
        }

        val historyStringsFile = rootDir.resolve(historyStringsPath)
        if (historyStringsFile.isFile) {
            val text = historyStringsFile.readText()
            val forbiddenResourceNames = listOf(
                "task_history_status_in_progress_percent",
                "task_history_default_completed_duration",
                "task_history_default_failed_duration",
                "task_history_default_running_duration",
                "task_history_cost_video",
                "task_history_cost_character",
                "task_history_cost_failed",
                "task_history_cost_default",
                "task_history_output_count_failed",
                "task_history_output_count_running",
                "task_history_output_count_video",
                "task_history_output_count_completed",
            )
            forbiddenResourceNames
                .filter { resourceName -> text.contains(resourceName) }
                .forEach { resourceName ->
                    violations += "$historyStringsPath must not define fixed History runtime data resource '$resourceName'."
                }
        } else {
            violations += "Task history string guard target is missing: $historyStringsPath."
        }
    }

    /**
     * 防止 QuickCreate 旧版 Domain 模型重新承载最终展示文案。
     *
     * 旧版图片/视频模型枚举只应作为 Data 路由用的稳定标识和能力开关。模型名称、说明或
     * 兼容展示文本必须留在 Presentation 语义或 Compose Resources 边界，避免 Domain
     * 再次保存中文 UI 文案。
     */
    private fun Project.checkQuickCreateDomainModelTextGuard(violations: MutableList<String>) {
        val relativePath = "feature/quickcreate/domain/src/commonMain/kotlin/com/runninghub/feature/quickcreate/domain/QuickCreationModels.kt"
        val file = rootDir.resolve(relativePath)
        if (!file.isFile) {
            violations += "QuickCreate domain model source is missing: $relativePath."
            return
        }

        val text = file.readText()
        listOf("val displayName:", "val description:").forEach { forbidden ->
            if (text.contains(forbidden)) {
                violations += "$relativePath must not define $forbidden on legacy Domain model enums; use stable Presentation text semantics instead."
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
     * 只在主分支 push 或人工触发时写入依赖图，不能在不可信 PR 上申请 `contents: write`。
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
                "workflow_dispatch:",
                "actions/setup-java@v4",
                "java-version: \"17\"",
                "gradle/actions/dependency-submission@v4",
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
