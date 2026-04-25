package com.runninghub.app.ui.feature.community.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.remote.model.MiniMaxAudioRequest
import com.runninghub.app.data.repository.AudioRepository
import com.runninghub.app.data.repository.AudioTaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioGenerationViewModel @Inject constructor(
    private val repository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioUiState(
        voiceOptions = listOf(
            VoiceOption("Wise_Woman", "智慧女性", "成熟、睿智的女性声线"),
            VoiceOption("Friendly_Person", "亲切友人", "温暖、自然的日常交谈"),
            VoiceOption("Inspirational_girl", "励志少女", "充满朝气、积极向上"),
            VoiceOption("Deep_Voice_Man", "深沉男声", "厚重、磁性的男性嗓音"),
            VoiceOption("Calm_Woman", "冷静女性", "平和、专业的叙述风格"),
            VoiceOption("Casual_Guy", "随性少年", "轻松、活泼的男声"),
            VoiceOption("Lively_Girl", "活泼少女", "灵动、俏皮的女孩声音"),
            VoiceOption("Patient_Man", "耐心男师", "稳重、亲和的教学风格"),
            VoiceOption("Young_Knight", "年轻骑士", "英气、坚定的青年男声"),
            VoiceOption("Determined_Man", "果敢男士", "强有力、富有决策感"),
            VoiceOption("Lovely_Girl", "萌系萝莉", "可爱、稚嫩的童声"),
            VoiceOption("Decent_Boy", "正气少年", "清亮、阳光的男孩子"),
            VoiceOption("Imposing_Manner", "威严长者", "庄重、大气的气场"),
            VoiceOption("Elegant_Man", "优雅绅士", "儒雅、得体的成年男声"),
            VoiceOption("Abbess", "严肃师太", "严厉、沉稳的年长女性"),
            VoiceOption("Sweet_Girl_2", "甜美少女", "甜而不腻、温柔可亲"),
            VoiceOption("Exuberant_Girl", "热情女孩", "高亢、充满能量的声音")
        )
    ))
    val uiState = _uiState.asStateFlow()

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun onVoiceChanged(voiceId: String) {
        _uiState.update { it.copy(selectedVoiceId = voiceId) }
    }

    fun onEmotionChanged(emotion: String?) {
        _uiState.update { it.copy(selectedEmotion = emotion) }
    }

    fun onSpeedChanged(speed: Float) {
        _uiState.update { it.copy(speed = speed) }
    }

    fun onVolumeChanged(volume: Float) {
        _uiState.update { it.copy(volume = volume) }
    }

    fun onPitchChanged(pitch: Int) {
        _uiState.update { it.copy(pitch = pitch) }
    }

    fun generateAudio() {
        if (_uiState.value.text.isBlank()) return

        viewModelScope.launch {
            val request = MiniMaxAudioRequest(
                text = _uiState.value.text,
                voiceId = _uiState.value.selectedVoiceId,
                speed = _uiState.value.speed,
                volume = _uiState.value.volume,
                pitch = _uiState.value.pitch,
                emotion = _uiState.value.selectedEmotion
            )

            repository.convertTextToAudio(request).collect { status ->
                _uiState.update { 
                    it.copy(
                        status = status,
                        isProcessing = status is AudioTaskStatus.Submitting || status is AudioTaskStatus.Running
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update { AudioUiState() }
    }
}

data class VoiceOption(
    val id: String,
    val name: String,
    val description: String
)

data class EmotionOption(
    val id: String?,
    val name: String
)

data class AudioUiState(
    val text: String = "",
    val selectedVoiceId: String = "Wise_Woman",
    val voiceOptions: List<VoiceOption> = emptyList(),
    val selectedEmotion: String? = null,
    val emotionOptions: List<EmotionOption> = listOf(
        EmotionOption(null, "默认 (无情绪)"),
        EmotionOption("happy", "开心 (Happy)"),
        EmotionOption("sad", "伤心 (Sad)"),
        EmotionOption("angry", "愤怒 (Angry)"),
        EmotionOption("fearful", "恐惧 (Fearful)"),
        EmotionOption("disgusted", "厌恶 (Disgusted)"),
        EmotionOption("surprised", "惊讶 (Surprised)"),
        EmotionOption("neutral", "平静 (Neutral)")
    ),
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Int = 0,
    val status: AudioTaskStatus? = null,
    val isProcessing: Boolean = false
)
