package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PROJECT_CREATE_MUTATION_ID = "__create_project__"

/**
 * 管理快捷创作中的项目列表、项目变更和项目详情状态。
 *
 * 该类位于 Presentation 层，只协调页面状态和 Domain Repository 接口，不直接访问网络、
 * DataStore 或平台能力。ScreenModel 保留项目与历史区域之间的跨职责协调，例如删除当前项目后
 * 重新加载最近历史；项目本身的加载、置顶、创建、重命名、删除和详情查询都收敛到这里。
 *
 * @param quickCreateRepository 快捷创作仓库接口，提供项目列表和项目变更能力。
 * @param scope ScreenModel 生命周期作用域，所有项目请求随页面释放而取消。
 * @param uiState 页面状态容器，项目状态通过不可变 `copy` 更新。
 * @param onSelectedProjectDeleted 当前选中项目被删除后的回调，由 ScreenModel 决定如何刷新历史区域。
 */
internal class QuickCreateProjectStateHolder(
    private val quickCreateRepository: QuickCreateRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val onSelectedProjectDeleted: () -> Unit,
) {

    /**
     * 加载项目第一页。
     *
     * 初始化页面和手动刷新项目区时调用；失败只收起加载状态，不清空已有列表，
     * 避免短暂网络错误导致用户正在查看的项目入口消失。
     */
    fun loadProjects() {
        scope.launch {
            uiState.update { it.copy(projectsLoading = true) }
            val projects = quickCreateRepository.listQuickCreationProjects(page = 1, size = 20)
            projects.fold(
                onSuccess = { page ->
                    uiState.update { state ->
                        state.copy(
                            projectsLoading = false,
                            projectsLoadingMore = false,
                            projects = page.items.map { it.toQuickCreateProjectUiItem() },
                            projectsPage = page.page,
                            projectsHasMore = page.hasNext,
                        )
                    }
                },
                onFailure = {
                    uiState.update { state -> state.copy(projectsLoading = false, projectsLoadingMore = false) }
                },
            )
        }
    }

    /**
     * 加载下一页项目并追加到现有列表。
     *
     * 使用 `projectId` 去重，避免服务端分页边界变动或重复请求时在页面上显示重复项目。
     */
    fun loadMoreProjects() {
        val state = uiState.value
        if (state.projectsLoading || state.projectsLoadingMore || !state.projectsHasMore) return

        scope.launch {
            val nextPage = uiState.value.projectsPage + 1
            uiState.update { it.copy(projectsLoadingMore = true, error = null) }
            val projects = quickCreateRepository.listQuickCreationProjects(page = nextPage, size = 20)
            projects.fold(
                onSuccess = { page ->
                    uiState.update { current ->
                        val loadedProjects = page.items.map { it.toQuickCreateProjectUiItem() }
                        current.copy(
                            projectsLoadingMore = false,
                            projects = (current.projects + loadedProjects).distinctBy { it.projectId },
                            projectsPage = page.page,
                            projectsHasMore = page.hasNext,
                        )
                    }
                },
                onFailure = { error ->
                    uiState.update { current ->
                        current.copy(
                            projectsLoadingMore = false,
                            error = error.message ?: "项目加载失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 切换项目置顶状态。
     *
     * 同一项目同时只允许一个置顶请求，避免快速点击导致本地状态与服务端目标状态交错。
     */
    fun toggleProjectPin(projectId: String) {
        val project = uiState.value.projects.firstOrNull { it.projectId == projectId } ?: return
        if (projectId in uiState.value.projectPinningIds) return

        val targetPinned = !project.isPinned
        scope.launch {
            uiState.update { state ->
                state.copy(projectPinningIds = state.projectPinningIds + projectId)
            }
            val result = quickCreateRepository.pinQuickCreationProject(projectId = projectId, pinned = targetPinned)
            result.fold(
                onSuccess = {
                    uiState.update { state ->
                        state.copy(
                            projectPinningIds = state.projectPinningIds - projectId,
                            projects = state.projects.map { item ->
                                if (item.projectId == projectId) {
                                    item.source.copy(pinned = targetPinned).toQuickCreateProjectUiItem()
                                } else {
                                    item
                                }
                            },
                        )
                    }
                },
                onFailure = { error ->
                    uiState.update { state ->
                        state.copy(
                            projectPinningIds = state.projectPinningIds - projectId,
                            error = error.message ?: "项目置顶失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 创建项目并插入列表顶部。
     *
     * 创建操作使用固定 mutation id 防重，避免用户连续提交同名项目时产生重复远程项目。
     */
    fun createProject(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val mutationId = PROJECT_CREATE_MUTATION_ID
        if (mutationId in uiState.value.projectMutatingIds) return

        scope.launch {
            uiState.update { state ->
                state.copy(projectMutatingIds = state.projectMutatingIds + mutationId)
            }
            val result = quickCreateRepository.createQuickCreationProject(trimmedName)
            result.fold(
                onSuccess = { project ->
                    uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - mutationId,
                            projects = (listOf(project.toQuickCreateProjectUiItem()) + state.projects)
                                .distinctBy { it.projectId },
                        )
                    }
                },
                onFailure = { error ->
                    uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - mutationId,
                            error = error.message ?: "项目创建失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 重命名项目并同步更新本地列表。
     *
     * 只有服务端确认成功后才改本地名称，避免离线或失败时 UI 展示不存在的项目名称。
     */
    fun renameProject(projectId: String, name: String) {
        val trimmedName = name.trim()
        if (projectId.isBlank() || trimmedName.isBlank()) return
        if (projectId in uiState.value.projectMutatingIds) return

        scope.launch {
            uiState.update { state ->
                state.copy(projectMutatingIds = state.projectMutatingIds + projectId)
            }
            val result = quickCreateRepository.renameQuickCreationProject(
                projectId = projectId,
                name = trimmedName,
            )
            result.fold(
                onSuccess = {
                    uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            projects = state.projects.map { project ->
                                if (project.projectId == projectId) {
                                    project.source.copy(name = trimmedName).toQuickCreateProjectUiItem()
                                } else {
                                    project
                                }
                            },
                        )
                    }
                },
                onFailure = { error ->
                    uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            error = error.message ?: "项目重命名失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 删除项目并维护本地项目列表。
     *
     * 如果删除的是当前选中项目，本类只清理项目和历史区域的本地选择状态，
     * 后续最近历史刷新交给 ScreenModel 的 [onSelectedProjectDeleted] 回调处理。
     */
    fun deleteProject(projectId: String) {
        if (projectId.isBlank()) return
        if (projectId in uiState.value.projectMutatingIds) return

        val wasSelected = uiState.value.selectedProjectId == projectId
        scope.launch {
            uiState.update { state ->
                state.copy(projectMutatingIds = state.projectMutatingIds + projectId)
            }
            val result = quickCreateRepository.deleteQuickCreationProject(projectId)
            result.fold(
                onSuccess = {
                    uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            projects = state.projects.filterNot { it.projectId == projectId },
                            selectedProjectId = if (wasSelected) null else state.selectedProjectId,
                            projectTasksLoading = if (wasSelected) false else state.projectTasksLoading,
                            historyItems = if (wasSelected) emptyList() else state.historyItems,
                            historyPage = if (wasSelected) 0 else state.historyPage,
                            historyTotal = if (wasSelected) 0 else state.historyTotal,
                            historyHasMore = if (wasSelected) false else state.historyHasMore,
                        )
                    }
                    if (wasSelected) {
                        onSelectedProjectDeleted()
                    }
                },
                onFailure = { error ->
                    uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            error = error.message ?: "项目删除失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 加载项目详情。
     *
     * 详情弹窗每次打开都重新请求，确保任务数量、封面和项目名称使用服务端最新数据。
     */
    fun selectProjectDetail(projectId: String) {
        if (projectId.isBlank()) return

        scope.launch {
            uiState.update {
                it.copy(
                    projectDetailLoading = true,
                    selectedProjectDetail = null,
                    error = null,
                )
            }
            val detail = quickCreateRepository.getQuickCreationProjectDetail(projectId)
            detail.fold(
                onSuccess = { project ->
                    uiState.update {
                        it.copy(
                            projectDetailLoading = false,
                            selectedProjectDetail = project.toQuickCreateProjectDetailUiItem(),
                        )
                    }
                },
                onFailure = { error ->
                    uiState.update {
                        it.copy(
                            projectDetailLoading = false,
                            error = error.message ?: "项目详情加载失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 关闭项目详情弹窗并清理详情状态。
     */
    fun dismissProjectDetail() {
        uiState.update {
            it.copy(
                projectDetailLoading = false,
                selectedProjectDetail = null,
            )
        }
    }
}
