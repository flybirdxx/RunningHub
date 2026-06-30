package com.runninghub.app.ui.feature.quickcreate.presentation.modelselector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.components.cards.ModelCardState
import com.runninghub.app.ui.designsystem.components.cards.ModelCardVisualState
import com.runninghub.app.ui.designsystem.components.sheets.ModelPickerFilterItem
import com.runninghub.app.ui.designsystem.components.sheets.ModelPickerSheet
import com.runninghub.app.ui.designsystem.components.sheets.ModelPickerSheetState
import com.runninghub.app.ui.feature.quickcreate.QuickCreateSheetHandle
import com.runninghub.app.ui.feature.quickcreate.presentation.asServiceModelText
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateModelPickerEmptyReason
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateModelPickerFilter
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateModelPickerState
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelKind
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelPrice
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelScene
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.modelcatalog.quickCreateModelPickerState
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_model_scene_audio
import runninghub.composeapp.generated.resources.quick_create_model_scene_general
import runninghub.composeapp.generated.resources.quick_create_model_scene_image
import runninghub.composeapp.generated.resources.quick_create_model_scene_video
import runninghub.composeapp.generated.resources.quick_create_model_selector_audio_group
import runninghub.composeapp.generated.resources.quick_create_model_selector_close_content_description
import runninghub.composeapp.generated.resources.quick_create_model_selector_empty
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_audio
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_image
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_other
import runninghub.composeapp.generated.resources.quick_create_model_selector_filter_video
import runninghub.composeapp.generated.resources.quick_create_model_selector_free_price
import runninghub.composeapp.generated.resources.quick_create_model_selector_image_group
import runninghub.composeapp.generated.resources.quick_create_model_selector_loading
import runninghub.composeapp.generated.resources.quick_create_model_selector_no_results
import runninghub.composeapp.generated.resources.quick_create_model_selector_search_placeholder
import runninghub.composeapp.generated.resources.quick_create_model_selector_title
import runninghub.composeapp.generated.resources.quick_create_model_selector_unknown_price
import runninghub.composeapp.generated.resources.quick_create_model_selector_video_group

@Composable
internal fun QuickCreateModelSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onImageServiceModelSelected: (String) -> Unit,
    onVideoServiceModelSelected: (String) -> Unit,
    onTabSwitch: (QuickCreateTab) -> Unit,
    onDismiss: () -> Unit,
    onModelPickerQueryChange: (String) -> Unit,
    onModelPickerFilterSelected: (QuickCreateModelPickerFilter) -> Unit,
    modifier: Modifier = Modifier,
    onSheetDragStart: () -> Unit = {},
    onSheetDrag: (Float) -> Unit = {},
    onSheetDragEnd: () -> Unit = {},
    onSheetDragCancel: () -> Unit = {},
) {
    val pickerState = quickCreateModelPickerState(uiState)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            ModelPickerSheet(
                state = pickerState.toModelPickerSheetState(),
                onSearchQueryChange = onModelPickerQueryChange,
                onFilterSelect = { id ->
                    id.toQuickCreateModelPickerFilterOrNull()?.let(onModelPickerFilterSelected)
                },
                onCardClick = { identityKey ->
                    pickerState.visibleItems
                        .firstOrNull { it.identityKey == identityKey }
                        ?.let { model ->
                            when (model.targetTab) {
                                QuickCreateTab.IMAGE -> {
                                    if (!isImage) onTabSwitch(QuickCreateTab.IMAGE)
                                    onImageServiceModelSelected(model.identityKey)
                                }
                                QuickCreateTab.VIDEO -> {
                                    if (isImage) onTabSwitch(QuickCreateTab.VIDEO)
                                    onVideoServiceModelSelected(model.identityKey)
                                }
                            }
                        }
                },
                onDismiss = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                dragHandle = {
                    QuickCreateSheetHandle(
                        onDragStart = onSheetDragStart,
                        onDrag = onSheetDrag,
                        onDragEnd = onSheetDragEnd,
                        onDragCancel = onSheetDragCancel,
                    )
                },
            )
        }
    }
}

