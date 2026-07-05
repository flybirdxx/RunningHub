package com.runninghub.app.history

import com.runninghub.feature.detail.presentation.AppDetailSubmittedTask
import com.runninghub.core.storage.UiStateSnapshotStore
import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistorySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * WebApp 本机任务历史补充缓存。
 *
 * 控制台任务宽表是 History 页的主数据源，但任务刚提交后服务端写入宽表可能存在短暂延迟。
 * 因此 App 需要把本机刚提交的 AI 应用任务暂存到内存列表；一旦宽表返回同一个 taskId，
 * 统一历史仓库会让服务端记录接管，避免长期展示本地兜底数据。
 */
class WebAppTaskHistoryOverlayStore(
    private val snapshotStore: UiStateSnapshotStore? = null,
    private val json: Json? = null,
) {
    // JVM synchronized 在 Kotlin/Native 不可用；用 MutableStateFlow 的 CAS 更新保证跨平台原子性。
    private val trackedItems = MutableStateFlow<List<GenerationHistoryItem>>(emptyList())
    private var restored = false

    /**
     * 记录详情页刚提交成功的 WebApp 任务。
     *
     * @param task 提交响应和详情页上下文组成的轻量任务快照，不包含 API Key、Cookie 或输入请求体。
     */
    fun trackSubmittedTask(
        task: AppDetailSubmittedTask,
        coroutineScope: CoroutineScope? = null,
    ) {
        upsert(
            GenerationHistoryItem(
                taskId = task.taskId,
                source = GenerationHistorySource.WEBAPP,
                status = task.status.ifBlank { "SUBMITTED" },
                modelId = task.webappId,
                taskType = task.taskName ?: task.webappId,
            ),
            coroutineScope = coroutineScope,
        )
    }

    /**
     * 插入或更新一条补充历史。
     *
     * 新任务放在列表前面，使刚提交的任务在历史页首屏可见；相同 taskId 后续刷新只替换内容。
     */
    fun upsert(
        item: GenerationHistoryItem,
        coroutineScope: CoroutineScope? = null,
    ) {
        trackedItems.update { current ->
            listOf(item) + current.filterNot { it.taskId == item.taskId }
        }
        persist(coroutineScope)
    }

    /**
     * 读取当前补充历史快照。
     */
    fun items(): List<GenerationHistoryItem> = trackedItems.value

    /**
     * 从本地快照恢复补充历史。
     *
     * 该方法由统一历史仓库在拉取历史前调用；快照损坏时会清理本地数据，避免反复恢复失败。
     */
    suspend fun restore() {
        if (restored) return
        restored = true
        val store = snapshotStore ?: return
        val parser = json ?: return
        val raw = store.getSnapshot(KEY) ?: return
        val restoredItems = runCatching {
            parser.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
                element.jsonObject.toGenerationHistoryItemOrNull()
            }
        }.getOrElse {
            store.clearSnapshot(KEY)
            return
        }
        if (restoredItems.isEmpty()) return
        trackedItems.update { current ->
            (current + restoredItems).distinctBy { it.taskId }
        }
    }

    private fun persist(coroutineScope: CoroutineScope?) {
        val store = snapshotStore ?: return
        coroutineScope ?: return
        val snapshot = trackedItems.value.toOverlayJsonString()
        coroutineScope.launch {
            if (snapshot == EMPTY_OVERLAY_JSON) {
                store.clearSnapshot(KEY)
            } else {
                store.saveSnapshot(KEY, snapshot)
            }
        }
    }

    private fun List<GenerationHistoryItem>.toOverlayJsonString(): String =
        buildJsonArray {
            take(MAX_OVERLAY_ITEMS).forEach { item ->
                add(item.toOverlayJsonObject())
            }
        }.toString()

    private fun GenerationHistoryItem.toOverlayJsonObject(): JsonObject =
        buildJsonObject {
            put("taskId", taskId)
            put("source", source.key)
            put("status", status)
            modelId?.let { put("modelId", it) }
            taskType?.let { put("taskType", it) }
            put("costAmount", costAmount)
            costCurrency?.let { put("costCurrency", it) }
            costTime?.let { put("costTime", it) }
            put(
                "outputs",
                buildJsonArray {
                    outputs.take(MAX_OVERLAY_OUTPUTS).forEach { output ->
                        add(output.toOverlayJsonObject())
                    }
                },
            )
        }

    private fun GenerationHistoryOutput.toOverlayJsonObject(): JsonObject =
        buildJsonObject {
            put("outputId", outputId)
            put("url", url)
            put("type", type)
            thumbnailUrl?.let { put("thumbnailUrl", it) }
            width?.let { put("width", it) }
            height?.let { put("height", it) }
            outputName?.let { put("outputName", it) }
            expireTime?.let { put("expireTime", it) }
            expireDays?.let { put("expireDays", it) }
        }

    private fun JsonObject.toGenerationHistoryItemOrNull(): GenerationHistoryItem? {
        val taskId = stringValue("taskId")?.takeIf { it.isNotBlank() } ?: return null
        return GenerationHistoryItem(
            taskId = taskId,
            source = stringValue("source").toGenerationHistorySource(),
            status = stringValue("status")?.takeIf { it.isNotBlank() } ?: "SUBMITTED",
            modelId = stringValue("modelId"),
            taskType = stringValue("taskType"),
            costAmount = this["costAmount"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            costCurrency = stringValue("costCurrency"),
            costTime = stringValue("costTime"),
            outputs = (this["outputs"] as? JsonArray)
                ?.mapNotNull { it.jsonObject.toGenerationHistoryOutputOrNull() }
                .orEmpty(),
        )
    }

    private fun JsonObject.toGenerationHistoryOutputOrNull(): GenerationHistoryOutput? {
        val outputId = stringValue("outputId")?.takeIf { it.isNotBlank() } ?: return null
        val url = stringValue("url")?.takeIf { it.isNotBlank() } ?: return null
        return GenerationHistoryOutput(
            outputId = outputId,
            url = url,
            type = stringValue("type")?.takeIf { it.isNotBlank() } ?: "file",
            thumbnailUrl = stringValue("thumbnailUrl"),
            width = this["width"]?.jsonPrimitive?.intOrNull,
            height = this["height"]?.jsonPrimitive?.intOrNull,
            outputName = stringValue("outputName"),
            expireTime = stringValue("expireTime"),
            expireDays = stringValue("expireDays"),
        )
    }

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun String?.toGenerationHistorySource(): GenerationHistorySource =
        enumValues<GenerationHistorySource>().firstOrNull { source ->
            source.name == this || source.key == this
        } ?: GenerationHistorySource.WEBAPP

    private companion object {
        private const val KEY = "webapp_task_history_overlay_v1"
        private const val EMPTY_OVERLAY_JSON = "[]"
        private const val MAX_OVERLAY_ITEMS = 30
        private const val MAX_OVERLAY_OUTPUTS = 8
    }
}
