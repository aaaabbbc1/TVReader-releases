package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.NetworkSourceEntity
import com.example.data.model.BookFormat
import com.example.data.model.GutenbergBook
import com.example.data.model.SourceProtocol
import com.example.data.model.StandardEbookEntry
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.components.TvSelectableChip
import com.example.ui.theme.TvColors
import com.example.ui.viewmodel.SourceTab
import com.example.ui.viewmodel.SourcesUiState
import com.example.ui.viewmodel.SourcesViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcesScreen(
    uiState: SourcesUiState,
    webDavSources: List<NetworkSourceEntity>,
    ftpSources: List<NetworkSourceEntity>,
    viewModel: SourcesViewModel,
    onBack: () -> Unit,
    onLaunchSystemPicker: () -> Unit,
    onOpenBook: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showUrlDialog by remember { mutableStateOf(false) }
    var showAddServerProtocol by remember { mutableStateOf<SourceProtocol?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        val isWide = maxWidth >= 700.dp
        val hPad = if (isWide) 36.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPad, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvActionButton(
                    text = "",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    tag = "sources_back_button"
                )
                Text(
                    text = "File Sources",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TvColors.TextPrimary
                )
            }

            // Source tabs container matching 07_sources.png
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = TvColors.SurfaceContainer,
                border = BorderStroke(1.dp, TvColors.BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TvSelectableChip(
                                label = SourceTab.LOCAL.label,
                                icon = Icons.Default.Storage,
                                selected = uiState.activeTab == SourceTab.LOCAL,
                                onClick = { viewModel.selectTab(SourceTab.LOCAL) },
                                tag = "source_tab_local"
                            )
                            TvSelectableChip(
                                label = SourceTab.WEBDAV.label,
                                icon = Icons.Default.Dns,
                                selected = uiState.activeTab == SourceTab.WEBDAV,
                                onClick = { viewModel.selectTab(SourceTab.WEBDAV) },
                                tag = "source_tab_webdav"
                            )
                            TvSelectableChip(
                                label = SourceTab.FTP.label,
                                icon = Icons.Default.Folder,
                                selected = uiState.activeTab == SourceTab.FTP,
                                onClick = { viewModel.selectTab(SourceTab.FTP) },
                                tag = "source_tab_ftp"
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = { showUrlDialog = true },
                            modifier = Modifier.testTag("download_from_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = TvColors.PrimaryTextAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_download_url),
                                color = TvColors.PrimaryTextAccent,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    HorizontalDivider(color = TvColors.BorderSubtle)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Online Libraries",
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.TextSecondary,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(end = 6.dp)
                        )
                        TvSelectableChip(
                            label = SourceTab.GUTENBERG.label,
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            selected = uiState.activeTab == SourceTab.GUTENBERG,
                            onClick = { viewModel.selectTab(SourceTab.GUTENBERG) },
                            tag = "source_tab_gutenberg"
                        )
                        TvSelectableChip(
                            label = SourceTab.STANDARD_EBOOKS.label,
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            selected = uiState.activeTab == SourceTab.STANDARD_EBOOKS,
                            onClick = { viewModel.selectTab(SourceTab.STANDARD_EBOOKS) },
                            tag = "source_tab_standard_ebooks"
                        )
                    }
                }
            }

            if (uiState.isDownloading) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TvColors.FocusContainer,
                    border = BorderStroke(1.dp, TvColors.PrimaryBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Downloading ${uiState.downloadLabel}... (${uiState.downloadProgress}%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.TextPrimary
                        )
                        LinearProgressIndicator(
                            progress = { (uiState.downloadProgress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TvColors.PrimaryTextAccent,
                            trackColor = TvColors.BorderSubtle
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TvColors.ErrorRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TvColors.ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = TvColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = TvColors.TextPrimary)
                        }
                    }
                }
            }

            // Active Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.activeTab) {
                    SourceTab.LOCAL -> LocalStoragePane(
                        localFiles = uiState.localFiles,
                        onLaunchSystemPicker = onLaunchSystemPicker,
                        onOpenLocalFile = { file ->
                            viewModel.importLocalFile(file, onOpenBook)
                        }
                    )
                    SourceTab.WEBDAV -> NetworkSourcePane(
                        protocol = SourceProtocol.WEBDAV,
                        sources = webDavSources,
                        uiState = uiState,
                        viewModel = viewModel,
                        onAddServer = { showAddServerProtocol = SourceProtocol.WEBDAV },
                        onOpenBook = onOpenBook
                    )
                    SourceTab.FTP -> NetworkSourcePane(
                        protocol = SourceProtocol.FTP,
                        sources = ftpSources,
                        uiState = uiState,
                        viewModel = viewModel,
                        onAddServer = { showAddServerProtocol = SourceProtocol.FTP },
                        onOpenBook = onOpenBook
                    )
                    SourceTab.GUTENBERG -> GutenbergPane(
                        uiState = uiState,
                        isWide = isWide,
                        viewModel = viewModel,
                        onOpenBook = onOpenBook
                    )
                    SourceTab.STANDARD_EBOOKS -> StandardEbooksPane(
                        uiState = uiState,
                        isWide = isWide,
                        viewModel = viewModel,
                        onOpenBook = onOpenBook
                    )
                }
            }
        }
    }

    if (showUrlDialog) {
        DownloadUrlDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url ->
                showUrlDialog = false
                viewModel.downloadFromUrl(url = url, onBookReady = onOpenBook)
            }
        )
    }

    if (showAddServerProtocol != null) {
        AddNetworkServerDialog(
            protocol = showAddServerProtocol!!,
            onDismiss = { showAddServerProtocol = null },
            onSave = { name, host, port, user, pass, basePath ->
                viewModel.addNetworkSource(
                    name = name,
                    protocol = showAddServerProtocol!!,
                    host = host,
                    port = port,
                    username = user,
                    password = pass,
                    basePath = basePath
                )
                showAddServerProtocol = null
            }
        )
    }
}

