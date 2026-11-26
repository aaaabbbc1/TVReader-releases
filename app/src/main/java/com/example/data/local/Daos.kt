package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastOpened DESC, dateAdded DESC")
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastOpened > 0 ORDER BY lastOpened DESC LIMIT 1")
    fun observeLastOpenedBook(): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE lastOpened > 0 ORDER BY lastOpened DESC LIMIT 10")
    fun observeRecentBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath LIMIT 1")
    suspend fun getBookByPath(filePath: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query(
        """
        UPDATE books
        SET currentChapterIndex = :chapterIndex,
            currentPageIndex = :pageIndex,
            scrollBlockIndex = :scrollBlockIndex,
            totalPagesEstimate = :totalPages,
            progressPercent = :progressPercent,
            lastOpened = :lastOpened
        WHERE id = :bookId
        """
    )
    suspend fun updateProgress(
        bookId: Long,
        chapterIndex: Int,
        pageIndex: Int,
        scrollBlockIndex: Int,
        totalPages: Int,
        progressPercent: Int,
        lastOpened: Long
    )

    @Query("UPDATE books SET lastOpened = :lastOpened WHERE id = :bookId")
    suspend fun touchLastOpened(bookId: Long, lastOpened: Long)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query(
        """
        SELECT * FROM bookmarks
        WHERE bookId = :bookId AND chapterIndex = :chapterIndex AND pageIndex = :pageIndex
        LIMIT 1
        """
    )
    suspend fun findBookmark(bookId: Long, chapterIndex: Int, pageIndex: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: Long)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksForBook(bookId: Long)
}

@Dao
interface NetworkSourceDao {
    @Query("SELECT * FROM network_sources WHERE protocol = :protocol ORDER BY createdAt DESC")
    fun observeSourcesByProtocol(protocol: String): Flow<List<NetworkSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: NetworkSourceEntity): Long

    @Query("DELETE FROM network_sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)
}
