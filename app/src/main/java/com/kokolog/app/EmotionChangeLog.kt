package com.kokolog.app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 感情変化ログ1件分のデータ構造
 *
 * 例：
 * かなしい → 少し落ち着いた
 *
 * originalLogId : 元になったココログのID
 * emotion       : 変化後の感情
 * intensity     : 変化後のこころの大きさ
 * memo          : 変化に対するメモ
 * createdAt     : 変化を記録した日時
 */
@Entity(tableName = "emotion_change_logs")
data class EmotionChangeLog(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val originalLogId: Long,

    val emotion: String,

    val intensity: Int,

    val memo: String,

    val createdAt: Long
)