@Composable
private fun LocalStoragePane(
    localFiles: List<java.io.File>,
    onLaunchSystemPicker: () -> Unit,
    onOpenLocalFile: (java.io.File) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Storage",
            style = MaterialTheme.typography.titleLarge,
            color = TvColors.TextPrimary
        )
        HorizontalDivider(color = TvColors.BorderSubtle)
        TvFocusCard(
            onClick = onLaunchSystemPicker,
            tag = "local_system_picker_card"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color(0xFFE7A9D7),
                    modifier = Modifier.size(30.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Browse Device & USB Storage",
                        style = MaterialTheme.typography.titleLarge,
                        color = TvColors.TextPrimary
                    )
                    Text(
                        text = "Open EPUB, FB2, PDF, TXT, MD, HTML, or CBZ using system picker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary
                    )
                }
                TvActionButton(
                    text = "Open Picker",
                    filled = true,
                    onClick = onLaunchSystemPicker,
                    tag = "local_open_picker_button"
                )
            }
        }

        if (localFiles.isNotEmpty()) {
            Text(
                text = "Imported & Downloaded Books (${localFiles.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(localFiles, key = { it.absolutePath }) { file ->
                    val fmt = BookFormat.fromFileName(file.name)?.label ?: "FILE"
                    TvFocusCard(onClick = { onOpenLocalFile(file) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = TvColors.PrimaryTextAccent
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TvColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$fmt • ${(file.length() / 1024).coerceAtLeast(1)} KB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TvColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkSourcePane(
    protocol: SourceProtocol,
    sources: List<NetworkSourceEntity>,
    uiState: SourcesUiState,
    viewModel: SourcesViewModel,
    onAddServer: () -> Unit,
    onOpenBook: (Long) -> Unit
) {
    val activeSource = uiState.activeNetworkSource
    if (activeSource == null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${protocol.label} Servers",
                    style = MaterialTheme.typography.titleLarge,
                    color = TvColors.TextPrimary
                )
                TvActionButton(
                    text = "Add ${protocol.label} Server",
                    icon = Icons.Default.Add,
                    filled = true,
                    onClick = onAddServer,
                    tag = "add_${protocol.name.lowercase()}_button"
                )
            }
            HorizontalDivider(color = TvColors.BorderSubtle)
            if (sources.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TvColors.Surface,
                    border = BorderStroke(1.dp, TvColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No ${protocol.label} servers configured",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvColors.TextPrimary
                        )
                        Text(
                            text = "Connect to a NAS, Calibre WebDAV, or home FTP share to browse and download books directly onto your TV.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sources, key = { it.id }) { src ->
                        TvFocusCard(onClick = { viewModel.openNetworkSource(src) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = src.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TvColors.TextPrimary
                                    )
                                    Text(
                                        text = "${src.host}:${src.port}${src.basePath}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TvColors.TextSecondary
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteNetworkSource(src.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete server",
                                        tint = TvColors.TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvActionButton(
                    text = "Servers",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { viewModel.closeRemoteBrowser() }
                )
                TvActionButton(
                    text = "Up",
                    onClick = { viewModel.navigateUpRemote() }
                )
                Text(
                    text = "${activeSource.name}: ${uiState.remotePath}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (uiState.isLoadingRemote) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.remoteItems) { item ->
                        TvFocusCard(
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.openNetworkSource(activeSource, item.path)
                                } else {
                                    viewModel.downloadRemoteBook(item, onOpenBook)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) TvColors.PrimaryTextAccent else TvColors.TextSecondary
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TvColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (item.format != null) {
                                    Text(
                                        text = item.format.label,
                                        fontSize = 11.sp,
                                        color = TvColors.PrimaryTextAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GutenbergPane(
    uiState: SourcesUiState,
    isWide: Boolean,
    viewModel: SourcesViewModel,
    onOpenBook: (Long) -> Unit
) {
    val searchRow = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.gutenbergQuery,
                onValueChange = { viewModel.updateGutenbergQuery(it) },
                label = { Text("Search") },
                placeholder = { Text("Author, title, or subject...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TvColors.FocusBorder,
                    unfocusedBorderColor = TvColors.BorderSubtle,
                    focusedTextColor = TvColors.TextPrimary,
                    unfocusedTextColor = TvColors.TextPrimary,
                    focusedLabelColor = TvColors.PrimaryTextAccent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("gutenberg_search_input")
            )
            TvActionButton(
                text = "Search",
                icon = Icons.Default.Search,
                filled = true,
                onClick = { viewModel.searchGutenberg() },
                tag = "gutenberg_search_button"
            )
        }
    }

    if (isWide) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                searchRow()
                if (uiState.isLoadingGutenberg) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.gutenbergResults, key = { it.id }) { book ->
                            GutenbergResultCard(
                                book = book,
                                selected = uiState.selectedGutenberg?.id == book.id,
                                onClick = { viewModel.selectGutenbergBook(book) }
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(TvColors.BorderSubtle)
            )
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                GutenbergDetailPane(
                    book = uiState.selectedGutenberg,
                    onDownloadFormat = { url, ext ->
                        uiState.selectedGutenberg?.let {
                            viewModel.downloadGutenbergBook(it, url, ext, onOpenBook)
                        }
                    }
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            searchRow()
            if (uiState.selectedGutenberg != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TvColors.Surface,
                    border = BorderStroke(1.dp, TvColors.BorderSubtle)
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        GutenbergDetailPane(
                            book = uiState.selectedGutenberg,
                            onDownloadFormat = { url, ext ->
                                viewModel.downloadGutenbergBook(uiState.selectedGutenberg, url, ext, onOpenBook)
                            }
                        )
                    }
                }
            }
            if (uiState.isLoadingGutenberg) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.gutenbergResults, key = { it.id }) { book ->
                        GutenbergResultCard(
                            book = book,
                            selected = uiState.selectedGutenberg?.id == book.id,
                            onClick = { viewModel.selectGutenbergBook(book) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GutenbergResultCard(
    book: GutenbergBook,
    selected: Boolean,
    onClick: () -> Unit
) {
    TvFocusCard(
        onClick = onClick,
        selected = selected,
        tag = "gutenberg_book_${book.id}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.authorDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FormatPill(book.primaryLanguage, highlighted = false)
                if (book.txtUrl != null) FormatPill("TXT", highlighted = true)
                if (book.epubUrl != null) FormatPill("EPUB", highlighted = true)
                if (book.htmlUrl != null) FormatPill("HTML", highlighted = false)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GutenbergDetailPane(
    book: GutenbergBook?,
    onDownloadFormat: (String, String) -> Unit
) {
    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Select a book from Project Gutenberg to view details and download.",
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineMedium,
            color = TvColors.TextPrimary
        )
        Text(
            text = "by ${book.authorDisplay}",
            style = MaterialTheme.typography.titleMedium,
            color = TvColors.TextSecondary
        )
        Text(
            text = book.primaryLanguage,
            style = MaterialTheme.typography.labelLarge,
            color = TvColors.TextSecondary
        )
        if (book.subjects.isNotEmpty()) {
            Text(
                text = book.subjects.take(4).joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            book.epubUrl?.let { url ->
                TvActionButton(
                    text = "EPUB",
                    filled = true,
                    onClick = { onDownloadFormat(url, "epub") },
                    tag = "gutenberg_download_epub"
                )
            }
            book.txtUrl?.let { url ->
                TvActionButton(
                    text = "TXT",
                    filled = false,
                    onClick = { onDownloadFormat(url, "txt") },
                    tag = "gutenberg_download_txt"
                )
            }
            book.htmlUrl?.let { url ->
                TvActionButton(
                    text = "HTML",
                    filled = false,
                    onClick = { onDownloadFormat(url, "html") },
                    tag = "gutenberg_download_html"
                )
            }
        }
    }
}

@Composable
private fun StandardEbooksPane(
    uiState: SourcesUiState,
    isWide: Boolean,
    viewModel: SourcesViewModel,
    onOpenBook: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.standardQuery,
                onValueChange = { viewModel.updateStandardQuery(it) },
                label = { Text("Filter Standard Ebooks") },
                placeholder = { Text("Title or author...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TvColors.FocusBorder,
                    unfocusedBorderColor = TvColors.BorderSubtle,
                    focusedTextColor = TvColors.TextPrimary,
                    unfocusedTextColor = TvColors.TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("standard_search_input")
            )
            TvActionButton(
                text = "Search",
                icon = Icons.Default.Search,
                filled = true,
                onClick = { viewModel.searchStandardEbooks() },
                tag = "standard_search_button"
            )
        }

        if (uiState.isLoadingStandard) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
            }
        } else {
            val detailBlock: @Composable (StandardEbookEntry?) -> Unit = { entry ->
                if (entry == null) {
                    Text(
                        text = "Select an edition from Standard Ebooks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TvColors.TextPrimary
                        )
                        Text(
                            text = "by ${entry.author}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvColors.TextSecondary
                        )
                        if (entry.summary.isNotBlank()) {
                            Text(
                                text = entry.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TvActionButton(
                            text = "Download EPUB",
                            icon = Icons.Default.CloudDownload,
                            filled = true,
                            onClick = {
                                viewModel.downloadFromUrl(
                                    url = entry.epubUrl,
                                    suggestedName = entry.title.take(40),
                                    forcedExtension = "epub",
                                    onBookReady = onOpenBook
                                )
                            },
                            tag = "standard_download_epub"
                        )
                    }
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.standardResults, key = { it.id }) { entry ->
                            TvFocusCard(
                                onClick = { viewModel.selectStandardBook(entry) },
                                selected = uiState.selectedStandard?.id == entry.id
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TvColors.TextPrimary
                                    )
                                    Text(
                                        text = entry.author,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TvColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(TvColors.BorderSubtle)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        detailBlock(uiState.selectedStandard)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.selectedStandard?.let { sel ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TvColors.Surface,
                                border = BorderStroke(1.dp, TvColors.BorderSubtle)
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) { detailBlock(sel) }
                            }
                        }
                    }
                    items(uiState.standardResults, key = { it.id }) { entry ->
                        TvFocusCard(
                            onClick = { viewModel.selectStandardBook(entry) },
                            selected = uiState.selectedStandard?.id == entry.id
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TvColors.TextPrimary
                                )
                                Text(
                                    text = entry.author,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TvColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatPill(label: String, highlighted: Boolean) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (highlighted) TvColors.ActiveChipText else TvColors.TextSecondary,
        modifier = Modifier
            .background(
                color = if (highlighted) TvColors.ActiveChipContainer else TvColors.SurfaceVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun DownloadUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvColors.Surface,
        title = { Text("Download Book from URL", color = TvColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a direct link to an EPUB, FB2, PDF, TXT, MD, HTML, or CBZ file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://example.com/book.epub") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TvColors.FocusBorder,
                        unfocusedBorderColor = TvColors.BorderSubtle,
                        focusedTextColor = TvColors.TextPrimary,
                        unfocusedTextColor = TvColors.TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_download_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url) },
                enabled = url.isNotBlank()
            ) {
                Text("Download & Open", color = TvColors.PrimaryTextAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TvColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun AddNetworkServerDialog(
    protocol: SourceProtocol,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(protocol.defaultPort.toString()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var basePath by remember { mutableStateOf("/") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvColors.Surface,
        title = { Text("Add ${protocol.label} Source", color = TvColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(if (protocol == SourceProtocol.WEBDAV) "Host or Base URL" else "Host / IP") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                    OutlinedTextField(
                        value = basePath,
                        onValueChange = { basePath = it },
                        label = { Text("Path") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        host,
                        port.toIntOrNull() ?: protocol.defaultPort,
                        username,
                        password,
                        basePath
                    )
                },
                enabled = host.isNotBlank()
            ) {
                Text("Save", color = TvColors.PrimaryTextAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TvColors.TextSecondary)
            }
        }
    )
}
