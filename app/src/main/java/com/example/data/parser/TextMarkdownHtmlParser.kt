package com.example.data.parser

import com.example.data.model.ChapterContent
import com.example.data.model.ContentBlock
import com.example.data.model.ParsedBook
import java.io.File
import java.net.URLDecoder
import java.util.Locale

object TextMarkdownHtmlParser {

    fun parseTxt(file: File): ParsedBook {
        val raw = file.readText(Charsets.UTF_8)
        val lines = raw.replace("\r\n", "\n").split("\n")
        val titleGuess = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
            .ifBlank { "Untitled Text" }

        val chapters = mutableListOf<ChapterContent>()
        var currentTitle = "Start"
        var currentParagraphs = mutableListOf<String>()
        val paragraphBuilder = StringBuilder()

        fun flushParagraph() {
            val text = paragraphBuilder.toString().trim()
            if (text.isNotEmpty()) {
                currentParagraphs.add(text)
            }
            paragraphBuilder.clear()
        }

        fun flushChapter() {
            flushParagraph()
            if (currentParagraphs.isNotEmpty()) {
                val blocks = buildList {
                    if (currentTitle != "Start" || chapters.isNotEmpty()) {
                        add(ContentBlock.Heading(2, currentTitle))
                    }
                    currentParagraphs.forEach { add(ContentBlock.Paragraph(it)) }
                }
                chapters.add(
                    ChapterContent(
                        index = chapters.size,
                        title = currentTitle,
                        blocks = blocks,
                        rawText = currentParagraphs.joinToString("\n\n")
                    )
                )
                currentParagraphs = mutableListOf()
            }
        }

        val chapterRegex = Regex(
            pattern = "^\\s*(CHAPTER|Chapter|BOOK|Book|PART|Part|SECTION|Section)\\s+[IVXLCDM0-9]+.*$"
        )

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                flushParagraph()
                if (currentParagraphs.size >= 45) {
                    flushChapter()
                    currentTitle = "Section ${chapters.size + 1}"
                }
            } else if (chapterRegex.matches(trimmed) && paragraphBuilder.isEmpty()) {
                flushChapter()
                currentTitle = trimmed
            } else {
                if (paragraphBuilder.isNotEmpty()) paragraphBuilder.append(' ')
                paragraphBuilder.append(trimmed)
            }
        }
        flushChapter()

        if (chapters.isEmpty()) {
            chapters.add(
                ChapterContent(
                    index = 0,
                    title = titleGuess,
                    blocks = listOf(ContentBlock.Paragraph(raw.trim().ifEmpty { "(Empty document)" })),
                    rawText = raw
                )
            )
        }

        return ParsedBook(
            title = titleGuess,
            author = "Plain Text",
            chapters = chapters
        )
    }

    fun parseMarkdown(file: File): ParsedBook {
        val raw = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val lines = raw.split("\n")
        val fallbackTitle = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
        var detectedTitle: String? = null

        val chapters = mutableListOf<ChapterContent>()
        var currentChapterTitle = fallbackTitle
        var blocks = mutableListOf<ContentBlock>()
        val paraBuffer = StringBuilder()

        fun flushPara() {
            val text = cleanMarkdownInline(paraBuffer.toString().trim())
            if (text.isNotEmpty()) {
                blocks.add(ContentBlock.Paragraph(text))
            }
            paraBuffer.clear()
        }

        fun flushChapter(nextTitle: String) {
            flushPara()
            if (blocks.isNotEmpty()) {
                val rawCh = blocks.joinToString("\n\n") {
                    when (it) {
                        is ContentBlock.Heading -> it.text
                        is ContentBlock.Paragraph -> it.text
                        else -> ""
                    }
                }
                chapters.add(
                    ChapterContent(
                        index = chapters.size,
                        title = currentChapterTitle,
                        blocks = blocks,
                        rawText = rawCh
                    )
                )
                blocks = mutableListOf()
            }
            currentChapterTitle = nextTitle
        }

        var inCodeBlock = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                flushPara()
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) {
                blocks.add(ContentBlock.Paragraph(line, isQuote = true))
                continue
            }
            when {
                trimmed.isEmpty() -> flushPara()
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    flushPara()
                    blocks.add(ContentBlock.Divider)
                }
                trimmed.startsWith("# ") -> {
                    val h = cleanMarkdownInline(trimmed.removePrefix("# ").trim())
                    if (detectedTitle == null) detectedTitle = h
                    if (blocks.isNotEmpty()) flushChapter(h) else currentChapterTitle = h
                    blocks.add(ContentBlock.Heading(1, h))
                }
                trimmed.startsWith("## ") -> {
                    val h = cleanMarkdownInline(trimmed.removePrefix("## ").trim())
                    if (blocks.size > 4) flushChapter(h)
                    blocks.add(ContentBlock.Heading(2, h))
                }
                trimmed.startsWith("### ") -> {
                    flushPara()
                    blocks.add(ContentBlock.Heading(3, cleanMarkdownInline(trimmed.removePrefix("### ").trim())))
                }
                trimmed.startsWith("> ") -> {
                    flushPara()
                    blocks.add(ContentBlock.Paragraph(cleanMarkdownInline(trimmed.removePrefix("> ").trim()), isQuote = true))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    flushPara()
                    blocks.add(ContentBlock.Paragraph("• ${cleanMarkdownInline(trimmed.drop(2).trim())}"))
                }
                else -> {
                    if (paraBuffer.isNotEmpty()) paraBuffer.append(' ')
                    paraBuffer.append(trimmed)
                }
            }
        }
        flushChapter(currentChapterTitle)

        if (chapters.isEmpty()) {
            chapters.add(
                ChapterContent(
                    index = 0,
                    title = detectedTitle ?: fallbackTitle,
                    blocks = listOf(ContentBlock.Paragraph(raw.ifBlank { "(Empty Markdown document)" })),
                    rawText = raw
                )
            )
        }

        return ParsedBook(
            title = detectedTitle ?: fallbackTitle,
            author = "Markdown Document",
            chapters = chapters
        )
    }

    fun parseHtmlFile(file: File): ParsedBook {
        val html = file.readText(Charsets.UTF_8)
        val title = extractHtmlTitle(html)
            ?: file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
        val blocks = extractBlocksFromHtml(html, basePath = "", availableImages = emptySet())
        val chapters = splitHtmlBlocksIntoChapters(blocks, defaultTitle = title)
        return ParsedBook(
            title = title,
            author = "HTML Document",
            chapters = chapters
        )
    }

    fun extractHtmlTitle(html: String): String? {
        val m = Regex("(?is)<title[^>]*>(.*?)</title>").find(html) ?: return null
        return decodeHtmlEntities(m.groupValues[1].replace(Regex("<[^>]+>"), "")).trim()
            .takeIf { it.isNotEmpty() }
    }

    fun extractBlocksFromHtml(
        html: String,
        basePath: String,
        availableImages: Set<String>
    ): List<ContentBlock> {
        val cleaned = html
            .replace(Regex("(?is)<head[^>]*>.*?</head>"), " ")
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")

        val blocks = mutableListOf<ContentBlock>()
        val tokenRegex = Regex(
            pattern = "(?is)<(h[1-4]|p|blockquote|li|div|hr|img|svg)([^>]*)>(.*?)</\\1>|<(hr|img)([^>]*)/?>"
        )

        var lastEnd = 0
        for (match in tokenRegex.findAll(cleaned)) {
            if (match.range.first > lastEnd) {
                val between = stripTagsAndDecode(cleaned.substring(lastEnd, match.range.first))
                if (between.isNotBlank() && blocks.isEmpty()) {
                    blocks.add(ContentBlock.Paragraph(between))
                }
            }
            lastEnd = match.range.last + 1

            val tag = (match.groupValues[1].ifEmpty { match.groupValues[4] }).lowercase(Locale.ROOT)
            val attrs = match.groupValues[2].ifEmpty { match.groupValues[5] }
            val inner = match.groupValues[3]

            when (tag) {
                "h1", "h2", "h3", "h4" -> {
                    val text = stripTagsAndDecode(inner)
                    if (text.isNotBlank()) {
                        blocks.add(ContentBlock.Heading(tag.drop(1).toInt(), text))
                    }
                }
                "hr" -> blocks.add(ContentBlock.Divider)
                "img" -> {
                    val srcMatch = Regex("(?i)\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']").find(attrs)
                    val resolved = srcMatch?.groupValues?.get(1)?.let { resolveRelativeEntry(basePath, it, availableImages) }
                    if (resolved != null) {
                        val alt = Regex("(?i)\\balt\\s*=\\s*[\"']([^\"']*)[\"']").find(attrs)?.groupValues?.get(1).orEmpty()
                        blocks.add(ContentBlock.ImageBlock(resolved, decodeHtmlEntities(alt)))
                    }
                }
                "blockquote" -> {
                    val text = stripTagsAndDecode(inner)
                    if (text.isNotBlank()) blocks.add(ContentBlock.Paragraph(text, isQuote = true))
                }
                "li" -> {
                    val text = stripTagsAndDecode(inner)
                    if (text.isNotBlank()) blocks.add(ContentBlock.Paragraph("• $text"))
                }
                "p" -> {
                    val nestedImg = Regex("(?is)<img([^>]*)/?>").find(inner)
                    if (nestedImg != null) {
                        val srcMatch = Regex("(?i)\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']").find(nestedImg.groupValues[1])
                        val resolved = srcMatch?.groupValues?.get(1)?.let { resolveRelativeEntry(basePath, it, availableImages) }
                        if (resolved != null) {
                            blocks.add(ContentBlock.ImageBlock(resolved))
                        }
                    }
                    val text = stripTagsAndDecode(inner)
                    if (text.isNotBlank()) blocks.add(ContentBlock.Paragraph(text))
                }
                "div" -> {
                    if (inner.contains("<p", ignoreCase = true) || inner.contains("<h", ignoreCase = true)) {
                        blocks.addAll(extractBlocksFromHtml(inner, basePath, availableImages))
                    } else {
                        val text = stripTagsAndDecode(inner)
                        if (text.isNotBlank()) blocks.add(ContentBlock.Paragraph(text))
                    }
                }
            }
        }

        if (blocks.isEmpty()) {
            val fallback = stripTagsAndDecode(cleaned)
            if (fallback.isNotBlank()) {
                fallback.split(Regex("\\n{2,}")).forEach { part ->
                    val cleanPart = part.trim()
                    if (cleanPart.isNotEmpty()) blocks.add(ContentBlock.Paragraph(cleanPart))
                }
            }
        }
        return blocks
    }

    private fun splitHtmlBlocksIntoChapters(
        blocks: List<ContentBlock>,
        defaultTitle: String
    ): List<ChapterContent> {
        if (blocks.isEmpty()) {
            return listOf(ChapterContent(0, defaultTitle, listOf(ContentBlock.Paragraph("(Empty document)")), ""))
        }
        val chapters = mutableListOf<ChapterContent>()
        var currentTitle = defaultTitle
        var currentBlocks = mutableListOf<ContentBlock>()

        for (block in blocks) {
            if (block is ContentBlock.Heading && block.level <= 2 && currentBlocks.size >= 8) {
                chapters.add(
                    ChapterContent(
                        index = chapters.size,
                        title = currentTitle,
                        blocks = currentBlocks,
                        rawText = blocksToRawText(currentBlocks)
                    )
                )
                currentTitle = block.text
                currentBlocks = mutableListOf(block)
            } else {
                if (block is ContentBlock.Heading && block.level == 1 && currentBlocks.isEmpty()) {
                    currentTitle = block.text
                }
                currentBlocks.add(block)
            }
        }
        if (currentBlocks.isNotEmpty()) {
            chapters.add(
                ChapterContent(
                    index = chapters.size,
                    title = currentTitle,
                    blocks = currentBlocks,
                    rawText = blocksToRawText(currentBlocks)
                )
            )
        }
        return chapters
    }

    fun blocksToRawText(blocks: List<ContentBlock>): String =
        blocks.mapNotNull {
            when (it) {
                is ContentBlock.Heading -> it.text
                is ContentBlock.Paragraph -> it.text
                is ContentBlock.ImageBlock -> it.caption.takeIf { c -> c.isNotBlank() }
                ContentBlock.Divider -> null
            }
        }.joinToString("\n\n")

    fun resolveRelativeEntry(baseFile: String, href: String, entries: Set<String>): String? {
        val cleanHref = runCatching { URLDecoder.decode(href.substringBefore('#'), "UTF-8") }
            .getOrDefault(href.substringBefore('#'))
            .trim()
            .removePrefix("./")
        if (cleanHref.isEmpty()) return null
        if (entries.contains(cleanHref)) return cleanHref

        val baseDir = baseFile.substringBeforeLast('/', "")
        val combined = if (baseDir.isNotEmpty()) "$baseDir/$cleanHref" else cleanHref
        val parts = mutableListOf<String>()
        for (segment in combined.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts.add(segment)
            }
        }
        val normalized = parts.joinToString("/")
        if (entries.contains(normalized)) return normalized
        return entries.firstOrNull { it.endsWith("/$cleanHref", ignoreCase = true) || it.equals(cleanHref, ignoreCase = true) }
    }

    fun stripTagsAndDecode(htmlFragment: String): String {
        val withBreaks = htmlFragment
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
        return decodeHtmlEntities(withBreaks)
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n\\s+"), "\n")
            .trim()
    }

    private fun cleanMarkdownInline(text: String): String =
        text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("_(.+?)_"), "$1")
            .replace(Regex("`(.+?)`"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")

    fun decodeHtmlEntities(input: String): String {
        var s = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
        s = Regex("&#(\\d+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull()?.let { code -> Char(code).toString() } ?: m.value
        }
        s = Regex("&#x([0-9a-fA-F]+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull(16)?.let { code -> Char(code).toString() } ?: m.value
        }
        return s
    }
}
