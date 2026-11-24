package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NetworkSourceEntity
import com.example.data.model.GutenbergBook
import com.example.data.model.RemoteFileItem
import com.example.data.model.SourceProtocol
import com.example.data.model.StandardEbookEntry
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class SourceTab(val label: String) {
    LOCAL("Local"),
    WEBDAV("WebDAV"),
    FTP("FTP"),
    GUTENBERG("Gutenberg"),
    STANDARD_EBOOKS("Standard Ebooks")
}

data class SourcesUiState(
    val activeTab: SourceTab = SourceTab.LOCAL,
    val localFiles: List<File> = emptyList(),
    val gutenbergQuery: String = "",
    val gutenbergResults: List<GutenbergBook> = emptyList(),
    val selectedGutenberg: GutenbergBook? = null,
    val isLoadingGutenberg: Boolean = false,
    val standardQuery: String = "",
    val standardResults: List<StandardEbookEntry> = emptyList(),
    val selectedStandard: StandardEbookEntry? = null,
    val isLoadingStandard: Boolean = false,
    val activeNetworkSource: NetworkSourceEntity? = null,
    val remotePath: String = "/",
    val remoteItems: List<RemoteFileItem> = emptyList(),
    val isLoadingRemote: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadLabel: String = "",
    val errorMessage: String? = null
)

class SourcesViewModel(private val repository: BookRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    val webDavSources: StateFlow<List<NetworkSourceEntity>> =
        repository.observeSources(SourceProtocol.WEBDAV)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ftpSources: StateFlow<List<NetworkSourceEntity>> =
        repository.observeSources(SourceProtocol.FTP)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshLocalFiles()
    }

    fun selectTab(tab: SourceTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                errorMessage = null,
                activeNetworkSource = null,
                remoteItems = emptyList()
            )
        }
        when (tab) {
            SourceTab.LOCAL -> refreshLocalFiles()
            SourceTab.GUTENBERG -> if (_uiState.value.gutenbergResults.isEmpty()) searchGutenberg("")
            SourceTab.STANDARD_EBOOKS -> if (_uiState.value.standardResults.isEmpty()) searchStandardEbooks("")
            else -> Unit
        }
    }

    fun refreshLocalFiles() {
        _uiState.update { it.copy(localFiles = repository.listAppStoredFiles()) }
    }

    fun importLocalFile(file: File, onOpened: (Long) -> Unit) {
        viewModelScope.launch {
            val book = repository.importLocalFile(file)
            onOpened(book.id)
        }
    }

    fun updateGutenbergQuery(query: String) {
        _uiState.update { it.copy(gutenbergQuery = query) }
    }

    fun selectGutenbergBook(book: GutenbergBook) {
        _uiState.update { it.copy(selectedGutenberg = book) }
    }

    fun searchGutenberg(query: String = _uiState.value.gutenbergQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGutenberg = true, errorMessage = null) }
            repository.searchGutenberg(query)
                .onSuccess { books ->
                    _uiState.update {
                        it.copy(
                            isLoadingGutenberg = false,
                            gutenbergResults = books,
                            selectedGutenberg = books.firstOrNull()
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingGutenberg = false,
                            errorMessage = err.message ?: "Unable to reach Project Gutenberg"
                        )
                    }
                }
        }
    }

    fun downloadGutenbergBook(
        book: GutenbergBook,
        url: String,
        extension: String,
        onBookReady: (Long) -> Unit
    ) {
        downloadFromUrl(
            url = url,
            suggestedName = book.title.take(40),
            forcedExtension = extension,
            onBookReady = onBookReady
        )
    }

    fun updateStandardQuery(query: String) {
        _uiState.update { it.copy(standardQuery = query) }
    }

    fun selectStandardBook(entry: StandardEbookEntry) {
        _uiState.update { it.copy(selectedStandard = entry) }
    }

    fun searchStandardEbooks(query: String = _uiState.value.standardQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStandard = true, errorMessage = null) }
            repository.fetchStandardEbooks(query)
                .onSuccess { entries ->
                    _uiState.update {
                        it.copy(
                            isLoadingStandard = false,
                            standardResults = entries,
                            selectedStandard = entries.firstOrNull()
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingStandard = false,
                            errorMessage = err.message ?: "Unable to load Standard Ebooks catalog"
                        )
                    }
                }
        }
    }

    fun downloadFromUrl(
        url: String,
        suggestedName: String = "book",
        forcedExtension: String? = null,
        onBookReady: (Long) -> Unit
    ) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _uiState.update { it.copy(errorMessage = "Enter a valid http:// or https:// URL") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = 0,
                    downloadLabel = suggestedName,
                    errorMessage = null
                )
            }
            repository.downloadAndImportUrl(
                url = cleanUrl,
                suggestedName = suggestedName,
                forcedExtension = forcedExtension,
                onProgress = { pct -> _uiState.update { it.copy(downloadProgress = pct) } }
            ).onSuccess { book ->
                _uiState.update { it.copy(isDownloading = false, downloadProgress = 100) }
                refreshLocalFiles()
                onBookReady(book.id)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = err.message ?: "Download failed"
                    )
                }
            }
        }
    }

    fun addNetworkSource(
        name: String,
        protocol: SourceProtocol,
        host: String,
        port: Int,
        username: String,
        password: String,
        basePath: String
    ) {
        viewModelScope.launch {
            repository.saveNetworkSource(
                NetworkSourceEntity(
                    name = name.ifBlank { host },
                    protocol = protocol.name,
                    host = host.trim(),
                    port = port,
                    username = username.trim(),
                    password = password,
                    basePath = basePath.ifBlank { "/" }
                )
            )
        }
    }

    fun deleteNetworkSource(id: Long) {
        viewModelScope.launch {
            repository.deleteNetworkSource(id)
            if (_uiState.value.activeNetworkSource?.id == id) {
                _uiState.update { it.copy(activeNetworkSource = null, remoteItems = emptyList()) }
            }
        }
    }

    fun openNetworkSource(source: NetworkSourceEntity, path: String = source.basePath) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeNetworkSource = source,
                    remotePath = path.ifBlank { "/" },
                    isLoadingRemote = true,
                    errorMessage = null
                )
            }
            repository.listRemoteDirectory(source, path)
                .onSuccess { items ->
                    _uiState.update { it.copy(isLoadingRemote = false, remoteItems = items) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingRemote = false,
                            errorMessage = err.message ?: "Connection failed"
                        )
                    }
                }
        }
    }

    fun closeRemoteBrowser() {
        _uiState.update { it.copy(activeNetworkSource = null, remoteItems = emptyList(), errorMessage = null) }
    }

    fun navigateUpRemote() {
        val source = _uiState.value.activeNetworkSource ?: return
        val curr = _uiState.value.remotePath.trimEnd('/')
        val parent = curr.substringBeforeLast('/', "").ifBlank { "/" }
        openNetworkSource(source, parent)
    }

    fun downloadRemoteBook(item: RemoteFileItem, onBookReady: (Long) -> Unit) {
        val source = _uiState.value.activeNetworkSource ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = 0,
                    downloadLabel = item.name,
                    errorMessage = null
                )
            }
            repository.downloadRemoteAndImport(source, item) { pct ->
                _uiState.update { it.copy(downloadProgress = pct) }
            }.onSuccess { book ->
                _uiState.update { it.copy(isDownloading = false) }
                refreshLocalFiles()
                onBookReady(book.id)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = err.message ?: "Failed to download ${item.name}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