@Composable
private fun QuickCreateModelPickerState.toModelPickerSheetState(): ModelPickerSheetState =
    ModelPickerSheetState(
        title = stringResource(Res.string.quick_create_model_selector_title),
        searchQuery = query,
        searchPlaceholder = stringResource(Res.string.quick_create_model_selector_search_placeholder),
        filters = QuickCreateModelPickerFilter.entries.map { filter ->
            ModelPickerFilterItem(
                id = filter.name,
                label = filter.labelText(),
                selected = filter == selectedFilter,
            )
        },
        cards = visibleItems.map { it.toModelCardState() },
        loading = loading || emptyReason == QuickCreateModelPickerEmptyReason.Loading,
        emptyText = when (emptyReason) {
            QuickCreateModelPickerEmptyReason.SearchNoResult ->
                stringResource(Res.string.quick_create_model_selector_no_results)
            QuickCreateModelPickerEmptyReason.Loading,
            QuickCreateModelPickerEmptyReason.EmptyCatalog,
            null,
            -> stringResource(Res.string.quick_create_model_selector_empty)
        },
        loadingText = stringResource(Res.string.quick_create_model_selector_loading),
        closeContentDescription = stringResource(
            Res.string.quick_create_model_selector_close_content_description,
        ),
    )

@Composable
private fun QuickCreateServiceModelUi.toModelCardState(): ModelCardState =
    ModelCardState(
        id = identityKey,
        title = displayName.asServiceModelText(),
        capability = kind.capabilityText(),
        scene = scene.sceneText(),
        technicalTags = technicalTags,
        price = price.priceText(),
        selected = selected,
        visualState = if (selected) ModelCardVisualState.Selected else ModelCardVisualState.Default,
    )

@Composable
private fun QuickCreateModelPickerFilter.labelText(): String =
    when (this) {
        QuickCreateModelPickerFilter.Image ->
            stringResource(Res.string.quick_create_model_selector_filter_image)
        QuickCreateModelPickerFilter.Video ->
            stringResource(Res.string.quick_create_model_selector_filter_video)
        QuickCreateModelPickerFilter.Audio ->
            stringResource(Res.string.quick_create_model_selector_filter_audio)
        QuickCreateModelPickerFilter.Other ->
            stringResource(Res.string.quick_create_model_selector_filter_other)
    }

@Composable
private fun QuickCreateServiceModelKind.capabilityText(): String =
    when (this) {
        QuickCreateServiceModelKind.Image ->
            stringResource(Res.string.quick_create_model_selector_image_group)
        QuickCreateServiceModelKind.Video ->
            stringResource(Res.string.quick_create_model_selector_video_group)
        QuickCreateServiceModelKind.Audio ->
            stringResource(Res.string.quick_create_model_selector_audio_group)
        QuickCreateServiceModelKind.Other ->
            stringResource(Res.string.quick_create_model_selector_filter_other)
    }

@Composable
private fun QuickCreateServiceModelScene.sceneText(): String =
    when (this) {
        QuickCreateServiceModelScene.ImageGeneration ->
            stringResource(Res.string.quick_create_model_scene_image)
        QuickCreateServiceModelScene.VideoGeneration ->
            stringResource(Res.string.quick_create_model_scene_video)
        QuickCreateServiceModelScene.AudioGeneration ->
            stringResource(Res.string.quick_create_model_scene_audio)
        QuickCreateServiceModelScene.General ->
            stringResource(Res.string.quick_create_model_scene_general)
    }

@Composable
private fun QuickCreateServiceModelPrice.priceText(): String =
    when (this) {
        QuickCreateServiceModelPrice.Free ->
            stringResource(Res.string.quick_create_model_selector_free_price)
        QuickCreateServiceModelPrice.Unknown ->
            stringResource(Res.string.quick_create_model_selector_unknown_price)
        is QuickCreateServiceModelPrice.Known -> text
    }

private fun String.toQuickCreateModelPickerFilterOrNull(): QuickCreateModelPickerFilter? =
    QuickCreateModelPickerFilter.entries.firstOrNull { it.name == this }
