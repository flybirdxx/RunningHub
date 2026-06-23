package com.runninghub.app.ui.feature.detail

import com.runninghub.app.di.appModule
import com.runninghub.app.platform.MediaResolver
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.Cover
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.PageData
import com.runninghub.core.model.StatisticsInfo
import com.runninghub.core.model.Tag
import com.runninghub.core.model.TagSimple
import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.model.TaskFailedReason
import com.runninghub.core.model.TaskOutput
import com.runninghub.core.model.TaskResult
import com.runninghub.core.model.UploadResult
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.CatalogQuery
import com.runninghub.feature.discovery.domain.CatalogTagRange
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import com.runninghub.feature.detail.presentation.AppDetailErrorText
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailTaskStep
import com.runninghub.feature.task.domain.WebAppTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDetail uses public detail and initializes input values`() = runTest {
        val catalogRepository = FakeWebAppCatalogRepository(
            appDetailResult = Result.success(
                appDetail(
                    id = "100",
                    inputNodes = listOf(
                        inputNode(nodeId = "1", fieldName = "prompt", fieldValue = "cat"),
                        inputNode(nodeId = "2", fieldName = "negativePrompt", fieldValue = null),
                    ),
                ),
            ),
        )
        val taskRepository = FakeWebAppTaskRepository()
        val screenModel = createScreenModel(catalogRepository, taskRepository)

        screenModel.loadDetail("100")
        advanceUntilIdle()

        assertFalse(screenModel.uiState.value.isLoading)
        assertEquals("100", screenModel.uiState.value.detail?.id)
        assertEquals("cat", screenModel.uiState.value.inputValues["1:prompt"])
        assertEquals("", screenModel.uiState.value.inputValues["2:negativePrompt"])
        assertEquals(1, catalogRepository.getAppDetailCalls)
        assertEquals(0, taskRepository.getApiCallDemoCalls)
    }

    @Test
    fun `loadDetail falls back to api call demo when public detail fails`() = runTest {
        val catalogRepository = FakeWebAppCatalogRepository(
            appDetailResult = Result.failure(IllegalStateException("public failed")),
        )
        val taskRepository = FakeWebAppTaskRepository(
            apiCallDemoResult = Result.success(
                appDetail(
                    id = "200",
                    inputNodes = listOf(inputNode(nodeId = "2", fieldName = "image", fieldValue = "demo.png")),
                ),
            ),
        )
        val screenModel = createScreenModel(catalogRepository, taskRepository)

        screenModel.loadDetail("200")
        advanceUntilIdle()

        assertFalse(screenModel.uiState.value.isLoading)
        assertEquals("200", screenModel.uiState.value.detail?.id)
        assertEquals("demo.png", screenModel.uiState.value.inputValues["2:image"])
        assertEquals(1, catalogRepository.getAppDetailCalls)
        assertEquals(1, taskRepository.getApiCallDemoCalls)
    }

    @Test
    fun `loadDetail skips duplicate app request after detail is loaded`() = runTest {
        val catalogRepository = FakeWebAppCatalogRepository(
            appDetailResult = Result.success(appDetail(id = "300")),
        )
        val screenModel = createScreenModel(catalogRepository, FakeWebAppTaskRepository())

        screenModel.loadDetail("300")
        advanceUntilIdle()
        screenModel.loadDetail("300")
        advanceUntilIdle()

        assertEquals(1, catalogRepository.getAppDetailCalls)
    }

    @Test
    fun `app module resolves AppDetailScreenModel without external dispatcher binding`() {
        val koinApp = koinApplication(createEagerInstances = false) {
            modules(
                appModule,
                module {
                    single<WebAppCatalogRepository> { FakeWebAppCatalogRepository() }
                    single<WebAppTaskRepository> { FakeWebAppTaskRepository() }
                    single<MediaResolver> { FakeMediaResolver() }
                },
            )
        }

        try {
            val screenModel = koinApp.koin.get<AppDetailScreenModel>()

            assertEquals(true, screenModel.uiState.value.isLoading)
        } finally {
            koinApp.close()
        }
    }

    @Test
    fun `media uri result uploads file and writes returned file name`() = runTest {
        val mediaResolver = FakeMediaResolver(
            displayName = "input.webp",
            bytes = byteArrayOf(1, 2, 3),
        )
        val taskRepository = FakeWebAppTaskRepository(
            uploadFileResult = Result.success(UploadResult(fileName = "remote-input.webp", fileType = "image/webp")),
        )
        val screenModel = createScreenModel(
            catalogRepository = FakeWebAppCatalogRepository(),
            taskRepository = taskRepository,
            mediaResolver = mediaResolver,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        screenModel.setPendingMediaPick(nodeId = "10", fieldName = "image", mediaType = AppDetailMediaType.IMAGE)
        screenModel.onMediaUriReceived("content://images/10")
        advanceUntilIdle()

        assertEquals(null, screenModel.uiState.value.pendingMediaPick)
        assertEquals("content://images/10", screenModel.uiState.value.localUris["10"])
        assertEquals("remote-input.webp", screenModel.uiState.value.inputValues["10:image"])
        assertEquals(false, screenModel.uiState.value.uploadingNodes.containsKey("10"))
        assertEquals("image/webp", taskRepository.lastUploadFileType)
        assertEquals("input.webp", taskRepository.lastUploadFileName)
        assertEquals(listOf("content://images/10"), mediaResolver.readUris)
    }

    @Test
    fun `upload failure marks node as failed without writing input value`() = runTest {
        val taskRepository = FakeWebAppTaskRepository(
            uploadFileResult = Result.failure(IllegalStateException("remote raw upload failure")),
        )
        val screenModel = createScreenModel(
            catalogRepository = FakeWebAppCatalogRepository(),
            taskRepository = taskRepository,
            mediaResolver = FakeMediaResolver(displayName = "bad.mp4"),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        screenModel.uploadFile(
            nodeId = "video-node",
            fieldName = "video",
            localUri = "content://videos/bad",
            mediaType = AppDetailMediaType.VIDEO,
        )
        advanceUntilIdle()

        val uploadState = screenModel.uiState.value.uploadingNodes["video-node"]
        assertEquals("content://videos/bad", uploadState?.localUri)
        assertEquals(null, screenModel.uiState.value.localUris["video-node"])
        assertEquals(null, screenModel.uiState.value.inputValues["video-node:video"])
        assertEquals(0f, uploadState?.progress)
        assertEquals(true, uploadState?.isError)
        assertEquals("video/mp4", taskRepository.lastUploadFileType)
    }

    @Test
    fun `runTask submits latest input values and maps successful output`() = runTest {
        val taskRepository = FakeWebAppTaskRepository(
            runTaskResult = Result.success(
                TaskResult(
                    netWssUrl = null,
                    taskId = 9001L,
                    clientId = null,
                    status = TaskExecutionStatus.Submitted,
                    promptTips = null,
                ),
            ),
            taskOutputs = listOf(
                listOf(TaskOutput(fileUrl = "https://cdn/output.png", fileName = "output.png", fileType = "image", failedReason = null)),
            ),
        )
        val screenModel = createScreenModel(
            catalogRepository = FakeWebAppCatalogRepository(
                appDetailResult = Result.success(
                    appDetail(
                        id = "400",
                        inputNodes = listOf(inputNode(nodeId = "4", fieldName = "prompt", fieldValue = "old")),
                    ),
                ),
            ),
            taskRepository = taskRepository,
        )

        screenModel.loadDetail("400")
        advanceUntilIdle()
        screenModel.updateInputValue(nodeId = "4", fieldName = "prompt", value = "new prompt")
        screenModel.runTask()
        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertEquals(400L, taskRepository.lastRunWebappId)
        assertEquals("new prompt", taskRepository.lastRunNodeInfoList.single().fieldValue)
        assertEquals(false, screenModel.uiState.value.isRunningTask)
        assertEquals(AppDetailTaskStep.SUCCESS, screenModel.uiState.value.taskStep)
        assertEquals("https://cdn/output.png", screenModel.uiState.value.taskOutputs.single().fileUrl)
    }

    @Test
    fun `failed task output maps to stable presentation message`() = runTest {
        val taskRepository = FakeWebAppTaskRepository(
            runTaskResult = Result.success(
                TaskResult(
                    netWssUrl = null,
                    taskId = 9002L,
                    clientId = null,
                    status = TaskExecutionStatus.Submitted,
                    promptTips = null,
                ),
            ),
            taskOutputs = listOf(
                listOf(
                    TaskOutput(
                        fileUrl = null,
                        fileName = null,
                        fileType = null,
                        failedReason = TaskFailedReason(
                            nodeName = "node",
                            exceptionMessage = "raw backend stack",
                            traceback = "trace",
                        ),
                    ),
                ),
            ),
        )
        val screenModel = createScreenModel(
            catalogRepository = FakeWebAppCatalogRepository(appDetailResult = Result.success(appDetail(id = "401"))),
            taskRepository = taskRepository,
        )

        screenModel.loadDetail("401")
        advanceUntilIdle()
        screenModel.runTask()
        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertEquals(false, screenModel.uiState.value.isRunningTask)
        assertEquals(AppDetailTaskStep.FAILED, screenModel.uiState.value.taskStep)
        assertEquals(AppDetailErrorText.TaskFailed, screenModel.uiState.value.taskError)
    }

    private fun createScreenModel(
        catalogRepository: FakeWebAppCatalogRepository,
        taskRepository: FakeWebAppTaskRepository,
        mediaResolver: MediaResolver = FakeMediaResolver(),
        ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): AppDetailScreenModel =
        AppDetailScreenModel(
            webAppCatalogRepository = catalogRepository,
            webAppTaskRepository = taskRepository,
            mediaResolver = mediaResolver,
            ioDispatcher = ioDispatcher,
        )

    private class FakeWebAppCatalogRepository(
        var appDetailResult: Result<AppDetail> = Result.failure(NotImplementedError()),
    ) : WebAppCatalogRepository {
        var getAppDetailCalls: Int = 0
            private set

        override suspend fun getAppList(query: CatalogQuery): Result<PageData<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getCarefullyChosenList(): Result<List<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getCustomMadeWebappList(tags: List<String>): Result<List<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getUserAppList(userId: String, pageNum: Int, pageSize: Int): Result<PageData<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getTagTree(range: CatalogTagRange): Result<List<Tag>> =
            Result.failure(NotImplementedError())

        override suspend fun getAppDetail(appId: String): Result<AppDetail> {
            getAppDetailCalls += 1
            return appDetailResult
        }

        override suspend fun searchApps(keyword: String, pageNum: Int, pageSize: Int): Result<PageData<WebApp>> =
            Result.failure(NotImplementedError())
    }

    private class FakeWebAppTaskRepository(
        var apiCallDemoResult: Result<AppDetail> = Result.failure(NotImplementedError()),
        var runTaskResult: Result<TaskResult> = Result.failure(NotImplementedError()),
        var taskOutputs: List<List<TaskOutput>> = emptyList(),
        var uploadFileResult: Result<UploadResult> = Result.failure(NotImplementedError()),
    ) : WebAppTaskRepository {
        var getApiCallDemoCalls: Int = 0
            private set
        var lastRunWebappId: Long? = null
            private set
        var lastRunNodeInfoList: List<InputNode> = emptyList()
            private set
        var lastUploadFileType: String? = null
            private set
        var lastUploadFileName: String? = null
            private set
        private var outputCallIndex: Int = 0

        override suspend fun getApiCallDemo(webappId: String): Result<AppDetail> {
            getApiCallDemoCalls += 1
            return apiCallDemoResult
        }

        override suspend fun runTask(
            webappId: Long,
            nodeInfoList: List<InputNode>,
            webhookUrl: String?,
            instanceType: String?,
        ): Result<TaskResult> {
            lastRunWebappId = webappId
            lastRunNodeInfoList = nodeInfoList
            return runTaskResult
        }

        override suspend fun getTaskOutputs(taskId: Long): Result<List<TaskOutput>> {
            val index = outputCallIndex
            outputCallIndex += 1
            return Result.success(taskOutputs.getOrElse(index) { emptyList() })
        }

        override suspend fun uploadFile(
            fileType: String,
            fileBytes: ByteArray,
            fileName: String,
        ): Result<UploadResult> {
            lastUploadFileType = fileType
            lastUploadFileName = fileName
            return uploadFileResult
        }
    }

    private class FakeMediaResolver(
        private val displayName: String? = null,
        private val bytes: ByteArray = byteArrayOf(1),
    ) : MediaResolver {
        val readUris: MutableList<String> = mutableListOf()

        override fun readBytes(uri: String): ByteArray {
            readUris += uri
            return bytes
        }

        override fun getDisplayName(uri: String): String? = displayName

        override fun getFileSizeBytes(uri: String): Long = bytes.size.toLong()
    }

    private fun appDetail(
        id: String,
        inputNodes: List<InputNode> = listOf(inputNode()),
    ): AppDetail =
        AppDetail(
            id = id,
            name = "Detail $id",
            workflowId = "workflow-$id",
            description = "description",
            tags = listOf(TagSimple(id = "tag", name = "tag")),
            owner = Author(
                id = "owner",
                name = "owner",
                avatar = null,
                intro = null,
                followCount = "0",
                fansCount = "0",
                likeCount = "0",
                collectCount = "0",
                bgImage = null,
            ),
            publishTime = "2026-06-22",
            inputNodes = inputNodes,
            covers = listOf(Cover(url = "https://example.com/cover.png", imageWidth = "100", imageHeight = "100")),
            statisticsInfo = StatisticsInfo(likeCount = "3", collectCount = "0", useCount = "2", pv = "1"),
            authorName = null,
            authorAvatar = null,
        )

    private fun inputNode(
        nodeId: String = "node",
        fieldName: String = "field",
        fieldValue: String? = "default",
    ): InputNode =
        InputNode(
            nodeId = nodeId,
            nodeName = "Node $nodeId",
            fieldName = fieldName,
            fieldValue = fieldValue,
            fieldData = null,
            fieldType = "STRING",
            description = "Description",
        )
}
