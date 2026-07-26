package com.yourname.simplenotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room entity for note categories / folders (synced to Drive; see SyncWorker.syncCategories). */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Int = 0xFF6650A4.toInt(),
    @ColumnInfo(name = "parent_id") val parentId: String? = null,
    val order: Int = 0,
    val updatedAt: Long = 0L
)
