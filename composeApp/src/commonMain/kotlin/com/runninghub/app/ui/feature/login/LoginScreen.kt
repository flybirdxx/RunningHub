package com.runninghub.app.ui.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.auth.presentation.login.LoginErrorText
import com.runninghub.feature.auth.presentation.login.LoginUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.login_agreement_notice
import runninghub.composeapp.generated.resources.login_brand_initial
import runninghub.composeapp.generated.resources.login_brand_name
import runninghub.composeapp.generated.resources.login_error_account_not_found
import runninghub.composeapp.generated.resources.login_error_captcha_invalid
import runninghub.composeapp.generated.resources.login_error_captcha_required
import runninghub.composeapp.generated.resources.login_error_invalid_phone
import runninghub.composeapp.generated.resources.login_error_login_failed
import runninghub.composeapp.generated.resources.login_error_network
import runninghub.composeapp.generated.resources.login_error_password_required
import runninghub.composeapp.generated.resources.login_error_phone_required
import runninghub.composeapp.generated.resources.login_error_sms_code_expired
import runninghub.composeapp.generated.resources.login_error_sms_code_incomplete
import runninghub.composeapp.generated.resources.login_error_sms_code_required
import runninghub.composeapp.generated.resources.login_error_sms_daily_limit
import runninghub.composeapp.generated.resources.login_error_sms_rate_limited
import runninghub.composeapp.generated.resources.login_error_wrong_sms_code
import runninghub.composeapp.generated.resources.login_password_label
import runninghub.composeapp.generated.resources.login_phone_country_code
import runninghub.composeapp.generated.resources.login_phone_label
import runninghub.composeapp.generated.resources.login_sms_captcha_captcha_load_failed
import runninghub.composeapp.generated.resources.login_sms_captcha_dialog_title
import runninghub.composeapp.generated.resources.login_sms_captcha_dismiss_action
import runninghub.composeapp.generated.resources.login_sms_captcha_image_load_failed_retry
import runninghub.composeapp.generated.resources.login_sms_captcha_init_failed_retry
import runninghub.composeapp.generated.resources.login_sms_captcha_load_failed_later
import runninghub.composeapp.generated.resources.login_sms_captcha_load_failed_retry
import runninghub.composeapp.generated.resources.login_sms_captcha_preparing
import runninghub.composeapp.generated.resources.login_sms_captcha_retry_action
import runninghub.composeapp.generated.resources.login_sms_captcha_rotate_title
import runninghub.composeapp.generated.resources.login_sms_captcha_script_load_failed_retry
import runninghub.composeapp.generated.resources.login_sms_captcha_script_timeout_retry
import runninghub.composeapp.generated.resources.login_send_code_action
import runninghub.composeapp.generated.resources.login_send_code_countdown_format
import runninghub.composeapp.generated.resources.login_send_code_sending
import runninghub.composeapp.generated.resources.login_sms_code_label
import runninghub.composeapp.generated.resources.login_sms_captcha_token_missing_retry
import runninghub.composeapp.generated.resources.login_sms_captcha_verify_failed
import runninghub.composeapp.generated.resources.login_sms_captcha_verify_success
import runninghub.composeapp.generated.resources.login_submit_button
import runninghub.composeapp.generated.resources.login_toggle_password_mode
import runninghub.composeapp.generated.resources.login_toggle_sms_mode

/**
 * 登录页在 Voyager 导航中的入口。
 *
 * 本页面只负责收集登录输入和展示认证错误，不再直接替换根导航栈。
 * 登录成功由 AuthRepository 实现写入 SessionManager，根 App 观察会话状态后统一切换到主页面。
 * 这样可以避免登录页和 App 根入口同时拥有根导航权。
 */
class LoginVoyagerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<LoginScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val currentErrorMessage = uiState.error?.let { loginErrorMessage(it) }

        LaunchedEffect(currentErrorMessage) {
            currentErrorMessage?.let {
                snackbarHostState.showSnackbar(it)
                screenModel.clearError()
            }
        }

        // 图形验证是短信发送的前置步骤，放在 Scaffold 外层可避免 Snackbar 和键盘布局影响 Web 容器尺寸。
        if (uiState.requiresSmsCaptcha) {
            SmsCaptchaDialog(
                phone = uiState.phone,
                copy = smsCaptchaCopy(),
                onToken = screenModel::onSmsCaptchaVerified,
                onDismiss = screenModel::dismissSmsCaptcha,
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = RhTheme.colors.backgroundPrimary,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { _ ->
            LoginContent(
                uiState = uiState,
                onPhoneChanged = screenModel::onPhoneChanged,
                onSmsCodeChanged = screenModel::onSmsCodeChanged,
                onPasswordChanged = screenModel::onPasswordChanged,
                onSendCodeClick = screenModel::sendSmsCode,
                onLoginClick = {
                    if (uiState.isSmsMode) screenModel.smsLogin()
                    else screenModel.pwdLogin()
                },
                onToggleMode = screenModel::toggleMode,
            )
        }
    }
}

