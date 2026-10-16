package io.github.anonymodgh.kairo.core.models

import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

class ModelStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var server: MockWebServer
    private lateinit var root: File
    private var honourRange = true
    private val requests = mutableListOf<RecordedRequest>()
    private val content = ByteArray(300_000) { (it * 31 + 7).toByte() }

    @Before
    fun setUp() {
        root = tmp.newFolder("models")
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return when (request.url.encodedPath) {
                    "/model.bin" -> serve(content, request)
                    "/archive.tar.bz2" -> serve(archive, request)
                    else -> MockResponse.Builder().code(404).build()
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private fun serve(data: ByteArray, req: RecordedRequest): MockResponse {
        val range = req.headers["Range"]?.removePrefix("bytes=")?.removeSuffix("-")?.toLongOrNull()
        if (range != null && honourRange) {
            if (range >= data.size) return MockResponse.Builder().code(416).build()
            return MockResponse.Builder().code(206).body(Buffer().write(data.copyOfRange(range.toInt(), data.size))).build()
        }
        return MockResponse.Builder().body(Buffer().write(data)).build()
    }

    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun tarBz2(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        TarArchiveOutputStream(BZip2CompressorOutputStream(bos)).use { tar ->
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            for ((name, data) in entries) {
                tar.putArchiveEntry(TarArchiveEntry(name).apply { size = data.size.toLong() })
                tar.write(data)
                tar.closeArchiveEntry()
            }
        }
        return bos.toByteArray()
    }

    private val archive by lazy {
        tarBz2("top/a.txt" to "hola".toByteArray(), "top/sub/b.txt" to "mundo".toByteArray())
    }

    private fun store(free: Long = Long.MAX_VALUE) = ModelStore(root, freeBytes = { free })

    private fun pkg(sha: String = sha(content), bytes: Long = content.size.toLong()) = ModelPackage(
        "test", "Test", "d", "MIT",
        listOf(ModelFile(server.url("/model.bin").toString(), "model.bin", sha, bytes)),
    )

    @Test
    fun freshDownloadWritesFileAndMarker() = runBlocking {
        val s = store(); val p = pkg()
        s.install(p)
        assertArrayEquals(content, File(s.dir(p), "model.bin").readBytes())
        assertTrue(s.isInstalled(p))
        assertEquals(InstallState.Installed, s.state(p).value)
        assertEquals(content.size.toLong(), s.installedBytes(p))
        assertFalse(File(s.dir(p), "model.bin.part").exists())
        val n = requests.size
        s.install(p)
        assertEquals(n, requests.size)
    }

    @Test
    fun shaMismatchFails() = runBlocking {
        val s = store(); val p = pkg(sha = "0".repeat(64))
        try { s.install(p); fail() } catch (e: ModelStoreException) { assertTrue(e.message!!.contains("dañado")) }
        assertFalse(File(s.dir(p), "model.bin").exists())
        assertFalse(File(s.dir(p), "model.bin.part").exists())
        assertFalse(s.isInstalled(p))
        assertTrue(s.state(p).value is InstallState.Failed)
    }

    @Test
    fun resumesWithRange() = runBlocking {
        val s = store(); val p = pkg()
        val half = content.size / 2
        File(s.dir(p), "model.bin.part").apply { parentFile!!.mkdirs(); writeBytes(content.copyOf(half)) }
        s.install(p)
        assertEquals("bytes=$half-", requests.single().headers["Range"])
        assertArrayEquals(content, File(s.dir(p), "model.bin").readBytes())
    }

    @Test
    fun restartsWhenServerIgnoresRange() = runBlocking {
        honourRange = false
        val s = store(); val p = pkg()
        File(s.dir(p), "model.bin.part").apply { parentFile!!.mkdirs(); writeBytes(content.copyOf(1000)) }
        s.install(p)
        assertEquals("bytes=1000-", requests.single().headers["Range"])
        assertArrayEquals(content, File(s.dir(p), "model.bin").readBytes())
    }

    @Test
    fun completePartGets416AndIsAccepted() = runBlocking {
        val s = store(); val p = pkg()
        File(s.dir(p), "model.bin.part").apply { parentFile!!.mkdirs(); writeBytes(content) }
        s.install(p)
        assertArrayEquals(content, File(s.dir(p), "model.bin").readBytes())
    }

    @Test
    fun http404Fails() = runBlocking {
        val s = store()
        val p = ModelPackage("x", "X", "", "", listOf(ModelFile(server.url("/nope").toString(), "nope", sha(content), 10)))
        try { s.install(p); fail() } catch (e: ModelStoreException) { assertTrue(e.message!!.contains("HTTP 404")) }
        assertEquals(InstallState.Failed("Error de red (HTTP 404) al descargar nope"), s.state(p).value)
    }

    @Test
    fun insufficientSpaceFailsWithoutRequest() = runBlocking {
        val s = store(free = 1000); val p = pkg()
        try { s.install(p); fail() } catch (e: ModelStoreException) { assertTrue(e.message!!.startsWith("Falta espacio")) }
        assertEquals(0, requests.size)
    }

    private fun archivePkg(bytes: ByteArray) = ModelPackage(
        "arch", "A", "", "",
        listOf(ModelFile(server.url("/archive.tar.bz2").toString(), "", sha(bytes), bytes.size.toLong(), ArchiveType.TarBz2, 1)),
    )

    @Test
    fun extractsTarBz2StrippingComponents() = runBlocking {
        val s = store(); val p = archivePkg(archive)
        s.install(p)
        assertEquals("hola", File(s.dir(p), "a.txt").readText())
        assertEquals("mundo", File(s.dir(p), "sub/b.txt").readText())
        assertFalse(File(s.dir(p), ".download").exists())
        assertTrue(s.isInstalled(p))
    }

    @Test
    fun zipSlipIsRejected() = runBlocking {
        val bad = tarBz2("top/a.txt" to "x".toByteArray(), "../evil" to "pwned".toByteArray())
        val s = store(); val p = archivePkg(bad)
        // El servidor sirve el archivo ya construido para este test.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = serve(bad, request)
        }
        try { s.install(p); fail() } catch (e: ModelStoreException) { assertTrue(e.message!!.contains("no segura")) }
        assertFalse(File(root, "evil").exists())
        assertFalse(File(s.dir(p), "evil").exists())
        assertFalse(s.isInstalled(p))
    }

    @Test
    fun deleteRemovesDir() = runBlocking {
        val s = store(); val p = pkg()
        s.install(p)
        s.delete(p)
        assertFalse(s.dir(p).exists())
        assertEquals(InstallState.NotInstalled, s.state(p).value)
    }
}
