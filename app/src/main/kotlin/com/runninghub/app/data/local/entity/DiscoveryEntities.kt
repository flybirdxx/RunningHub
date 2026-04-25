package com.runninghub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val tag: String?,
    val order: Int = 0 // For sorting
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val tagIds: List<String>
)

@Entity(tableName = "discovery_apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val authorAvatar: String?,
    val imageUrl: String,
    val likes: Int,
    val stars: Int,
    val useCount: String,
    val views: String,
    val categoryName: String, // To filter list by category
    val timestamp: Long = System.currentTimeMillis() // To handle expiration
)

@Entity(tableName = "app_details")
data class AppDetailEntity(
    @PrimaryKey val id: String,
    val jsonContent: String,
    val timestamp: Long = System.currentTimeMillis()
)

class DiscoveryConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
