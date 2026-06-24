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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import runninghub.composeapp.generated.resources.quick_create_model_selector_apply
import runninghub.composeapp.generated.resources.quick_create_model_selector_empty
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_3d
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_all
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_audio
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_image
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_recent
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_video
import runninghub.composeapp.generated.resources.quick_create_model_selector_loading
import runninghub.composeapp.generated.resources.quick_create_model_selector_path_format
import runninghub.composeapp.generated.resources.quick_create_model_selector_search_placeholder
import runninghub.composeapp.generated.resources.quick_create_model_selector_selected_label
import runninghub.composeapp.generated.resources.quick_create_model_selector_title
import runninghub.composeapp.generated.resources.quick_create_model_selector_unknown_price
import runninghub.composeapp.generated.resources.quick_create_model_selector_video_group

private const val RECENT_GROUP_MARKER = "\u6700\u8fd1\u4e0a\u65b0"

/**
 * 展示设计稿版快捷创作服务端模型选择面板。
 *
 * 面板只消费 [QuickCreateUiState] 中已经加载好的模型目录，搜索过滤与分类筛选属于本地临时 UI 状态；
 * 模型点击只通过回调更新 ScreenModel 中的选中项，底部“套用”负责关闭弹层，避免选择动作绕过状态层。
 * 顶部手柄只上报拖拽过程，真正的 sheet 位移和收起判定由页面边界统一处理。
 */
