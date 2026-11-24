package com.example.data.remote

import android.util.Xml
import com.example.data.local.NetworkSourceEntity
import com.example.data.model.BookFormat
import com.example.data.model.RemoteFileItem
import com.example.data.model.SourceProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringReader
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class NetworkFileClient {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun listDirectory(
        source: NetworkSourceEntity,
        currentPath: String
    ): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        when (SourceProtocol.valueOf(source.protocol)) {
            SourceProtocol.WEBDAV -> listWebDav(source, currentPath)
            SourceProtocol.FTP -> listFtp(source, currentPath)
        }
    }

    suspend fun downloadFile(
        source: NetworkSourceEntity,
        item: RemoteFileItem,
        destDir: File,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!destDir.exists()) destDir.mkdirs()
        val destFile = File(destDir, item.name)
        when (SourceProtocol.valueOf(source.protocol)) {
            SourceProtocol.WEBDAV -> downloadWebDav(source, item.path, destFile, onProgress)
            SourceProtocol.FTP -> downloadFtp(source, item.path, destFile, onProgress)
        }
    }

    private fun listWebDav(
        source: NetworkSourceEntity,
        currentPath: String
    ): Result<List<RemoteFileItem>> = runCatching {
        val url = buildWebDavUrl(source, currentPath)
        val propfindXml = """
            <?xml version="1.0" encoding="utf-8" ?>
            <D:propfind xmlns:D="DAV:">
              <D:prop>
                <D:displayname/>
                <D:getcontentlength/>
                <D:resourcetype/>
              </D:prop>
            </D:propfind>
        """.trimIndent()

        val reqBuilder = Request.Builder()
            .url(url)
            .method("PROPFIND", propfindXml.toRequestBody("application/xml".toMediaType()))
            .header("Depth", "1")
        if (source.username.isNotBlank()) {
            reqBuilder.header("Authorization", Credentials.basic(source.username, source.password))
        }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 207) {
                error("WebDAV PROPFIND failed (HTTP ${resp.code})")
            }
            val xml = resp.body?.string().orEmpty()
            parseWebDavXml(xml, currentPath)
        }
    }

    private fun parseWebDavXml(xml: String, requestedPath: String): List<RemoteFileItem> {
        val items = mutableListOf<RemoteFileItem>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        var inResponse = false
        var href = ""
        var displayName = ""
        var contentLength = 0L
        var isCollection = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    when (tag) {
                        "response" -> {
                            inResponse = true
                            href = ""
                            displayName = ""
                            contentLength = 0L
                            isCollection = false
                        }
                        "href" -> if (inResponse) href = safeNextText(parser)
                        "displayname" -> if (inResponse) displayName = safeNextText(parser)
                        "getcontentlength" -> if (inResponse) contentLength = safeNextText(parser).toLongOrNull() ?: 0L
                        "collection" -> if (inResponse) isCollection = true
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    if (tag == "response" && inResponse) {
                        inResponse = false
                        val decodedPath = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
                        val normReq = requestedPath.trimEnd('/')
                        val normHref = decodedPath.trimEnd('/')
                        if (normHref.isNotEmpty() && !normHref.endsWith(normReq, ignoreCase = true) || normHref != normReq) {
                            val name = displayName.ifBlank {
                                normHref.substringAfterLast('/').ifBlank { normHref }
                            }
                            if (name.isNotBlank() && normHref != normReq) {
                                val fmt = if (isCollection) null else BookFormat.fromFileName(name)
                                if (isCollection || fmt != null) {
                                    items.add(
                                        RemoteFileItem(
                                            name = name,
                                            path = decodedPath,
                                            isDirectory = isCollection,
                                            sizeBytes = contentLength,
                                            format = fmt
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return items.sortedWith(compareBy<RemoteFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun downloadWebDav(
        source: NetworkSourceEntity,
        remotePath: String,
        destFile: File,
        onProgress: (Int) -> Unit
    ): Result<File> = runCatching {
        val url = if (remotePath.startsWith("http://") || remotePath.startsWith("https://")) {
            remotePath
        } else {
            buildWebDavUrl(source, remotePath)
        }
        val reqBuilder = Request.Builder().url(url).get()
        if (source.username.isNotBlank()) {
            reqBuilder.header("Authorization", Credentials.basic(source.username, source.password))
        }
        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) error("WebDAV GET failed (HTTP ${resp.code})")
            val body = resp.body ?: error("Empty WebDAV file body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(16 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        if (total > 0) onProgress(((readTotal * 100L) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        }
        onProgress(100)
        destFile
    }

    private fun buildWebDavUrl(source: NetworkSourceEntity, path: String): String {
        val hostRaw = source.host.trim().removeSuffix("/")
        val base = if (hostRaw.startsWith("http://") || hostRaw.startsWith("https://")) {
            hostRaw
        } else {
            val scheme = if (source.port == 80 || source.port == 8080) "http" else "https"
            "$scheme://$hostRaw:${source.port}"
        }
        val cleanPath = path.ifBlank { source.basePath }.let {
            if (it.startsWith("/")) it else "/$it"
        }
        return "$base$cleanPath"
    }

    private fun listFtp(
        source: NetworkSourceEntity,
        currentPath: String
    ): Result<List<RemoteFileItem>> = runCatching {
        Socket().use { control ->
            control.connect(InetSocketAddress(source.host.trim(), source.port), 12_000)
            control.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(control.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(control.getOutputStream()))

            fun sendCmd(cmd: String): String {
                writer.write("$cmd\r\n")
                writer.flush()
                return readFtpResponse(reader)
            }

            readFtpResponse(reader) // 220 welcome
            val user = source.username.ifBlank { "anonymous" }
            val pass = source.password.ifBlank { "anonymous@" }
            val uResp = sendCmd("USER $user")
            if (uResp.startsWith("331")) {
                val pResp = sendCmd("PASS $pass")
                if (!pResp.startsWith("230")) error("FTP login failed: $pResp")
            } else if (!uResp.startsWith("230")) {
                error("FTP USER rejected: $uResp")
            }

            sendCmd("TYPE I")
            val targetDir = currentPath.ifBlank { source.basePath.ifBlank { "/" } }
            sendCmd("CWD $targetDir")

            val pasvResp = sendCmd("PASV")
            val (dataHost, dataPort) = parsePasvEndpoint(pasvResp)

            val entries = mutableListOf<RemoteFileItem>()
            Socket().use { dataSocket ->
                dataSocket.connect(InetSocketAddress(dataHost, dataPort), 12_000)
                val listResp = sendCmd("LIST")
                if (!listResp.startsWith("150") && !listResp.startsWith("125")) {
                    error("FTP LIST failed: $listResp")
                }
                val listReader = BufferedReader(InputStreamReader(dataSocket.getInputStream()))
                var line: String?
                while (listReader.readLine().also { line = it } != null) {
                    parseFtpListLine(line!!, targetDir)?.let { entries.add(it) }
                }
            }
            readFtpResponse(reader) // 226 transfer complete
            runCatching { sendCmd("QUIT") }
            entries.sortedWith(compareBy<RemoteFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
        }
    }

    private fun downloadFtp(
        source: NetworkSourceEntity,
        remotePath: String,
        destFile: File,
        onProgress: (Int) -> Unit
    ): Result<File> = runCatching {
        Socket().use { control ->
            control.connect(InetSocketAddress(source.host.trim(), source.port), 12_000)
            control.soTimeout = 25_000
            val reader = BufferedReader(InputStreamReader(control.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(control.getOutputStream()))

            fun sendCmd(cmd: String): String {
                writer.write("$cmd\r\n")
                writer.flush()
                return readFtpResponse(reader)
            }

            readFtpResponse(reader)
            val user = source.username.ifBlank { "anonymous" }
            val pass = source.password.ifBlank { "anonymous@" }
            val uResp = sendCmd("USER $user")
            if (uResp.startsWith("331")) sendCmd("PASS $pass")
            sendCmd("TYPE I")

            val pasvResp = sendCmd("PASV")
            val (dataHost, dataPort) = parsePasvEndpoint(pasvResp)

            Socket().use { dataSocket ->
                dataSocket.connect(InetSocketAddress(dataHost, dataPort), 15_000)
                val retrResp = sendCmd("RETR $remotePath")
                if (!retrResp.startsWith("150") && !retrResp.startsWith("125")) {
                    error("FTP RETR failed: $retrResp")
                }
                BufferedInputStream(dataSocket.getInputStream()).use { input ->
                    BufferedOutputStream(FileOutputStream(destFile)).use { output ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                        }
                    }
                }
            }
            readFtpResponse(reader)
            runCatching { sendCmd("QUIT") }
        }
        onProgress(100)
        destFile
    }

    private fun readFtpResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        var codePrefix: String? = null
        while (true) {
            val line = reader.readLine() ?: break
            sb.appendLine(line)
            if (codePrefix == null && line.length >= 3 && line.take(3).all { it.isDigit() }) {
                codePrefix = line.take(3)
                if (line.length == 3 || line[3] == ' ') break
            } else if (codePrefix != null && line.startsWith("$codePrefix ")) {
                break
            }
        }
        return sb.toString().trim()
    }

    private fun parsePasvEndpoint(response: String): Pair<String, Int> {
        val match = Regex("\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\)").find(response)
            ?: error("Unable to parse FTP PASV endpoint: $response")
        val g = match.groupValues
        val host = "${g[1]}.${g[2]}.${g[3]}.${g[4]}"
        val port = g[5].toInt() * 256 + g[6].toInt()
        return host to port
    }

    private fun parseFtpListLine(line: String, parentDir: String): RemoteFileItem? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(Regex("\\s+"), limit = 9)
        if (parts.size < 9) return null
        val perms = parts[0]
        val isDir = perms.startsWith("d")
        val size = parts[4].toLongOrNull() ?: 0L
        val name = parts[8]
        if (name == "." || name == "..") return null
        val fmt = if (isDir) null else BookFormat.fromFileName(name)
        if (!isDir && fmt == null) return null
        val base = parentDir.trimEnd('/')
        val fullPath = if (base.isEmpty()) "/$name" else "$base/$name"
        return RemoteFileItem(
            name = name,
            path = fullPath,
            isDirectory = isDir,
            sizeBytes = size,
            format = fmt
        )
    }

    private fun safeNextText(parser: XmlPullParser): String =
        runCatching { parser.nextText().orEmpty().trim() }.getOrDefault("")
}
