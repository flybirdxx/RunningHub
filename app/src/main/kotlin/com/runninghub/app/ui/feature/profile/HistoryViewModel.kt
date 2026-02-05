package com.runninghub.app.ui.feature.profile

import androidx.lifecycle.ViewModel
import com.runninghub.app.data.local.TaskHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    val historyManager: TaskHistoryManager
) : ViewModel()