@Composable
internal fun QuickCreateModelSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onDismiss: () -> Unit,
    onImageServiceModelSelected: (String) -> Unit,
    onVideoServiceModelSelected: (String) -> Unit,
    onTabSwitch: (QuickCreateTab) -> Unit,
    modifier: Modifier = Modifier,
    onSheetDragStart: () -> Unit = {},
    onSheetDrag: (Float) -> Unit = {},
    onSheetDragEnd: () -> Unit = {},
    onSheetDragCancel: () -> Unit = {},
) {
    val models = (uiState.serviceImageModelItems + uiState.serviceVideoModelItems)
        .distinctBy { it.identityKey }
    val selectedModel = if (isImage) uiState.selectedImageServiceModelUi else uiState.selectedVideoServiceModelUi
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ModelFilter.ALL) }
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = stringResource(Res.string.quick_create_model_selector_title),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 28.dp, bottom = 18.dp),
                )
                ModelSearchField(query = query, onQueryChange = { query = it })
                ModelFilterRow(selectedFilter = selectedFilter, onSelect = { selectedFilter = it })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .padding(top = 16.dp, bottom = 12.dp),
                ) {
                    when {
                        uiState.serviceModelsLoading -> LoadingRow()
                        filteredModels.isEmpty() -> EmptyText()
                        else -> ModelList(
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
                SelectedModelBar(
                    model = selectedModel,
                    onApply = onDismiss,
                )
            }
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
    val icon = when {
        model.matchesAudioCapability() -> Icons.Default.AudioFile
        model.matches3dCapability() -> Icons.Default.ViewInAr
        model.targetTab() == QuickCreateTab.IMAGE -> Icons.Default.Image
        else -> Icons.Default.Movie
    }
    val accent = when {
        model.matchesAudioCapability() -> Color(0xFF7B5FF4)
        model.matches3dCapability() -> Color(0xFF5E63D7)
        model.targetTab() == QuickCreateTab.IMAGE -> QuickCreateDesignTokens.Purple
        else -> Color(0xFF5B4BD4)
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
private fun SelectedModelBar(model: QuickCreateServiceModelUi?, onApply: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = Color(0xF515191E),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickCreateModelGlyph(modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = QuickCreateDesignTokens.Text,
                    modifier = Modifier.size(25.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.quick_create_model_selector_selected_label),
                    color = Color(0xFFB8BAC3),
                    fontSize = 11.sp,
                )
                Text(
                    text = model?.displayName?.asServiceModelText().orEmpty(),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = model?.let {
                        stringResource(
                            Res.string.quick_create_model_selector_path_format,
                            it.source.bindingId.ifBlank { it.source.skuId },
                        )
                    }.orEmpty(),
                    color = Color(0xFF90929A),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                onClick = onApply,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.height(42.dp).width(110.dp),
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.horizontalGradient(listOf(Color(0xFF7556F6), Color(0xFF9B61FF))),
                        RoundedCornerShape(12.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.quick_create_model_selector_apply),
                        color = QuickCreateDesignTokens.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
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
    ALL,
    IMAGE,
    VIDEO,
    AUDIO,
    THREE_D,
    RECENT;

    val icon: androidx.compose.ui.graphics.vector.ImageVector?
        get() = when (this) {
            ALL -> null
            IMAGE -> Icons.Default.Image
            VIDEO -> Icons.Default.Movie
            AUDIO -> Icons.Default.AudioFile
            THREE_D -> Icons.Default.ViewInAr
            RECENT -> Icons.Default.Schedule
        }

    @Composable
    fun label(): String =
        when (this) {
            ALL -> stringResource(Res.string.quick_create_model_selector_filter_all)
            IMAGE -> stringResource(Res.string.quick_create_model_selector_filter_image)
            VIDEO -> stringResource(Res.string.quick_create_model_selector_filter_video)
            AUDIO -> stringResource(Res.string.quick_create_model_selector_filter_audio)
            THREE_D -> stringResource(Res.string.quick_create_model_selector_filter_3d)
            RECENT -> stringResource(Res.string.quick_create_model_selector_filter_recent)
        }

    fun accepts(model: QuickCreateServiceModelUi): Boolean =
        when (this) {
            ALL -> true
            IMAGE -> model.targetTab() == QuickCreateTab.IMAGE
            VIDEO -> model.matchesVideoCapability()
            AUDIO -> model.matchesAudioCapability()
            THREE_D -> model.matches3dCapability()
            RECENT -> model.source.groupName?.contains(RECENT_GROUP_MARKER) == true ||
                model.source.name.contains(RECENT_GROUP_MARKER)
        }
}

private fun QuickCreateServiceModelUi.targetTab(): QuickCreateTab =
    if (source.categoryId.equals("VIDEO", ignoreCase = true) ||
        source.apiType.normalizedCapabilityText().isNonImageOutputCapability()
    ) {
        QuickCreateTab.VIDEO
    } else {
        QuickCreateTab.IMAGE
    }

private fun QuickCreateServiceModelUi.matchesVideoCapability(): Boolean =
    capabilitySearchText().let { text ->
        text.contains("video") ||
            text.contains("\u89c6\u9891") ||
            (
                source.categoryId.equals("VIDEO", ignoreCase = true) &&
                    !matchesAudioCapability() &&
                    !matches3dCapability()
                )
    }

private fun QuickCreateServiceModelUi.matchesAudioCapability(): Boolean =
    capabilitySearchText().let { text ->
        text.contains("audio") ||
            text.contains("music") ||
            text.contains("suno") ||
            text.contains("\u97f3\u9891") ||
            text.contains("\u97f3\u4e50")
    }

private fun QuickCreateServiceModelUi.matches3dCapability(): Boolean =
    capabilitySearchText().contains("3d")

private fun QuickCreateServiceModelUi.inferredCapabilityTags(): List<String> {
    val text = capabilitySearchText()
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
        explicitTag ?: when {
            matches3dCapability() -> "image-to-3d"
            matchesAudioCapability() -> "text-to-audio"
            matchesVideoCapability() -> "text-to-video"
            else -> "text-to-image"
        },
    )
}

private fun String.cleanCapabilityTag(): String? =
    trim()
        .takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("unknown", ignoreCase = true) }

private fun QuickCreateServiceModelUi.capabilitySearchText(): String =
    buildString {
        append(source.categoryId)
        append(' ')
        append(source.apiType.orEmpty())
        append(' ')
        append(source.groupName.orEmpty())
        append(' ')
        append(source.name)
    }.lowercase()

private fun String?.normalizedCapabilityText(): String =
    orEmpty().lowercase()

private fun String.isNonImageOutputCapability(): Boolean =
    contains("video") ||
        contains("audio") ||
        contains("music") ||
        contains("3d")

private fun List<QuickCreateServiceModelUi>.typeDistributionLog(): String =
    map { it.source.apiType?.takeIf { type -> type.isNotBlank() } ?: "unknown" }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(8)
        .joinToString(prefix = "[", postfix = "]") { "${it.key}:${it.value}" }
