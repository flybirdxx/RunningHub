package com.runninghub.app.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * RunningHub 设计系统的语义色板。
 *
 * 该类型位于应用壳 UI 层，只承载 roadmap RM-01/RM-02 的视觉语义，不绑定具体页面、远端字段或业务枚举。
 *
 * @property backgroundPrimary 页面最底层背景色，暗色模式下用于全局根容器，不表达业务状态。
 * @property backgroundSecondary 次级背景色，用于局部区域和渐变过渡，来源于设计系统静态 token。
 * @property backgroundGradient 页面背景渐变色序列，顺序有意义，空集合不允许。
 * @property surfaceDefault 默认卡片和列表项表面色，用于普通可读内容容器。
 * @property surfaceElevated 视觉层级更高的表面色，用于浮层、弹窗或强调卡片。
 * @property surfaceSunken 下沉表面色，用于输入区、等待态和弱化容器。
 * @property surfaceSelected 已选中或已激活容器色，不代表远端业务成功。
 * @property surfaceDisabled 不可交互容器色，用于禁用按钮或不可选区域。
 * @property textPrimary 最高优先级正文颜色，要求满足暗色背景可读性。
 * @property textSecondary 次级正文颜色，用于说明、副标题和弱强调信息。
 * @property textTertiary 第三级文字颜色，用于辅助元信息、占位和禁用文案。
 * @property textInverse 反色文字颜色，主要用于品牌色按钮上的内容。
 * @property borderDefault 默认边框颜色，用于普通分隔和控件边界。
 * @property borderSubtle 弱边框颜色，用于暗色表面上的轻量分隔。
 * @property borderActive 激活边框颜色，用于焦点、选中和主操作强调。
 * @property brandPrimary 主品牌色，暗色模式下保持低饱和强调。
 * @property brandSecondary 辅助品牌色，用于少量强调，不作为主页面基调。
 * @property brandMuted 品牌色的低饱和背景，用于选中态或轻强调底色。
 * @property statusSuccess 成功状态颜色，来源于本地状态映射，不直接展示服务端 message。
 * @property statusFailed 失败状态颜色，用于错误或任务失败，不包含错误文案。
 * @property statusProcessing 进行中状态颜色，用于轮询、生成、上传等处理中状态。
 * @property statusWarning 警告状态颜色，用于运行前费用确认、余额提醒等可恢复状态。
 * @property priceCredit 积分或 credit 价格颜色，只表达价格视觉层级。
 * @property priceMoney 现金或法币价格颜色，只表达价格视觉层级。
 * @property overlayScrim 遮罩层颜色，用于弹窗和底部弹层背后的弱化背景。
 * @property overlaySheet 底部弹层表面色，透明度需兼顾暗色背景可读性。
 */
@Immutable
data class RhColors(
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val backgroundGradient: List<Color>,
    val surfaceDefault: Color,
    val surfaceElevated: Color,
    val surfaceSunken: Color,
    val surfaceSelected: Color,
    val surfaceDisabled: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverse: Color,
    val borderDefault: Color,
    val borderSubtle: Color,
    val borderActive: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val brandMuted: Color,
    val statusSuccess: Color,
    val statusFailed: Color,
    val statusProcessing: Color,
    val statusWarning: Color,
    val priceCredit: Color,
    val priceMoney: Color,
    val overlayScrim: Color,
    val overlaySheet: Color,
) {
    /**
     * 将背景渐变 token 转换为 Compose Brush，避免页面重复拼装渐变方向。
     */
    val backgroundGradientBrush: Brush
        get() = Brush.verticalGradient(backgroundGradient)
}

/**
 * roadmap RM-01 的暗色主色板，是当前 redesign 的默认视觉基准。
 */
val RhDarkColors = RhColors(
    backgroundPrimary = Color(0xFF050608),
    backgroundSecondary = Color(0xFF090B0F),
    backgroundGradient = listOf(Color(0xFF050608), Color(0xFF10151C)),
    surfaceDefault = Color(0xFF11151A),
    surfaceElevated = Color(0xFF171B22),
    surfaceSunken = Color(0xFF0B0E13),
    surfaceSelected = Color(0xFF1B2117),
    surfaceDisabled = Color(0xFF1A1D22),
    textPrimary = Color(0xFFF5F7FA),
    textSecondary = Color(0xFFC5CBD3),
    textTertiary = Color(0xFF8B929D),
    textInverse = Color(0xFF050608),
    borderDefault = Color(0xFF2A3038),
    borderSubtle = Color(0xFF1B2028),
    borderActive = Color(0xFF7F9460),
    brandPrimary = Color(0xFFA3B565),
    brandSecondary = Color(0xFF8D63FF),
    brandMuted = Color(0xFF242C1D),
    statusSuccess = Color(0xFF6EA77A),
    statusFailed = Color(0xFFE26066),
    statusProcessing = Color(0xFF69A7C2),
    statusWarning = Color(0xFFD6A85C),
    priceCredit = Color(0xFFC3B36A),
    priceMoney = Color(0xFFD6B56D),
    overlayScrim = Color(0xB3000000),
    overlaySheet = Color(0xF2171B22),
)

/**
 * 浅色兼容色板，用于系统浅色模式下保持组件可读；当前不是 redesign 的主视觉验收基准。
 */
val RhLightColors = RhColors(
    backgroundPrimary = Color(0xFFF8FAFC),
    backgroundSecondary = Color(0xFFF1F5F9),
    backgroundGradient = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)),
    surfaceDefault = Color.White,
    surfaceElevated = Color(0xFFF8FAFC),
    surfaceSunken = Color(0xFFE2E8F0),
    surfaceSelected = Color(0xFFE9FBC4),
    surfaceDisabled = Color(0xFFE5E7EB),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF64748B),
    textInverse = Color(0xFF050608),
    borderDefault = Color(0xFFCBD5E1),
    borderSubtle = Color(0xFFE2E8F0),
    borderActive = Color(0xFF4E6200),
    brandPrimary = Color(0xFF4E6200),
    brandSecondary = Color(0xFF6C5CE7),
    brandMuted = Color(0xFFDFF7A7),
    statusSuccess = Color(0xFF16A34A),
    statusFailed = Color(0xFFDC2626),
    statusProcessing = Color(0xFF0284C7),
    statusWarning = Color(0xFFD97706),
    priceCredit = Color(0xFF4E6200),
    priceMoney = Color(0xFFB45309),
    overlayScrim = Color(0x99000000),
    overlaySheet = Color(0xF2FFFFFF),
)
