package com.example.data.parser

import com.example.data.model.BookFormat
import com.example.data.model.ParsedBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BookParser {

    suspend fun parse(
        file: File,
        format: BookFormat,
        extractContent: Boolean = true
    ): ParsedBook = withContext(Dispatchers.IO) {
        when (format) {
            BookFormat.EPUB -> EpubParser.parse(file, extractContent)
            BookFormat.FB2 -> Fb2Parser.parse(file, extractContent)
            BookFormat.PDF -> PdfAndCbzParser.inspectPdf(file, extractCover = true)
            BookFormat.CBZ -> PdfAndCbzParser.inspectCbz(file, extractCover = true)
            BookFormat.TXT -> TextMarkdownHtmlParser.parseTxt(file)
            BookFormat.MD -> TextMarkdownHtmlParser.parseMarkdown(file)
            BookFormat.HTML -> TextMarkdownHtmlParser.parseHtmlFile(file)
        }
    }
}
