package com.runninghub.feature.quickcreate.presentation

import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryException
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateErrorMessagesTest {

    @Test
    fun `repository issue code maps to presentation message`() {
        val error = QuickCreateRepositoryException(
            issueCode = QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED,
        )

        assertEquals("历史加载失败", error.toQuickCreateDisplayMessage("fallback"))
    }

    @Test
    fun `remote business summary is not exposed as display message`() {
        val error = QuickCreateRepositoryException(
            issueCode = QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED,
            remoteMessage = "duplicate project name",
            remoteStatusCode = 409,
        )

        assertEquals("项目创建失败", error.toQuickCreateDisplayMessage("项目创建失败"))
        assertEquals(QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED, error.message)
    }

    @Test
    fun `project issue codes reuse project fallback messages`() {
        val mappings = listOf(
            QuickCreateRepositoryIssueCode.PROJECT_LIST_LOAD_FAILED to
                QuickCreateErrorFallbackText.PROJECT_LIST_LOAD_FAILED,
            QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED to
                QuickCreateErrorFallbackText.PROJECT_CREATE_FAILED,
            QuickCreateRepositoryIssueCode.PROJECT_RENAME_FAILED to
                QuickCreateErrorFallbackText.PROJECT_RENAME_FAILED,
            QuickCreateRepositoryIssueCode.PROJECT_DELETE_FAILED to
                QuickCreateErrorFallbackText.PROJECT_DELETE_FAILED,
            QuickCreateRepositoryIssueCode.PROJECT_PIN_FAILED to
                QuickCreateErrorFallbackText.PROJECT_PIN_FAILED,
            QuickCreateRepositoryIssueCode.PROJECT_DETAIL_LOAD_FAILED to
                QuickCreateErrorFallbackText.PROJECT_DETAIL_LOAD_FAILED,
        )

        mappings.forEach { (issueCode, expectedMessage) ->
            val error = QuickCreateRepositoryException(
                issueCode = issueCode,
                remoteMessage = "server detail should not be displayed",
            )

            assertEquals(expectedMessage, error.toQuickCreateDisplayMessage("fallback"))
        }
    }


    @Test
    fun `task issue code can share the same mapper outside polling controller`() {
        val error = IllegalStateException(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED)

        assertEquals("价格预览失败", error.toQuickCreateDisplayMessage("fallback"))
    }
}
