package com.example.data.parser

import android.util.Base64
import android.util.Xml
import com.example.data.model.ChapterContent
import com.example.data.model.ContentBlock
import com.example.data.model.ParsedBook
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

object Fb2Parser {

    fun parse(file: File, extractContent: Boolean = true): ParsedBook {
        val xmlText = if (file.name.endsWith(".zip", ignoreCase = true)) {
            ZipFile(file).use { zip ->
                val fb2Entry = zip.entries().asSequence()
                    .firstOrNull { it.name.endsWith(".fb2", ignoreCase = true) }
                    ?: zip.entries().asSequence().firstOrNull { !it.isDirectory }
                fb2Entry?.let { zip.getInputStream(it).bufferedReader(Charsets.UTF_8).readText() }
                    ?: ""
            }
        } else {
            file.readText(Charsets.UTF_8)
        }

        if (xmlText.isBlank()) {
            return ParsedBook(title = file.nameWithoutExtension, author = "Unknown Author")
        }

        var bookTitle = file.nameWithoutExtension
        var firstName = ""
        var lastName = ""
        var coverImageId: String? = null
        val binaries = mutableMapOf<String, ByteArray>()
        val chapters = mutableListOf<ChapterContent>()

        var inTitleInfo = false
        var inAuthor = false
        var inBody = false
        var inCoverpage = false
        var sectionDepth = 0
        var inTitleTag = false
        var currentChapterTitle = ""
        var currentBlocks = mutableListOf<ContentBlock>()
        var currentBinaryId: String? = null
        val binaryBuffer = StringBuilder()

        fun flushChapter() {
            if (currentBlocks.isNotEmpty()) {
                val title = currentChapterTitle.ifBlank { "Section ${chapters.size + 1}" }
                chapters.add(
                    ChapterContent(
                        index = chapters.size,
                        title = title,
                        blocks = currentBlocks,
                        rawText = TextMarkdownHtmlParser.blocksToRawText(currentBlocks)
                    )
                )
                currentBlocks = mutableListOf()
                currentChapterTitle = ""
            }
        }

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xmlText))
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "title-info" -> inTitleInfo = true
                        "author" -> if (inTitleInfo) inAuthor = true
                        "coverpage" -> if (inTitleInfo) inCoverpage = true
                        "first-name" -> if (inAuthor) firstName = safeNextText(parser)
                        "last-name" -> if (inAuthor) lastName = safeNextText(parser)
                        "book-title" -> if (inTitleInfo) {
                            val t = safeNextText(parser)
                            if (t.isNotBlank()) bookTitle = t
                        }
                        "image" -> {
                            val href = (0 until parser.attributeCount)
                                .firstOrNull { parser.getAttributeName(it).endsWith("href") }
                                ?.let { parser.getAttributeValue(it) }
                                ?.removePrefix("#")
                            if (inCoverpage && href != null && coverImageId == null) {
                                coverImageId = href
                            } else if (inBody && extractContent && href != null) {
                                currentBlocks.add(ContentBlock.ImageBlock(href))
                            }
                        }
                        "body" -> if (extractContent) inBody = true
                        "section" -> if (inBody) {
                            sectionDepth++
                            if (sectionDepth == 1) flushChapter()
                        }
                        "title" -> if (inBody) inTitleTag = true
                        "p" -> if (inBody) {
                            val text = safeReadInnerText(parser, "p")
                            if (text.isNotBlank()) {
                                if (inTitleTag) {
                                    if (currentChapterTitle.isBlank()) currentChapterTitle = text
                                    currentBlocks.add(ContentBlock.Heading(2, text))
                                } else {
                                    currentBlocks.add(ContentBlock.Paragraph(text))
                                }
                            }
                        }
                        "cite" -> if (inBody) {
                            val text = safeReadInnerText(parser, "cite")
                            if (text.isNotBlank()) currentBlocks.add(ContentBlock.Paragraph(text, isQuote = true))
                        }
                        "empty-line" -> if (inBody && currentBlocks.isNotEmpty()) {
                            currentBlocks.add(ContentBlock.Divider)
                        }
                        "binary" -> {
                            val id = parser.getAttributeValue(null, "id")
                            if (!id.isNullOrBlank()) {
                                currentBinaryId = id
                                binaryBuffer.clear()
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (currentBinaryId != null) {
                        binaryBuffer.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "title-info" -> inTitleInfo = false
                        "author" -> inAuthor = false
                        "coverpage" -> inCoverpage = false
                        "title" -> inTitleTag = false
                        "section" -> if (inBody) {
                            if (sectionDepth == 1) flushChapter()
                            sectionDepth = (sectionDepth - 1).coerceAtLeast(0)
                        }
                        "body" -> if (inBody) {
                            flushChapter()
                            inBody = false
                        }
                        "binary" -> {
                            val id = currentBinaryId
                            if (id != null && (extractContent || id == coverImageId)) {
                                runCatching {
                                    val cleaned = binaryBuffer.toString().replace("\\s".toRegex(), "")
                                    binaries[id] = Base64.decode(cleaned, Base64.DEFAULT)
                                }
                            }
                            currentBinaryId = null
                            binaryBuffer.clear()
                        }
                    }
                }
            }
            event = parser.next()
        }

        val authorFull = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { "Unknown Author" }
        val coverBytes = coverImageId?.let { binaries[it] } ?: binaries.values.firstOrNull()

        return ParsedBook(
            title = bookTitle,
            author = authorFull,
            chapters = chapters.ifEmpty {
                listOf(ChapterContent(0, bookTitle, listOf(ContentBlock.Paragraph("No body content found in FB2.")), ""))
            },
            coverBytes = coverBytes,
            imageEntries = binaries
        )
    }

    private fun safeNextText(parser: XmlPullParser): String =
        runCatching { parser.nextText().orEmpty().trim() }.getOrDefault("")

    private fun safeReadInnerText(parser: XmlPullParser, endTag: String): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            val next = parser.next()
            when (next) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> {
                    depth--
                    if (depth == 0 && parser.name.substringAfterLast(':') == endTag) break
                }
                XmlPullParser.TEXT -> sb.append(parser.text)
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
