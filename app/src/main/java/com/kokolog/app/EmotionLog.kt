package com.kokolog.app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 感情ログ1件分のデータ構造
 */
@Entity(tableName = "emotion_logs")
data class EmotionLog(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val emotion: String,

    val category: String = "その他",

    val tags: String = "",

    val isIgnored: Boolean = false,

    val intensity: Int,

    val memo: String,

    val createdAt: Long
)
