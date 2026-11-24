package com.example.data.remote

import android.util.Xml
import com.example.data.model.BookFormat
import com.example.data.model.GutenbergBook
import com.example.data.model.StandardEbookEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OnlineLibraryService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun searchGutenberg(query: String): Result<List<GutenbergBook>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = query.trim()
                val url = if (trimmed.isEmpty()) {
                    "https://gutendex.com/books/?sort=popular"
                } else {
                    "https://gutendex.com/books/?search=${URLEncoder.encode(trimmed, "UTF-8")}"
                }
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TVReader/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("Gutenberg API returned HTTP ${resp.code}")
                    val body = resp.body?.string() ?: error("Empty response from Gutenberg")
                    val root = JSONObject(body)
                    val results = root.optJSONArray("results") ?: return@runCatching emptyList()
                    val books = mutableListOf<GutenbergBook>()
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val id = obj.optInt("id")
                        val title = obj.optString("title", "Untitled")
                        val authorsArr = obj.optJSONArray("authors")
                        val authors = mutableListOf<String>()
                        if (authorsArr != null) {
                            for (a in 0 until authorsArr.length()) {
                                val name = authorsArr.optJSONObject(a)?.optString("name").orEmpty()
                                if (name.isNotBlank()) authors.add(name)
                            }
                        }
                        val langArr = obj.optJSONArray("languages")
                        val languages = mutableListOf<String>()
                        if (langArr != null) {
                            for (l in 0 until langArr.length()) {
                                val code = langArr.optString(l)
                                if (code.isNotBlank()) languages.add(code)
                            }
                        }
                        val subjArr = obj.optJSONArray("subjects")
                        val subjects = mutableListOf<String>()
                        if (subjArr != null) {
                            for (s in 0 until subjArr.length()) {
                                val sub = subjArr.optString(s)
                                if (sub.isNotBlank()) subjects.add(sub)
                            }
                        }
                        val formatsObj = obj.optJSONObject("formats")
                        val formats = mutableMapOf<String, String>()
                        if (formatsObj != null) {
                            val keys = formatsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val v = formatsObj.optString(k)
                                if (v.isNotBlank() && !v.endsWith(".zip")) {
                                    formats[k] = v
                                }
                            }
                        }
                        val coverUrl = formats.entries
                            .firstOrNull { it.key.startsWith("image/") }
                            ?.value
                        books.add(
                            GutenbergBook(
                                id = id,
                                title = title,
                                authors = authors,
                                languages = languages,
                                subjects = subjects,
                                formats = formats,
                                coverUrl = coverUrl,
                                downloadCount = obj.optInt("download_count", 0)
                            )
                        )
                    }
                    books
                }
            }
        }

    suspend fun fetchStandardEbooks(query: String): Result<List<StandardEbookEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://standardebooks.org/feeds/opds/all"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TVReader/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("Standard Ebooks feed returned HTTP ${resp.code}")
                    val xml = resp.body?.string() ?: error("Empty OPDS feed")
                    val allEntries = parseOpdsEntries(xml)
                    val trimmed = query.trim().lowercase()
                    if (trimmed.isEmpty()) {
                        allEntries.take(60)
                    } else {
                        allEntries.filter {
                            it.title.lowercase().contains(trimmed) ||
                                it.author.lowercase().contains(trimmed) ||
                                it.summary.lowercase().contains(trimmed)
                        }.take(60)
                    }
                }
            }
        }

    private fun parseOpdsEntries(xml: String): List<StandardEbookEntry> {
        val list = mutableListOf<StandardEbookEntry>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        var inEntry = false
        var inAuthor = false
        var id = ""
        var title = ""
        var author = ""
        var summary = ""
        var epubUrl = ""
        var coverUrl: String? = null
        var updated = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "entry" -> {
                            inEntry = true
                            id = ""
                            title = ""
                            author = ""
                            summary = ""
                            epubUrl = ""
                            coverUrl = null
                            updated = ""
                        }
                        "author" -> if (inEntry) inAuthor = true
                        "id" -> if (inEntry) id = safeNextText(parser)
                        "title" -> if (inEntry && !inAuthor) title = safeNextText(parser)
                        "name" -> if (inEntry && inAuthor) {
                            val n = safeNextText(parser)
                            if (n.isNotBlank()) {
                                author = if (author.isEmpty()) n else "$author, $n"
                            }
                        }
                        "summary", "content" -> if (inEntry && summary.isBlank()) {
                            summary = safeNextText(parser).replace(Regex("<[^>]+>"), " ").trim()
                        }
                        "updated" -> if (inEntry) updated = safeNextText(parser)
                        "link" -> if (inEntry) {
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            val rel = parser.getAttributeValue(null, "rel").orEmpty()
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            val fullHref = if (href.startsWith("/")) "https://standardebooks.org$href" else href
                            if (type.contains("application/epub+zip") && epubUrl.isBlank()) {
                                epubUrl = fullHref
                            } else if (rel.contains("image") && type.startsWith("image/") && coverUrl == null) {
                                coverUrl = fullHref
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "author" -> inAuthor = false
                        "entry" -> {
                            if (title.isNotBlank() && epubUrl.isNotBlank()) {
                                list.add(
                                    StandardEbookEntry(
                                        id = id.ifBlank { epubUrl },
                                        title = title,
                                        author = author.ifBlank { "Standard Ebooks" },
                                        summary = summary,
                                        epubUrl = epubUrl,
                                        coverUrl = coverUrl,
                                        updated = updated.take(10)
                                    )
                                )
                            }
                            inEntry = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    suspend fun downloadToFile(
        url: String,
        destDir: File,
        suggestedName: String,
        forcedExtension: String? = null,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!destDir.exists()) destDir.mkdirs()
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "TVReader/1.0 (Android)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Download failed (HTTP ${resp.code})")
                val body = resp.body ?: error("Empty download body")
                val totalBytes = body.contentLength()
                val disposition = resp.header("Content-Disposition").orEmpty()
                val contentType = resp.header("Content-Type")

                val headerFilename = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";\\r\\n]+)\"?")
                    .find(disposition)?.groupValues?.get(1)
                val urlFilename = url.substringBefore('?').substringAfterLast('/', "")

                val baseFileName = (headerFilename?.takeIf { it.isNotBlank() }
                    ?: urlFilename.takeIf { BookFormat.fromFileName(it) != null }
                    ?: suggestedName)
                    .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
                    .trim()
                    .ifBlank { "book_${System.currentTimeMillis()}" }

                val finalName = if (BookFormat.fromFileName(baseFileName) != null) {
                    baseFileName
                } else {
                    val ext = forcedExtension
                        ?: BookFormat.fromMimeType(contentType)?.extensions?.firstOrNull()
                        ?: "epub"
                    "$baseFileName.$ext"
                }

                val outFile = uniqueFile(destDir, finalName)
                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buf = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var lastPct = -1
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
                onProgress(100)
                outFile
            }
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val stem = candidate.nameWithoutExtension
        val ext = candidate.extension
        var counter = 1
        while (candidate.exists()) {
            val nextName = if (ext.isNotEmpty()) "${stem}_$counter.$ext" else "${stem}_$counter"
            candidate = File(dir, nextName)
            counter++
        }
        return candidate
    }

    private fun safeNextText(parser: XmlPullParser): String =
        runCatching { parser.nextText().orEmpty().trim() }.getOrDefault("")
}
