package com.parhar.noor.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NoorDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE user_preferences ADD COLUMN cannot_offer INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_preferences_new (
                    user_uid TEXT NOT NULL PRIMARY KEY,
                    primary_task_ids TEXT NOT NULL DEFAULT '',
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO user_preferences_new (user_uid, primary_task_ids, updated_at)
                SELECT user_uid, primary_task_ids, updated_at FROM user_preferences
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE user_preferences")
            db.execSQL("ALTER TABLE user_preferences_new RENAME TO user_preferences")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE categories ADD COLUMN position INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS task_definitions_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    category TEXT NOT NULL,
                    name TEXT NOT NULL,
                    points INTEGER NOT NULL DEFAULT 0,
                    position INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO task_definitions_new (id, category, name, points, position, updated_at, sync_status)
                SELECT id, category, name, points, sort_order, updated_at, sync_status FROM task_definitions
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE task_definitions")
            db.execSQL("ALTER TABLE task_definitions_new RENAME TO task_definitions")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_definitions_category ON task_definitions(category)",
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE categories ADD COLUMN description TEXT NOT NULL DEFAULT ''",
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE task_definitions ADD COLUMN emoji TEXT NOT NULL DEFAULT ''",
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN avatar_text TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN avatar_bg TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN avatar_border TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN avatar_style TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE users ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE users ADD COLUMN privacy_tasks_today TEXT NOT NULL DEFAULT 'private'",
            )
            db.execSQL(
                "ALTER TABLE users ADD COLUMN privacy_tasks_history TEXT NOT NULL DEFAULT 'private'",
            )
        }
    }
}
