package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkEntity
import com.example.data.model.AstigmaPreset
import com.example.data.model.PdfPageMode
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: BookRepository) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettings())

    fun setTheme(theme: ReaderTheme) = viewModelScope.launch { repository.setTheme(theme) }
    fun setAstigmaPreset(preset: AstigmaPreset) = viewModelScope.launch { repository.setAstigmaPreset(preset) }
    fun setFontSize(sizeSp: Int) = viewModelScope.launch { repository.setFontSize(sizeSp) }
    fun setFontFamily(family: ReaderFontFamily) = viewModelScope.launch { repository.setFontFamily(family) }
    fun setLineHeight(multiplier: Float) = viewModelScope.launch { repository.setLineHeight(multiplier) }
    fun setReadingMode(mode: ReadingMode) = viewModelScope.launch { repository.setReadingMode(mode) }
    fun setAlternativeLayout(enabled: Boolean) = viewModelScope.launch { repository.setAlternativeLayout(enabled) }
    fun setFocusMode(enabled: Boolean) = viewModelScope.launch { repository.setFocusMode(enabled) }
    fun setPdfPageMode(mode: PdfPageMode) = viewModelScope.launch { repository.setPdfPageMode(mode) }
    fun setBrightness(percent: Int) = viewModelScope.launch { repository.setBrightness(percent) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenOn(enabled) }
}

class BookmarksViewModel(private val repository: BookRepository) : ViewModel() {
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteBookmark(id: Long) = viewModelScope.launch {
        repository.deleteBookmark(id)
    }
}

data class LocalSearchResult(
    val book: BookEntity,
    val chapterIndex: Int,
    val chapterTitle: String,
    val matchSnippet: String
)

class SearchViewModel(private val repository: BookRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<LocalSearchResult>>(emptyList())
    val results: StateFlow<List<LocalSearchResult>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun updateQuery(q: String) {
        _query.value = q
    }

    fun executeSearch() {
        val q = _query.value.trim()
        if (q.isEmpty()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val allBooks = repository.allBooksFlow.first()
            val matches = mutableListOf<LocalSearchResult>()
            val lowerQ = q.lowercase()
            for (book in allBooks) {
                if (book.title.lowercase().contains(lowerQ) || book.author.lowercase().contains(lowerQ)) {
                    matches.add(
                        LocalSearchResult(
                            book = book,
                            chapterIndex = book.currentChapterIndex,
                            chapterTitle = "Title / Author match",
                            matchSnippet = "${book.title} — ${book.author}"
                        )
                    )
                }
                val parsed = repository.loadParsedBook(book).getOrNull()
                if (parsed != null) {
                    for (ch in parsed.chapters) {
                        val idx = ch.rawText.lowercase().indexOf(lowerQ)
                        if (idx >= 0) {
                            val start = (idx - 40).coerceAtLeast(0)
                            val end = (idx + 110).coerceAtMost(ch.rawText.length)
                            val snippet = ch.rawText.substring(start, end).replace('\n', ' ').trim()
                            matches.add(
                                LocalSearchResult(
                                    book = book,
                                    chapterIndex = ch.index,
                                    chapterTitle = ch.title,
                                    matchSnippet = "...$snippet..."
                                )
                            )
                            if (matches.size >= 40) break
                        }
                    }
                }
                if (matches.size >= 40) break
            }
            _results.value = matches
            _isSearching.value = false
        }
    }
}

class AppViewModelFactory(
    private val repository: BookRepository,
    private val readerBookId: Long = 0L,
    private val initialChapter: Int? = null,
    private val initialPage: Int? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(repository) as T
            modelClass.isAssignableFrom(SourcesViewModel::class.java) -> SourcesViewModel(repository) as T
            modelClass.isAssignableFrom(ReaderViewModel::class.java) ->
                ReaderViewModel(readerBookId, initialChapter, initialPage, repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
            modelClass.isAssignableFrom(BookmarksViewModel::class.java) -> BookmarksViewModel(repository) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
