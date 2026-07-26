package com.yourname.simplenotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, CategoryEntity::class, NoteSearchEntity::class, SearchHistoryEntity::class],
    version = 8,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteSearchDao(): NoteSearchDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        fun create(context: Context): NoteDatabase =
            Room.databaseBuilder(context, NoteDatabase::class.java, "notes.db")
                .addMigrations(MIGRATION_3_TO_4, MIGRATION_4_TO_5, MIGRATION_5_TO_6, MIGRATION_6_TO_7, MIGRATION_7_TO_8)
                // Fallback for any migration path not explicitly covered.
                // Drive is the source of truth, so destructive reset is acceptable.
                .fallbackToDestructiveMigration()
                .build()
    }
}
