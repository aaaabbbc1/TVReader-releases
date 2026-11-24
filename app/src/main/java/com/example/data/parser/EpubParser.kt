package com.example.data.parser

import android.util.Xml
import com.example.data.model.ChapterContent
import com.example.data.model.ContentBlock
import com.example.data.model.ParsedBook
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

object EpubParser {

    private data class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String,
        val properties: String = ""
    )

    fun parse(file: File, extractContent: Boolean = true): ParsedBook {
        ZipFile(file).use { zip ->
            val entriesMap = zip.entries().asSequence()
                .filter { !it.isDirectory }
                .associateBy { it.name }

            val opfPath = findOpfPath(zip, entriesMap.keys)
                ?: return fallbackBook(file)
            val opfXml = zip.getInputStream(entriesMap[opfPath] ?: return fallbackBook(file))
                .bufferedReader(Charsets.UTF_8).readText()

            var title = file.nameWithoutExtension
            var author = "Unknown Author"
            var description = ""
            var coverId: String? = null
            val manifest = mutableMapOf<String, ManifestItem>()
            val spineIds = mutableListOf<String>()
            var ncxId: String? = null
            var navHref: String? = null

            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(opfXml))
            }

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "title" -> {
                            val text = safeNextText(parser)
                            if (text.isNotBlank() && title == file.nameWithoutExtension) {
                                title = text
                            }
                        }
                        "creator" -> {
                            val text = safeNextText(parser)
                            if (text.isNotBlank() && author == "Unknown Author") {
                                author = text
                            }
                        }
                        "description" -> {
                            val text = safeNextText(parser)
                            if (text.isNotBlank() && description.isBlank()) {
                                description = TextMarkdownHtmlParser.stripTagsAndDecode(text)
                            }
                        }
                        "meta" -> {
                            val name = parser.getAttributeValue(null, "name")
                            val content = parser.getAttributeValue(null, "content")
                            if (name == "cover" && !content.isNullOrBlank()) {
                                coverId = content
                            }
                        }
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id").orEmpty()
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            val mediaType = parser.getAttributeValue(null, "media-type").orEmpty()
                            val props = parser.getAttributeValue(null, "properties").orEmpty()
                            if (id.isNotBlank() && href.isNotBlank()) {
                                val resolved = TextMarkdownHtmlParser.resolveRelativeEntry(
                                    opfPath,
                                    href,
                                    entriesMap.keys
                                ) ?: href
                                manifest[id] = ManifestItem(id, resolved, mediaType, props)
                                if (props.contains("cover-image") && coverId == null) {
                                    coverId = id
                                }
                                if (props.contains("nav") && navHref == null) {
                                    navHref = resolved
                                }
                                if (mediaType == "application/x-dtbncx+xml" && ncxId == null) {
                                    ncxId = id
                                }
                            }
                        }
                        "spine" -> {
                            val tocAttr = parser.getAttributeValue(null, "toc")
                            if (!tocAttr.isNullOrBlank()) ncxId = tocAttr
                        }
                        "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (!idref.isNullOrBlank()) spineIds.add(idref)
                        }
                    }
                }
                event = parser.next()
            }

            // Extract cover image bytes
            val coverHref = coverId?.let { manifest[it]?.href }
                ?: manifest.values.firstOrNull {
                    it.mediaType.startsWith("image/") &&
                        it.href.contains("cover", ignoreCase = true)
                }?.href
                ?: entriesMap.keys.firstOrNull {
                    it.contains("cover", ignoreCase = true) &&
                        (it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true))
                }

            val coverBytes = coverHref?.let { href ->
                entriesMap[href]?.let { entry -> zip.getInputStream(entry).readBytes() }
            }

            if (!extractContent) {
                return ParsedBook(
                    title = title,
                    author = author,
                    description = description,
                    coverBytes = coverBytes
                )
            }

            val tocTitles = mutableMapOf<String, String>()
            ncxId?.let { manifest[it]?.href }?.let { ncxPath ->
                entriesMap[ncxPath]?.let { entry ->
                    val ncxXml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                    tocTitles.putAll(parseNcxTitles(ncxXml, ncxPath, entriesMap.keys))
                }
            }
            if (tocTitles.isEmpty() && navHref != null) {
                entriesMap[navHref]?.let { entry ->
                    val navHtml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                    tocTitles.putAll(parseNavTitles(navHtml, navHref, entriesMap.keys))
                }
            }

            val imageEntries = mutableMapOf<String, ByteArray>()
            manifest.values.filter { it.mediaType.startsWith("image/") }.forEach { item ->
                entriesMap[item.href]?.let { entry ->
                    if (entry.size in 1..(8 * 1024 * 1024)) {
                        imageEntries[item.href] = zip.getInputStream(entry).readBytes()
                    }
                }
            }

            val chapters = mutableListOf<ChapterContent>()
            val spineItems = if (spineIds.isNotEmpty()) {
                spineIds.mapNotNull { manifest[it] }
            } else {
                manifest.values.filter {
                    it.mediaType.contains("html") || it.href.endsWith(".xhtml", true) || it.href.endsWith(".html", true)
                }
            }

            for (item in spineItems) {
                val entry = entriesMap[item.href] ?: continue
                val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                val blocks = TextMarkdownHtmlParser.extractBlocksFromHtml(
                    html = html,
                    basePath = item.href,
                    availableImages = imageEntries.keys
                )
                if (blocks.isEmpty()) continue

                val firstHeading = blocks.filterIsInstance<ContentBlock.Heading>().firstOrNull()?.text
                val chTitle = tocTitles[item.href]
                    ?: firstHeading
                    ?: TextMarkdownHtmlParser.extractHtmlTitle(html)
                    ?: "Chapter ${chapters.size + 1}"

                chapters.add(
                    ChapterContent(
                        index = chapters.size,
                        title = chTitle,
                        blocks = blocks,
                        rawText = TextMarkdownHtmlParser.blocksToRawText(blocks)
                    )
                )
            }

            if (chapters.isEmpty()) {
                chapters.add(
                    ChapterContent(
                        index = 0,
                        title = title,
                        blocks = listOf(ContentBlock.Paragraph("No readable text chapters were found in this EPUB.")),
                        rawText = ""
                    )
                )
            }

            return ParsedBook(
                title = title,
                author = author,
                description = description,
                chapters = chapters,
                coverBytes = coverBytes,
                imageEntries = imageEntries
            )
        }
    }

    private fun findOpfPath(zip: ZipFile, entries: Set<String>): String? {
        val containerEntry = entries.firstOrNull { it.equals("META-INF/container.xml", ignoreCase = true) }
        if (containerEntry != null) {
            val xml = zip.getInputStream(zip.getEntry(containerEntry)).bufferedReader(Charsets.UTF_8).readText()
            val match = Regex("full-path\\s*=\\s*[\"']([^\"']+)[\"']").find(xml)
            val path = match?.groupValues?.get(1)
            if (path != null && entries.contains(path)) return path
        }
        return entries.firstOrNull { it.endsWith(".opf", ignoreCase = true) }
    }

    private fun parseNcxTitles(
        ncxXml: String,
        ncxPath: String,
        entries: Set<String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(ncxXml))
        }
        var currentLabel = ""
        var inText = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    if (tag == "text") inText = true
                    if (tag == "content") {
                        val src = parser.getAttributeValue(null, "src")
                        if (!src.isNullOrBlank() && currentLabel.isNotBlank()) {
                            val resolved = TextMarkdownHtmlParser.resolveRelativeEntry(ncxPath, src, entries)
                            if (resolved != null && !result.containsKey(resolved)) {
                                result[resolved] = currentLabel.trim()
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText) currentLabel = parser.text.orEmpty()
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    if (tag == "text") inText = false
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseNavTitles(
        navHtml: String,
        navPath: String,
        entries: Set<String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val anchorRegex = Regex("(?is)<a[^>]+href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>")
        for (match in anchorRegex.findAll(navHtml)) {
            val href = match.groupValues[1]
            val label = TextMarkdownHtmlParser.stripTagsAndDecode(match.groupValues[2])
            if (label.isNotBlank()) {
                val resolved = TextMarkdownHtmlParser.resolveRelativeEntry(navPath, href, entries)
                if (resolved != null && !result.containsKey(resolved)) {
                    result[resolved] = label
                }
            }
        }
        return result
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return runCatching { parser.nextText().orEmpty().trim() }.getOrDefault("")
    }

    private fun fallbackBook(file: File) = ParsedBook(
        title = file.nameWithoutExtension,
        author = "Unknown Author",
        chapters = listOf(
            ChapterContent(
                index = 0,
                title = file.nameWithoutExtension,
                blocks = listOf(ContentBlock.Paragraph("Unable to locate OPF manifest inside EPUB archive.")),
                rawText = ""
            )
        )
    )
}
