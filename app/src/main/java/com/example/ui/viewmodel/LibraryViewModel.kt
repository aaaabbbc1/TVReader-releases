package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookEntity
import com.example.data.model.LibrarySort
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val lastOpenedBook: BookEntity? = null,
    val recentBooks: List<BookEntity> = emptyList(),
    val sort: LibrarySort = LibrarySort.TITLE,
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class LibraryViewModel(private val repository: BookRepository) : ViewModel() {

    private val _sort = MutableStateFlow(LibrarySort.TITLE)
    private val _searchQuery = MutableStateFlow("")
    private val _isImporting = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allBooksFlow,
        repository.lastOpenedBookFlow,
        repository.recentBooksFlow,
        _sort,
        _searchQuery
    ) { allBooks, lastOpened, recent, sort, query ->
        val filtered = if (query.isBlank()) {
            allBooks
        } else {
            val q = query.trim().lowercase()
            allBooks.filter {
                it.title.lowercase().contains(q) ||
                    it.author.lowercase().contains(q) ||
                    it.fileName.lowercase().contains(q)
            }
        }
        val sorted = when (sort) {
            LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.AUTHOR -> filtered.sortedBy { it.author.lowercase() }
            LibrarySort.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
            LibrarySort.LAST_OPENED -> filtered.sortedByDescending { it.lastOpened }
        }
        LibraryUiState(
            books = sorted,
            lastOpenedBook = lastOpened,
            recentBooks = recent,
            sort = sort,
            searchQuery = query,
            isImporting = _isImporting.value,
            statusMessage = _statusMessage.value,
            errorMessage = _errorMessage.value
        )
    }.combine(_isImporting) { state, importing ->
        state.copy(isImporting = importing)
    }.combine(_statusMessage) { state, status ->
        state.copy(statusMessage = status)
    }.combine(_errorMessage) { state, error ->
        state.copy(errorMessage = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun setSort(sort: LibrarySort) {
        _sort.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun importBookFromUri(uri: Uri, onImported: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            repository.importFromUri(uri)
                .onSuccess { book ->
                    _statusMessage.value = "Imported \"${book.title}\""
                    onImported(book.id)
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Failed to import file"
                }
            _isImporting.value = false
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            _statusMessage.value = "Removed \"${book.title}\""
        }
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }
}
