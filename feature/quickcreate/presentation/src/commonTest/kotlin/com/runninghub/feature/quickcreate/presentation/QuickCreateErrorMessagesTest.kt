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
    fun `history issue codes reuse history fallback messages`() {
        val mappings = listOf(
            QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED to
                QuickCreateErrorFallbackText.HISTORY_LOAD_FAILED,
            QuickCreateRepositoryIssueCode.HISTORY_DETAIL_LOAD_FAILED to
                QuickCreateErrorFallbackText.HISTORY_DETAIL_LOAD_FAILED,
            QuickCreateRepositoryIssueCode.TASK_CANCEL_FAILED to
                QuickCreateErrorFallbackText.TASK_CANCEL_FAILED,
            QuickCreateRepositoryIssueCode.PROJECT_TASK_LIST_LOAD_FAILED to
                QuickCreateErrorFallbackText.PROJECT_TASK_LIST_LOAD_FAILED,
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

    @Test
    fun `task polling issue codes reuse centralized presentation messages`() {
        val mappings = listOf(
            QuickCreateTaskIssueCode.TASK_FAILED to "任务失败",
            QuickCreateTaskIssueCode.TASK_TIMEOUT to "任务超时",
            QuickCreateTaskIssueCode.TASK_QUERY_FAILED to "任务查询失败",
            QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED to QuickCreateErrorFallbackText.FEE_PREVIEW_FAILED,
            QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED to QuickCreateErrorFallbackText.FEE_PREVIEW_NOT_PASSED,
            QuickCreateTaskIssueCode.PREPARE_FAILED to "任务预提交失败",
            QuickCreateTaskIssueCode.COMMIT_FAILED to "任务提交失败",
            QuickCreateTaskIssueCode.UNKNOWN_ERROR to QuickCreateErrorFallbackText.GENERATION_FAILED,
            "network unavailable" to QuickCreateErrorFallbackText.GENERATION_FAILED,
        )

        mappings.forEach { (issueCode, expectedMessage) ->
            assertEquals(expectedMessage, issueCode.toQuickCreateTaskIssueDisplayMessage())
        }
    }
}
