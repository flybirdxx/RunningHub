package com.runninghub.app.ui.feature.history

import com.runninghub.app.ui.designsystem.components.billing.BillingInfoRow
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TaskDetailResultLayoutContractTest {
    @Test
    fun `task detail layout state keeps result processing before technical diagnostics`() {
        val state = TaskDetailLayoutState(
            sectionOrder = listOf(
                TaskDetailLayoutSectionType.StatusSummary,
                TaskDetailLayoutSectionType.ResultPreview,
                TaskDetailLayoutSectionType.Actions,
                TaskDetailLayoutSectionType.Billing,
                TaskDetailLayoutSectionType.PromptParameters,
                TaskDetailLayoutSectionType.TechnicalDetails,
            ),
            resultPreview = ResultPreviewState(
                title = "生成结果",
                taskIdLabel = null,
                statusLabel = "生成完成",
                media = ResultPreviewMediaState(
                    url = "https://example.com/result.png",
                    previewUrl = "https://example.com/result-thumb.png",
                    mediaType = ResultPreviewMediaType.Image,
                    aspectRatio = 1f,
                ),
                expiryLabel = "云端结果将在 1 天后过期",
                actions = listOf(
                    ResultPreviewActionState(ResultPreviewActionType.Save, "保存"),
                    ResultPreviewActionState(ResultPreviewActionType.Download, "下载"),
                    ResultPreviewActionState(ResultPreviewActionType.ReuseParameters, "复用参数"),
                ),
            ),
            billingRows = listOf(
                BillingInfoRow(label = "RHB 消耗", value = "12", emphasized = true),
                BillingInfoRow(label = "最终金额", value = "¥0.1"),
            ),
            saveStateLabel = "未保存到本地",
            promptParameters = listOf(TaskDetailPromptParameterState(label = "prompt", value = "city")),
            technicalSections = listOf(
                TaskDetailTechnicalSectionState(
                    title = "请求信息",
                    content = """{"prompt":"city"}""",
                    initiallyExpanded = false,
                ),
            ),
        )

        assertEquals(TaskDetailLayoutSectionType.ResultPreview, state.sectionOrder[1])
        assertEquals(ResultPreviewActionType.Save, state.resultPreview.actions.first().type)
        assertEquals("12", state.billingRows.first().value)
        assertEquals("city", state.promptParameters.single().value)
        assertFalse(state.technicalSections.single().initiallyExpanded)
    }
}
