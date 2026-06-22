package com.runninghub.feature.quickcreate.presentation.project

import com.runninghub.feature.quickcreate.domain.QuickCreationProject
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectPage
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateProjectStateHolderTest {

    @Test
    fun `load projects writes first page ui items`() = runTest {
        val repository = FakeProjectRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadProjects()
        runCurrent()

        assertEquals(listOf(1), repository.requestedPages)
        assertFalse(state.value.projectsLoading)
        assertEquals("project-1", state.value.projects.single().projectId)
        assertEquals("世界杯广告", state.value.projects.single().name)
        assertEquals(true, state.value.projects.single().isPinned)
        assertEquals(
            QuickCreateProjectPinContentDescription.UnpinProject,
            state.value.projects.single().pinContentDescription,
        )
        assertEquals(QuickCreateProjectTaskCountText(3), state.value.projects.single().taskCountText)
        assertEquals("https://example.com/project.png", state.value.projects.single().coverUrl)
        assertFalse(state.value.projectsHasMore)
    }

    @Test
    fun `load more projects appends and deduplicates by project id`() = runTest {
        val repository = FakeProjectRepository().apply {
            pages = mapOf(
                1 to projectPage(
                    page = 1,
                    total = 3,
                    hasNext = true,
                    items = listOf(project("project-1", "世界杯广告")),
                ),
                2 to projectPage(
                    page = 2,
                    total = 3,
                    hasNext = false,
                    items = listOf(
                        project("project-1", "世界杯广告重复"),
                        project("project-2", "新品海报"),
                    ),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadProjects()
        runCurrent()
        holder.loadMoreProjects()
        runCurrent()

        assertEquals(listOf(1, 2), repository.requestedPages)
        assertEquals(listOf("project-1", "project-2"), state.value.projects.map { it.projectId })
        assertEquals(2, state.value.projectsPage)
        assertFalse(state.value.projectsHasMore)
        assertFalse(state.value.projectsLoadingMore)
    }

    @Test
    fun `project failures use safe fallback messages without exposing exception text`() = runTest {
        val repository = FakeProjectRepository().apply {
            listMoreFailure = IllegalStateException("remote list stack")
            pinFailure = IllegalStateException("remote pin stack")
            createFailure = IllegalStateException("remote create stack")
            renameFailure = IllegalStateException("remote rename stack")
            deleteFailure = IllegalStateException("remote delete stack")
            detailFailure = IllegalStateException("remote detail stack")
        }
        val state = MutableStateFlow(
            QuickCreateUiState(
                projects = listOf(project("project-1", "世界杯广告").toQuickCreateProjectUiItem()),
                projectsPage = 1,
                projectsHasMore = true,
            )
        )
        val holder = createHolder(repository, state, this)

        holder.loadMoreProjects()
        runCurrent()
        assertEquals(QuickCreatePresentationError.ProjectListLoadFailed.asQuickCreateUiMessage(), state.value.error)

        holder.toggleProjectPin("project-1")
        runCurrent()
        assertEquals(QuickCreatePresentationError.ProjectPinFailed.asQuickCreateUiMessage(), state.value.error)

        holder.createProject("新项目")
        runCurrent()
        assertEquals(QuickCreatePresentationError.ProjectCreateFailed.asQuickCreateUiMessage(), state.value.error)

        holder.renameProject("project-1", "新名称")
        runCurrent()
        assertEquals(QuickCreatePresentationError.ProjectRenameFailed.asQuickCreateUiMessage(), state.value.error)

        holder.deleteProject("project-1")
        runCurrent()
        assertEquals(QuickCreatePresentationError.ProjectDeleteFailed.asQuickCreateUiMessage(), state.value.error)

        holder.selectProjectDetail("project-1")
        runCurrent()
        assertEquals(QuickCreatePresentationError.ProjectDetailLoadFailed.asQuickCreateUiMessage(), state.value.error)
    }

    @Test
    fun `toggle project pin calls repository and updates local project state`() = runTest {
        val repository = FakeProjectRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadProjects()
        runCurrent()
        holder.toggleProjectPin("project-1")
        runCurrent()

        assertEquals(listOf("project-1" to false), repository.pinRequests)
        assertEquals(false, state.value.projects.single().isPinned)
        assertEquals(
            QuickCreateProjectPinContentDescription.PinProject,
            state.value.projects.single().pinContentDescription,
        )
        assertEquals(QuickCreateProjectPinStatusText.NotPinned, state.value.projects.single().pinStatusText)
        assertEquals(emptySet(), state.value.projectPinningIds)
    }

    @Test
    fun `create project trims name and prepends created project`() = runTest {
        val repository = FakeProjectRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadProjects()
        runCurrent()
        holder.createProject("  新项目  ")
        runCurrent()

        assertEquals(listOf("新项目"), repository.createdNames)
        assertEquals("project-new", state.value.projects.first().projectId)
        assertEquals("新项目", state.value.projects.first().name)
        assertEquals(QuickCreateProjectTaskCountText(0), state.value.projects.first().taskCountText)
        assertEquals(emptySet(), state.value.projectMutatingIds)
    }

    @Test
    fun `rename project updates local item after repository succeeds`() = runTest {
        val repository = FakeProjectRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadProjects()
        runCurrent()
        holder.renameProject("project-1", "  新名称  ")
        runCurrent()

        assertEquals(listOf("project-1" to "新名称"), repository.renameRequests)
        assertEquals("新名称", state.value.projects.single().name)
        assertEquals(emptySet(), state.value.projectMutatingIds)
    }

    @Test
    fun `delete selected project clears selection and invokes callback`() = runTest {
        val repository = FakeProjectRepository()
        val state = MutableStateFlow(
            QuickCreateUiState(
                selectedProjectId = "project-1",
                historyPage = 2,
                historyTotal = 2,
                historyHasMore = true,
            )
        )
        var selectedProjectDeletedCallbacks = 0
        val holder = createHolder(repository, state, this) {
            selectedProjectDeletedCallbacks += 1
        }

        holder.loadProjects()
        runCurrent()
        holder.deleteProject("project-1")
        runCurrent()

        assertEquals(listOf("project-1"), repository.deletedProjectIds)
        assertEquals(emptyList(), state.value.projects)
        assertNull(state.value.selectedProjectId)
        assertEquals(0, state.value.historyPage)
        assertEquals(0, state.value.historyTotal)
        assertFalse(state.value.historyHasMore)
        assertEquals(1, selectedProjectDeletedCallbacks)
        assertEquals(emptySet(), state.value.projectMutatingIds)
    }

    @Test
    fun `select project detail loads detail ui state`() = runTest {
        val repository = FakeProjectRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.selectProjectDetail("project-1")
        runCurrent()

        assertEquals("project-1", repository.lastDetailProjectId)
        assertFalse(state.value.projectDetailLoading)
        assertEquals("项目详情", state.value.selectedProjectDetail?.name)
        assertEquals("https://example.com/project-detail.png", state.value.selectedProjectDetail?.coverUrl)
        assertEquals(
            listOf(
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.TaskCount,
                    value = QuickCreateProjectDetailRowValue.TaskCount(9),
                ),
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.PinStatus,
                    value = QuickCreateProjectDetailRowValue.PinStatus(QuickCreateProjectPinStatusText.Pinned),
                ),
            ),
            state.value.selectedProjectDetail?.rows,
        )
    }

    private fun createHolder(
        repository: FakeProjectRepository,
        state: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
        onSelectedProjectDeleted: () -> Unit = {},
    ): QuickCreateProjectStateHolder =
        QuickCreateProjectStateHolder(
            projectRepository = repository,
            scope = scope,
            uiState = state,
            onSelectedProjectDeleted = onSelectedProjectDeleted,
        )

    /**
     * 项目 StateHolder 的最小仓库替身。
     *
     * 测试只覆盖项目集合本身的加载和变更状态，因此 fake 只实现项目仓库接口，
     * 不混入历史、生成、上传或灵感模板能力，避免 feature presentation 测试重新耦合到 app 层大仓库替身。
     */
    private class FakeProjectRepository : QuickCreationProjectRepository {
        var pages: Map<Int, QuickCreationProjectPage>? = null
        val requestedPages = mutableListOf<Int>()
        val pinRequests = mutableListOf<Pair<String, Boolean>>()
        val createdNames = mutableListOf<String>()
        val renameRequests = mutableListOf<Pair<String, String>>()
        val deletedProjectIds = mutableListOf<String>()
        var lastDetailProjectId: String? = null
        var createdProject = project("project-new", "新项目")
        var listMoreFailure: Throwable? = null
        var pinFailure: Throwable? = null
        var createFailure: Throwable? = null
        var renameFailure: Throwable? = null
        var deleteFailure: Throwable? = null
        var detailFailure: Throwable? = null
        var detailProject = project(
            projectId = "project-1",
            name = "项目详情",
            coverUrl = "https://example.com/project-detail.png",
            taskCount = 9,
            pinned = true,
        )

        /**
         * 返回测试预设的项目分页，并记录页码以验证加载顺序。
         */
        override suspend fun listQuickCreationProjects(
            page: Int,
            size: Int,
        ): Result<QuickCreationProjectPage> {
            requestedPages += page
            if (page > 1) {
                listMoreFailure?.let { return Result.failure(it) }
            }
            return Result.success(pages?.get(page) ?: projectPage(page = page, size = size))
        }

        /**
         * 创建项目时记录裁剪后的名称，确保 StateHolder 不提交空白或未裁剪输入。
         */
        override suspend fun createQuickCreationProject(name: String): Result<QuickCreationProject> {
            createdNames += name
            createFailure?.let { return Result.failure(it) }
            return Result.success(createdProject.copy(name = name))
        }

        /**
         * 记录重命名请求；本 fake 默认返回成功，让测试关注本地状态是否在成功后更新。
         */
        override suspend fun renameQuickCreationProject(projectId: String, name: String): Result<Unit> {
            renameRequests += projectId to name
            renameFailure?.let { return Result.failure(it) }
            return Result.success(Unit)
        }

        /**
         * 记录删除请求；删除后是否刷新历史由 StateHolder 的回调边界验证。
         */
        override suspend fun deleteQuickCreationProject(projectId: String): Result<Unit> {
            deletedProjectIds += projectId
            deleteFailure?.let { return Result.failure(it) }
            return Result.success(Unit)
        }

        /**
         * 记录目标置顶状态，用于验证本地状态只在远端成功后同步。
         */
        override suspend fun pinQuickCreationProject(projectId: String, pinned: Boolean): Result<Unit> {
            pinRequests += projectId to pinned
            pinFailure?.let { return Result.failure(it) }
            return Result.success(Unit)
        }

        /**
         * 返回项目详情快照，并记录调用的项目 ID。
         */
        override suspend fun getQuickCreationProjectDetail(projectId: String): Result<QuickCreationProject> {
            lastDetailProjectId = projectId
            detailFailure?.let { return Result.failure(it) }
            return Result.success(detailProject.copy(projectId = projectId))
        }
    }
}

private fun projectPage(
    page: Int,
    size: Int = 20,
    total: Int = 1,
    pages: Int = 1,
    hasNext: Boolean = false,
    items: List<QuickCreationProject> = listOf(
        project(
            projectId = "project-1",
            name = "世界杯广告",
            coverUrl = "https://example.com/project.png",
            taskCount = 3,
            pinned = true,
        )
    ),
): QuickCreationProjectPage =
    QuickCreationProjectPage(
        page = page,
        size = size,
        total = total,
        pages = pages,
        hasNext = hasNext,
        hasPrevious = page > 1,
        items = items,
    )

private fun project(
    projectId: String,
    name: String,
    coverUrl: String? = null,
    taskCount: Int = 0,
    pinned: Boolean = false,
): QuickCreationProject =
    QuickCreationProject(
        projectId = projectId,
        name = name,
        coverUrl = coverUrl,
        taskCount = taskCount,
        pinned = pinned,
    )
