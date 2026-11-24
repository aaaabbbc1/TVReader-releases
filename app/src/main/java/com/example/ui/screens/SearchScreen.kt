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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.TvColors
import com.example.ui.viewmodel.LocalSearchResult

@Composable
fun SearchScreen(
    query: String,
    results: List<LocalSearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onBack: () -> Unit,
    onOpenResult: (LocalSearchResult) -> Unit,
    onOpenGutenberg: () -> Unit,
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
                    tag = "search_back_button"
                )
                Text(
                    text = "Search Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TvColors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search titles, authors, or passages inside books...") },
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
                        .testTag("library_full_search_input")
                )
                TvActionButton(
                    text = "Search",
                    icon = Icons.Default.Search,
                    filled = true,
                    onClick = onSearchSubmit,
                    tag = "library_full_search_button"
                )
            }

            when {
                isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
                    }
                }
                results.isEmpty() -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TvColors.SurfaceContainer,
                        border = BorderStroke(1.dp, TvColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (query.isBlank()) "Enter a keyword to search your downloaded books"
                                else "No local matches for \"$query\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary
                            )
                            Text(
                                text = "Looking for a new book? Search 70,000+ free titles on Project Gutenberg.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextSecondary
                            )
                            TvActionButton(
                                text = "Search Project Gutenberg",
                                filled = true,
                                onClick = onOpenGutenberg,
                                tag = "search_to_gutenberg_button"
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(results) { match ->
                            TvFocusCard(onClick = { onOpenResult(match) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = match.book.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TvColors.TextPrimary
                                    )
                                    Text(
                                        text = match.chapterTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TvColors.PrimaryTextAccent
                                    )
                                    Text(
                                        text = match.matchSnippet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TvColors.TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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
