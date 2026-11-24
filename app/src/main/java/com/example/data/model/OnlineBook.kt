package com.example.data.model

enum class SourceProtocol(val label: String, val defaultPort: Int) {
    WEBDAV("WebDAV", 443),
    FTP("FTP", 21)
}

data class GutenbergBook(
    val id: Int,
    val title: String,
    val authors: List<String>,
    val languages: List<String>,
    val subjects: List<String>,
    val formats: Map<String, String>,
    val coverUrl: String?,
    val downloadCount: Int
) {
    val authorDisplay: String
        get() = authors.joinToString("; ").ifBlank { "Unknown Author" }

    val primaryLanguage: String
        get() = languages.firstOrNull()?.uppercase() ?: "EN"

    val epubUrl: String?
        get() = formats.entries.firstOrNull { it.key.startsWith("application/epub") }?.value

    val txtUrl: String?
        get() = formats.entries.firstOrNull { it.key.startsWith("text/plain") }?.value

    val htmlUrl: String?
        get() = formats.entries.firstOrNull { it.key.startsWith("text/html") }?.value
}

data class StandardEbookEntry(
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val epubUrl: String,
    val coverUrl: String?,
    val updated: String
)

data class RemoteFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val format: BookFormat? = null
)
