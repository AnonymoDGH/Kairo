package io.github.anonymodgh.kairo.core.models

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@OptIn(InternalCoroutinesApi::class)
class ModelStore(
    private val root: File,
    private val client: OkHttpClient = OkHttpClient(),
    private val freeBytes: () -> Long = { root.usableSpace },
) {
    private val states = ConcurrentHashMap<String, MutableStateFlow<InstallState>>()
    private val mutex = Mutex()

    fun dir(pkg: ModelPackage): File = File(root, pkg.id)

    fun isInstalled(pkg: ModelPackage): Boolean {
        val marker = File(dir(pkg), MARKER)
        return marker.isFile && runCatching { marker.readText() }.getOrNull() == fingerprint(pkg)
    }

    fun state(pkg: ModelPackage): StateFlow<InstallState> = flow(pkg).asStateFlow()

    private fun flow(pkg: ModelPackage) = states.getOrPut(pkg.id) {
        MutableStateFlow(if (isInstalled(pkg)) InstallState.Installed else InstallState.NotInstalled)
    }

    fun installedBytes(pkg: ModelPackage): Long =
        dir(pkg).walkTopDown().filter { it.isFile && it.name != MARKER && !it.name.endsWith(PART) }.sumOf { it.length() }

    fun delete(pkg: ModelPackage) {
        dir(pkg).deleteRecursively()
        flow(pkg).value = InstallState.NotInstalled
    }

    suspend fun install(pkg: ModelPackage) = mutex.withLock {
        val state = flow(pkg)
        if (isInstalled(pkg)) {
            state.value = InstallState.Installed
            return@withLock
        }
        try {
            withContext(Dispatchers.IO) { doInstall(pkg, state) }
            state.value = InstallState.Installed
        } catch (e: CancellationException) {
            state.value = InstallState.NotInstalled
            throw e
        } catch (e: ModelStoreException) {
            state.value = InstallState.Failed(e.message.orEmpty())
            throw e
        } catch (e: IOException) {
            val msg = "Error de red o de disco: ${e.message ?: e.javaClass.simpleName}"
            state.value = InstallState.Failed(msg)
            throw ModelStoreException(msg).also { it.initCause(e) }
        }
    }

    private suspend fun doInstall(pkg: ModelPackage, state: MutableStateFlow<InstallState>) {
        val dir = dir(pkg)
        File(dir, MARKER).delete()
        dir.mkdirs()
        val dl = File(dir, DOWNLOAD_DIR)

        fun target(f: ModelFile) =
            if (f.archive != null) File(dl, f.url.substringAfterLast('/')) else File(dir, f.path)

        var needed = 0L
        for (f in pkg.files) {
            val t = target(f)
            if (t.isFile && t.length() == f.bytes) continue
            val have = File(t.path + PART).takeIf { it.isFile }?.length()?.coerceAtMost(f.bytes) ?: 0L
            needed += (f.bytes - have) + if (f.archive != null) f.bytes * 2 else 0L
        }
        if (freeBytes() < needed) {
            throw ModelStoreException("Falta espacio: necesitas ${(needed + MB - 1) / MB} MB libres")
        }

        var done = 0L
        val progress = Progress(pkg.totalBytes, state)
        for (f in pkg.files) {
            val t = target(f)
            t.parentFile?.mkdirs()
            if (!(t.isFile && t.length() == f.bytes && sha256(t).equals(f.sha256, true))) {
                t.delete()
                download(f, t, done, progress)
                state.value = InstallState.Verifying
                verify(f, t)
            }
            done += f.bytes
            progress.report(done, force = true)
            if (f.archive != null) {
                state.value = InstallState.Extracting
                extract(t, File(dir, f.path), f.stripComponents)
                t.delete()
            }
        }
        dl.deleteRecursively()
        File(dir, MARKER).writeText(fingerprint(pkg))
    }

    private class Progress(val total: Long, val state: MutableStateFlow<InstallState>) {
        private var lastBytes = -1L
        private var lastTime = 0L
        fun report(downloaded: Long, force: Boolean = false) {
            val now = System.nanoTime()
            if (!force && downloaded - lastBytes < 64 * 1024 && now - lastTime < 100_000_000L) return
            lastBytes = downloaded
            lastTime = now
            state.value = InstallState.Downloading(downloaded, total)
        }
    }

    private suspend fun download(f: ModelFile, target: File, done: Long, progress: Progress) {
        val part = File(target.path + PART)
        if (part.isFile && part.length() > f.bytes) part.delete()
        val name = target.name.ifEmpty { f.path }
        val offset = if (part.isFile) part.length() else 0L
        val req = Request.Builder().url(f.url).apply { if (offset > 0) header("Range", "bytes=$offset-") }.build()
        val call = client.newCall(req)
        val reg = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true) { if (it != null) call.cancel() }
        try {
            call.execute().use { resp ->
                when (resp.code) {
                    416 -> {
                        if (part.length() != f.bytes) {
                            part.delete()
                            throw ModelStoreException("Descarga de $name interrumpida; vuelve a intentarlo")
                        }
                        return finish(part, target)
                    }
                    206, 200 -> {
                        val append = resp.code == 206 && offset > 0
                        var written = if (append) offset else 0L
                        if (!append) part.delete()
                        progress.report(done + written, force = true)
                        resp.body.byteStream().use { input ->
                            java.io.FileOutputStream(part, append).use { out ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    written += n
                                    progress.report(done + written)
                                }
                            }
                        }
                    }
                    else -> throw ModelStoreException("Error de red (HTTP ${resp.code}) al descargar $name")
                }
            }
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            throw e
        } finally {
            reg.dispose()
        }
        finish(part, target)
    }

    private fun finish(part: File, target: File) {
        if (!part.renameTo(target)) throw ModelStoreException("No se pudo guardar ${target.name}")
    }

    private fun verify(f: ModelFile, file: File) {
        if (!sha256(file).equals(f.sha256, true)) {
            file.delete()
            throw ModelStoreException("El archivo ${file.name} está dañado (sha256 no coincide)")
        }
    }

    private fun extract(archive: File, destDir: File, strip: Int) {
        destDir.mkdirs()
        val base = destDir.canonicalFile
        archive.inputStream().buffered().use { raw ->
            TarArchiveInputStream(BZip2CompressorInputStream(raw)).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    val parts = entry.name.split('/').filter { it.isNotEmpty() && it != "." }
                    if (parts.any { it == ".." }) throw ModelStoreException("Entrada de archivo no segura: ${entry.name}")
                    if (entry.isSymbolicLink || entry.isLink || parts.size <= strip) continue
                    val out = File(base, parts.drop(strip).joinToString("/")).canonicalFile
                    if (!out.path.startsWith(base.path + File.separator)) {
                        throw ModelStoreException("Entrada de archivo no segura: ${entry.name}")
                    }
                    if (entry.isDirectory) out.mkdirs() else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { tar.copyTo(it) }
                    }
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { s ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun fingerprint(pkg: ModelPackage): String {
        val text = pkg.files.joinToString("\n") { "${it.url}|${it.path}|${it.sha256}|${it.bytes}|${it.archive}|${it.stripComponents}" }
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MARKER = ".installed"
        const val PART = ".part"
        const val DOWNLOAD_DIR = ".download"
        const val MB = 1024L * 1024L
    }
}
