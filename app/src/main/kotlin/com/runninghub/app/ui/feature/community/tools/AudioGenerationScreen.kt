package com.runninghub.app.ui.feature.community.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.SettingsInputComponent
import com.runninghub.app.data.repository.AudioTaskStatus
import com.runninghub.app.ui.theme.RunningHubTeal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioGenerationScreen(
    viewModel: AudioGenerationViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音频生成", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Card
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(RunningHubTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Audiotrack, null, tint = RunningHubTeal)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("MiniMax Speech 2.8 HD", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("标准模型 API 接入", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Text Input
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::onTextChanged,
                label = { Text("输入要转换的文本") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                placeholder = { Text("例如：你好，今天天气真不错！") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RunningHubTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = RunningHubTeal,
                    cursorColor = RunningHubTeal,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(24.dp))

            // Voice Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择音色", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = RunningHubTeal.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "17 种高保真音色",
                        color = RunningHubTeal,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // Voice ID Selection Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.voiceOptions.forEach { option ->
                    val isSelected = uiState.selectedVoiceId == option.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onVoiceChanged(option.id) },
                        label = {
                            Text(option.name)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E1E1E),
                            labelColor = Color.Gray,
                            selectedContainerColor = RunningHubTeal.copy(alpha = 0.2f),
                            selectedLabelColor = RunningHubTeal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = RunningHubTeal
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            
            // Voice Description
            uiState.voiceOptions.find { it.id == uiState.selectedVoiceId }?.let { option ->
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = RunningHubTeal.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${option.name}：${option.description}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Emotion Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("情感倾向 (Emotion)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Mood, null, tint = RunningHubTeal.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
            
            Spacer(Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.emotionOptions.forEach { option ->
                    val isSelected = uiState.selectedEmotion == option.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onEmotionChanged(option.id) },
                        label = {
                            Text(option.name)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E1E1E),
                            labelColor = Color.Gray,
                            selectedContainerColor = RunningHubTeal.copy(alpha = 0.2f),
                            selectedLabelColor = RunningHubTeal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = RunningHubTeal
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Advanced Parameters Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("高级参数设置", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.SettingsInputComponent, null, tint = RunningHubTeal.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
            
            Spacer(Modifier.height(16.dp))

            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Speed Slider
                    ParameterSlider(
                        label = "语速 (Speed)",
                        value = uiState.speed,
                        range = 0.5f..2.0f,
                        onValueChange = viewModel::onSpeedChanged,
                        valueDisplay = String.format("%.1fx", uiState.speed)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Volume Slider
                    ParameterSlider(
                        label = "音量 (Volume)",
                        value = uiState.volume,
                        range = 0.1f..10.0f,
                        onValueChange = viewModel::onVolumeChanged,
                        valueDisplay = String.format("%.1f", uiState.volume)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Pitch Slider
                    ParameterSlider(
                        label = "音调 (Pitch)",
                        value = uiState.pitch.toFloat(),
                        range = -12f..12f,
                        onValueChange = { viewModel.onPitchChanged(it.toInt()) },
                        valueDisplay = "${if (uiState.pitch > 0) "+" else ""}${uiState.pitch}"
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Status Display
            uiState.status?.let { status ->
                StatusCard(status)
                Spacer(Modifier.height(24.dp))
            }

            // Generate Button
            Button(
                onClick = viewModel::generateAudio,
                enabled = uiState.text.isNotBlank() && !uiState.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RunningHubTeal,
                    contentColor = Color.Black
                )
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("开始生成音频", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusCard(status: AudioTaskStatus) {
    val containerColor = when(status) {
        is AudioTaskStatus.Error -> Color(0xFF3E2723)
        is AudioTaskStatus.Success -> Color(0xFF1B5E20)
        else -> Color(0xFF1E1E1E)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val text = when(status) {
                is AudioTaskStatus.Submitting -> "正在提交请求..."
                is AudioTaskStatus.Running -> "任务正在运行，ID: ${status.taskId}"
                is AudioTaskStatus.Success -> "生成成功！"
                is AudioTaskStatus.Error -> "出错了: ${status.message}"
            }
            Text(text, color = Color.White, fontSize = 14.sp)
            
            if (status is AudioTaskStatus.Success) {
                Spacer(Modifier.height(8.dp))
                Text(status.url, color = RunningHubTeal, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Text(valueDisplay, color = RunningHubTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = RunningHubTeal,
                activeTrackColor = RunningHubTeal,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
