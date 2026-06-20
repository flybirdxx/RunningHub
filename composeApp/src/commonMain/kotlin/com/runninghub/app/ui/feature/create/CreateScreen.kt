package com.runninghub.app.ui.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import com.runninghub.app.platform.rememberPermissionController
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.ImageUploadButton
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground
import com.runninghub.app.ui.theme.RhAppBottomBar
import com.runninghub.app.ui.theme.RhAppCard
import com.runninghub.app.ui.theme.RhAppLine
import com.runninghub.app.ui.theme.RhAppMuted
import com.runninghub.app.ui.theme.RhAppSurface
import com.runninghub.app.ui.theme.RhAppText
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.repository.QuickCreationFeePreview
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import org.koin.compose.koinInject

class CreateVoyagerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel: CreateScreenModel = koinScreenModel()
        val uiState by screenModel.uiState.collectAsState()
        val dataStore: PermissionDataStore = koinInject()
        val permissionController = rememberPermissionController(dataStore)

        LaunchedEffect(Unit) { screenModel.loadModels() }

        CreateScreenContent(
            uiState = uiState,
            onCategorySelected = screenModel::updateCategory,
            onSearch = screenModel::loadModels,
            onModelSelected = screenModel::selectModel,
            onFieldValueChange = screenModel::updateFieldValue,
            onSubmit = screenModel::submitSelectedModel,
            onUploadFieldPick = { fieldKey, mediaType ->
                permissionController.pickMedia(
                    mediaPermission = mediaType.mediaPermission,
                    mediaType = mediaType,
                    onSuccess = { uriString -> screenModel.pickUploadField(fieldKey, uriString) },
                    onPermissionDenied = {},
                )
            },
            onUploadFieldRemove = screenModel::removeUploadField,
            onHistoryItemSelected = screenModel::selectHistoryOutput,
            onHistoryDetailDismiss = screenModel::dismissHistoryDetail,
            onHistoryFilterSelected = screenModel::updateHistoryFilter,
            onHistoryRetry = screenModel::refreshRecentHistory,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreenContent(
    uiState: CreateUiState,
    onCategorySelected: (CreateCategory) -> Unit,
    onSearch: () -> Unit,
    onModelSelected: (String) -> Unit,
    onFieldValueChange: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onUploadFieldPick: (String, MediaType) -> Unit,
    onUploadFieldRemove: (String) -> Unit,
    onHistoryItemSelected: (String) -> Unit,
    onHistoryDetailDismiss: () -> Unit,
    onHistoryFilterSelected: (CreateHistoryFilter) -> Unit,
    onHistoryRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowInfo = LocalRhWindowInfo.current
    var historyDrawerOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = RhBackground,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            bottomBar = {
                BottomActionBar(
                    uiState = uiState,
                    onSubmit = onSubmit,
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(RhBackground)
                    .widthIn(max = windowInfo.formContentMaxWidth),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { TopBar(onHistoryClick = { historyDrawerOpen = true }) }
                item {
                    CategoryTabs(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = onCategorySelected,
                    )
                }
                uiState.catalogNotice?.let { notice -> item { NoticeBar(notice) } }

                when {
                    uiState.isLoading && uiState.serviceModels.isEmpty() -> item { LoadingPanel(Modifier.height(420.dp)) }
                    uiState.error != null && uiState.serviceModels.isEmpty() -> item { ErrorPanel(uiState.error, Modifier.height(420.dp), onSearch) }
                    !uiState.isLoading && uiState.serviceModels.isEmpty() -> item {
                        EmptyPanel("暂无可用模型", Modifier.height(300.dp))
                    }
                    else -> {
                        item {
                            ModelHero(
                                model = uiState.selectedModel,
                                feePreview = uiState.feePreview,
                                feePreviewLoading = uiState.feePreviewLoading,
                                feePreviewError = uiState.feePreviewError,
                            )
                        }
                        item {
                            ModelSelectorStrip(
                                models = uiState.serviceModels,
                                selectedKey = uiState.selectedModel?.identityKey,
                                onModelSelected = onModelSelected,
                            )
                        }
                        item {
                            PromptSection(
                                model = uiState.selectedModel,
                                fieldValues = uiState.fieldValues,
                                onFieldValueChange = onFieldValueChange,
                            )
                        }
                        item {
                            AspectRatioSection(
                                model = uiState.selectedModel,
                                fieldValues = uiState.fieldValues,
                                onFieldValueChange = onFieldValueChange,
                            )
                        }
                        item {
                            ResolutionSection(
                                model = uiState.selectedModel,
                                fieldValues = uiState.fieldValues,
                                onFieldValueChange = onFieldValueChange,
                            )
                        }
                        item {
                            UploadFieldsSection(
                                model = uiState.selectedModel,
                                fieldValues = uiState.fieldValues,
                                uploadFieldStates = uiState.uploadFieldStates,
                                onFieldValueChange = onFieldValueChange,
                                onUploadFieldPick = onUploadFieldPick,
                                onUploadFieldRemove = onUploadFieldRemove,
                            )
                        }
                        item {
                            AdvancedFieldsSection(
                                model = uiState.selectedModel,
                                fieldValues = uiState.fieldValues,
                                onFieldValueChange = onFieldValueChange,
                            )
                        }
                    }
                }
            }
        }

        if (historyDrawerOpen) {
            HistoryDrawer(
                items = uiState.filteredHistory,
                totalCount = uiState.recentHistory.size,
                selectedFilter = uiState.selectedHistoryFilter,
                isLoading = uiState.historyLoading,
                error = uiState.historyError,
                detailLoading = uiState.historyDetailLoading,
                selectedDetail = uiState.selectedHistoryDetail,
                detailError = uiState.historyDetailError,
                onItemSelected = onHistoryItemSelected,
                onDetailDismiss = onHistoryDetailDismiss,
                onFilterSelected = onHistoryFilterSelected,
                onRetry = onHistoryRetry,
                onDismiss = {
                    historyDrawerOpen = false
                    onHistoryDetailDismiss()
                },
            )
        }
    }
}

