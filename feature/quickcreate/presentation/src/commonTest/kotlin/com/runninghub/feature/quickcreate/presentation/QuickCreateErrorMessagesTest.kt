package com.runninghub.feature.quickcreate.presentation

import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryException
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateErrorMessagesTest {

    @Test
    fun `repository issue code maps to stable presentation error`() {
        val error = QuickCreateRepositoryException(
            issueCode = QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED,
        )

        assertEquals(
            QuickCreatePresentationError.HistoryLoadFailed,
            error.toQuickCreatePresentationError(QuickCreatePresentationError.GenerationFailed),
        )
    }

    @Test
    fun `remote business summary is not exposed as presentation error`() {
        val error = QuickCreateRepositoryException(
            issueCode = QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED,
            remoteMessage = "duplicate project name",
            remoteStatusCode = 409,
        )

        assertEquals(
            QuickCreatePresentationError.ProjectCreateFailed,
            error.toQuickCreatePresentationError(QuickCreatePresentationError.GenerationFailed),
        )
        assertEquals(QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED, error.message)
    }

    @Test
    fun `project issue codes map to project presentation errors`() {
        val mappings = listOf(
            QuickCreateRepositoryIssueCode.PROJECT_LIST_LOAD_FAILED to
                QuickCreatePresentationError.ProjectListLoadFailed,
            QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED to
                QuickCreatePresentationError.ProjectCreateFailed,
            QuickCreateRepositoryIssueCode.PROJECT_RENAME_FAILED to
                QuickCreatePresentationError.ProjectRenameFailed,
            QuickCreateRepositoryIssueCode.PROJECT_DELETE_FAILED to
                QuickCreatePresentationError.ProjectDeleteFailed,
            QuickCreateRepositoryIssueCode.PROJECT_PIN_FAILED to
                QuickCreatePresentationError.ProjectPinFailed,
            QuickCreateRepositoryIssueCode.PROJECT_DETAIL_LOAD_FAILED to
                QuickCreatePresentationError.ProjectDetailLoadFailed,
        )

        mappings.forEach { (issueCode, expectedError) ->
            val error = QuickCreateRepositoryException(
                issueCode = issueCode,
                remoteMessage = "server detail should not be displayed",
            )

            assertEquals(
                expectedError,
                error.toQuickCreatePresentationError(QuickCreatePresentationError.GenerationFailed),
            )
        }
    }

    @Test
    fun `history issue codes map to history presentation errors`() {
        val mappings = listOf(
            QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED to
                QuickCreatePresentationError.HistoryLoadFailed,
            QuickCreateRepositoryIssueCode.HISTORY_DETAIL_LOAD_FAILED to
                QuickCreatePresentationError.HistoryDetailLoadFailed,
            QuickCreateRepositoryIssueCode.TASK_CANCEL_FAILED to
                QuickCreatePresentationError.TaskCancelFailed,
            QuickCreateRepositoryIssueCode.PROJECT_TASK_LIST_LOAD_FAILED to
                QuickCreatePresentationError.ProjectTaskListLoadFailed,
        )

        mappings.forEach { (issueCode, expectedError) ->
            val error = QuickCreateRepositoryException(
                issueCode = issueCode,
                remoteMessage = "server detail should not be displayed",
            )

            assertEquals(
                expectedError,
                error.toQuickCreatePresentationError(QuickCreatePresentationError.GenerationFailed),
            )
        }
    }

    @Test
    fun `task issue code can share the same mapper outside polling controller`() {
        val error = IllegalStateException(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED)

        assertEquals(
            QuickCreatePresentationError.FeePreviewFailed,
            error.toQuickCreatePresentationError(QuickCreatePresentationError.GenerationFailed),
        )
    }

    @Test
    fun `task polling issue codes reuse centralized presentation errors`() {
        val mappings = listOf(
            QuickCreateTaskIssueCode.TASK_FAILED to QuickCreatePresentationError.TaskFailed,
            QuickCreateTaskIssueCode.TASK_TIMEOUT to QuickCreatePresentationError.TaskTimeout,
            QuickCreateTaskIssueCode.TASK_QUERY_FAILED to QuickCreatePresentationError.TaskQueryFailed,
            QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED to QuickCreatePresentationError.FeePreviewFailed,
            QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED to QuickCreatePresentationError.FeePreviewNotPassed,
            QuickCreateTaskIssueCode.PREPARE_FAILED to QuickCreatePresentationError.PrepareFailed,
            QuickCreateTaskIssueCode.COMMIT_FAILED to QuickCreatePresentationError.CommitFailed,
            QuickCreateTaskIssueCode.UNKNOWN_ERROR to QuickCreatePresentationError.GenerationFailed,
            "network unavailable" to QuickCreatePresentationError.GenerationFailed,
        )

        mappings.forEach { (issueCode, expectedError) ->
            assertEquals(expectedError, issueCode.toQuickCreateTaskIssueError())
        }
    }
}
