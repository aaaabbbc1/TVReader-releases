package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.model.BookFormat
import com.example.data.model.ChapterContent
import com.example.data.model.ContentBlock
import com.example.data.model.ParsedBook
import com.example.data.model.PdfPageMode
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode
import com.example.data.parser.PdfAndCbzParser
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.theme.ReaderPalette
import com.example.ui.theme.TvColors
import com.example.ui.theme.resolveReaderPalette
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.viewmodel.ReaderUiState
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

private data class TextPageSlice(
    val content: AnnotatedString,
    val imageKey: String? = null
)

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    settings: ReaderSettings,
    bookmarks: List<BookmarkEntity>,
    viewModel: ReaderViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = resolveReaderPalette(settings.theme, settings.astigmaPreset)
    val readerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.showToolbar, uiState.showTocDrawer, uiState.isLoading) {
        if (!uiState.showToolbar && !uiState.showTocDrawer && !uiState.isLoading) {
            runCatching { readerFocusRequester.requestFocus() }
        }
    }

    BackHandler {
        when {
            uiState.showTocDrawer || uiState.showToolbar -> viewModel.closeOverlays()
            else -> onExit()
        }
    }

    val currentChapter = uiState.parsedBook?.chapters?.getOrNull(uiState.currentChapterIndex)
    val currentSnippet = remember(uiState.currentChapterIndex, uiState.currentPageIndex, currentChapter) {
        currentChapter?.rawText?.take(180) ?: uiState.book?.title.orEmpty()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .focusRequester(readerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (uiState.showTocDrawer || uiState.showToolbar) {
                    if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape || keyEvent.key == Key.Menu) {
                        viewModel.closeOverlays()
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }
                when (keyEvent.key) {
                    Key.DirectionRight, Key.NavigateNext -> {
                        viewModel.nextPage()
                        true
                    }
                    Key.DirectionLeft, Key.NavigatePrevious -> {
                        viewModel.previousPage()
                        true
                    }
                    Key.ChannelUp, Key.PageUp -> {
                        viewModel.previousChapter()
                        true
                    }
                    Key.ChannelDown, Key.PageDown -> {
                        viewModel.nextChapter()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Menu, Key.DirectionUp, Key.DirectionDown -> {
                        viewModel.toggleToolbar()
                        true
                    }
                    else -> false
                }
            }
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent)
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        TvActionButton(
                            text = stringResource(R.string.action_back),
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            filled = true,
                            onClick = onExit
                        )
                    }
                }
            }

            uiState.parsedBook != null -> {
                val parsed = uiState.parsedBook
                val isPdfOrCbz = uiState.format == BookFormat.PDF || uiState.format == BookFormat.CBZ

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(uiState.showToolbar, uiState.showTocDrawer) {
                            detectTapGestures { offset ->
                                if (uiState.showTocDrawer || uiState.showToolbar) {
                                    viewModel.closeOverlays()
                                } else {
                                    val w = size.width.toFloat()
                                    when {
                                        offset.x < w * 0.28f -> viewModel.previousPage()
                                        offset.x > w * 0.72f -> viewModel.nextPage()
                                        else -> viewModel.toggleToolbar()
                                    }
                                }
                            }
                        }
                ) {
                    val hMargin = if (maxWidth >= 700.dp) 56.dp else 20.dp
                    val vMargin = if (maxHeight >= 600.dp) 32.dp else 18.dp

                    when {
                        uiState.format == BookFormat.PDF -> {
                            PdfReaderCanvas(
                                pdfPath = parsed.pdfFilePath.orEmpty(),
                                pageIndex = uiState.currentPageIndex,
                                pageMode = settings.pdfPageMode,
                                invertDark = settings.theme == ReaderTheme.AMOLED || settings.theme == ReaderTheme.DARK
                            )
                        }
                        uiState.format == BookFormat.CBZ -> {
                            val entryName = parsed.cbzImageEntries.getOrNull(uiState.currentPageIndex).orEmpty()
                            CbzReaderCanvas(
                                cbzPath = parsed.cbzFilePath.orEmpty(),
                                entryName = entryName
                            )
                        }
                        settings.alternativeLayout -> {
                            AlternativeDocumentLayout(
                                parsedBook = parsed,
                                settings = settings,
                                palette = palette,
                                onSelectChapter = { viewModel.jumpToChapter(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = hMargin, vertical = vMargin)
                            )
                        }
                        settings.readingMode == ReadingMode.CONTINUOUS -> {
                            ContinuousChapterView(
                                chapter = currentChapter,
                                parsedBook = parsed,
                                settings = settings,
                                palette = palette,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = hMargin, vertical = vMargin)
                            )
                        }
                        else -> {
                            PaginatedChapterView(
                                chapter = currentChapter,
                                parsedBook = parsed,
                                pageIndex = uiState.currentPageIndex,
                                settings = settings,
                                palette = palette,
                                onPagesMeasured = { count -> viewModel.updatePaginationCount(count) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = hMargin,
                                        end = hMargin,
                                        top = vMargin,
                                        bottom = vMargin + 22.dp
                                    )
                            )
                        }
                    }

                    // Bottom status indicator (hidden in Focus Mode, matching 03_reader.png)
                    if (!settings.focusMode && !uiState.showToolbar) {
                        val footerText = buildFooterText(uiState, parsed, isPdfOrCbz)
                        Text(
                            text = footerText,
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("reader_footer_status")
                        )
                    }
                }
            }
        }

        // Brightness dimming overlay
        val dimAlpha = ((100 - settings.brightnessPercent).coerceIn(0, 85) / 100f) * 0.78f
        if (dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        // Reader Toolbar Overlay (matching 04_reader_toolbar.png)
        AnimatedVisibility(
            visible = uiState.showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val isBookmarked = bookmarks.any {
                it.chapterIndex == uiState.currentChapterIndex && it.pageIndex == uiState.currentPageIndex
            }
            ReaderToolbarOverlay(
                uiState = uiState,
                snippet = currentSnippet,
                isBookmarked = isBookmarked,
                onSeekFraction = { viewModel.seekPercent(it) },
                onDecreaseFont = { viewModel.adjustFontSize(-2) },
                onIncreaseFont = { viewModel.adjustFontSize(2) },
                onOpenToc = { viewModel.toggleTocDrawer() },
                onToggleBookmark = { viewModel.toggleBookmark(currentSnippet) },
                onExitReader = onExit
            )
        }

        // Table of Contents Side Drawer (matching 05_toc.png)
        AnimatedVisibility(
            visible = uiState.showTocDrawer,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            TocSidePanel(
                chapters = uiState.parsedBook?.chapters.orEmpty(),
                currentChapterIndex = uiState.currentChapterIndex,
                alternativeLayout = settings.alternativeLayout,
                onToggleAlternativeLayout = { viewModel.toggleAlternativeLayout() },
                onSelectChapter = { viewModel.jumpToChapter(it) }
            )
        }

        // Brief toast indicator for bookmark toggle
        if (uiState.statusToast != null) {
            LaunchedEffect(uiState.statusToast) {
                kotlinx.coroutines.delay(1800)
                viewModel.clearToast()
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TvColors.Surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, TvColors.PrimaryBright),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = uiState.statusToast,
                    color = TvColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun PaginatedChapterView(
    chapter: ChapterContent?,
    parsedBook: ParsedBook,
    pageIndex: Int,
    settings: ReaderSettings,
    palette: ReaderPalette,
    onPagesMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chapter == null) return
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fontFamily = settings.fontFamily.toComposeFontFamily()
    val bodyStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = settings.fontSizeSp.sp,
        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
        color = palette.textPrimary
    )

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(100) }
        val heightPx = with(density) { maxHeight.roundToPx().coerceAtLeast(100) }

        val pages = remember(chapter, widthPx, heightPx, settings.fontSizeSp, settings.fontFamily, settings.lineHeightMultiplier) {
            paginateChapterBlocks(
                blocks = chapter.blocks,
                textMeasurer = textMeasurer,
                bodyStyle = bodyStyle,
                maxWidthPx = widthPx,
                maxHeightPx = heightPx,
                palette = palette
            )
        }

        LaunchedEffect(pages.size) {
            onPagesMeasured(pages.size)
        }

        val currentSlice = pages.getOrNull(pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
        if (currentSlice != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                if (currentSlice.imageKey != null) {
                    val bytes = parsedBook.imageEntries[currentSlice.imageKey]
                    val bmp = remember(currentSlice.imageKey) {
                        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Book illustration",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (currentSlice.content.isNotEmpty()) {
                    Text(
                        text = currentSlice.content,
                        style = bodyStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun paginateChapterBlocks(
    blocks: List<ContentBlock>,
    textMeasurer: TextMeasurer,
    bodyStyle: TextStyle,
    maxWidthPx: Int,
    maxHeightPx: Int,
    palette: ReaderPalette
): List<TextPageSlice> {
    if (blocks.isEmpty()) {
        return listOf(TextPageSlice(AnnotatedString("")))
    }

    val fullAnnotated = buildAnnotatedString {
        blocks.forEachIndexed { idx, block ->
            when (block) {
                is ContentBlock.Heading -> {
                    val scale = when (block.level) {
                        1 -> 1.45f
                        2 -> 1.28f
                        else -> 1.15f
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = bodyStyle.fontSize * scale,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    ) {
                        append(block.text)
                    }
                    append("\n\n")
                }
                is ContentBlock.Paragraph -> {
                    if (block.isQuote) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = palette.textSecondary)) {
                            append("“${block.text}”")
                        }
                    } else {
                        append("    ${block.text}")
                    }
                    if (idx < blocks.lastIndex) append("\n\n")
                }
                is ContentBlock.Divider -> {
                    withStyle(SpanStyle(color = palette.textSecondary)) {
                        append("•  •  •\n\n")
                    }
                }
                is ContentBlock.ImageBlock -> {
                    if (block.caption.isNotBlank()) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = palette.textSecondary)) {
                            append("[${block.caption}]\n\n")
                        }
                    }
                }
            }
        }
    }

    if (fullAnnotated.isEmpty()) {
        val firstImg = blocks.filterIsInstance<ContentBlock.ImageBlock>().firstOrNull()?.entryKey
        return listOf(TextPageSlice(AnnotatedString(""), imageKey = firstImg))
    }

    val result = textMeasurer.measure(
        text = fullAnnotated,
        style = bodyStyle,
        constraints = Constraints(maxWidth = maxWidthPx)
    )

    val slices = mutableListOf<TextPageSlice>()
    val totalLines = result.lineCount
    var lineIdx = 0
    while (lineIdx < totalLines) {
        val pageTop = result.getLineTop(lineIdx)
        var lastFittingLine = lineIdx
        while (lastFittingLine + 1 < totalLines) {
            val nextBottom = result.getLineBottom(lastFittingLine + 1)
            if (nextBottom - pageTop <= maxHeightPx) {
                lastFittingLine++
            } else {
                break
            }
        }
        val startOffset = result.getLineStart(lineIdx)
        val endOffset = result.getLineEnd(lastFittingLine, visibleEnd = true)
        val sliceText = fullAnnotated.subSequence(startOffset, endOffset)
        val imgForFirstPage = if (slices.isEmpty()) {
            blocks.filterIsInstance<ContentBlock.ImageBlock>().firstOrNull()?.entryKey
        } else null
        slices.add(TextPageSlice(sliceText, imageKey = imgForFirstPage))
        lineIdx = lastFittingLine + 1
    }

    return slices.ifEmpty { listOf(TextPageSlice(fullAnnotated)) }
}

