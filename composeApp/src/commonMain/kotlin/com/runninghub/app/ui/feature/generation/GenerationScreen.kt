package com.runninghub.app.ui.feature.generation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationScreen(
    appName: String,
    onBack: () -> Unit,
    onGenerate: (prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableIntStateOf(0) }
    var selectedSize by remember { mutableIntStateOf(0) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var refinement by remember { mutableFloatStateOf(50f) }
    var generateCount by remember { mutableIntStateOf(1) }

    val styles = listOf("写实", "油画", "动漫", "水彩", "线稿", "3D渲染")
    val canvasSizes = listOf("1:1", "3:4", "4:3", "9:16")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = AppDimens.PaddingSmall)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingLarge)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(
                        "输入画面描述...",
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                Text(
                    text = "选择风格",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                ) {
                    styles.forEachIndexed { index, style ->
                        val isSelected = index == selectedStyle
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStyle = index },
                            label = {
                                Text(
                                    text = style,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                Text(
                    text = "画布尺寸",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                ) {
                    canvasSizes.forEachIndexed { index, size ->
                        val isSelected = index == selectedSize
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(AppDimens.ButtonCornerRadius)
                                        )
                                    } else {
                                        Modifier.border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(AppDimens.ButtonCornerRadius)
                                        )
                                    }
                                )
                                .clickable { selectedSize = index }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = size,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(vertical = AppDimens.PaddingSmall)
                ) {
                    Text(
                        text = "高级设置",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        if (advancedExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = advancedExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(AppDimens.PaddingMedium),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingLarge)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "精细度",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${refinement.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = refinement,
                                onValueChange = { refinement = it },
                                valueRange = 0f..100f
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)) {
                            Text(
                                text = "生成数量",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingMedium),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { if (generateCount > 1) generateCount-- },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription = "减少",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "$generateCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { if (generateCount < 8) generateCount++ },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "增加",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
        }

        Button(
            onClick = { onGenerate(promptText) },
            enabled = promptText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.PaddingMedium)
                .padding(bottom = AppDimens.PaddingLarge)
                .navigationBarsPadding()
                .height(52.dp),
            shape = RoundedCornerShape(AppDimens.ButtonCornerRadius)
        ) {
            Text(
                text = "立即生成",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
