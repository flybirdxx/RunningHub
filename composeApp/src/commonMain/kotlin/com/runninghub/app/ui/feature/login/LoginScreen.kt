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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.navigation.MainVoyagerScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class LoginVoyagerScreen : Screen, KoinComponent {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { LoginScreenModel(get()) }
        val navigator = LocalNavigator.currentOrThrow
        val uiState by screenModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.loginSuccess) {
            if (uiState.loginSuccess) {
                navigator.replaceAll(MainVoyagerScreen())
            }
        }

        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                screenModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { _ ->
            LoginContent(
                uiState = uiState,
                onPhoneChanged = screenModel::onPhoneChanged,
                onPasswordChanged = screenModel::onPasswordChanged,
                onLoginClick = screenModel::login,
                onSkipClick = { navigator.replaceAll(MainVoyagerScreen()) },
            )
        }
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onPhoneChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B0F1A),
            Color(0xFF141929),
            Color(0xFF1A1F35),
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
                text = "R",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6C5CE7),
            )

            Text(
                text = "RunningHUB",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(48.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFCBD5E1),
                focusedBorderColor = Color(0xFF6C5CE7),
                unfocusedBorderColor = Color(0xFF2A3050),
                cursorColor = Color(0xFF6C5CE7),
                focusedLabelColor = Color(0xFF6C5CE7),
                unfocusedLabelColor = Color(0xFF64748B),
                focusedLeadingIconColor = Color(0xFF6C5CE7),
                unfocusedLeadingIconColor = Color(0xFF64748B),
                focusedContainerColor = Color(0xFF1E2438),
                unfocusedContainerColor = Color(0xFF1E2438),
            )

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = onPhoneChanged,
                label = { Text("手机号") },
                leadingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "+86",
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(Color(0xFF2A3050))
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                label = { Text("密码") },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            tint = Color(0xFF64748B),
                        )
                    }
                },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C5CE7),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF6C5CE7).copy(alpha = 0.5f),
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
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = "立即登录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onSkipClick) {
                Text(
                    text = "暂不登录，直接浏览",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "登录即表示您同意《用户协议》和《隐私政策》",
                color = Color(0xFF475569),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
