package com.example.data.model

enum class BookFormat(
    val label: String,
    val extensions: List<String>,
    val mimeTypes: List<String>
) {
    EPUB("EPUB", listOf("epub", "epub3"), listOf("application/epub+zip")),
    FB2("FB2", listOf("fb2"), listOf("application/x-fictionbook+xml", "text/xml")),
    PDF("PDF", listOf("pdf"), listOf("application/pdf")),
    TXT("TXT", listOf("txt"), listOf("text/plain")),
    MD("MD", listOf("md", "markdown"), listOf("text/markdown", "text/x-markdown")),
    HTML("HTML", listOf("html", "htm", "xhtml"), listOf("text/html", "application/xhtml+xml")),
    CBZ("CBZ", listOf("cbz"), listOf("application/x-cbz", "application/vnd.comicbook+zip"));

    companion object {
        fun fromExtension(ext: String): BookFormat? {
            val clean = ext.trim().lowercase().removePrefix(".")
            return entries.firstOrNull { clean in it.extensions }
        }

        fun fromFileName(fileName: String): BookFormat? {
            val lower = fileName.lowercase()
            if (lower.endsWith(".fb2.zip")) return FB2
            val dot = lower.lastIndexOf('.')
            if (dot < 0 || dot == lower.lastIndex) return null
            return fromExtension(lower.substring(dot + 1))
        }

        fun fromMimeType(mime: String?): BookFormat? {
            if (mime.isNullOrBlank()) return null
            val clean = mime.substringBefore(';').trim().lowercase()
            return entries.firstOrNull { clean in it.mimeTypes }
        }
    }
}
