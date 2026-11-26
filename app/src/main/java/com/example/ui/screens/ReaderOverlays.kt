package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.model.BookFormat
import com.example.data.model.ChapterContent
import com.example.data.model.ParsedBook
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.components.TvSelectableChip
import com.example.ui.theme.TvColors
import com.example.ui.viewmodel.ReaderUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderToolbarOverlay(
    uiState: ReaderUiState,
    snippet: String,
    bookmarkCount: Int,
    hasBookmarkOnPage: Boolean,
    onSeekFraction: (Float) -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onOpenPageJump: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenToc: () -> Unit,
    onAddBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onExitReader: () -> Unit
) {
    val parsed = uiState.parsedBook
    val (pageNum, totalNum, pct) = computeProgressNumbers(uiState, parsed)
    val isTextFlow = uiState.format != BookFormat.PDF && uiState.format != BookFormat.CBZ
    val chapterCount = parsed?.chapters?.size ?: 1
    var pageChipFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0A0B0E).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, TvColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TvColors.Surface,
                border = BorderStroke(1.dp, TvColors.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = snippet.ifBlank { uiState.book?.title.orEmpty() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Slider(
                value = (pct / 100f).coerceIn(0f, 1f),
                onValueChange = onSeekFraction,
                colors = SliderDefaults.colors(
                    thumbColor = TvColors.PrimaryTextAccent,
                    activeTrackColor = TvColors.PrimaryBright,
                    inactiveTrackColor = TvColors.BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reader_seek_slider")
            )

            // Title + D-pad focusable Page Number jump button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.book?.title?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isTextFlow && chapterCount > 1) {
                        Text(
                            text = "Chapter ${uiState.currentChapterIndex + 1} of $chapterCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (pageChipFocused) TvColors.FocusContainer else TvColors.Surface,
                    border = BorderStroke(
                        width = if (pageChipFocused) 2.dp else 1.dp,
                        color = if (pageChipFocused) TvColors.FocusBorder else TvColors.PrimaryBright.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .testTag("reader_page_number_button")
                        .defaultMinSize(minHeight = 48.dp)
                        .onFocusChanged { pageChipFocused = it.isFocused }
                        .focusable()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenPageJump)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Type page number",
                            tint = TvColors.PrimaryTextAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Page $pageNum/$totalNum  •  $pct%",
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.PrimaryTextAccent
                        )
                    }
                }
            }

            // Controls row: Back | A- Aa A+ || Settings | TOC | Add Bookmark | Bookmarks(N)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvActionButton(
                        text = "",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        filled = false,
                        onClick = onExitReader,
                        tag = "reader_exit_button"
                    )
                    TvActionButton(
                        text = "A−",
                        filled = true,
                        onClick = onDecreaseFont,
                        tag = "reader_font_minus_button"
                    )
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.TextSecondary
                    )
                    TvActionButton(
                        text = "A+",
                        filled = true,
                        onClick = onIncreaseFont,
                        tag = "reader_font_plus_button"
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvActionButton(
                        text = stringResource(R.string.action_settings),
                        icon = Icons.Default.Settings,
                        filled = false,
                        onClick = onOpenSettings,
                        tag = "reader_settings_button"
                    )
                    TvActionButton(
                        text = stringResource(R.string.contents_title),
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        filled = true,
                        onClick = onOpenToc,
                        tag = "reader_toc_button"
                    )
                    TvActionButton(
                        text = if (hasBookmarkOnPage) "+ Bookmark" else "Bookmark",
                        icon = Icons.Default.BookmarkAdd,
                        filled = true,
                        onClick = onAddBookmark,
                        tag = "reader_add_bookmark_button"
                    )
                    TvActionButton(
                        text = "($bookmarkCount)",
                        icon = Icons.Default.Bookmarks,
                        filled = bookmarkCount > 0,
                        onClick = onOpenBookmarks,
                        tag = "reader_bookmarks_button"
                    )
                }
            }

            Text(
                text = stringResource(R.string.reader_dpad_legend),
                fontSize = 12.sp,
                color = TvColors.TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InReaderSettingsPanel(
    settings: ReaderSettings,
    onSetBrightness: (Int) -> Unit,
    onSetTheme: (ReaderTheme) -> Unit,
    onAdjustFontSize: (Int) -> Unit,
    onSetLineHeight: (Float) -> Unit,
    onSetReadingMode: (ReadingMode) -> Unit,
    onSetFocusMode: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstFocusRequester.requestFocus() }
    }

    Surface(
        modifier = modifier
            .widthIn(min = 300.dp, max = 440.dp)
            .fillMaxHeight()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141519).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, TvColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reader_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TvColors.TextPrimary
                )
                TvActionButton(
                    text = "",
                    icon = Icons.Default.Close,
                    onClick = onClose,
                    tag = "in_reader_settings_close"
                )
            }

            HorizontalDivider(color = TvColors.BorderSubtle)

            // Book Brightness Card (highlighted)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TvColors.Surface,
                border = BorderStroke(1.dp, TvColors.PrimaryBright.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = TvColors.PrimaryTextAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.book_brightness_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary
                            )
                        }
                        Text(
                            text = "${settings.brightnessPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvColors.PrimaryTextAccent,
                            modifier = Modifier.testTag("in_reader_brightness_value")
                        )
                    }

                    Slider(
                        value = settings.brightnessPercent.toFloat(),
                        onValueChange = { onSetBrightness(it.roundToInt()) },
                        valueRange = 5f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = TvColors.PrimaryTextAccent,
                            activeTrackColor = TvColors.PrimaryBright,
                            inactiveTrackColor = TvColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_reader_brightness_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TvActionButton(
                            text = "−10%",
                            filled = true,
                            onClick = { onSetBrightness(settings.brightnessPercent - 10) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(firstFocusRequester),
                            tag = "in_reader_brightness_minus_10"
                        )
                        TvActionButton(
                            text = "−5%",
                            filled = false,
                            onClick = { onSetBrightness(settings.brightnessPercent - 5) },
                            modifier = Modifier.weight(1f),
                            tag = "in_reader_brightness_minus_5"
                        )
                        TvActionButton(
                            text = "+5%",
                            filled = false,
                            onClick = { onSetBrightness(settings.brightnessPercent + 5) },
                            modifier = Modifier.weight(1f),
                            tag = "in_reader_brightness_plus_5"
                        )
                        TvActionButton(
                            text = "+10%",
                            filled = true,
                            onClick = { onSetBrightness(settings.brightnessPercent + 10) },
                            modifier = Modifier.weight(1f),
                            tag = "in_reader_brightness_plus_10"
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 25, 50, 75, 100).forEach { preset ->
                            TvSelectableChip(
                                label = "$preset%",
                                selected = settings.brightnessPercent == preset,
                                usePrimaryActiveColor = true,
                                onClick = { onSetBrightness(preset) },
                                tag = "in_reader_brightness_preset_$preset"
                            )
                        }
                    }
                }
            }

            // Theme Quick Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.TextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderTheme.entries.forEach { theme ->
                        TvSelectableChip(
                            label = theme.label,
                            selected = settings.theme == theme,
                            usePrimaryActiveColor = true,
                            onClick = { onSetTheme(theme) },
                            tag = "in_reader_theme_${theme.name.lowercase()}"
                        )
                    }
                }
            }

            // Font Size & Line Spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TvColors.Surface,
                    border = BorderStroke(1.dp, TvColors.BorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Font: ${settings.fontSizeSp}sp",
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvActionButton(
                                text = "A−",
                                filled = true,
                                onClick = { onAdjustFontSize(-2) },
                                tag = "in_reader_font_minus"
                            )
                            TvActionButton(
                                text = "A+",
                                filled = true,
                                onClick = { onAdjustFontSize(2) },
                                tag = "in_reader_font_plus"
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TvColors.Surface,
                    border = BorderStroke(1.dp, TvColors.BorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Spacing: ${String.format("%.2fx", settings.lineHeightMultiplier)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TvColors.TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvActionButton(
                                text = "−",
                                filled = true,
                                onClick = { onSetLineHeight(settings.lineHeightMultiplier - 0.1f) },
                                tag = "in_reader_line_minus"
                            )
                            TvActionButton(
                                text = "+",
                                filled = true,
                                onClick = { onSetLineHeight(settings.lineHeightMultiplier + 0.1f) },
                                tag = "in_reader_line_plus"
                            )
                        }
                    }
                }
            }

            // Reading Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reading Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.TextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingMode.entries.forEach { mode ->
                        TvSelectableChip(
                            label = mode.label,
                            selected = settings.readingMode == mode,
                            usePrimaryActiveColor = true,
                            onClick = { onSetReadingMode(mode) },
                            tag = "in_reader_mode_${mode.name.lowercase()}"
                        )
                    }
                }
            }

            // Focus Mode toggle
            TvFocusCard(
                onClick = { onSetFocusMode(!settings.focusMode) },
                tag = "in_reader_focus_mode_toggle"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Focus Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvColors.TextPrimary
                        )
                        Text(
                            text = "Hide page footer while reading",
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.focusMode,
                        onCheckedChange = onSetFocusMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TvColors.PrimaryBright
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GoToPageDialog(
    currentPage: Int,
    totalPages: Int,
    currentChapterIndex: Int,
    chapterCount: Int,
    isTextFlow: Boolean,
    initialDigits: String,
    onJumpToPage: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onDismiss: () -> Unit
) {
    var typedDigits by remember(initialDigits) { mutableStateOf(initialDigits) }
    val key1Focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { key1Focus.requestFocus() }
    }

    val parsedPage = typedDigits.toIntOrNull()
    val isValid = parsedPage != null && parsedPage in 1..totalPages
    val errorHint = when {
        typedDigits.isEmpty() -> null
        parsedPage == null || parsedPage < 1 || parsedPage > totalPages ->
            "Enter a page between 1 and $totalPages"
        else -> null
    }

    fun appendDigit(d: Char) {
        val next = (typedDigits + d).take(6)
        typedDigits = if (next.startsWith("0") && next.length > 1) next.trimStart('0').ifEmpty { "0" } else next
    }

    fun backspace() {
        if (typedDigits.isNotEmpty()) {
            typedDigits = typedDigits.dropLast(1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141519),
            border = BorderStroke(1.dp, TvColors.BorderSubtle),
            modifier = Modifier
                .widthIn(min = 300.dp, max = 420.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val digit = keyToDigitChar(event.key)
                    when {
                        digit != null -> {
                            appendDigit(digit)
                            true
                        }
                        event.key == Key.Backspace || event.key == Key.Delete -> {
                            backspace()
                            true
                        }
                        event.key == Key.Escape || event.key == Key.Back -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.go_to_page_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TvColors.TextPrimary
                    )
                    Text(
                        text = "Now: $currentPage / $totalPages",
                        style = MaterialTheme.typography.labelLarge,
                        color = TvColors.TextSecondary
                    )
                }

                if (isTextFlow && chapterCount > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvActionButton(
                            text = "‹ Ch",
                            filled = false,
                            enabled = currentChapterIndex > 0,
                            onClick = {
                                typedDigits = ""
                                onPreviousChapter()
                            },
                            tag = "page_jump_prev_chapter"
                        )
                        Text(
                            text = "Chapter ${currentChapterIndex + 1} / $chapterCount",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvColors.PrimaryTextAccent
                        )
                        TvActionButton(
                            text = "Ch ›",
                            filled = false,
                            enabled = currentChapterIndex + 1 < chapterCount,
                            onClick = {
                                typedDigits = ""
                                onNextChapter()
                            },
                            tag = "page_jump_next_chapter"
                        )
                    }
                }

                // Digital page input readout
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TvColors.Surface,
                    border = BorderStroke(
                        1.5.dp,
                        if (errorHint != null) TvColors.ErrorRed else TvColors.PrimaryBright
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("page_jump_display")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (typedDigits.isEmpty()) "—" else typedDigits,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (typedDigits.isEmpty()) TvColors.TextMuted else TvColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "/ $totalPages",
                            fontSize = 20.sp,
                            color = TvColors.TextSecondary
                        )
                    }
                }

                if (errorHint != null) {
                    Text(
                        text = errorHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.ErrorRed
                    )
                } else {
                    Text(
                        text = "Use D-pad or remote 0–9 keys to enter a page (1–$totalPages)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // D-pad 3x4 numeric keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )
                    rows.forEachIndexed { rIdx, rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEachIndexed { cIdx, digit ->
                                val mod = if (rIdx == 0 && cIdx == 0) {
                                    Modifier
                                        .weight(1f)
                                        .focusRequester(key1Focus)
                                } else {
                                    Modifier.weight(1f)
                                }
                                TvActionButton(
                                    text = digit,
                                    filled = false,
                                    onClick = { appendDigit(digit[0]) },
                                    modifier = mod,
                                    tag = "page_keypad_$digit"
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TvActionButton(
                            text = "",
                            icon = Icons.AutoMirrored.Filled.Backspace,
                            filled = false,
                            onClick = { backspace() },
                            modifier = Modifier.weight(1f),
                            tag = "page_keypad_backspace"
                        )
                        TvActionButton(
                            text = "0",
                            filled = false,
                            onClick = { appendDigit('0') },
                            modifier = Modifier.weight(1f),
                            tag = "page_keypad_0"
                        )
                        TvActionButton(
                            text = "Clear",
                            filled = false,
                            onClick = { typedDigits = "" },
                            modifier = Modifier.weight(1f),
                            tag = "page_keypad_clear"
                        )
                    }
                }

                // Confirm / Cancel row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvActionButton(
                        text = "Cancel",
                        filled = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        tag = "page_jump_cancel_button"
                    )
                    TvActionButton(
                        text = "Go",
                        filled = true,
                        enabled = isValid,
                        onClick = {
                            if (parsedPage != null && parsedPage in 1..totalPages) {
                                onJumpToPage(parsedPage - 1)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        tag = "page_jump_confirm_button"
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksSidePanel(
    bookmarks: List<BookmarkEntity>,
    currentChapterIndex: Int,
    currentPageIndex: Int,
    onAddQuickBookmark: () -> Unit,
    onAddBookmarkWithLabel: (String) -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .widthIn(min = 290.dp, max = 420.dp)
            .fillMaxHeight()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141519).copy(alpha = 0.97f),
        border = BorderStroke(1.dp, TvColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.bookmarks_panel_title)} (${bookmarks.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TvColors.TextPrimary
                )
                TvActionButton(
                    text = "",
                    icon = Icons.Default.Close,
                    onClick = onClose,
                    tag = "bookmarks_panel_close"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvActionButton(
                    text = stringResource(R.string.add_bookmark_action),
                    icon = Icons.Default.Add,
                    filled = true,
                    onClick = onAddQuickBookmark,
                    modifier = Modifier.weight(1f),
                    tag = "panel_add_bookmark_button"
                )
                TvActionButton(
                    text = "With Note",
                    icon = Icons.Default.Edit,
                    filled = false,
                    onClick = {
                        noteText = ""
                        showNoteDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    tag = "panel_add_noted_bookmark_button"
                )
            }

            HorizontalDivider(color = TvColors.BorderSubtle)

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = TvColors.PrimaryTextAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No bookmarks in this book yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvColors.TextPrimary
                        )
                        Text(
                            text = "Add as many bookmarks as you want to mark favorite passages or scenes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        val isHere = bm.chapterIndex == currentChapterIndex && bm.pageIndex == currentPageIndex
                        TvFocusCard(
                            onClick = { onSelectBookmark(bm) },
                            selected = isHere,
                            padding = PaddingValues(12.dp),
                            tag = "reader_bookmark_item_${bm.id}"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = TvColors.PrimaryTextAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (bm.label.isNotBlank()) {
                                        Text(
                                            text = bm.label,
                                            style = MaterialTheme.typography.titleMedium,
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TvColors.TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteBookmark(bm.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("reader_delete_bookmark_${bm.id}")
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

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            containerColor = TvColors.Surface,
            title = { Text("Add Bookmark Note", color = TvColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("e.g. Favorite quote, plot twist...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TvColors.FocusBorder,
                        unfocusedBorderColor = TvColors.BorderSubtle,
                        focusedTextColor = TvColors.TextPrimary,
                        unfocusedTextColor = TvColors.TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bookmark_note_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddBookmarkWithLabel(noteText)
                        showNoteDialog = false
                    }
                ) {
                    Text("Save", color = TvColors.PrimaryTextAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel", color = TvColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
fun TocSidePanel(
    chapters: List<ChapterContent>,
    currentChapterIndex: Int,
    alternativeLayout: Boolean,
    onToggleAlternativeLayout: () -> Unit,
    onSelectChapter: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 380.dp)
            .fillMaxHeight()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141519).copy(alpha = 0.97f),
        border = BorderStroke(1.dp, TvColors.BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.contents_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TvColors.TextPrimary
            )
            TvActionButton(
                text = if (alternativeLayout) stringResource(R.string.standard_layout)
                else stringResource(R.string.alternative_layout),
                filled = true,
                onClick = onToggleAlternativeLayout,
                modifier = Modifier.fillMaxWidth(),
                tag = "toc_alternative_layout_button"
            )
            HorizontalDivider(color = TvColors.BorderSubtle)

            if (chapters.isEmpty()) {
                Text(
                    text = "No table of contents entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(chapters) { index, ch ->
                        val isCurrent = index == currentChapterIndex
                        TvFocusCard(
                            onClick = { onSelectChapter(index) },
                            selected = isCurrent,
                            containerColor = Color.Transparent,
                            padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            tag = "toc_chapter_$index"
                        ) {
                            Text(
                                text = ch.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) TvColors.PrimaryTextAccent else TvColors.TextPrimary,
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

fun keyToDigitChar(key: Key): Char? = when (key) {
    Key.Zero, Key.NumPad0 -> '0'
    Key.One, Key.NumPad1 -> '1'
    Key.Two, Key.NumPad2 -> '2'
    Key.Three, Key.NumPad3 -> '3'
    Key.Four, Key.NumPad4 -> '4'
    Key.Five, Key.NumPad5 -> '5'
    Key.Six, Key.NumPad6 -> '6'
    Key.Seven, Key.NumPad7 -> '7'
    Key.Eight, Key.NumPad8 -> '8'
    Key.Nine, Key.NumPad9 -> '9'
    else -> null
}

fun computeProgressNumbers(
    uiState: ReaderUiState,
    parsed: ParsedBook?
): Triple<Int, Int, Int> {
    if (parsed == null) return Triple(1, 1, 0)
    return when (uiState.format) {
        BookFormat.PDF -> {
            val total = parsed.pdfPageCount.coerceAtLeast(1)
            val page = (uiState.currentPageIndex + 1).coerceIn(1, total)
            val pct = ((page.toFloat() / total) * 100f).toInt().coerceIn(1, 100)
            Triple(page, total, pct)
        }
        BookFormat.CBZ -> {
            val total = parsed.cbzImageEntries.size.coerceAtLeast(1)
            val page = (uiState.currentPageIndex + 1).coerceIn(1, total)
            val pct = ((page.toFloat() / total) * 100f).toInt().coerceIn(1, 100)
            Triple(page, total, pct)
        }
        else -> {
            val total = uiState.totalPagesInChapter.coerceAtLeast(1)
            val page = (uiState.currentPageIndex + 1).coerceIn(1, total)
            val totalCh = parsed.chapters.size.coerceAtLeast(1)
            val chFrac = uiState.currentChapterIndex.toFloat() / totalCh
            val pageFrac = (page.toFloat() / total) / totalCh
            val pct = ((chFrac + pageFrac) * 100f).toInt().coerceIn(1, 100)
            Triple(page, total, pct)
        }
    }
}