@Composable
private fun smsCaptchaCopy(): SmsCaptchaCopy =
    SmsCaptchaCopy(
        dialogTitle = stringResource(Res.string.login_sms_captcha_dialog_title),
        dismissAction = stringResource(Res.string.login_sms_captcha_dismiss_action),
        preparing = stringResource(Res.string.login_sms_captcha_preparing),
        retryAction = stringResource(Res.string.login_sms_captcha_retry_action),
        loadFailedRetry = stringResource(Res.string.login_sms_captcha_load_failed_retry),
        imageLoadFailedRetry = stringResource(Res.string.login_sms_captcha_image_load_failed_retry),
        loadFailedLater = stringResource(Res.string.login_sms_captcha_load_failed_later),
        rotateTitle = stringResource(Res.string.login_sms_captcha_rotate_title),
        captchaLoadFailed = stringResource(Res.string.login_sms_captcha_captcha_load_failed),
        verifyFailed = stringResource(Res.string.login_sms_captcha_verify_failed),
        verifySuccess = stringResource(Res.string.login_sms_captcha_verify_success),
        tokenMissingRetry = stringResource(Res.string.login_sms_captcha_token_missing_retry),
        initFailedRetry = stringResource(Res.string.login_sms_captcha_init_failed_retry),
        scriptLoadFailedRetry = stringResource(Res.string.login_sms_captcha_script_load_failed_retry),
        scriptTimeoutRetry = stringResource(Res.string.login_sms_captcha_script_timeout_retry),
    )

