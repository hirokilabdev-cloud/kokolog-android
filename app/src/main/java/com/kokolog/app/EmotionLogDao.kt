package com.kokolog.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

/**
 * 感情ログ用のDAO
 *
 * DAOは「データベースを操作するための窓口」
 * insertや取得処理などを書く場所
 */
@Dao
interface EmotionLogDao {

    /**
     * 感情ログを1件保存する
     */
    @Insert
    suspend fun insert(log: EmotionLog)

    /**
     * 感情ログを全件取得する
     *
     * createdAt DESC にすることで、
     * 新しい記録から順番に取得する
     */
    @Query("SELECT * FROM emotion_logs ORDER BY createdAt DESC")
    suspend fun getAllLogs(): List<EmotionLog>

    /**
     * 感情ログを削除する
     */
    @Delete
    suspend fun delete(log: EmotionLog)

    /**
     * 気にしない更新
     */
    @Query("UPDATE emotion_logs SET isIgnored = :isIgnored WHERE id = :logId")
    suspend fun updateIgnored(
        logId: Long,
        isIgnored: Boolean
    )

    /**
     * 編集して更新
     */
    @Update
    suspend fun updateLog(log: EmotionLog)
}