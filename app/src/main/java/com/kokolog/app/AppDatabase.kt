package com.kokolog.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * アプリ全体で使うRoomデータベース
 *
 * entities に保存対象のデータクラスを指定する
 * 今回は EmotionLog だけ
 */
@Database(
    entities = [
        EmotionLog::class,
        EmotionChangeLog::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * EmotionLog用のDAOを取得する
     */
    abstract fun emotionLogDao(): EmotionLogDao
    abstract fun emotionChangeLogDao(): EmotionChangeLogDao
    companion object {

        /**
         * データベースのインスタンスを保持する
         *
         * @Volatile を付けることで、
         * 複数スレッドから見ても安全に扱いやすくする
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * データベースを取得する関数
         *
         * すでに作成済みならそれを返し、
         * まだ作成されていなければ新しく作る
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kokolog_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}