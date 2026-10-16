package io.github.anonymodgh.kairo.core.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun catalogIsSane() {
        val all = ModelCatalog.all
        assertEquals(8, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (pkg in all) {
            assertTrue(pkg.id, pkg.files.isNotEmpty() && pkg.totalBytes > 0)
            for (f in pkg.files) {
                assertTrue(f.sha256, Regex("[0-9a-f]{64}").matches(f.sha256))
                assertTrue(f.bytes > 0)
                assertTrue(f.url, f.url.startsWith("https://"))
            }
        }
    }
}
