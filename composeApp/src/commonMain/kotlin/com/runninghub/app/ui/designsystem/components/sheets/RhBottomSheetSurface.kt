package com.runninghub.app.ui.designsystem.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme

/**
 * RunningHub 底部弹层的统一表面容器。
 *
 * @param modifier 外部布局修饰符，组件内部负责宽度、导航栏避让和默认内距。
 * @param content 弹层内容，由调用方提供业务控件和本地化文案。
 */
@Composable
fun RhBottomSheetSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = RhTheme.shapes.sheet, topEnd = RhTheme.shapes.sheet),
        color = RhTheme.colors.overlaySheet,
        border = BorderStroke(1.dp, RhTheme.colors.borderSubtle),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(RhSpacing.lg),
            content = content,
        )
    }
}
