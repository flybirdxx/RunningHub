package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作项目管理的领域仓库边界。
 *
 * 本接口只承载项目列表、项目详情和项目增删改置顶能力，供项目区域维护项目集合本身。
 * 项目下的任务列表仍返回 [QuickCreationHistoryPage]，由历史区域负责读取和刷新，避免项目仓库
 * 同时承担历史分页与任务轮询职责。
 */
interface QuickCreationProjectRepository {

    /**
     * 分页读取快捷创作项目列表。
     *
     * @param page 页码，从 1 开始；小于 1 的值应由调用方避免传入。
     * @param size 每页项目数量，单位为个；默认值保持与快捷创作项目栏一致。
     * @return 项目分页结果；失败时通过 [Result.failure] 返回，由 Presentation 保留或展示旧状态。
     */
    suspend fun listQuickCreationProjects(page: Int = 1, size: Int = 20): Result<QuickCreationProjectPage>

    /**
     * 创建一个快捷创作项目。
     *
     * @param name 用户输入并裁剪后的项目名称；空字符串不应提交到仓库。
     * @return 服务端创建后的项目对象，包含可用于后续重命名、删除和筛选任务的稳定项目 ID。
     */
    suspend fun createQuickCreationProject(name: String): Result<QuickCreationProject>

    /**
     * 重命名快捷创作项目。
     *
     * @param projectId 项目稳定标识，来源于项目列表或详情；空字符串不应提交。
     * @param name 用户输入并裁剪后的新名称；空字符串不应提交。
     * @return 成功时返回 [Unit]；失败时调用方应保留旧项目名称。
     */
    suspend fun renameQuickCreationProject(projectId: String, name: String): Result<Unit>

    /**
     * 删除快捷创作项目。
     *
     * @param projectId 项目稳定标识，来源于项目列表或详情；删除后该项目不应再作为历史筛选条件。
     * @return 成功时返回 [Unit]；失败时调用方应保留项目列表和当前选中状态。
     */
    suspend fun deleteQuickCreationProject(projectId: String): Result<Unit>

    /**
     * 切换项目置顶状态。
     *
     * @param projectId 项目稳定标识，来源于项目列表或详情。
     * @param pinned `true` 表示请求置顶该项目；`false` 表示取消置顶。
     * @return 成功时返回 [Unit]；失败时调用方应保留原置顶状态。
     */
    suspend fun pinQuickCreationProject(projectId: String, pinned: Boolean): Result<Unit>

    /**
     * 读取快捷创作项目详情。
     *
     * @param projectId 项目稳定标识，来源于项目列表；空字符串不应提交。
     * @return 项目详情，通常包含服务端最新名称、封面、任务数量和置顶状态。
     */
    suspend fun getQuickCreationProjectDetail(projectId: String): Result<QuickCreationProject>
}
