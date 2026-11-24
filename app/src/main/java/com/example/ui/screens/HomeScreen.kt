package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BookEntity
import com.example.ui.components.BookCoverArt
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.TvColors
import com.example.ui.viewmodel.LibraryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: LibraryUiState,
    onOpenLibrary: () -> Unit,
    onBrowseFiles: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        val isWide = maxWidth >= 680.dp
        val horizontalPadding = if (isWide) 36.dp else 18.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 20.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = if (isWide) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                        color = TvColors.TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = stringResource(R.string.action_library),
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            onClick = onOpenLibrary,
                            filled = false,
                            tag = "home_library_button"
                        )
                        TvActionButton(
                            text = stringResource(R.string.action_browse_files),
                            onClick = onBrowseFiles,
                            filled = true,
                            tag = "home_browse_files_button"
                        )
                    }
                }
            }

            if (uiState.isImporting) {
                item {
                    Surface(
                        color = TvColors.FocusContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TvColors.PrimaryBright),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = TvColors.PrimaryTextAccent
                            )
                            Text(
                                text = "Importing book into your library...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextPrimary
                            )
                        }
                    }
                }
            }

            // Continue Reading Hero Card or Empty Welcome Hero
            item {
                val heroBook = uiState.lastOpenedBook ?: uiState.books.firstOrNull()
                if (heroBook != null) {
                    ContinueReadingHeroCard(
                        book = heroBook,
                        isWide = isWide,
                        onResume = { onOpenBook(heroBook.id) }
                    )
                } else {
                    EmptyLibraryHeroCard(
                        isWide = isWide,
                        onBrowseFiles = onBrowseFiles,
                        onOpenSources = onOpenSources
                    )
                }
            }

            // Quick Actions Strip (Search, Bookmarks, Settings, Sources, About)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = TvColors.SurfaceContainer,
                    border = BorderStroke(1.dp, TvColors.BorderSubtle)
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvActionButton(
                            text = stringResource(R.string.action_search),
                            icon = Icons.Default.Search,
                            onClick = onOpenSearch,
                            tag = "home_search_button"
                        )
                        TvActionButton(
                            text = stringResource(R.string.action_bookmarks),
                            icon = Icons.Default.Bookmark,
                            onClick = onOpenBookmarks,
                            tag = "home_bookmarks_button"
                        )
                        TvActionButton(
                            text = stringResource(R.string.action_settings),
                            icon = Icons.Default.Settings,
                            onClick = onOpenSettings,
                            tag = "home_settings_button"
                        )
                        TvActionButton(
                            text = stringResource(R.string.action_sources),
                            icon = Icons.Default.FolderOpen,
                            onClick = onOpenSources,
                            tag = "home_sources_button"
                        )
                        TvActionButton(
                            text = stringResource(R.string.action_about),
                            icon = Icons.Default.Info,
                            onClick = onOpenAbout,
                            tag = "home_about_button"
                        )
                    }
                }
            }

            // Recent Books section
            item {
                val recentList = uiState.recentBooks.ifEmpty { uiState.books.take(10) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = null,
                            tint = TvColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.recent_books),
                            style = MaterialTheme.typography.headlineSmall,
                            color = TvColors.TextPrimary
                        )
                    }

                    if (recentList.isEmpty()) {
                        Text(
                            text = "Books you open or download will appear here for quick couch access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(recentList, key = { it.id }) { book ->
                                RecentBookCard(
                                    book = book,
                                    onClick = { onOpenBook(book.id) }
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
private fun ContinueReadingHeroCard(
    book: BookEntity,
    isWide: Boolean,
    onResume: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(TvColors.HeroStart, TvColors.HeroMid, TvColors.HeroEnd)
                )
            )
            .border(1.dp, Color(0xFF284F86), RoundedCornerShape(18.dp))
            .padding(if (isWide) 24.dp else 18.dp)
    ) {
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BookCoverArt(
                    book = book,
                    modifier = Modifier.width(116.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.continue_reading),
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.PrimaryTextAccent
                    )
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = TvColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.author} • ${book.format}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TvColors.PrimaryTextAccent,
                            trackColor = Color.White.copy(alpha = 0.14f)
                        )
                        Text(
                            text = "${book.progressPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.PrimaryTextAccent
                        )
                    }
                }
                TvActionButton(
                    text = stringResource(R.string.action_resume),
                    icon = Icons.Default.PlayArrow,
                    filled = true,
                    onClick = onResume,
                    tag = "home_resume_button"
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCoverArt(
                        book = book,
                        modifier = Modifier.width(84.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.continue_reading),
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.PrimaryTextAccent
                        )
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TvColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.author} • ${book.format}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = TvColors.PrimaryTextAccent,
                        trackColor = Color.White.copy(alpha = 0.14f)
                    )
                    Text(
                        text = "${book.progressPercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = TvColors.PrimaryTextAccent
                    )
                }
                TvActionButton(
                    text = stringResource(R.string.action_resume),
                    icon = Icons.Default.PlayArrow,
                    filled = true,
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "home_resume_button"
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryHeroCard(
    isWide: Boolean,
    onBrowseFiles: () -> Unit,
    onOpenSources: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(TvColors.HeroStart, TvColors.HeroEnd)
                )
            )
            .border(1.dp, Color(0xFF284F86), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isWide) 24.dp else 18.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWide) {
                Image(
                    painter = painterResource(id = R.drawable.img_tv_banner),
                    contentDescription = "TV Reader banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(200.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, TvColors.BorderSubtle, RoundedCornerShape(12.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.empty_library_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TvColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.empty_library_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvActionButton(
                        text = stringResource(R.string.action_sources),
                        icon = Icons.Default.FolderOpen,
                        filled = true,
                        onClick = onOpenSources,
                        tag = "empty_open_sources_button"
                    )
                    TvActionButton(
                        text = stringResource(R.string.action_browse_files),
                        onClick = onBrowseFiles,
                        filled = false,
                        tag = "empty_browse_files_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentBookCard(
    book: BookEntity,
    onClick: () -> Unit
) {
    TvFocusCard(
        onClick = onClick,
        modifier = Modifier.width(156.dp),
        padding = PaddingValues(10.dp),
        tag = "recent_book_${book.id}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BookCoverArt(
                book = book,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.labelLarge,
                color = TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.format,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TvColors.TextSecondary
                )
                Text(
                    text = "${book.progressPercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TvColors.PrimaryTextAccent
                )
            }
        }
    }
}
