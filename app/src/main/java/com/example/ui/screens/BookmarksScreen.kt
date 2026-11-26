package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.TvColors

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onBack: () -> Unit,
    onOpenBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        val hPad = if (maxWidth >= 680.dp) 36.dp else 16.dp

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
                    tag = "bookmarks_back_button"
                )
                Text(
                    text = "Bookmarks (${bookmarks.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TvColors.TextPrimary
                )
            }

            if (bookmarks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TvColors.SurfaceContainer,
                    border = BorderStroke(1.dp, TvColors.BorderSubtle),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = TvColors.PrimaryTextAccent,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = stringResource(R.string.empty_bookmarks_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = TvColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.empty_bookmarks_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        TvFocusCard(
                            onClick = { onOpenBookmark(bm) },
                            tag = "bookmark_item_${bm.id}"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = TvColors.PrimaryTextAccent
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = bm.bookTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TvColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (bm.label.isNotBlank()) {
                                        Text(
                                            text = bm.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TvColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${bm.chapterTitle} • Page ${bm.pageIndex + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TvColors.PrimaryTextAccent
                                    )
                                    Text(
                                        text = bm.snippet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TvColors.TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteBookmark(bm.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_bookmark_${bm.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete bookmark",
                                        tint = TvColors.TextMuted
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
