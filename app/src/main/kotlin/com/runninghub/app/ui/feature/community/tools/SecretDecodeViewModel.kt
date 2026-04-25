package com.runninghub.app.ui.feature.community.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.repository.DecodeResult
import com.runninghub.app.data.repository.SteganographyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecretDecodeUiState(
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val needsPassword: Boolean = false,
    val decodedData: ByteArray? = null,
    val decodedFilename: String? = null,
    val error: String? = null,
    val decodeSuccess: Boolean = false
)

@HiltViewModel
class SecretDecodeViewModel @Inject constructor(
    private val repository: SteganographyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecretDecodeUiState())
    val uiState: StateFlow<SecretDecodeUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri, error = null, decodeSuccess = false, needsPassword = false) }
        startDecode(uri, null)
    }

    fun onPasswordEntered(password: String) {
        val uri = uiState.value.imageUri ?: return
        startDecode(uri, password)
    }

    private fun startDecode(uri: Uri, password: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, needsPassword = false) }
            
            val result = repository.decodeImage(uri, password)
            
            _uiState.update { state ->
                when (result) {
                    is DecodeResult.Success -> state.copy(
                        isLoading = false,
                        decodedData = result.data,
                        decodedFilename = result.filename,
                        decodeSuccess = true
                    )
                    is DecodeResult.NeedPassword -> state.copy(
                        isLoading = false,
                        needsPassword = true
                    )
                    is DecodeResult.Error -> state.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    fun reset() {
        _uiState.update { SecretDecodeUiState() }
    }
}