@Composable
private fun loginErrorMessage(error: LoginErrorText): String =
    when (error) {
        LoginErrorText.PhoneRequired -> stringResource(Res.string.login_error_phone_required)
        LoginErrorText.InvalidPhone -> stringResource(Res.string.login_error_invalid_phone)
        LoginErrorText.SmsCodeRequired -> stringResource(Res.string.login_error_sms_code_required)
        LoginErrorText.SmsCodeIncomplete -> stringResource(Res.string.login_error_sms_code_incomplete)
        LoginErrorText.PasswordRequired -> stringResource(Res.string.login_error_password_required)
        LoginErrorText.CaptchaInvalid -> stringResource(Res.string.login_error_captcha_invalid)
        LoginErrorText.WrongSmsCode -> stringResource(Res.string.login_error_wrong_sms_code)
        LoginErrorText.SmsCodeExpired -> stringResource(Res.string.login_error_sms_code_expired)
        LoginErrorText.AccountNotFound -> stringResource(Res.string.login_error_account_not_found)
        LoginErrorText.SmsRateLimited -> stringResource(Res.string.login_error_sms_rate_limited)
        LoginErrorText.SmsDailyLimit -> stringResource(Res.string.login_error_sms_daily_limit)
        LoginErrorText.CaptchaRequired -> stringResource(Res.string.login_error_captcha_required)
        LoginErrorText.Network -> stringResource(Res.string.login_error_network)
        LoginErrorText.LoginFailed -> stringResource(Res.string.login_error_login_failed)
    }

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onPhoneChanged: (String) -> Unit,
    onSmsCodeChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSendCodeClick: () -> Unit,
    onLoginClick: () -> Unit,
    onToggleMode: () -> Unit,
) {
    val colors = RhTheme.colors
    val focusManager = LocalFocusManager.current
    val smsCodeFocusRequester = remember { FocusRequester() }

    // AC1：短信发送成功后（倒计时回到 60）自动聚焦验证码输入框。
    LaunchedEffect(uiState.countdownSeconds == 60) {
        smsCodeFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundGradientBrush)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RhSpacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                text = stringResource(Res.string.login_brand_initial),
                style = RhTypography.display,
                color = colors.brandPrimary,
            )

            Text(
                text = stringResource(Res.string.login_brand_name),
                style = RhTypography.sectionTitle,
                color = colors.textPrimary,
            )

            Spacer(Modifier.height(48.dp))

            // 手机号输入：+86 前缀 + 竖线分隔，仍走 Rh 下沉输入样式。
            RhLoginField(
                value = uiState.phone,
                onValueChange = onPhoneChanged,
                placeholder = stringResource(Res.string.login_phone_label),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { smsCodeFocusRequester.requestFocus() }
                ),
                leadingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.login_phone_country_code),
                            color = colors.textSecondary,
                            style = RhTypography.body,
                        )
                        Spacer(Modifier.width(RhSpacing.sm))
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(colors.borderDefault)
                        )
                        Spacer(Modifier.width(RhSpacing.md))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(RhSpacing.lg))

            if (uiState.isSmsMode) {
                // === 短信验证码输入 + 内联获取验证码按钮 ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RhLoginField(
                        value = uiState.smsCode,
                        onValueChange = onSmsCodeChanged,
                        placeholder = stringResource(Res.string.login_sms_code_label),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onLoginClick()
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(smsCodeFocusRequester),
                    )

                    val sendEnabled = !uiState.isSendingCode && uiState.countdownSeconds == 0
                    TextButton(
                        onClick = onSendCodeClick,
                        enabled = sendEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.brandPrimary,
                            disabledContentColor = colors.textTertiary,
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Text(
                            text = when {
                                uiState.isSendingCode -> stringResource(Res.string.login_send_code_sending)
                                uiState.countdownSeconds > 0 -> stringResource(
                                    Res.string.login_send_code_countdown_format,
                                    uiState.countdownSeconds,
                                )
                                else -> stringResource(Res.string.login_send_code_action)
                            },
                            style = RhTypography.bodyStrong,
                        )
                    }
                }
            } else {
                // === 密码输入 ===
                RhLoginField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    placeholder = stringResource(Res.string.login_password_label),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLoginClick()
                        }
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(RhSpacing.xxxl))

            RhPrimaryButton(
                text = stringResource(Res.string.login_submit_button),
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                enabled = !uiState.isLoading,
                loading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(RhSpacing.lg))

            TextButton(
                onClick = onToggleMode,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.brandPrimary,
                    containerColor = Color.Transparent,
                ),
            ) {
                Text(
                    text = if (uiState.isSmsMode) {
                        stringResource(Res.string.login_toggle_password_mode)
                    } else {
                        stringResource(Res.string.login_toggle_sms_mode)
                    },
                    color = colors.textSecondary,
                    style = RhTypography.body,
                )
            }

            Spacer(Modifier.height(RhSpacing.huge))

            Text(
                text = stringResource(Res.string.login_agreement_notice),
                color = colors.textTertiary,
                style = RhTypography.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(RhSpacing.xxxl))
        }
    }
}

/**
 * 登录页专用的 Rh 下沉输入框。
 *
 * 与设计系统 `RhSearchBar` / `RhCredentialField` 保持一致，基于 [BasicTextField] 与
 * [OutlinedTextFieldDefaults.DecorationBox] 组装：下沉表面色承载输入区，聚焦时边框切换为激活色，
 * 光标使用主品牌色。手机号 +86 前缀等前缀内容通过 [leadingContent] 注入，密码遮罩通过
 * [visualTransformation] 控制。组件不持久化或记录任何输入内容。
 *
 * @param value 当前输入的原始字符串，空字符串表示尚未输入。
 * @param onValueChange 用户编辑时触发，调用方负责保存状态；组件不缓存敏感输入。
 * @param placeholder 占位文案，调用方负责本地化。
 * @param modifier 外层布局修饰符。
 * @param keyboardOptions 软键盘类型与动作配置。
 * @param keyboardActions 软键盘动作回调。
 * @param leadingContent 输入区前导内容，例如手机号国家码前缀；为 null 时不渲染前导区。
 * @param visualTransformation 文本视觉变换，密码输入传入 [PasswordVisualTransformation] 实现遮罩。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RhLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = RhTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceSunken,
        unfocusedContainerColor = colors.surfaceSunken,
        focusedBorderColor = colors.borderActive,
        unfocusedBorderColor = colors.borderDefault,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 56.dp),
        textStyle = RhTypography.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.brandPrimary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                placeholder = {
                    Text(text = placeholder, style = RhTypography.body, color = colors.textTertiary)
                },
                leadingIcon = leadingContent,
                colors = fieldColors,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = fieldColors,
                        shape = RoundedCornerShape(RhTheme.shapes.md),
                    )
                },
            )
        },
    )
}
