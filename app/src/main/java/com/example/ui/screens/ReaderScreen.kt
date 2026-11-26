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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.theme.ReaderPalette
import com.example.ui.theme.TvColors
import com.example.ui.theme.resolveReaderPalette
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.viewmodel.ReaderUiState
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var livePageSnippet by remember { mutableStateOf("") }

    val anyOverlayOpen = uiState.showToolbar ||
        uiState.showTocDrawer ||
        uiState.showBookmarksDrawer ||
        uiState.showSettingsPanel ||
        uiState.showPageJumpDialog

    LaunchedEffect(anyOverlayOpen, uiState.isLoading) {
        if (!anyOverlayOpen && !uiState.isLoading) {
            runCatching { readerFocusRequester.requestFocus() }
        }
    }

    // Always persist reading position when leaving screen or backgrounding app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.saveProgressNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.saveProgressNow()
        }
    }

    BackHandler {
        when {
            uiState.showPageJumpDialog -> viewModel.closePageJumpDialog()
            anyOverlayOpen -> viewModel.closeOverlays()
            else -> {
                viewModel.saveProgressNow()
                onExit()
            }
        }
    }

    val currentChapter = uiState.parsedBook?.chapters?.getOrNull(uiState.currentChapterIndex)
    val currentSnippet = remember(
        uiState.currentChapterIndex,
        uiState.currentPageIndex,
        currentChapter,
        livePageSnippet
    ) {
        livePageSnippet.ifBlank {
            currentChapter?.rawText?.take(180) ?: uiState.book?.title.orEmpty()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .focusRequester(readerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (uiState.showPageJumpDialog) {
                    return@onPreviewKeyEvent false
                }
                if (anyOverlayOpen) {
                    if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape || keyEvent.key == Key.Menu) {
                        viewModel.closeOverlays()
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }
                val digitChar = keyToDigitChar(keyEvent.key)
                if (digitChar != null) {
                    viewModel.openPageJumpDialog(digitChar.toString())
                    return@onPreviewKeyEvent true
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
                        .pointerInput(anyOverlayOpen) {
                            detectTapGestures { offset ->
                                if (anyOverlayOpen) {
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
                            livePageSnippet = "Page ${uiState.currentPageIndex + 1} of ${parsed.pdfPageCount}"
                            PdfReaderCanvas(
                                pdfPath = parsed.pdfFilePath.orEmpty(),
                                pageIndex = uiState.currentPageIndex,
                                pageMode = settings.pdfPageMode,
                                invertDark = settings.theme == ReaderTheme.AMOLED || settings.theme == ReaderTheme.DARK
                            )
                        }
                        uiState.format == BookFormat.CBZ -> {
                            val entryName = parsed.cbzImageEntries.getOrNull(uiState.currentPageIndex).orEmpty()
                            livePageSnippet = "Page ${uiState.currentPageIndex + 1} of ${parsed.cbzImageEntries.size}"
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
                                initialScrollBlockIndex = uiState.scrollBlockIndex,
                                settings = settings,
                                palette = palette,
                                onScrollBlockChanged = { idx, snippet ->
                                    if (snippet.isNotBlank()) livePageSnippet = snippet
                                    viewModel.updateScrollBlock(idx)
                                },
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
                                onVisibleSnippetChanged = { snippet -> livePageSnippet = snippet },
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

                    // Bottom status indicator (tappable to jump to page)
                    if (!settings.focusMode && !uiState.showToolbar) {
                        val footerText = buildFooterText(uiState, parsed, isPdfOrCbz)
                        Text(
                            text = footerText,
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .clickable { viewModel.openPageJumpDialog() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .testTag("reader_footer_status")
                        )
                    }
                }
            }
        }

        // Brightness dimming overlay applied ONLY over the book itself (below overlays)
        val dimAlpha = ((100 - settings.brightnessPercent).coerceIn(0, 95) / 100f) * 0.90f
        if (dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        // Reader Toolbar Overlay
        AnimatedVisibility(
            visible = uiState.showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val hasBookmarkOnPage = bookmarks.any {
                it.chapterIndex == uiState.currentChapterIndex && it.pageIndex == uiState.currentPageIndex
            }
            ReaderToolbarOverlay(
                uiState = uiState,
                snippet = currentSnippet,
                bookmarkCount = bookmarks.size,
                hasBookmarkOnPage = hasBookmarkOnPage,
                onSeekFraction = { viewModel.seekPercent(it) },
                onDecreaseFont = { viewModel.adjustFontSize(-2) },
                onIncreaseFont = { viewModel.adjustFontSize(2) },
                onOpenPageJump = { viewModel.openPageJumpDialog() },
                onOpenSettings = { viewModel.toggleSettingsPanel() },
                onOpenToc = { viewModel.toggleTocDrawer() },
                onAddBookmark = { viewModel.addBookmark(currentSnippet) },
                onOpenBookmarks = { viewModel.toggleBookmarksDrawer() },
                onExitReader = {
                    viewModel.saveProgressNow()
                    onExit()
                }
            )
        }

        // Table of Contents Side Drawer
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

        // In-Reader Settings Panel (with live Book Brightness)
        AnimatedVisibility(
            visible = uiState.showSettingsPanel,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            InReaderSettingsPanel(
                settings = settings,
                onSetBrightness = { viewModel.setBrightness(it) },
                onSetTheme = { viewModel.setTheme(it) },
                onAdjustFontSize = { viewModel.adjustFontSize(it) },
                onSetLineHeight = { viewModel.setLineHeight(it) },
                onSetReadingMode = { viewModel.setReadingMode(it) },
                onSetFocusMode = { viewModel.setFocusMode(it) },
                onClose = { viewModel.closeOverlays() }
            )
        }

        // Multiple Bookmarks Side Panel
        AnimatedVisibility(
            visible = uiState.showBookmarksDrawer,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BookmarksSidePanel(
                bookmarks = bookmarks,
                currentChapterIndex = uiState.currentChapterIndex,
                currentPageIndex = uiState.currentPageIndex,
                onAddQuickBookmark = { viewModel.addBookmark(currentSnippet) },
                onAddBookmarkWithLabel = { label -> viewModel.addBookmark(currentSnippet, label) },
                onSelectBookmark = { bm -> viewModel.jumpToBookmark(bm) },
                onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                onClose = { viewModel.closeOverlays() }
            )
        }

        // D-pad Go-To-Page Dialog
        if (uiState.showPageJumpDialog) {
            val (pageNum, totalNum, _) = computeProgressNumbers(uiState, uiState.parsedBook)
            val isTextFlow = uiState.format != BookFormat.PDF && uiState.format != BookFormat.CBZ
            GoToPageDialog(
                currentPage = pageNum,
                totalPages = totalNum,
                currentChapterIndex = uiState.currentChapterIndex,
                chapterCount = uiState.parsedBook?.chapters?.size ?: 1,
                isTextFlow = isTextFlow,
                initialDigits = uiState.initialPageJumpDigits,
                onJumpToPage = { pageIdx -> viewModel.jumpToPage(pageIdx) },
                onPreviousChapter = { viewModel.previousChapter() },
                onNextChapter = { viewModel.nextChapter() },
                onDismiss = { viewModel.closePageJumpDialog() }
            )
        }

        // Brief toast indicator
        if (uiState.statusToast != null) {
            LaunchedEffect(uiState.statusToast) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearToast()
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TvColors.Surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, TvColors.PrimaryBright),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .testTag("reader_status_toast")
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
    onVisibleSnippetChanged: (String) -> Unit,
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

        LaunchedEffect(currentSlice) {
            val snippet = currentSlice?.content?.text?.trim()?.replace(Regex("\\s+"), " ")?.take(180).orEmpty()
            if (snippet.isNotBlank()) {
                onVisibleSnippetChanged(snippet)
            }
        }

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
    initialScrollBlockIndex: Int,
    settings: ReaderSettings,
    palette: ReaderPalette,
    onScrollBlockChanged: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chapter == null) return
    val fontFamily = settings.fontFamily.toComposeFontFamily()
    val safeInitial = initialScrollBlockIndex.coerceIn(0, (chapter.blocks.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitial)

    LaunchedEffect(chapter.index, safeInitial) {
        if (listState.firstVisibleItemIndex != safeInitial && safeInitial > 0) {
            listState.scrollToItem(safeInitial)
        }
    }

    LaunchedEffect(listState, chapter.blocks) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                val block = chapter.blocks.getOrNull(idx)
                val snippet = when (block) {
                    is ContentBlock.Heading -> block.text
                    is ContentBlock.Paragraph -> block.text
                    is ContentBlock.ImageBlock -> block.caption
                    else -> ""
                }.take(180)
                onScrollBlockChanged(idx, snippet)
            }
    }

    LazyColumn(
        state = listState,
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
