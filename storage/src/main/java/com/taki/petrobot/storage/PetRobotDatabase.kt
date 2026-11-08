package com.taki.petrobot.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StructuredLogEntity::class],
    version = 2,
    exportSchema = false,
)
internal abstract class PetRobotDatabase : RoomDatabase() {
    abstract fun structuredLogDao(): StructuredLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE structured_logs ADD COLUMN timingRating TEXT",
                )
                db.execSQL(
                    "ALTER TABLE structured_logs ADD COLUMN timingOffsetMillis INTEGER",
                )
            }
        }
    }
}
