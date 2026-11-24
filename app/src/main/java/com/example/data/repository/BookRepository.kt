package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.BookDao
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.NetworkSourceDao
import com.example.data.local.NetworkSourceEntity
import com.example.data.local.ReaderSettingsDataStore
import com.example.data.local.TvReaderDatabase
import com.example.data.model.AstigmaPreset
import com.example.data.model.BookFormat
import com.example.data.model.GutenbergBook
import com.example.data.model.ParsedBook
import com.example.data.model.PdfPageMode
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode
import com.example.data.model.RemoteFileItem
import com.example.data.model.SourceProtocol
import com.example.data.model.StandardEbookEntry
import com.example.data.parser.BookParser
import com.example.data.remote.NetworkFileClient
import com.example.data.remote.OnlineLibraryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookRepository(private val context: Context) {

    private val db = TvReaderDatabase.getInstance(context)
    private val bookDao: BookDao = db.bookDao()
    private val bookmarkDao: BookmarkDao = db.bookmarkDao()
    private val networkSourceDao: NetworkSourceDao = db.networkSourceDao()
    private val settingsStore = ReaderSettingsDataStore(context)
    private val onlineService = OnlineLibraryService()
    private val networkClient = NetworkFileClient()

    private val booksDir: File by lazy {
        File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
    }
    private val coversDir: File by lazy {
        File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
    }

    private val parsedCache = object : LinkedHashMap<Long, ParsedBook>(6, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ParsedBook>?): Boolean {
            return size > 4
        }
    }

    val allBooksFlow: Flow<List<BookEntity>> = bookDao.observeAllBooks()
    val lastOpenedBookFlow: Flow<BookEntity?> = bookDao.observeLastOpenedBook()
    val recentBooksFlow: Flow<List<BookEntity>> = bookDao.observeRecentBooks()
    val allBookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.observeAllBookmarks()
    val settingsFlow: Flow<ReaderSettings> = settingsStore.settingsFlow

    fun observeBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeBookmarksForBook(bookId)

    fun observeSources(protocol: SourceProtocol): Flow<List<NetworkSourceEntity>> =
        networkSourceDao.observeSourcesByProtocol(protocol.name)

    suspend fun getBookById(bookId: Long): BookEntity? = bookDao.getBookById(bookId)

    suspend fun importFromUri(uri: Uri): Result<BookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri)
            val displayName = resolveUriDisplayName(uri)
            val inferredFormat = BookFormat.fromFileName(displayName)
                ?: BookFormat.fromMimeType(mime)
                ?: error("Unsupported book format: $displayName")

            val safeName = displayName.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                .let {
                    if (BookFormat.fromFileName(it) != null) it
                    else "$it.${inferredFormat.extensions.first()}"
                }

            val destFile = uniqueBookFile(safeName)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 16 * 1024)
                }
            } ?: error("Cannot read selected file")

            importLocalFile(destFile, inferredFormat)
        }
    }

    suspend fun importLocalFile(file: File, forcedFormat: BookFormat? = null): BookEntity =
        withContext(Dispatchers.IO) {
            val existing = bookDao.getBookByPath(file.absolutePath)
            if (existing != null) {
                val updated = existing.copy(lastOpened = System.currentTimeMillis())
                bookDao.updateBook(updated)
                return@withContext updated
            }

            val format = forcedFormat
                ?: BookFormat.fromFileName(file.name)
                ?: BookFormat.TXT

            val parsed = runCatching {
                BookParser.parse(file, format, extractContent = false)
            }.getOrNull()

            val coverPath = parsed?.coverBytes?.let { bytes ->
                runCatching {
                    val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}_${file.nameWithoutExtension.take(16)}.png")
                    coverFile.writeBytes(bytes)
                    coverFile.absolutePath
                }.getOrNull()
            }

            val estimatedPages = when (format) {
                BookFormat.PDF -> (parsed?.pdfPageCount ?: 1).coerceAtLeast(1)
                BookFormat.CBZ -> (parsed?.cbzImageEntries?.size ?: 1).coerceAtLeast(1)
                else -> 1
            }

            val entity = BookEntity(
                title = parsed?.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension,
                author = parsed?.author?.takeIf { it.isNotBlank() } ?: "Unknown Author",
                fileName = file.name,
                filePath = file.absolutePath,
                coverPath = coverPath,
                format = format.name,
                totalPagesEstimate = estimatedPages,
                dateAdded = System.currentTimeMillis(),
                lastOpened = System.currentTimeMillis(),
                fileSizeBytes = file.length()
            )
            val id = bookDao.insertBook(entity)
            entity.copy(id = id)
        }

    suspend fun loadParsedBook(book: BookEntity): Result<ParsedBook> = withContext(Dispatchers.IO) {
        parsedCache[book.id]?.let { return@withContext Result.success(it) }
        runCatching {
            val file = File(book.filePath)
            if (!file.exists()) error("Book file missing on device")
            val format = runCatching { BookFormat.valueOf(book.format) }.getOrDefault(BookFormat.TXT)
            val parsed = BookParser.parse(file, format, extractContent = true)
            synchronized(parsedCache) {
                parsedCache[book.id] = parsed
            }
            parsed
        }
    }

    suspend fun updateReadingProgress(
        bookId: Long,
        chapterIndex: Int,
        pageIndex: Int,
        totalPages: Int,
        progressPercent: Int
    ) {
        bookDao.updateProgress(
            bookId = bookId,
            chapterIndex = chapterIndex,
            pageIndex = pageIndex,
            totalPages = totalPages.coerceAtLeast(1),
            progressPercent = progressPercent.coerceIn(0, 100),
            lastOpened = System.currentTimeMillis()
        )
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        synchronized(parsedCache) { parsedCache.remove(book.id) }
        bookmarkDao.deleteBookmarksForBook(book.id)
        bookDao.deleteBookById(book.id)
        runCatching {
            val f = File(book.filePath)
            if (f.absolutePath.startsWith(booksDir.absolutePath)) f.delete()
            book.coverPath?.let { File(it).delete() }
        }
        Unit
    }

    suspend fun toggleBookmark(
        book: BookEntity,
        chapterIndex: Int,
        chapterTitle: String,
        pageIndex: Int,
        snippet: String
    ): Boolean {
        val existing = bookmarkDao.findBookmark(book.id, chapterIndex, pageIndex)
        return if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    bookId = book.id,
                    bookTitle = book.title,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapterTitle,
                    pageIndex = pageIndex,
                    snippet = snippet.take(180)
                )
            )
            true
        }
    }

    suspend fun deleteBookmark(bookmarkId: Long) = bookmarkDao.deleteBookmarkById(bookmarkId)

    suspend fun searchLocalContent(query: String): List<Pair<BookEntity, String>> =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            if (q.isBlank()) return@withContext emptyList()
            val snapshot = mutableListOf<Pair<BookEntity, String>>()
            val books = db.openHelper.readableDatabase.let { emptyList<BookEntity>() }
            // Use DAO or passed list; see SearchViewModel for reactive filtering + chapter match
            snapshot
        }

    // Online libraries & URL download
    suspend fun searchGutenberg(query: String): Result<List<GutenbergBook>> =
        onlineService.searchGutenberg(query)

    suspend fun fetchStandardEbooks(query: String): Result<List<StandardEbookEntry>> =
        onlineService.fetchStandardEbooks(query)

    suspend fun downloadAndImportUrl(
        url: String,
        suggestedName: String,
        forcedExtension: String? = null,
        onProgress: (Int) -> Unit = {}
    ): Result<BookEntity> {
        val dlResult = onlineService.downloadToFile(
            url = url,
            destDir = booksDir,
            suggestedName = suggestedName,
            forcedExtension = forcedExtension,
            onProgress = onProgress
        )
        return dlResult.mapCatching { file ->
            importLocalFile(file)
        }
    }

    // Network Sources (WebDAV / FTP)
    suspend fun saveNetworkSource(source: NetworkSourceEntity): Long =
        networkSourceDao.insertSource(source)

    suspend fun deleteNetworkSource(id: Long) =
        networkSourceDao.deleteSourceById(id)

    suspend fun listRemoteDirectory(
        source: NetworkSourceEntity,
        path: String
    ): Result<List<RemoteFileItem>> = networkClient.listDirectory(source, path)

    suspend fun downloadRemoteAndImport(
        source: NetworkSourceEntity,
        item: RemoteFileItem,
        onProgress: (Int) -> Unit = {}
    ): Result<BookEntity> {
        return networkClient.downloadFile(source, item, booksDir, onProgress).mapCatching { file ->
            importLocalFile(file, item.format)
        }
    }

    fun listAppStoredFiles(): List<File> {
        return booksDir.listFiles()?.filter { it.isFile && BookFormat.fromFileName(it.name) != null }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    // Settings mutators
    suspend fun setTheme(theme: ReaderTheme) = settingsStore.setTheme(theme)
    suspend fun setAstigmaPreset(preset: AstigmaPreset) = settingsStore.setAstigmaPreset(preset)
    suspend fun setFontSize(sizeSp: Int) = settingsStore.setFontSize(sizeSp)
    suspend fun setFontFamily(family: ReaderFontFamily) = settingsStore.setFontFamily(family)
    suspend fun setLineHeight(multiplier: Float) = settingsStore.setLineHeight(multiplier)
    suspend fun setReadingMode(mode: ReadingMode) = settingsStore.setReadingMode(mode)
    suspend fun setAlternativeLayout(enabled: Boolean) = settingsStore.setAlternativeLayout(enabled)
    suspend fun setFocusMode(enabled: Boolean) = settingsStore.setFocusMode(enabled)
    suspend fun setPdfPageMode(mode: PdfPageMode) = settingsStore.setPdfPageMode(mode)
    suspend fun setBrightness(percent: Int) = settingsStore.setBrightness(percent)
    suspend fun setKeepScreenOn(enabled: Boolean) = settingsStore.setKeepScreenOn(enabled)

    private fun resolveUriDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
            ?: "book_${System.currentTimeMillis()}"
    }

    private fun uniqueBookFile(fileName: String): File {
        var candidate = File(booksDir, fileName)
        if (!candidate.exists()) return candidate
        val stem = candidate.nameWithoutExtension
        val ext = candidate.extension
        var counter = 1
        while (candidate.exists()) {
            val next = if (ext.isNotEmpty()) "${stem}_$counter.$ext" else "${stem}_$counter"
            candidate = File(booksDir, next)
            counter++
        }
        return candidate
    }
}
