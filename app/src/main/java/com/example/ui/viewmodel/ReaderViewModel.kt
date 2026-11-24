package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkEntity
import com.example.data.model.BookFormat
import com.example.data.model.ParsedBook
import com.example.data.model.ReaderSettings
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val isLoading: Boolean = true,
    val book: BookEntity? = null,
    val parsedBook: ParsedBook? = null,
    val format: BookFormat = BookFormat.TXT,
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val totalPagesInChapter: Int = 1,
    val showToolbar: Boolean = false,
    val showTocDrawer: Boolean = false,
    val statusToast: String? = null,
    val errorMessage: String? = null
)

class ReaderViewModel(
    private val bookId: Long,
    initialChapter: Int?,
    initialPage: Int?,
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val settings: StateFlow<ReaderSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettings())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.observeBookmarksForBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBook(initialChapter, initialPage)
    }

    private fun loadBook(initialChapter: Int?, initialPage: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val entity = repository.getBookById(bookId)
            if (entity == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Book not found in library.") }
                return@launch
            }
            val fmt = runCatching { BookFormat.valueOf(entity.format) }.getOrDefault(BookFormat.TXT)
            repository.loadParsedBook(entity)
                .onSuccess { parsed ->
                    val maxChapter = (parsed.chapters.size - 1).coerceAtLeast(0)
                    val ch = (initialChapter ?: entity.currentChapterIndex).coerceIn(0, maxChapter)
                    val pg = (initialPage ?: entity.currentPageIndex).coerceAtLeast(0)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            book = entity,
                            parsedBook = parsed,
                            format = fmt,
                            currentChapterIndex = ch,
                            currentPageIndex = pg
                        )
                    }
                    persistProgress()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            book = entity,
                            format = fmt,
                            errorMessage = err.message ?: "Failed to open book."
                        )
                    }
                }
        }
    }

    fun updatePaginationCount(totalPagesInCurrentUnit: Int) {
        val safeTotal = totalPagesInCurrentUnit.coerceAtLeast(1)
        val curr = _uiState.value
        val clampedPage = curr.currentPageIndex.coerceIn(0, safeTotal - 1)
        if (curr.totalPagesInChapter != safeTotal || curr.currentPageIndex != clampedPage) {
            _uiState.update {
                it.copy(
                    totalPagesInChapter = safeTotal,
                    currentPageIndex = clampedPage
                )
            }
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val parsed = state.parsedBook ?: return
        if (state.format == BookFormat.PDF || state.format == BookFormat.CBZ) {
            val maxPage = when (state.format) {
                BookFormat.PDF -> (parsed.pdfPageCount - 1).coerceAtLeast(0)
                BookFormat.CBZ -> (parsed.cbzImageEntries.size - 1).coerceAtLeast(0)
                else -> 0
            }
            if (state.currentPageIndex < maxPage) {
                _uiState.update { it.copy(currentPageIndex = state.currentPageIndex + 1) }
                persistProgress()
            }
            return
        }

        if (state.currentPageIndex + 1 < state.totalPagesInChapter) {
            _uiState.update { it.copy(currentPageIndex = state.currentPageIndex + 1) }
            persistProgress()
        } else if (state.currentChapterIndex + 1 < parsed.chapters.size) {
            _uiState.update {
                it.copy(
                    currentChapterIndex = state.currentChapterIndex + 1,
                    currentPageIndex = 0
                )
            }
            persistProgress()
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.format == BookFormat.PDF || state.format == BookFormat.CBZ) {
            if (state.currentPageIndex > 0) {
                _uiState.update { it.copy(currentPageIndex = state.currentPageIndex - 1) }
                persistProgress()
            }
            return
        }

        if (state.currentPageIndex > 0) {
            _uiState.update { it.copy(currentPageIndex = state.currentPageIndex - 1) }
            persistProgress()
        } else if (state.currentChapterIndex > 0) {
            _uiState.update {
                it.copy(
                    currentChapterIndex = state.currentChapterIndex - 1,
                    currentPageIndex = 0
                )
            }
            persistProgress()
        }
    }

    fun nextChapter() {
        val state = _uiState.value
        val parsed = state.parsedBook ?: return
        if (state.format == BookFormat.PDF || state.format == BookFormat.CBZ) {
            val jump = (state.currentPageIndex + 10).coerceAtMost(
                when (state.format) {
                    BookFormat.PDF -> (parsed.pdfPageCount - 1).coerceAtLeast(0)
                    else -> (parsed.cbzImageEntries.size - 1).coerceAtLeast(0)
                }
            )
            _uiState.update { it.copy(currentPageIndex = jump) }
            persistProgress()
            return
        }
        if (state.currentChapterIndex + 1 < parsed.chapters.size) {
            _uiState.update {
                it.copy(currentChapterIndex = state.currentChapterIndex + 1, currentPageIndex = 0)
            }
            persistProgress()
        }
    }

    fun previousChapter() {
        val state = _uiState.value
        if (state.format == BookFormat.PDF || state.format == BookFormat.CBZ) {
            val jump = (state.currentPageIndex - 10).coerceAtLeast(0)
            _uiState.update { it.copy(currentPageIndex = jump) }
            persistProgress()
            return
        }
        if (state.currentChapterIndex > 0) {
            _uiState.update {
                it.copy(currentChapterIndex = state.currentChapterIndex - 1, currentPageIndex = 0)
            }
            persistProgress()
        }
    }

    fun jumpToChapter(chapterIndex: Int) {
        val parsed = _uiState.value.parsedBook ?: return
        val maxCh = (parsed.chapters.size - 1).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                currentChapterIndex = chapterIndex.coerceIn(0, maxCh),
                currentPageIndex = 0,
                showTocDrawer = false
            )
        }
        persistProgress()
    }

    fun jumpToPage(pageIndex: Int) {
        val state = _uiState.value
        val maxPage = when (state.format) {
            BookFormat.PDF -> ((state.parsedBook?.pdfPageCount ?: 1) - 1).coerceAtLeast(0)
            BookFormat.CBZ -> ((state.parsedBook?.cbzImageEntries?.size ?: 1) - 1).coerceAtLeast(0)
            else -> (state.totalPagesInChapter - 1).coerceAtLeast(0)
        }
        _uiState.update { it.copy(currentPageIndex = pageIndex.coerceIn(0, maxPage)) }
        persistProgress()
    }

    fun seekPercent(fraction: Float) {
        val state = _uiState.value
        val parsed = state.parsedBook ?: return
        val clamped = fraction.coerceIn(0f, 1f)
        if (state.format == BookFormat.PDF || state.format == BookFormat.CBZ) {
            val total = if (state.format == BookFormat.PDF) parsed.pdfPageCount else parsed.cbzImageEntries.size
            val target = ((total - 1).coerceAtLeast(0) * clamped).toInt()
            _uiState.update { it.copy(currentPageIndex = target) }
        } else if (parsed.chapters.isNotEmpty()) {
            val totalChapters = parsed.chapters.size
            if (totalChapters == 1) {
                val targetPage = ((state.totalPagesInChapter - 1).coerceAtLeast(0) * clamped).toInt()
                _uiState.update { it.copy(currentPageIndex = targetPage) }
            } else {
                val targetCh = ((totalChapters - 1) * clamped).toInt()
                _uiState.update { it.copy(currentChapterIndex = targetCh, currentPageIndex = 0) }
            }
        }
        persistProgress()
    }

    fun toggleToolbar() {
        _uiState.update { it.copy(showToolbar = !it.showToolbar, showTocDrawer = false) }
    }

    fun setToolbarVisible(visible: Boolean) {
        _uiState.update { it.copy(showToolbar = visible) }
    }

    fun toggleTocDrawer() {
        _uiState.update { it.copy(showTocDrawer = !it.showTocDrawer, showToolbar = false) }
    }

    fun closeOverlays() {
        _uiState.update { it.copy(showToolbar = false, showTocDrawer = false) }
    }

    fun adjustFontSize(deltaSp: Int) {
        viewModelScope.launch {
            repository.setFontSize(settings.value.fontSizeSp + deltaSp)
        }
    }

    fun toggleAlternativeLayout() {
        viewModelScope.launch {
            repository.setAlternativeLayout(!settings.value.alternativeLayout)
            _uiState.update { it.copy(showTocDrawer = false) }
        }
    }

    fun toggleBookmark(currentSnippet: String) {
        val state = _uiState.value
        val book = state.book ?: return
        val chapterTitle = when (state.format) {
            BookFormat.PDF, BookFormat.CBZ -> "Page ${state.currentPageIndex + 1}"
            else -> state.parsedBook?.chapters?.getOrNull(state.currentChapterIndex)?.title
                ?: "Chapter ${state.currentChapterIndex + 1}"
        }
        viewModelScope.launch {
            val added = repository.toggleBookmark(
                book = book,
                chapterIndex = state.currentChapterIndex,
                chapterTitle = chapterTitle,
                pageIndex = state.currentPageIndex,
                snippet = currentSnippet.ifBlank { chapterTitle }
            )
            _uiState.update {
                it.copy(statusToast = if (added) "Bookmark added" else "Bookmark removed")
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(statusToast = null) }
    }

    private fun persistProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        val parsed = state.parsedBook ?: return

        val (totalPages, pct) = when (state.format) {
            BookFormat.PDF -> {
                val total = parsed.pdfPageCount.coerceAtLeast(1)
                val p = (((state.currentPageIndex + 1).toFloat() / total) * 100f).toInt().coerceIn(1, 100)
                total to p
            }
            BookFormat.CBZ -> {
                val total = parsed.cbzImageEntries.size.coerceAtLeast(1)
                val p = (((state.currentPageIndex + 1).toFloat() / total) * 100f).toInt().coerceIn(1, 100)
                total to p
            }
            else -> {
                val totalCh = parsed.chapters.size.coerceAtLeast(1)
                val chFraction = state.currentChapterIndex.toFloat() / totalCh
                val inChFraction = (state.currentPageIndex + 1).toFloat() /
                    state.totalPagesInChapter.coerceAtLeast(1) / totalCh
                val p = ((chFraction + inChFraction) * 100f).toInt().coerceIn(1, 100)
                state.totalPagesInChapter to p
            }
        }

        viewModelScope.launch {
            repository.updateReadingProgress(
                bookId = book.id,
                chapterIndex = state.currentChapterIndex,
                pageIndex = state.currentPageIndex,
                totalPages = totalPages,
                progressPercent = pct
            )
        }
    }
}
