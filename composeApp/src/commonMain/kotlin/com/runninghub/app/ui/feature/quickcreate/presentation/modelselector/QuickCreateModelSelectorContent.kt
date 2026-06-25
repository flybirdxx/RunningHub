package com.runninghub.app.ui.feature.quickcreate.presentation.modelselector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.feature.quickcreate.QuickCreateDesignTokens
import com.runninghub.app.ui.feature.quickcreate.QuickCreateModelGlyph
import com.runninghub.app.ui.feature.quickcreate.QuickCreateSheetHandle
import com.runninghub.app.ui.feature.quickcreate.presentation.asServiceModelText
import com.runninghub.app.ui.feature.quickcreate.quickCreateDebugLog
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_model_selector_close_content_description
import runninghub.composeapp.generated.resources.quick_create_model_selector_empty
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_audio
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_image
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_other
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_video
import runninghub.composeapp.generated.resources.quick_create_model_selector_loading
import runninghub.composeapp.generated.resources.quick_create_model_selector_path_format
import runninghub.composeapp.generated.resources.quick_create_model_selector_search_placeholder
import runninghub.composeapp.generated.resources.quick_create_model_selector_title
import runninghub.composeapp.generated.resources.quick_create_model_selector_unknown_price
import runninghub.composeapp.generated.resources.quick_create_model_selector_video_group

/**
 * 展示设计稿版快捷创作服务端模型选择面板。
 *
 * 面板只消费 [QuickCreateUiState] 中已经加载好的模型目录，搜索过滤与分类筛选属于本地临时 UI 状态；
 * 模型点击只通过回调更新 ScreenModel 中的选中项，列表勾选态就是当前生效模型，避免额外确认区挤占小屏高度。
 * 顶部手柄只上报拖拽过程，真正的 sheet 位移和收起判定由页面边界统一处理。
 */
