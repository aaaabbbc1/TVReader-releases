package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val author: String,
    val fileName: String,
    val filePath: String,
    val coverPath: String? = null,
    val format: String,
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val totalPagesEstimate: Int = 1,
    val progressPercent: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long = 0L,
    val fileSizeBytes: Long = 0L
)
