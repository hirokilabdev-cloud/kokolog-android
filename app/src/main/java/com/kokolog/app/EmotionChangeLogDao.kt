package com.kokolog.app

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * 感情変化ログ操作DAO
 */
@Dao
interface EmotionChangeLogDao {

    /**
     * 感情変化ログ保存
     */
    @Insert
    suspend fun insertChangeLog(changeLog: EmotionChangeLog)

    /**
     * 削除
     */
    @Delete
    suspend fun deleteChangeLog(changeLog: EmotionChangeLog)

    /**
     * 特定ココログに紐づく変化ログ取得
     */
    @Query("""
        SELECT * FROM emotion_change_logs
        WHERE originalLogId = :logId
        ORDER BY createdAt ASC
    """)
    suspend fun getChangeLogsByLogId(
        logId: Long
    ): List<EmotionChangeLog>

    /**
     * 感情変化ログ全件取得
     */
    @Query("""
    SELECT * FROM emotion_change_logs
    ORDER BY createdAt ASC
    """)
    suspend fun getAllChangeLogs():
            List<EmotionChangeLog>
}