@Composable
internal fun QuickCreateModelSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onImageServiceModelSelected: (String) -> Unit,
    onVideoServiceModelSelected: (String) -> Unit,
    onTabSwitch: (QuickCreateTab) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSheetDragStart: () -> Unit = {},
    onSheetDrag: (Float) -> Unit = {},
    onSheetDragEnd: () -> Unit = {},
    onSheetDragCancel: () -> Unit = {},
) {
    val currentModels = (uiState.serviceImageModelItems + uiState.serviceVideoModelItems)
        .distinctBy { it.identityKey }
    var modelSnapshot by remember { mutableStateOf<List<QuickCreateServiceModelUi>>(emptyList()) }
    LaunchedEffect(currentModels) {
        if (currentModels.isNotEmpty()) {
            modelSnapshot = currentModels
        }
    }
    val models = currentModels.ifEmpty { modelSnapshot }
    val hasModelSnapshot = models.isNotEmpty()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(if (isImage) ModelFilter.IMAGE else ModelFilter.VIDEO) }
    LaunchedEffect(visible, isImage) {
        if (visible) {
            selectedFilter = if (isImage) ModelFilter.IMAGE else ModelFilter.VIDEO
        }
    }
    val filteredModels = models
        .filter { model ->
            val text = buildString {
                append(model.source.name)
                append(' ')
                append(model.source.groupName.orEmpty())
                append(' ')
                append(model.source.bindingId)
                append(' ')
                append(model.source.skuId)
                append(' ')
                append(model.source.apiType.orEmpty())
                append(' ')
                append(model.source.apiSource.orEmpty())
            }
            query.isBlank() || text.contains(query, ignoreCase = true)
        }
        .filter { selectedFilter.accepts(it) }
    LaunchedEffect(
        visible,
        isImage,
        selectedFilter,
        models.size,
        filteredModels.size,
        uiState.serviceImageModelItems.size,
        uiState.serviceVideoModelItems.size,
    ) {
        if (visible) {
            quickCreateDebugLog(
                tag = "ModelSheet",
                message = "tab=${if (isImage) "IMAGE" else "VIDEO"} filter=$selectedFilter " +
                    "image=${uiState.serviceImageModelItems.size} video=${uiState.serviceVideoModelItems.size} " +
                    "combined=${models.size} filtered=${filteredModels.size} types=${models.typeDistributionLog()}",
            )
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = QuickCreateDesignTokens.PanelStrong,
                border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    QuickCreateSheetHandle(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onDragStart = onSheetDragStart,
                        onDrag = onSheetDrag,
                        onDragEnd = onSheetDragEnd,
                        onDragCancel = onSheetDragCancel,
                    )
                    ModelSheetHeader(onDismiss = onDismiss)
                    ModelSearchField(query = query, onQueryChange = { query = it })
                    ModelFilterRow(selectedFilter = selectedFilter, onSelect = { selectedFilter = it })
                    // 列表区域按剩余高度收缩；行尾勾选就是当前生效模型，不再额外占用底部确认区。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 520.dp)
                            .padding(top = 16.dp, bottom = 12.dp),
                    ) {
                        when {
                            // 模型目录刷新时保留上一份可用快照，避免每次进入或后台同步都闪回加载态。
                            uiState.serviceModelsLoading && !hasModelSnapshot -> LoadingRow()
                            filteredModels.isEmpty() -> EmptyText()
                            else -> {
                                // 列表滚动状态必须跟筛选上下文绑定，否则切到新分类会继承上一分类的底部位置。
                                key(isImage, selectedFilter, query) {
                                    ModelList(
                                        models = filteredModels,
                                        onSelect = { model ->
                                            when (model.targetTab()) {
                                                QuickCreateTab.IMAGE -> {
                                                    if (!isImage) onTabSwitch(QuickCreateTab.IMAGE)
                                                    onImageServiceModelSelected(model.identityKey)
                                                }
                                                QuickCreateTab.VIDEO -> {
                                                    if (isImage) onTabSwitch(QuickCreateTab.VIDEO)
                                                    onVideoServiceModelSelected(model.identityKey)
                                                }
                                            }
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
}

@Composable
private fun ModelSheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.quick_create_model_selector_title),
            color = QuickCreateDesignTokens.Text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(
                    Res.string.quick_create_model_selector_close_content_description,
                ),
                tint = Color(0xFFC9CAD2),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ModelSearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFF1B2028),
        border = BorderStroke(1.dp, Color(0xFF414652)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFFB8BAC3),
                modifier = Modifier.size(22.dp),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.quick_create_model_selector_search_placeholder),
                        color = Color(0xFFA2A4AD),
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = QuickCreateDesignTokens.Text,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(QuickCreateDesignTokens.Purple),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ModelFilterRow(selectedFilter: ModelFilter, onSelect: (ModelFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModelFilter.entries.forEach { filter ->
            val active = filter == selectedFilter
            Surface(
                onClick = { onSelect(filter) },
                shape = RoundedCornerShape(9.dp),
                color = if (active) Color(0x9939285B) else Color(0xFF1B1E23),
                border = BorderStroke(
                    1.dp,
                    if (active) QuickCreateDesignTokens.Purple else Color(0xFF454853),
                ),
                modifier = Modifier.height(32.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    filter.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (active) QuickCreateDesignTokens.PurpleSoft else Color(0xFFD7D8DE),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = filter.label(),
                        color = if (active) QuickCreateDesignTokens.PurpleSoft else Color(0xFFD7D8DE),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelList(
    models: List<QuickCreateServiceModelUi>,
    onSelect: (QuickCreateServiceModelUi) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        models.groupBy { it.groupTitle.asServiceModelText() }
            .forEach { (groupTitle, groupModels) ->
                GroupHeader(
                    title = groupTitle.ifBlank { stringResource(Res.string.quick_create_model_selector_video_group) },
                    count = groupModels.size,
                )
                groupModels.forEach { model ->
                    ServiceModelListRow(
                        model = model,
                        onClick = { onSelect(model) },
                    )
                }
            }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Text(
            text = title,
            color = QuickCreateDesignTokens.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = count.toString(),
            color = QuickCreateDesignTokens.Dim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ServiceModelListRow(
    model: QuickCreateServiceModelUi,
    onClick: () -> Unit,
) {
    val selected = model.selected
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = Color(0xFF15191E),
        border = BorderStroke(1.dp, Color(0xFF3A3D46)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ModelGlyphForKind(model = model)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = model.displayName.asServiceModelText(),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modelCapabilityTags(model).forEachIndexed { index, tag ->
                        CapabilityTag(text = tag, muted = index > 0)
                    }
                }
                Text(
                    text = stringResource(
                        Res.string.quick_create_model_selector_path_format,
                        model.source.bindingId.ifBlank { model.source.skuId },
                    ),
                    color = Color(0xFF90929A),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = modelPriceText(model),
                color = Color(0xFFC9CAD1),
                fontSize = 14.sp,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (selected) QuickCreateDesignTokens.Purple else Color.Transparent,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF090A0D),
                        modifier = Modifier.size(15.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = Color(0xFFC5C7D0),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelGlyphForKind(model: QuickCreateServiceModelUi) {
    val kind = model.outputKind()
    val icon = when (kind) {
        ModelOutputKind.IMAGE -> Icons.Default.Image
        ModelOutputKind.VIDEO -> Icons.Default.Movie
        ModelOutputKind.AUDIO -> Icons.Default.AudioFile
        ModelOutputKind.OTHER -> Icons.Default.MoreHoriz
    }
    val accent = when (kind) {
        ModelOutputKind.IMAGE -> QuickCreateDesignTokens.Purple
        ModelOutputKind.VIDEO -> Color(0xFF5B4BD4)
        ModelOutputKind.AUDIO -> Color(0xFF7B5FF4)
        ModelOutputKind.OTHER -> Color(0xFF5E63D7)
    }
    QuickCreateModelGlyph(
        modifier = Modifier.size(52.dp),
        accent = accent,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QuickCreateDesignTokens.Text,
            modifier = Modifier.size(27.dp),
        )
    }
}

@Composable
private fun CapabilityTag(text: String, muted: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (muted) Color(0xFF22252B) else Color(0xFF2A2240),
        border = BorderStroke(1.dp, if (muted) Color(0xFF535761) else QuickCreateDesignTokens.Purple),
    ) {
        Text(
            text = text,
            color = if (muted) Color(0xFFBEC0C7) else QuickCreateDesignTokens.PurpleSoft,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = QuickCreateDesignTokens.Purple,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(Res.string.quick_create_model_selector_loading),
            color = QuickCreateDesignTokens.Muted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun EmptyText() {
    Text(
        text = stringResource(Res.string.quick_create_model_selector_empty),
        color = QuickCreateDesignTokens.Muted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(vertical = 20.dp),
    )
}

@Composable
private fun modelPriceText(model: QuickCreateServiceModelUi): String =
    model.source.pricing?.priceSummaryRaw
        ?: model.source.pricing?.flatPriceRaw
        ?: model.source.pricing?.dimensionPricingRaw
        ?: stringResource(Res.string.quick_create_model_selector_unknown_price)

private fun modelCapabilityTags(model: QuickCreateServiceModelUi): List<String> =
    buildList {
        model.source.apiType?.cleanCapabilityTag()?.let(::add)
        model.source.apiSource?.cleanCapabilityTag()?.let(::add)
    }.ifEmpty {
        model.inferredCapabilityTags()
    }.distinct().take(2)

private enum class ModelFilter {
    IMAGE,
    VIDEO,
    AUDIO,
    OTHER;

    val icon: androidx.compose.ui.graphics.vector.ImageVector?
        get() = when (this) {
            IMAGE -> Icons.Default.Image
            VIDEO -> Icons.Default.Movie
            AUDIO -> Icons.Default.AudioFile
            OTHER -> Icons.Default.MoreHoriz
        }

    @Composable
    fun label(): String =
        when (this) {
            IMAGE -> stringResource(Res.string.quick_create_model_selector_filter_image)
            VIDEO -> stringResource(Res.string.quick_create_model_selector_filter_video)
            AUDIO -> stringResource(Res.string.quick_create_model_selector_filter_audio)
            OTHER -> stringResource(Res.string.quick_create_model_selector_filter_other)
        }

    fun accepts(model: QuickCreateServiceModelUi): Boolean =
        when (this) {
            IMAGE -> model.outputKind() == ModelOutputKind.IMAGE
            VIDEO -> model.outputKind() == ModelOutputKind.VIDEO
            AUDIO -> model.outputKind() == ModelOutputKind.AUDIO
            OTHER -> model.outputKind() == ModelOutputKind.OTHER
        }
}

private enum class ModelOutputKind {
    IMAGE,
    VIDEO,
    AUDIO,
    OTHER,
}

private fun QuickCreateServiceModelUi.targetTab(): QuickCreateTab =
    if (outputKind() == ModelOutputKind.IMAGE) QuickCreateTab.IMAGE else QuickCreateTab.VIDEO

private fun QuickCreateServiceModelUi.outputKind(): ModelOutputKind {
    val type = source.apiType.normalizedCapabilityText()
    return when {
        type.isVideoOutputCapability() -> ModelOutputKind.VIDEO
        type.isAudioOutputCapability() -> ModelOutputKind.AUDIO
        type.isImageOutputCapability() -> ModelOutputKind.IMAGE
        else -> when (source.categoryId.uppercase()) {
            "IMAGE" -> ModelOutputKind.IMAGE
            "VIDEO" -> ModelOutputKind.VIDEO
            "AUDIO" -> ModelOutputKind.AUDIO
            else -> ModelOutputKind.OTHER
        }
    }
}

private fun QuickCreateServiceModelUi.inferredCapabilityTags(): List<String> {
    val text = source.apiType.normalizedCapabilityText()
    val explicitTag = listOf(
        "multi-image-to-3d",
        "image-to-3d",
        "text-to-3d",
        "reference-to-video",
        "image-to-video",
        "text-to-video",
        "image-to-image",
        "text-to-image",
        "text-to-audio",
        "image-to-audio",
    ).firstOrNull(text::contains)

    return listOf(
        explicitTag
            ?: source.categoryId.cleanCapabilityTag()
            ?: "unknown",
    ).filterNot { it.equals("unknown", ignoreCase = true) }
}

private fun String.cleanCapabilityTag(): String? =
    trim()
        .takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("unknown", ignoreCase = true) }

private fun String?.normalizedCapabilityText(): String =
    orEmpty().lowercase()

private fun String.isVideoOutputCapability(): Boolean =
    contains("video") ||
        contains("\u89c6\u9891")

private fun String.isAudioOutputCapability(): Boolean =
    contains("audio") ||
        contains("music") ||
        contains("\u97f3\u9891") ||
        contains("\u97f3\u4e50")

private fun String.isImageOutputCapability(): Boolean =
    contains("image") &&
        !isVideoOutputCapability() &&
        !isAudioOutputCapability() &&
        !contains("3d")

private fun List<QuickCreateServiceModelUi>.typeDistributionLog(): String =
    map { it.source.apiType?.takeIf { type -> type.isNotBlank() } ?: "unknown" }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(8)
        .joinToString(prefix = "[", postfix = "]") { "${it.key}:${it.value}" }
