package com.runninghub.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class HistoryTask(
    val taskId: Long,
    val appName: String,
    val resultUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class TaskHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("task_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTask(task: HistoryTask) {
        val history = getHistory().toMutableList()
        // Avoid duplicates
        if (history.none { it.taskId == task.taskId }) {
            history.add(0, task)
            prefs.edit().putString("history_list", gson.toJson(history)).apply()
        }
    }

    fun getHistory(): List<HistoryTask> {
        val json = prefs.getString("history_list", null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryTask>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