@Composable
private fun ContinuousChapterView(
    chapter: ChapterContent?,
    parsedBook: ParsedBook,
    settings: ReaderSettings,
    palette: ReaderPalette,
    modifier: Modifier = Modifier
) {
    if (chapter == null) return
    val fontFamily = settings.fontFamily.toComposeFontFamily()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        items(chapter.blocks) { block ->
            when (block) {
                is ContentBlock.Heading -> Text(
                    text = block.text,
                    fontFamily = fontFamily,
                    fontSize = (settings.fontSizeSp * if (block.level == 1) 1.45f else 1.25f).sp,
                    lineHeight = (settings.fontSizeSp * 1.55f).sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                is ContentBlock.Paragraph -> Text(
                    text = block.text,
                    fontFamily = fontFamily,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                    fontStyle = if (block.isQuote) FontStyle.Italic else FontStyle.Normal,
                    color = if (block.isQuote) palette.textSecondary else palette.textPrimary
                )
                is ContentBlock.Divider -> HorizontalDivider(
                    color = palette.divider,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                is ContentBlock.ImageBlock -> {
                    val bytes = parsedBook.imageEntries[block.entryKey]
                    val bmp = remember(block.entryKey) {
                        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = block.caption.ifBlank { "Illustration" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlternativeDocumentLayout(
    parsedBook: ParsedBook,
    settings: ReaderSettings,
    palette: ReaderPalette,
    onSelectChapter: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontFamily = settings.fontFamily.toComposeFontFamily()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        if (parsedBook.description.isNotBlank()) {
            item {
                Text(
                    text = parsedBook.description,
                    fontFamily = fontFamily,
                    fontSize = (settings.fontSizeSp * 0.85f).sp,
                    color = palette.textSecondary
                )
            }
        }
        item {
            Text(
                text = parsedBook.title.uppercase(),
                fontFamily = fontFamily,
                fontSize = (settings.fontSizeSp * 1.5f).sp,
                lineHeight = (settings.fontSizeSp * 1.8f).sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "By ${parsedBook.author}",
                fontFamily = fontFamily,
                fontSize = (settings.fontSizeSp * 1.1f).sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            )
            HorizontalDivider(color = palette.divider)
        }
        item {
            Text(
                text = "CONTENTS",
                fontFamily = fontFamily,
                fontSize = (settings.fontSizeSp * 0.9f).sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        itemsIndexed(parsedBook.chapters) { index, chapter ->
            Text(
                text = chapter.title,
                fontFamily = fontFamily,
                fontSize = settings.fontSizeSp.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectChapter(index) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PdfReaderCanvas(
    pdfPath: String,
    pageIndex: Int,
    pageMode: PdfPageMode,
    invertDark: Boolean
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, pdfPath, pageIndex, invertDark) {
        value = withContext(Dispatchers.IO) {
            PdfAndCbzParser.renderPdfPageBitmap(
                pdfPath = pdfPath,
                pageIndex = pageIndex,
                targetWidth = 1400,
                invertForDark = invertDark
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp == null) {
            CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
        } else if (pageMode == PdfPageMode.ADAPTIVE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PDF page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CbzReaderCanvas(
    cbzPath: String,
    entryName: String
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, cbzPath, entryName) {
        value = withContext(Dispatchers.IO) {
            PdfAndCbzParser.loadCbzPageBitmap(cbzPath, entryName)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp == null) {
            CircularProgressIndicator(color = TvColors.PrimaryTextAccent)
        } else {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Comic page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReaderToolbarOverlay(
    uiState: ReaderUiState,
    snippet: String,
    isBookmarked: Boolean,
    onSeekFraction: (Float) -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onOpenToc: () -> Unit,
    onToggleBookmark: () -> Unit,
    onExitReader: () -> Unit
) {
    val parsed = uiState.parsedBook
    val (pageNum, totalNum, pct) = computeProgressNumbers(uiState, parsed)

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
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Snippet box matching 04_reader_toolbar.png
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

            // Progress Slider
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

            // Title + page/total + %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.book?.title?.uppercase().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$pageNum/$totalNum   $pct%",
                    style = MaterialTheme.typography.labelLarge,
                    color = TvColors.PrimaryTextAccent
                )
            }

            // Controls row: Back | A- Aa A+ | TOC | Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvActionButton(
                        text = "",
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        filled = true,
                        onClick = onOpenToc,
                        tag = "reader_toc_button"
                    )
                    TvActionButton(
                        text = "",
                        icon = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        filled = true,
                        onClick = onToggleBookmark,
                        tag = "reader_bookmark_button"
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

@Composable
private fun TocSidePanel(
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

private fun computeProgressNumbers(
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

private fun buildFooterText(
    uiState: ReaderUiState,
    parsed: ParsedBook,
    isPdfOrCbz: Boolean
): String {
    if (isPdfOrCbz) {
        val total = if (uiState.format == BookFormat.PDF) parsed.pdfPageCount else parsed.cbzImageEntries.size
        val page = uiState.currentPageIndex + 1
        return "Page $page/$total"
    }
    val chCount = parsed.chapters.size.coerceAtLeast(1)
    val chNum = uiState.currentChapterIndex + 1
    val totalPages = uiState.totalPagesInChapter.coerceAtLeast(1)
    val pageNum = (uiState.currentPageIndex + 1).coerceIn(1, totalPages)
    val remainingWords = parsed.chapters
        .drop(uiState.currentChapterIndex)
        .sumOf { it.rawText.length / 5 }
    val minsLeft = ceil(remainingWords / 220.0).toInt().coerceAtLeast(1)
    return "Ch $chNum/$chCount  •  $pageNum/$totalPages  •  $minsLeft min left"
}
