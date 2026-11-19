package com.kokolog.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 感情記録・感情変化・レポート閲覧権限を保持する Room DB。
 * DB version 2 -> 3 は既存テーブルに触れず、新しい権限テーブルのみ追加する。
 */
@Database(
    entities = [
        EmotionLog::class,
        EmotionChangeLog::class,
        ReportEntitlementEntity::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emotionLogDao(): EmotionLogDao
    abstract fun emotionChangeLogDao(): EmotionChangeLogDao
    abstract fun reportEntitlementDao(): ReportEntitlementDao

    companion object {
        /** version 2 からのデータ保持マイグレーション */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `report_entitlements` (
                        `reportMonth` TEXT NOT NULL,
                        `unlockMethod` TEXT NOT NULL,
                        `grantedAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `playProductId` TEXT,
                        `purchaseToken` TEXT,
                        PRIMARY KEY(`reportMonth`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kokolog_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    // 感情記録を消さないため、破壊的マイグレーションは禁止。
                    // 古い version 1 からの経路は別途、実スキーマ確認後に追加する。
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
