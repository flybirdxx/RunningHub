package com.runninghub.feature.detail.presentation

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
import com.runninghub.feature.task.domain.WebAppTaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailStateHolderTest {
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
        val stateHolder = createStateHolder(catalogRepository, taskRepository)

        stateHolder.loadDetail("100")
        advanceUntilIdle()

        assertFalse(stateHolder.uiState.value.isLoading)
        assertEquals("100", stateHolder.uiState.value.detail?.id)
        assertEquals("cat", stateHolder.uiState.value.inputValues["1:prompt"])
        assertEquals("", stateHolder.uiState.value.inputValues["2:negativePrompt"])
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
        val stateHolder = createStateHolder(catalogRepository, taskRepository)

        stateHolder.loadDetail("200")
        advanceUntilIdle()

        assertFalse(stateHolder.uiState.value.isLoading)
        assertEquals("200", stateHolder.uiState.value.detail?.id)
        assertEquals("demo.png", stateHolder.uiState.value.inputValues["2:image"])
        assertEquals(1, catalogRepository.getAppDetailCalls)
        assertEquals(1, taskRepository.getApiCallDemoCalls)
    }

    @Test
    fun `loadDetail stores stable error when both detail sources fail`() = runTest {
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(
                appDetailResult = Result.failure(IllegalStateException("raw public failure")),
            ),
            taskRepository = FakeWebAppTaskRepository(
                apiCallDemoResult = Result.failure(IllegalStateException("raw demo failure")),
            ),
        )

        stateHolder.loadDetail("404")
        advanceUntilIdle()

        assertFalse(stateHolder.uiState.value.isLoading)
        assertEquals(AppDetailErrorText.DetailLoadFailed, stateHolder.uiState.value.error)
    }

    @Test
    fun `loadDetail skips duplicate app request after detail is loaded`() = runTest {
        val catalogRepository = FakeWebAppCatalogRepository(
            appDetailResult = Result.success(appDetail(id = "300")),
        )
        val stateHolder = createStateHolder(catalogRepository, FakeWebAppTaskRepository())

        stateHolder.loadDetail("300")
        advanceUntilIdle()
        stateHolder.loadDetail("300")
        advanceUntilIdle()

        assertEquals(1, catalogRepository.getAppDetailCalls)
    }

    @Test
    fun `creation entry prioritizes purpose inputs cost and generate action`() = runTest {
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(
                appDetailResult = Result.success(
                    appDetail(
                        id = "350",
                        inputNodes = listOf(
                            inputNode(
                                nodeId = "prompt-node",
                                fieldName = "prompt",
                                fieldValue = "",
                                description = "Prompt",
                            ),
                            inputNode(
                                nodeId = "ratio-node",
                                fieldName = "aspectRatio",
                                fieldValue = "1:1",
                                description = "画面比例",
                            ),
                        ),
                    ),
                ),
            ),
            taskRepository = FakeWebAppTaskRepository(),
        )

        stateHolder.loadDetail("350")
        advanceUntilIdle()

        val creationEntry = stateHolder.uiState.value.creationEntry
        assertEquals("Detail 350", creationEntry?.title)
        assertEquals("description", creationEntry?.purpose)
        assertEquals(
            listOf(
                AppDetailCreationSection.PURPOSE,
                AppDetailCreationSection.REQUIRED_INPUTS,
                AppDetailCreationSection.ESTIMATED_COST,
                AppDetailCreationSection.PRIMARY_ACTION,
                AppDetailCreationSection.TECHNICAL_DETAILS,
            ),
            creationEntry?.firstScreenSections,
        )
        assertEquals(2, creationEntry?.requiredInputs?.size)
        assertEquals(false, creationEntry?.technicalDetailsExpanded)
        assertEquals(AppDetailEstimatedCostKind.UNKNOWN, creationEntry?.estimatedCost?.kind)
        assertEquals(AppDetailCreationPrimaryAction.GENERATE_NOW, creationEntry?.primaryAction?.type)
        assertEquals(true, creationEntry?.primaryAction?.enabled)
    }

    @Test
    fun `media uri result uploads file and writes returned file name`() = runTest {
        val mediaReader = FakeAppDetailMediaReader(
            displayName = "input.webp",
            bytes = byteArrayOf(1, 2, 3),
        )
        val taskRepository = FakeWebAppTaskRepository(
            uploadFileResult = Result.success(UploadResult(fileName = "remote-input.webp", fileType = "image/webp")),
        )
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(),
            taskRepository = taskRepository,
            mediaReader = mediaReader,
        )

        stateHolder.setPendingMediaPick(nodeId = "10", fieldName = "image", mediaType = AppDetailMediaType.IMAGE)
        stateHolder.onMediaUriReceived("content://images/10")
        advanceUntilIdle()

        assertEquals(null, stateHolder.uiState.value.pendingMediaPick)
        assertEquals("content://images/10", stateHolder.uiState.value.localUris["10"])
        assertEquals("remote-input.webp", stateHolder.uiState.value.inputValues["10:image"])
        assertEquals(false, stateHolder.uiState.value.uploadingNodes.containsKey("10"))
        assertEquals("image/webp", taskRepository.lastUploadFileType)
        assertEquals("input.webp", taskRepository.lastUploadFileName)
        assertEquals(listOf("content://images/10"), mediaReader.readUris)
    }

    @Test
    fun `upload failure marks node as failed without writing input value`() = runTest {
        val taskRepository = FakeWebAppTaskRepository(
            uploadFileResult = Result.failure(IllegalStateException("remote raw upload failure")),
        )
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(),
            taskRepository = taskRepository,
            mediaReader = FakeAppDetailMediaReader(displayName = "bad.mp4"),
        )

        stateHolder.uploadFile(
            nodeId = "video-node",
            fieldName = "video",
            localUri = "content://videos/bad",
            mediaType = AppDetailMediaType.VIDEO,
        )
        advanceUntilIdle()

        val uploadState = stateHolder.uiState.value.uploadingNodes["video-node"]
        assertEquals("content://videos/bad", uploadState?.localUri)
        assertEquals(null, stateHolder.uiState.value.localUris["video-node"])
        assertEquals(null, stateHolder.uiState.value.inputValues["video-node:video"])
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
        val stateHolder = createStateHolder(
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

        stateHolder.loadDetail("400")
        advanceUntilIdle()
        stateHolder.updateInputValue(nodeId = "4", fieldName = "prompt", value = "new prompt")
        stateHolder.runTask()
        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertEquals(400L, taskRepository.lastRunWebappId)
        assertEquals("new prompt", taskRepository.lastRunNodeInfoList.single().fieldValue)
        assertEquals(false, stateHolder.uiState.value.isRunningTask)
        assertEquals(AppDetailTaskStep.SUCCESS, stateHolder.uiState.value.taskStep)
        assertEquals("https://cdn/output.png", stateHolder.uiState.value.taskOutputs.single().fileUrl)
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
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(appDetailResult = Result.success(appDetail(id = "401"))),
            taskRepository = taskRepository,
        )

        stateHolder.loadDetail("401")
        advanceUntilIdle()
        stateHolder.runTask()
        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertEquals(false, stateHolder.uiState.value.isRunningTask)
        assertEquals(AppDetailTaskStep.FAILED, stateHolder.uiState.value.taskStep)
        assertEquals(AppDetailErrorText.TaskFailed, stateHolder.uiState.value.taskError)
    }

    @Test
    fun `runTask stores stable task submit failure error`() = runTest {
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(appDetailResult = Result.success(appDetail(id = "402"))),
            taskRepository = FakeWebAppTaskRepository(
                runTaskResult = Result.failure(IllegalStateException("raw submit failure")),
            ),
        )

        stateHolder.loadDetail("402")
        advanceUntilIdle()
        stateHolder.runTask()
        advanceUntilIdle()

        assertEquals(false, stateHolder.uiState.value.isRunningTask)
        assertEquals(AppDetailTaskStep.FAILED, stateHolder.uiState.value.taskStep)
        assertEquals(AppDetailErrorText.TaskSubmitFailed, stateHolder.uiState.value.taskError)
    }

    @Test
    fun `task polling timeout stores stable timeout error`() = runTest {
        val stateHolder = createStateHolder(
            catalogRepository = FakeWebAppCatalogRepository(appDetailResult = Result.success(appDetail(id = "403"))),
            taskRepository = FakeWebAppTaskRepository(
                runTaskResult = Result.success(
                    TaskResult(
                        netWssUrl = null,
                        taskId = 9003L,
                        clientId = null,
                        status = TaskExecutionStatus.Submitted,
                        promptTips = null,
                    ),
                ),
                taskOutputs = emptyList(),
            ),
        )

        stateHolder.loadDetail("403")
        advanceUntilIdle()
        stateHolder.runTask()
        advanceTimeBy(5_000L * 120)
        advanceUntilIdle()

        assertEquals(false, stateHolder.uiState.value.isRunningTask)
        assertEquals(AppDetailTaskStep.FAILED, stateHolder.uiState.value.taskStep)
        assertEquals(AppDetailErrorText.TaskTimeout, stateHolder.uiState.value.taskError)
    }

    private fun TestScope.createStateHolder(
        catalogRepository: FakeWebAppCatalogRepository,
        taskRepository: FakeWebAppTaskRepository,
        mediaReader: AppDetailMediaReader = FakeAppDetailMediaReader(),
    ): AppDetailStateHolder =
        AppDetailStateHolder(
            webAppCatalogRepository = catalogRepository,
            webAppTaskRepository = taskRepository,
            mediaReader = mediaReader,
            coroutineScope = this,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
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

    private class FakeAppDetailMediaReader(
        private val displayName: String? = null,
        private val bytes: ByteArray = byteArrayOf(1),
    ) : AppDetailMediaReader {
        val readUris: MutableList<String> = mutableListOf()

        override fun readBytes(uri: String): ByteArray {
            readUris += uri
            return bytes
        }

        override fun getDisplayName(uri: String): String? = displayName
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
        fieldType: String = "STRING",
        fieldData: String? = null,
        description: String? = "Description",
    ): InputNode =
        InputNode(
            nodeId = nodeId,
            nodeName = "Node $nodeId",
            fieldName = fieldName,
            fieldValue = fieldValue,
            fieldData = fieldData,
            fieldType = fieldType,
            description = description,
        )
}
