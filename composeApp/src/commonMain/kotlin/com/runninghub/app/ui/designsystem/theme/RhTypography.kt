package com.runninghub.app.ui.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * RunningHub 设计系统的文字层级。
 *
 * 所有样式的 letterSpacing 固定为 0.sp，避免移动端中文和英文混排时出现不可控的负字距。
 */
object RhTypography {
    /** 32sp 首屏展示标题，用于真正的高优先级页面入口。 */
    val display = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, letterSpacing = 0.sp)
    /** 26sp 页面标题，用于页面级主要标题。 */
    val pageTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = 0.sp)
    /** 18sp 区块标题，用于列表分组或面板标题。 */
    val sectionTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, letterSpacing = 0.sp)
    /** 16sp 卡片标题，用于单个内容卡片的主标题。 */
    val cardTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, letterSpacing = 0.sp)
    /** 14sp 默认正文，用于说明和常规内容。 */
    val body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.sp)
    /** 14sp 强调正文，用于短标签和局部重点信息。 */
    val bodyStrong = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp)
    /** 12sp 辅助说明，用于次级提示和弱信息。 */
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.sp)
    /** 11sp 元信息，用于时间、状态补充和低优先级数字。 */
    val meta = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.sp)
    /** 15sp 按钮文字，用于主要和次要操作按钮。 */
    val button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, letterSpacing = 0.sp)
    /** 16sp 价格文字，用于 credit、现金金额和价格确认结果。 */
    val price = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp, letterSpacing = 0.sp)
    /** 12sp 状态徽标文字，用于任务、价格和处理状态。 */
    val statusBadge = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, letterSpacing = 0.sp)
}