@Composable
private fun TopBar(onHistoryClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(BrandLime),
                contentAlignment = Alignment.Center,
            ) {
                Text("R", color = Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Text(
                text = "RunningHub",
                color = RhText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        BalancePill()
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, RhLine, RoundedCornerShape(21.dp)),
        ) {
            Icon(Icons.Default.History, contentDescription = "生成历史", tint = RhText)
        }
    }
}

@Composable
private fun BalancePill() {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(18.dp))
            .padding(start = 9.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFFC928)),
            contentAlignment = Alignment.Center,
        ) {
            Text("S", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Text("--", color = RhText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.Add, contentDescription = "充值", tint = RhMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: CreateCategory,
    onCategorySelected: (CreateCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CreateCategory.entries.forEach { category ->
            SelectionChip(
                label = category.displayName,
                selected = category == selectedCategory,
                minWidth = 72.dp,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun NoticeBar(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, BrandLime.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = BrandLime,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModelHero(
    model: QuickCreationServiceModel?,
    feePreview: QuickCreationFeePreview?,
    feePreviewLoading: Boolean,
    feePreviewError: String?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RhLine, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RhSurface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RhCard)
                    .border(1.dp, BrandLime.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Image, contentDescription = "图像生成", tint = BrandLime, modifier = Modifier.size(34.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = model?.name ?: "选择模型",
                        color = RhText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge("推荐")
                }
                Text(
                    text = model?.groupName ?: model?.categoryId ?: "图片生成",
                    color = RhMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = feePreviewText(feePreview, feePreviewLoading, feePreviewError),
                        color = if (feePreviewError == null) BrandLime else StatusError,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    EndpointPill(model?.skuId ?: "--")
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "详情", tint = RhText, modifier = Modifier.size(30.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(RhCard)
                .border(1.dp, RhLine, RoundedCornerShape(9.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaText("在线")
            MetaText("${model?.fields?.size ?: 0} 个字段")
            MetaText(model?.pricing?.settlementMode ?: "服务端计费")
            MetaText(model?.categoryId ?: "IMAGE")
        }
    }
}

@Composable
private fun ModelSelectorStrip(
    models: List<QuickCreationServiceModel>,
    selectedKey: String?,
    onModelSelected: (String) -> Unit,
) {
    if (models.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(models.take(12), key = { it.identityKey }) { model ->
            SelectionChip(
                label = model.name,
                selected = model.identityKey == selectedKey,
                minWidth = 112.dp,
                onClick = { onModelSelected(model.identityKey) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptSection(
    model: QuickCreationServiceModel?,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
) {
    val field = model.promptField()
    val key = field?.fieldKey ?: "prompt"
    val value = fieldValues.rawValueFor(field, "prompt")
    val maxLength = field?.inputExtra?.maxLength ?: 2000

    SectionHeader(title = "提示词", required = true, trailing = "清空  |  AI 助手 ✨")
    OutlinedTextField(
        value = value,
        onValueChange = { onFieldValueChange(key, it) },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 116.dp),
        minLines = 4,
        placeholder = { Text(field?.inputExtra?.placeholder ?: "描述你想要生成的画面，越详细越好...", color = RhMuted) },
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text("${value.length} / $maxLength", color = RhMuted, style = MaterialTheme.typography.labelSmall)
    }
    InputChildrenFields(
        parent = field,
        fieldValues = fieldValues,
        onFieldValueChange = onFieldValueChange,
    )
}

@Composable
private fun AspectRatioSection(
    model: QuickCreationServiceModel?,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
) {
    val field = model.findField("aspect", "ratio")
    val key = field?.fieldKey ?: "aspectRatio"
    val options = field?.options?.map { it.value.ifBlank { it.label } }?.takeIf { it.isNotEmpty() }
        ?: listOf("1:1", "16:9", "9:16", "4:3")
    val current = fieldValues.rawValueFor(field, "aspectRatio").ifBlank { field?.defaultValue ?: options.first() }

    SectionHeader(title = "画面比例")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { ratio ->
            SelectionChip(
                label = ratio,
                selected = current == ratio,
                minWidth = 76.dp,
                onClick = { onFieldValueChange(key, ratio) },
            )
        }
    }
    InputChildrenFields(
        parent = field,
        fieldValues = fieldValues,
        onFieldValueChange = onFieldValueChange,
    )
}

@Composable
private fun ResolutionSection(
    model: QuickCreationServiceModel?,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
) {
    val field = model.findField("resolution", "width", "height")
    val key = field?.fieldKey ?: "resolution"
    val options = field?.options?.map { it.value.ifBlank { it.label } }.orEmpty()
    val current = fieldValues.rawValueFor(field, "resolution").ifBlank {
        field?.defaultValue ?: options.firstOrNull() ?: "2k"
    }

    SectionHeader(title = "分辨率")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.ifEmpty { listOf(current) }.forEach { option ->
            SelectionChip(
                label = option,
                selected = current == option,
                minWidth = 90.dp,
                onClick = { onFieldValueChange(key, option) },
            )
        }
    }
    InputChildrenFields(
        parent = field,
        fieldValues = fieldValues,
        onFieldValueChange = onFieldValueChange,
    )
}

@Composable
private fun ReferenceUploadBox() {
    SectionHeader(title = "参考图", optional = true)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Add, contentDescription = "上传", tint = RhText, modifier = Modifier.size(24.dp))
            Text("点击上传或拖拽到此处", color = RhText, style = MaterialTheme.typography.bodyMedium)
            Text("支持 JPG / PNG，单图 ≤ 10MB", color = RhMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun UploadFieldsSection(
    model: QuickCreationServiceModel?,
    fieldValues: Map<String, String>,
    uploadFieldStates: Map<String, CreateUploadFieldState>,
    onFieldValueChange: (String, String) -> Unit,
    onUploadFieldPick: (String, MediaType) -> Unit,
    onUploadFieldRemove: (String) -> Unit,
) {
    val fields = model?.fields.orEmpty().filter { it.visible && it.isUploadField }
    if (fields.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        fields.forEach { field ->
            val mediaType = field.uploadMediaType()
            val uploadState = uploadFieldStates[field.fieldKey]
            val remoteUrl = uploadState?.remoteUrl ?: fieldValues.uploadUrlFor(field)
            val maxCount = field.inputExtra?.maxInputCount ?: field.maxUploadCount
            val limitText = listOfNotNull(
                maxCount?.let { "最多 $it 个文件" },
                field.maxUploadSize?.let { "单个 ${formatFileSize(it)}" },
            ).joinToString(" · ")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    title = field.displayTitle,
                    required = field.required,
                    optional = !field.required,
                    trailing = limitText.takeIf { it.isNotBlank() },
                )
                ImageUploadButton(
                    localUri = uploadState?.localUri,
                    remoteUrl = remoteUrl,
                    fileName = uploadState?.fileName,
                    isUploading = uploadState?.isUploading == true,
                    uploadProgress = if (uploadState?.isUploading == true) 0.7f else 0f,
                    isError = uploadState?.isError == true,
                    mediaType = mediaType,
                    onPickFile = { onUploadFieldPick(field.fieldKey, mediaType) },
                    onRemoveFile = { onUploadFieldRemove(field.fieldKey) },
                    modifier = Modifier.height(104.dp),
                )
                uploadState?.errorMessage?.let { error ->
                    Text(error, color = StatusError, style = MaterialTheme.typography.labelSmall)
                }
                InputChildrenFields(
                    parent = field,
                    fieldValues = fieldValues,
                    onFieldValueChange = onFieldValueChange,
                )
            }
        }
    }
}

@Composable
private fun AdvancedFieldsSection(
    model: QuickCreationServiceModel?,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
) {
    val fields = model?.fields.orEmpty().filter { it.visible && it.isAdvancedField }
    if (fields.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "高级选项",
            trailing = fields.joinToString("、") { it.displayTitle },
        )
        fields.forEach { field ->
            DynamicServiceField(
                field = field,
                value = fieldValues.rawValueFor(field, field.fieldKey),
                onValueChange = { onFieldValueChange(field.fieldKey, it) },
            )
            field.activeInputChildren(fieldValues).forEach { child ->
                DynamicInputChildField(
                    child = child,
                    value = fieldValues.rawValueFor(child, child.fieldKey),
                    onValueChange = { onFieldValueChange(child.fieldKey, it) },
                )
            }
        }
    }
}

@Composable
private fun DynamicServiceField(
    field: QuickCreationServiceField,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = field.displayTitle,
            required = field.required,
            optional = !field.required,
            trailing = field.inputExtra?.paramDescription?.takeIf { it.isNotBlank() },
        )
        if (field.options.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(field.options, key = { "${field.fieldKey}:${it.value}:${it.label}" }) { option ->
                    val optionValue = option.value.ifBlank { option.label }
                    SelectionChip(
                        label = option.label.ifBlank { option.value },
                        selected = value.equals(optionValue, ignoreCase = true),
                        minWidth = 72.dp,
                        onClick = { onValueChange(optionValue) },
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        field.inputExtra?.placeholder ?: "输入${field.displayTitle}",
                        color = RhMuted,
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
            )
        }
    }
}

@Composable
private fun InputChildrenFields(
    parent: QuickCreationServiceField?,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
) {
    val children = parent?.activeInputChildren(fieldValues).orEmpty()
    if (children.isEmpty()) return

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        children.forEach { child ->
            DynamicInputChildField(
                child = child,
                value = fieldValues.rawValueFor(child, child.fieldKey),
                onValueChange = { onFieldValueChange(child.fieldKey, it) },
            )
        }
    }
}

@Composable
private fun DynamicInputChildField(
    child: QuickCreationServiceFieldInputChild,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(
            title = child.displayTitle,
            required = child.required,
            optional = !child.required,
            trailing = child.paramDescription?.takeIf { it.isNotBlank() },
        )
        if (child.options.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(child.options, key = { "${child.fieldKey}:${it.value}:${it.label}" }) { option ->
                    val optionValue = option.value.ifBlank { option.label }
                    SelectionChip(
                        label = option.label.ifBlank { option.value },
                        selected = value.equals(optionValue, ignoreCase = true),
                        minWidth = 72.dp,
                        onClick = { onValueChange(optionValue) },
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        child.placeholder ?: "输入${child.displayTitle}",
                        color = RhMuted,
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
            )
        }
    }
}

@Composable
private fun AdvancedOptionsCard(model: QuickCreationServiceModel?) {
    val advanced = model?.fields.orEmpty()
        .filterNot { it.isPromptField || it.isUploadField || it.matches("aspect", "ratio") || it.matches("resolution", "width", "height") }
        .take(3)
        .joinToString("、") { it.displayTitle }
        .ifBlank { "采样方法、风格、参数等" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("高级选项", color = RhText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = advanced,
            color = RhMuted,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "更多", tint = RhText, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HistoryDrawer(
    items: List<QuickCreationHistoryItem>,
    totalCount: Int,
    selectedFilter: CreateHistoryFilter,
    isLoading: Boolean,
    error: String?,
    detailLoading: Boolean,
    selectedDetail: QuickCreationHistoryItem?,
    detailError: String?,
    onItemSelected: (String) -> Unit,
    onDetailDismiss: () -> Unit,
    onFilterSelected: (CreateHistoryFilter) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.88f)
                .align(Alignment.CenterEnd)
                .border(1.dp, RhLine, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            color = RhBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("历史记录", color = RhText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("最近生成的文件 · $totalCount", color = RhMuted, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .border(1.dp, RhLine, RoundedCornerShape(19.dp)),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = RhText)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HistoryFilterChips(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                )
                Spacer(Modifier.height(12.dp))
                if (detailLoading || selectedDetail != null || detailError != null) {
                    HistoryDetailPanel(
                        isLoading = detailLoading,
                        item = selectedDetail,
                        error = detailError,
                        onDismiss = onDetailDismiss,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    when {
                        isLoading && items.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(RhSurface)
                                        .border(1.dp, RhLine, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        CircularProgressIndicator(color = BrandLime, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text("正在同步历史记录", color = RhMuted, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        error != null && items.isEmpty() -> {
                            item { HistoryErrorCard(message = error, onRetry = onRetry) }
                        }
                        items.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(RhSurface)
                                        .border(1.dp, RhLine, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (selectedFilter == CreateHistoryFilter.ALL) "暂无生成文件" else "暂无匹配的生成文件",
                                        color = RhMuted,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                        else -> {
                            if (error != null) {
                                item { HistoryErrorCard(message = error, onRetry = onRetry) }
                            }
                            items(items.take(12), key = { it.taskId }) { item ->
                                DrawerHistoryRow(
                                    item = item,
                                    onClick = {
                                        item.outputs.firstOrNull()?.outputId?.let(onItemSelected)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChips(
    selectedFilter: CreateHistoryFilter,
    onFilterSelected: (CreateHistoryFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CreateHistoryFilter.entries, key = { it.name }) { filter ->
            SelectionChip(
                label = filter.displayName,
                selected = filter == selectedFilter,
                minWidth = 64.dp,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun HistoryErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RhSurface)
            .border(1.dp, StatusError.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(message, color = StatusError, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("重试", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DrawerHistoryRow(
    item: QuickCreationHistoryItem,
    onClick: () -> Unit,
) {
    val thumbnailUrl = item.outputs.firstOrNull()?.thumbnailUrl ?: item.outputs.firstOrNull()?.url
    val hasDetail = item.outputs.firstOrNull()?.outputId?.isNotBlank() == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(14.dp))
            .clickable(enabled = hasDetail, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RhCard),
            contentAlignment = Alignment.Center,
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "生成文件",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.Image, contentDescription = null, tint = RhMuted, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.taskType ?: item.skuId ?: "快捷创作",
                    color = RhText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                HistoryStatusPill(item.status)
            }
            Text(
                text = item.params["prompt"] ?: item.taskId,
                color = RhMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(historyMeta(item), color = RhMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(historyCost(item), color = BrandLime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "打开生成文件", tint = RhMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HistoryDetailPanel(
    isLoading: Boolean,
    item: QuickCreationHistoryItem?,
    error: String?,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandLime.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RhSurface),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "文件详情",
                    color = RhText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭详情", tint = RhMuted, modifier = Modifier.size(18.dp))
                }
            }
            when {
                isLoading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(color = BrandLime, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("正在加载历史详情", color = RhMuted, style = MaterialTheme.typography.bodySmall)
                }
                error != null -> Text(error, color = StatusError, style = MaterialTheme.typography.bodySmall)
                item != null -> HistoryDetailContent(item)
            }
        }
    }
}

@Composable
private fun HistoryDetailContent(item: QuickCreationHistoryItem) {
    val output = item.outputs.firstOrNull()
    val previewUrl = output?.thumbnailUrl ?: output?.url
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!previewUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RhCard),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = previewUrl,
                    contentDescription = "生成文件预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = item.params["prompt"] ?: item.taskId,
            color = RhText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(
                item.categoryId,
                item.status,
                output?.type?.uppercase(),
                output?.let { "${it.width ?: "-"}x${it.height ?: "-"}" },
                output?.expireDays?.let { "有效期 $it 天" },
            ).joinToString(" · "),
            color = RhMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(historyCost(item), color = BrandLime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryStatusPill(status: String) {
    val normalized = status.lowercase()
    val isSuccess = normalized in setOf("success", "completed", "done")
    val isRunning = normalized in setOf("running", "processing", "queued", "queuing")
    val isFailed = normalized in setOf("failed", "fail", "error")
    val color = if (isFailed) StatusError else BrandLime
    val label = when {
        isSuccess -> "成功"
        isRunning -> "进行中"
        isFailed -> "失败"
        else -> status.ifBlank { "未知" }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val icon = when {
            isSuccess -> Icons.Default.CheckCircle
            isFailed -> Icons.Default.Error
            else -> Icons.Default.Refresh
        }
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(12.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomActionBar(
    uiState: CreateUiState,
    onSubmit: () -> Unit,
) {
    val canSubmit = uiState.selectedModel != null &&
        uiState.selectedCategory == CreateCategory.IMAGE &&
        !uiState.isSubmitting &&
        !uiState.feePreviewLoading &&
        uiState.feePreview != null &&
        uiState.feePreviewError == null

    Surface(
        color = RhBottomBar,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("预计费用", color = RhMuted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = feePreviewText(uiState.feePreview, uiState.feePreviewLoading, uiState.feePreviewError),
                        color = if (uiState.feePreviewError == null) BrandLime else StatusError,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = Color.Black,
                        disabledContainerColor = RhLine,
                        disabledContentColor = RhMuted,
                    ),
                ) {
                    Text(
                        if (uiState.isSubmitting) "生成中" else "生成",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            val statusMessage = uiState.submitMessage ?: uiState.feePreviewError
            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = if (message.contains("成功") || message.contains("排队")) BrandLime else StatusError,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    required: Boolean = false,
    optional: Boolean = false,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = RhText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (required) Text(" *", color = StatusError, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (optional) Text("（可选）", color = RhMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(it, color = BrandLime, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    minWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .widthIn(min = minWidth)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BrandLime else RhSurface)
            .border(1.dp, if (selected) BrandLime else RhLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.Black else RhText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BrandLime.copy(alpha = 0.12f))
            .border(1.dp, BrandLime.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = BrandLime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EndpointPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, RhLine, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, color = RhMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text, color = RhMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun LoadingPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandLime)
    }
}

@Composable
private fun ErrorPanel(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(14.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = StatusError, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = Color.Black)) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyPanel(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = RhMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RhText,
    unfocusedTextColor = RhText,
    focusedContainerColor = RhSurface,
    unfocusedContainerColor = RhSurface,
    focusedBorderColor = BrandLime,
    unfocusedBorderColor = RhLine,
    cursorColor = BrandLime,
)

private val RhBackground = RhAppBackground
private val RhSurface = RhAppSurface
private val RhCard = RhAppCard
private val RhText = RhAppText
private val RhMuted = RhAppMuted
private val RhLine = RhAppLine
private val RhBottomBar = RhAppBottomBar

private val QuickCreationServiceModel.identityKey: String
    get() = "$bindingId:$skuId"

private fun QuickCreationServiceModel?.promptField(): QuickCreationServiceField? =
    this?.fields?.firstOrNull { it.visible && it.isPromptField }

private fun QuickCreationServiceModel?.findField(vararg needles: String): QuickCreationServiceField? =
    this?.fields?.firstOrNull { field -> field.visible && field.matches(*needles) }

private val QuickCreationServiceField.isPromptField: Boolean
    get() = matches("prompt") || matches("提示词")

private val QuickCreationServiceField.isUploadField: Boolean
    get() {
        if (isPromptField) return false
        val normalizedParam = paramKey.ifBlank { fieldKey }.lowercase()
        val type = fieldType.lowercase()
        val key = fieldKey.lowercase()
        return type.contains("upload") ||
            type.contains("file") ||
            key.contains("upload") ||
            normalizedParam in uploadParamKeys ||
            uploadParamKeys.any { normalizedParam.endsWith(it) }
    }

private val QuickCreationServiceField.isAdvancedField: Boolean
    get() = !isPromptField &&
        !isUploadField &&
        !matches("aspect", "ratio") &&
        !matches("resolution", "width", "height")

private fun QuickCreationServiceField.activeInputChildren(values: Map<String, String>): List<QuickCreationServiceFieldInputChild> =
    inputExtra?.inputChildren.orEmpty()
        .filter { child -> child.visible && child.isActive(values) }

private fun QuickCreationServiceFieldInputChild.isActive(values: Map<String, String>): Boolean {
    val condition = visibleWhen ?: return true
    val actual = values[condition.fieldKey].orEmpty()
    return condition.values.any { expected -> expected.equals(actual, ignoreCase = true) }
}

private val QuickCreationServiceField.displayTitle: String
    get() = commonUploadTitle
        ?: inputExtra?.title?.takeIf { it.isNotBlank() }
        ?: inputExtra?.titleEn?.takeIf { it.isNotBlank() }
        ?: fieldKey

private val QuickCreationServiceFieldInputChild.displayTitle: String
    get() = title?.takeIf { it.isNotBlank() } ?: fieldKey

private val QuickCreationServiceField.commonUploadTitle: String?
    get() = when (paramKey.ifBlank { fieldKey }.lowercase()) {
        "imageurls", "imageurl" -> "参考图"
        "videourls", "videourl" -> "参考视频"
        "audiourls", "audiourl" -> "参考音频"
        else -> null
    }

private fun QuickCreationServiceField.matches(vararg needles: String): Boolean {
    val searchable = listOf(fieldKey, paramKey, inputExtra?.title, inputExtra?.titleEn, fieldType)
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
    return needles.any { searchable.contains(it.lowercase()) }
}

private fun QuickCreationServiceField.uploadMediaType(): MediaType {
    val marker = listOf(fieldType, fieldKey, paramKey, inputExtra?.title, inputExtra?.titleEn)
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
    return when {
        marker.contains("video") -> MediaType.VIDEO
        marker.contains("audio") -> MediaType.AUDIO
        else -> MediaType.IMAGE
    }
}

private val MediaType.mediaPermission: Permission
    get() = when (this) {
        MediaType.IMAGE -> Permission.MediaImages
        MediaType.VIDEO -> Permission.MediaVideo
        MediaType.AUDIO -> Permission.MediaAudio
    }

private fun Map<String, String>.rawValueFor(field: QuickCreationServiceField?, fallbackKey: String): String =
    if (field == null) {
        this[fallbackKey].orEmpty()
    } else {
        this[field.fieldKey] ?: this[field.paramKey] ?: field.defaultValue.orEmpty()
    }

private fun Map<String, String>.rawValueFor(child: QuickCreationServiceFieldInputChild, fallbackKey: String): String =
    this[child.fieldKey] ?: this[child.paramKey] ?: child.defaultValue ?: this[fallbackKey].orEmpty()

private fun Map<String, String>.uploadUrlFor(field: QuickCreationServiceField): String? =
    rawValueFor(field, field.fieldKey)
        .split(Regex("""[\s,\[\]"]+"""))
        .firstOrNull { value ->
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true) ||
                value.startsWith("content://", ignoreCase = true)
        }

private fun feePreviewText(
    preview: QuickCreationFeePreview?,
    loading: Boolean,
    error: String?,
): String = when {
    loading -> "价格确认中"
    error != null -> "价格待确认"
    preview == null -> "--"
    preview.free -> "免费"
    preview.requiredCashAmount > 0.0 -> "¥${formatAmount(preview.requiredCashAmount)}"
    preview.requiredRhAmount > 0.0 -> "${formatAmount(preview.requiredRhAmount)} 金币"
    else -> "¥0.00"
}

private fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().trimEnd('0').trimEnd('.')

private fun formatFileSize(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L -> "${formatAmount(bytes / 1024.0 / 1024.0)}MB"
        bytes >= 1024L -> "${formatAmount(bytes / 1024.0)}KB"
        bytes > 0L -> "${bytes}MB"
        else -> "${bytes}B"
    }

private fun historyMeta(item: QuickCreationHistoryItem): String {
    val output = item.outputs.firstOrNull()
    val size = listOfNotNull(output?.width, output?.height).takeIf { it.size == 2 }?.joinToString("x")
    return listOfNotNull(item.categoryId, size, item.taskCostTime?.let { "${it}s" })
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
        ?: item.taskId
}

private val uploadParamKeys = setOf(
    "imageurl",
    "imageurls",
    "videourl",
    "videourls",
    "audiourl",
    "audiourls",
    "fileurl",
    "fileurls",
)

private fun historyCost(item: QuickCreationHistoryItem): String =
    if (item.cashAmount > 0.0) {
        "${formatAmount(item.cashAmount)} ${item.cashCurrency ?: "CNY"}"
    } else {
        "免费"
    }
