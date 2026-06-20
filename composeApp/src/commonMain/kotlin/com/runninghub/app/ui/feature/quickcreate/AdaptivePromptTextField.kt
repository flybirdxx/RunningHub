package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.theme.*
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS

private const val MIN_HEIGHT_DP = 52
private const val LINE_HEIGHT_SP = 22
private const val MAX_LINES = 6

@Composable
fun AdaptivePromptTextField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    placeholder: String,
    charCount: Int,
    nearLimit: Boolean,
    overLimit: Boolean,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        color = Neutral200,
        fontSize = 14.sp,
        lineHeight = LINE_HEIGHT_SP.sp,
        fontWeight = FontWeight.Normal,
    )

    val minHeight = MIN_HEIGHT_DP.dp
    val maxHeight = (MIN_HEIGHT_DP + (LINE_HEIGHT_SP * (MAX_LINES - 1))).dp

    val borderColor = when {
        overLimit -> ErrorDark
        else -> DarkOutlineVariant
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(Dimens.RadiusMD),
            color = DarkSurfaceVariant,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight, max = maxHeight)
                    .padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            ) {
                if (prompt.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = Neutral500),
                    )
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = { newValue ->
                        if (newValue.length <= MAX_PROMPT_CHARS) {
                            onPromptChange(newValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minHeight),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(Primary300),
                )
            }
        }

        val countColor = when {
            overLimit -> ErrorDark
            nearLimit -> WarningDark
            else -> Neutral500
        }
        Text(
            text = "$charCount",
            color = countColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Dimens.SpaceMD, bottom = Dimens.SpaceXS),
        )
    }
}
