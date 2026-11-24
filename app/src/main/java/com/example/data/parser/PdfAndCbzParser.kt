package com.example.data.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.ParsedBook
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

object PdfAndCbzParser {

    fun inspectPdf(file: File, extractCover: Boolean = true): ParsedBook {
        val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
        var pageCount = 1
        var coverBytes: ByteArray? = null

        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount.coerceAtLeast(1)
                    if (extractCover && renderer.pageCount > 0) {
                        renderPage(renderer, 0, targetWidth = 420)?.let { bmp ->
                            val baos = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.PNG, 90, baos)
                            coverBytes = baos.toByteArray()
                            bmp.recycle()
                        }
                    }
                }
            }
        }

        return ParsedBook(
            title = title,
            author = "PDF Document",
            coverBytes = coverBytes,
            pdfFilePath = file.absolutePath,
            pdfPageCount = pageCount
        )
    }

    fun renderPdfPageBitmap(
        pdfPath: String,
        pageIndex: Int,
        targetWidth: Int,
        invertForDark: Boolean = false
    ): Bitmap? {
        val file = File(pdfPath)
        if (!file.exists()) return null
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    renderPage(renderer, pageIndex, targetWidth, invertForDark)
                }
            }
        }.getOrNull()
    }

    private fun renderPage(
        renderer: PdfRenderer,
        pageIndex: Int,
        targetWidth: Int,
        invertForDark: Boolean = false
    ): Bitmap? {
        renderer.openPage(pageIndex).use { page ->
            val safeWidth = targetWidth.coerceIn(320, 1920)
            val aspect = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
            val targetHeight = (safeWidth * aspect).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(safeWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(if (invertForDark) Color.rgb(20, 20, 22) else Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    fun inspectCbz(file: File, extractCover: Boolean = true): ParsedBook {
        val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
        val imageEntries = ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && isImageEntry(it.name) }
                .map { it.name }
                .sortedWith(naturalComparator())
                .toList()
        }

        val coverBytes = if (extractCover && imageEntries.isNotEmpty()) {
            loadCbzPageBytes(file.absolutePath, imageEntries.first())
        } else null

        return ParsedBook(
            title = title,
            author = "Comic Archive (CBZ)",
            coverBytes = coverBytes,
            cbzFilePath = file.absolutePath,
            cbzImageEntries = imageEntries
        )
    }

    fun loadCbzPageBitmap(cbzPath: String, entryName: String, maxDimension: Int = 1800): Bitmap? {
        val bytes = loadCbzPageBytes(cbzPath, entryName) ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sample = 1
        while (opts.outWidth / sample > maxDimension || opts.outHeight / sample > maxDimension) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }

    private fun loadCbzPageBytes(cbzPath: String, entryName: String): ByteArray? {
        val file = File(cbzPath)
        if (!file.exists()) return null
        return runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(entryName) ?: return null
                zip.getInputStream(entry).readBytes()
            }
        }.getOrNull()
    }

    private fun isImageEntry(name: String): Boolean {
        val lower = name.lowercase()
        return !lower.startsWith("__macosx") &&
            (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp"))
    }

    private fun naturalComparator(): Comparator<String> = Comparator { a, b ->
        val regex = Regex("\\d+|\\D+")
        val partsA = regex.findAll(a.lowercase()).map { it.value }.toList()
        val partsB = regex.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(partsA.size, partsB.size)) {
            val pa = partsA[i]
            val pb = partsB[i]
            val na = pa.toLongOrNull()
            val nb = pb.toLongOrNull()
            val cmp = if (na != null && nb != null) na.compareTo(nb) else pa.compareTo(pb)
            if (cmp != 0) return@Comparator cmp
        }
        partsA.size.compareTo(partsB.size)
    }
}
