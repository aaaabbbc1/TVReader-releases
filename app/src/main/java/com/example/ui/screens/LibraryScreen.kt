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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookEntity
import com.example.data.model.LibrarySort
import com.example.ui.components.BookCoverArt
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.components.TvSelectableChip
import com.example.ui.theme.TvColors
import com.example.ui.viewmodel.LibraryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onBack: () -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenBook: (Long) -> Unit,
    onDeleteBook: (BookEntity) -> Unit,
    onOpenSources: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        val isWide = maxWidth >= 680.dp
        val hPad = if (isWide) 36.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPad, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header + Sort & Search bar
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvActionButton(
                        text = "",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        tag = "library_back_button"
                    )
                    Text(
                        text = "Library (${uiState.books.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TvColors.TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    LibraryControlsStrip(
                        sort = uiState.sort,
                        searchQuery = uiState.searchQuery,
                        onSortChange = onSortChange,
                        onSearchQueryChange = onSearchQueryChange
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TvActionButton(
                            text = "",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onBack,
                            tag = "library_back_button"
                        )
                        Text(
                            text = "Library (${uiState.books.size})",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TvColors.TextPrimary
                        )
                    }
                    LibraryControlsStrip(
                        sort = uiState.sort,
                        searchQuery = uiState.searchQuery,
                        onSortChange = onSortChange,
                        onSearchQueryChange = onSearchQueryChange
                    )
                }
            }

            // Book Collection Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = TvColors.SurfaceContainer,
                border = BorderStroke(1.dp, TvColors.BorderSubtle)
            ) {
                if (uiState.books.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isBlank()) "No books in your library yet"
                                else "No books matching \"${uiState.searchQuery}\"",
                                style = MaterialTheme.typography.titleLarge,
                                color = TvColors.TextPrimary
                            )
                            Text(
                                text = "Add books from Local Storage, Direct URL, Project Gutenberg, or Standard Ebooks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary
                            )
                            TvActionButton(
                                text = "Browse Sources",
                                icon = Icons.Default.Add,
                                filled = true,
                                onClick = onOpenSources,
                                tag = "library_empty_sources_button"
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.books, key = { it.id }) { book ->
                            LibraryBookGridItem(
                                book = book,
                                onClick = { onOpenBook(book.id) },
                                onRequestDelete = { bookToDelete = book }
                            )
                        }
                    }
                }
            }
        }
    }

    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            containerColor = TvColors.Surface,
            title = { Text("Remove Book", color = TvColors.TextPrimary) },
            text = {
                Text(
                    "Remove \"${bookToDelete?.title}\" and its bookmarks from your library?",
                    color = TvColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookToDelete?.let(onDeleteBook)
                        bookToDelete = null
                    }
                ) {
                    Text("Remove", color = TvColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", color = TvColors.TextPrimary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryControlsStrip(
    sort: LibrarySort,
    searchQuery: String,
    onSortChange: (LibrarySort) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TvColors.SurfaceContainer,
        border = BorderStroke(1.dp, TvColors.BorderSubtle)
    ) {
        FlowRow(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibrarySort.entries.forEach { option ->
                TvSelectableChip(
                    label = option.label,
                    selected = sort == option,
                    usePrimaryActiveColor = true,
                    onClick = { onSortChange(option) },
                    modifier = Modifier.align(Alignment.CenterVertically),
                    tag = "library_sort_${option.name.lowercase()}"
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text("Search books...", color = TvColors.TextSecondary, maxLines = 1)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search books",
                        tint = TvColors.TextSecondary
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TvColors.FocusBorder,
                    unfocusedBorderColor = TvColors.BorderSubtle,
                    focusedTextColor = TvColors.TextPrimary,
                    unfocusedTextColor = TvColors.TextPrimary,
                    cursorColor = TvColors.PrimaryTextAccent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .testTag("library_search_input")
            )
        }
    }
}

@Composable
private fun LibraryBookGridItem(
    book: BookEntity,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    TvFocusCard(
        onClick = onClick,
        padding = PaddingValues(10.dp),
        tag = "library_book_${book.id}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BookCoverArt(
                book = book,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = TvColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = book.format,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TvColors.PrimaryTextAccent,
                        modifier = Modifier
                            .background(TvColors.FocusContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    val spotText = if (book.lastOpened > 0L) {
                        "Pg ${book.currentPageIndex + 1} • ${book.progressPercent}%"
                    } else {
                        "${book.progressPercent}%"
                    }
                    Text(
                        text = spotText,
                        fontSize = 12.sp,
                        color = TvColors.TextSecondary
                    )
                }
                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_book_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ${book.title}",
                        tint = TvColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = TvColors.PrimaryBright,
                trackColor = TvColors.BorderSubtle
            )
        }
    }
}
