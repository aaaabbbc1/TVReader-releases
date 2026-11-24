package com.example.data.model

sealed interface ContentBlock {
    data class Heading(val level: Int, val text: String) : ContentBlock
    data class Paragraph(val text: String, val isQuote: Boolean = false) : ContentBlock
    data class ImageBlock(val entryKey: String, val caption: String = "") : ContentBlock
    data object Divider : ContentBlock
}

data class ChapterContent(
    val index: Int,
    val title: String,
    val blocks: List<ContentBlock>,
    val rawText: String
)

data class ParsedBook(
    val title: String,
    val author: String,
    val description: String = "",
    val chapters: List<ChapterContent> = emptyList(),
    val coverBytes: ByteArray? = null,
    val imageEntries: Map<String, ByteArray> = emptyMap(),
    val pdfFilePath: String? = null,
    val pdfPageCount: Int = 0,
    val cbzFilePath: String? = null,
    val cbzImageEntries: List<String> = emptyList()
)
