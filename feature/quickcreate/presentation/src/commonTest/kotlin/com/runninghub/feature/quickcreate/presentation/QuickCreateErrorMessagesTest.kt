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
    fun `remote business summary is preserved instead of fallback mapping`() {
        val error = QuickCreateRepositoryException(
            issueCode = QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED,
            remoteMessage = "duplicate project name",
            remoteStatusCode = 409,
        )

        assertEquals("duplicate project name", error.toQuickCreateDisplayMessage("项目创建失败"))
    }

    @Test
    fun `task issue code can share the same mapper outside polling controller`() {
        val error = IllegalStateException(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED)

        assertEquals("价格预览失败", error.toQuickCreateDisplayMessage("fallback"))
    }
}
