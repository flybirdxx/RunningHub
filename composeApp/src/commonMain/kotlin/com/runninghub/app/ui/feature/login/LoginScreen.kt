package com.runninghub.app.ui.feature.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
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
import runninghub.composeapp.generated.resources.login_send_code_action
import runninghub.composeapp.generated.resources.login_send_code_countdown_format
import runninghub.composeapp.generated.resources.login_send_code_sending
import runninghub.composeapp.generated.resources.login_sms_code_label
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
                onToken = screenModel::onSmsCaptchaVerified,
                onDismiss = screenModel::dismissSmsCaptcha,
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                countdownHasStarted = screenModel.uiState.value.countdownSeconds > 0,
            )
        }
    }
}

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
    countdownHasStarted: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val smsCodeFocusRequester = remember { FocusRequester() }

    // AC1: auto-focus code field after SMS sent successfully
    LaunchedEffect(uiState.countdownSeconds == 60) {
        smsCodeFocusRequester.requestFocus()
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                text = stringResource(Res.string.login_brand_initial),
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = stringResource(Res.string.login_brand_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(48.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.outline,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Phone number input with +86 prefix
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = onPhoneChanged,
                label = { Text(stringResource(Res.string.login_phone_label)) },
                leadingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.login_phone_country_code),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { smsCodeFocusRequester.requestFocus() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            if (uiState.isSmsMode) {
                // === SMS code input ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.smsCode,
                        onValueChange = onSmsCodeChanged,
                        label = { Text(stringResource(Res.string.login_sms_code_label)) },
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
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(smsCodeFocusRequester),
                    )

                    Spacer(Modifier.width(12.dp))

                    val sendEnabled = !uiState.isSendingCode && uiState.countdownSeconds == 0
                    Button(
                        onClick = onSendCodeClick,
                        enabled = sendEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.height(56.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                // === Password input ===
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    label = { Text(stringResource(Res.string.login_password_label)) },
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
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Login button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                AnimatedVisibility(
                    visible = uiState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp,
                    )
                }
                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(Res.string.login_submit_button),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (uiState.isSmsMode) {
                        stringResource(Res.string.login_toggle_password_mode)
                    } else {
                        stringResource(Res.string.login_toggle_sms_mode)
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(Res.string.login_agreement_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
