package com.runninghub.app.ui.feature.plaza

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.community.domain.PlazaRepository
import com.runninghub.feature.community.presentation.PlazaMode
import com.runninghub.feature.community.presentation.PlazaStateHolder
import com.runninghub.feature.community.presentation.PlazaUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * 社区广场页面在 composeApp 中的 Voyager 生命周期适配器。
 *
 * 实际的页面状态、筛选、分页和降级内容逻辑已迁入 [PlazaStateHolder]，本类只负责把
 * Voyager 的 [screenModelScope] 交给 Community Presentation 层，并向 UI 暴露兼容的操作方法。
 *
 * @param plazaRepository 社区领域仓库，由平台启动层装配的 Community Data 模块提供实现。
 */
class PlazaScreenModel(
    plazaRepository: PlazaRepository,
) : ScreenModel {
    private val stateHolder = PlazaStateHolder(
        plazaRepository = plazaRepository,
        scope = screenModelScope,
    )

    /**
     * 社区广场页面的只读状态流。
     *
     * UI 通过该状态渲染灵感创作、短片、筛选、分页和错误提示。
     */
    val uiState: StateFlow<PlazaUiState> = stateHolder.uiState

    /**
     * 加载广场首屏数据。
     */
    fun loadInitialData() {
        stateHolder.loadInitialData()
    }

    /**
     * 刷新当前灵感创作列表。
     */
    fun refreshCreations() {
        stateHolder.refreshCreations()
    }

    /**
     * 加载下一页灵感创作。
     */
    fun loadMoreCreations() {
        stateHolder.loadMoreCreations()
    }

    /**
     * 切换灵感标签筛选。
     *
     * @param tagId 用户选择的标签 ID；`null` 表示清空标签筛选。
     */
    fun selectTag(tagId: String?) {
        stateHolder.selectTag(tagId)
    }

    /**
     * 切换灵感排序。
     *
     * @param sort 社区接口排序协议值。
     */
    fun selectSort(sort: String) {
        stateHolder.selectSort(sort)
    }

    /**
     * 切换广场内容模式。
     *
     * @param mode 目标内容模式，灵感或短片。
     */
    fun selectMode(mode: PlazaMode) {
        stateHolder.selectMode(mode)
    }

    /**
     * 加载短片分类和第一页短片。
     */
    fun loadShorts() {
        stateHolder.loadShorts()
    }

    /**
     * 切换短片分类筛选。
     *
     * @param categoryCode 服务端短片分类编码；`null` 表示全部短片。
     */
    fun selectShortCategory(categoryCode: String?) {
        stateHolder.selectShortCategory(categoryCode)
    }

    /**
     * 加载下一页短片。
     */
    fun loadMoreShorts() {
        stateHolder.loadMoreShorts()
    }
}
