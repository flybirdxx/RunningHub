package com.runninghub.app

import android.util.Log
import com.runninghub.core.storage.AppStartupStore
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android 首次安装启动时的标准模型目录预热器。
 *
 * 预热只填充标准模型目录的本地快照，不触碰 Compose 状态，也不阻塞首屏渲染。启动标记会在访问网络前写入，
 * 因此离线、接口失败或进程被系统结束后不会在后续每次启动重复预热；用户打开模型列表时仍会走正常加载兜底。
 *
 * @param startupStore 应用启动一次性任务状态存储，用于判断当前安装实例是否已经发起过预热。
 * @param modelCatalogRepository 快捷创作模型目录仓库，负责触发标准模型目录回源并把脱敏快照写入本地缓存。
 */
class ModelCatalogInitialPreloader(
    private val startupStore: AppStartupStore,
    private val modelCatalogRepository: QuickCreationModelCatalogRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 在后台发起首次安装模型目录预热。
     *
     * 该方法可安全地在每次 Application 启动时调用；真正的网络预热只会在标记缺失时运行一次。
     */
    fun start() {
        scope.launch {
            if (startupStore.hasStartedInitialModelCatalogPreload()) return@launch
            startupStore.markInitialModelCatalogPreloadStarted()
            runCatching { preloadStandardModelCatalog() }
                .onFailure { Log.d("RH.ModelPreload", "initial preload failed") }
        }
    }

    private suspend fun preloadStandardModelCatalog() {
        val imageModels = modelCatalogRepository.getModels(QuickCreationServiceKind.IMAGE).getOrNull().orEmpty()
        val videoModels = modelCatalogRepository.getModels(QuickCreationServiceKind.VIDEO).getOrNull().orEmpty()
        Log.d("RH.ModelPreload", "initial preload image=${imageModels.size} video=${videoModels.size}")
    }
}
