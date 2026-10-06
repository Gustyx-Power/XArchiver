package id.xms.xarchiver

import id.xms.xarchiver.core.update.UpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testParseVersion() {
        val v1 = UpdateManager.parseVersion("release-2.1.3")
        assertEquals(listOf(2, 1, 3), v1.baseDigits)
        assertEquals(null, v1.buildSuffix)

        val v2 = UpdateManager.parseVersion("2.1.3-20261004")
        assertEquals(listOf(2, 1, 3), v2.baseDigits)
        assertEquals(20261004L, v2.buildSuffix)

        val v3 = UpdateManager.parseVersion("v2.1.0")
        assertEquals(listOf(2, 1, 0), v3.baseDigits)
        assertEquals(null, v3.buildSuffix)

        val v4 = UpdateManager.parseVersion("2.1.3.1")
        assertEquals(listOf(2, 1, 3, 1), v4.baseDigits)
        assertEquals(null, v4.buildSuffix)

        val v5 = UpdateManager.parseVersion("2.1.3-b5")
        assertEquals(listOf(2, 1, 3), v5.baseDigits)
        assertEquals(5L, v5.buildSuffix)
    }

    @Test
    fun testExtractVersionString() {
        assertEquals("2.1.3", UpdateManager.extractVersionString("release-2.1.3"))
        assertEquals("2.1.3", UpdateManager.extractVersionString("v2.1.3"))
        assertEquals("2.1.3", UpdateManager.extractVersionString("2.1.3-20261004"))
        assertEquals("2.1.3.1", UpdateManager.extractVersionString("2.1.3.1"))
    }

    @Test
    fun testSameVersionWithBuildDate_isNotNewer() {
        // This was the exact bug reported:
        // Remote is "2.1.3" (or "release-2.1.3"), installed app is "2.1.3-20261004"
        assertFalse(UpdateManager.isNewerVersion("2.1.3", "2.1.3-20261004"))
        assertFalse(UpdateManager.isNewerVersion("release-2.1.3", "2.1.3-20261004"))
        assertFalse(UpdateManager.isNewerVersion("v2.1.3", "2.1.3-20261004"))
        assertFalse(UpdateManager.isNewerVersion("2.1.3", "2.1.3"))
        assertFalse(UpdateManager.isNewerVersion("2.1.5", "2.1.5-20261006"))
        assertFalse(UpdateManager.isNewerVersion("release-2.1.5", "2.1.5-20261006"))
    }

    @Test
    fun testHigherVersion_isNewer() {
        assertTrue(UpdateManager.isNewerVersion("2.1.4", "2.1.3-20261004"))
        assertTrue(UpdateManager.isNewerVersion("release-2.1.4", "2.1.3-20261004"))
        assertTrue(UpdateManager.isNewerVersion("2.1.5", "2.1.3-20261004"))
        assertTrue(UpdateManager.isNewerVersion("release-2.1.5", "2.1.3-20261004"))
        assertTrue(UpdateManager.isNewerVersion("2.2.0", "2.1.3-20261004"))
        assertTrue(UpdateManager.isNewerVersion("3.0.0", "2.1.3-20261004"))
        assertTrue(UpdateManager.isNewerVersion("2.1.3.1", "2.1.3"))
    }

    @Test
    fun testLowerVersion_isNotNewer() {
        assertFalse(UpdateManager.isNewerVersion("2.1.2", "2.1.3-20261004"))
        assertFalse(UpdateManager.isNewerVersion("2.0.9", "2.1.3-20261004"))
        assertFalse(UpdateManager.isNewerVersion("1.9.9", "2.1.3-20261004"))
    }

    @Test
    fun testBuildDateComparison() {
        // Same base version, remote has newer build date
        assertTrue(UpdateManager.isNewerVersion("2.1.3-20261008", "2.1.3-20261004"))
        // Same base version, remote has older build date
        assertFalse(UpdateManager.isNewerVersion("2.1.3-20261002", "2.1.3-20261004"))
        // Same build date
        assertFalse(UpdateManager.isNewerVersion("2.1.3-20261004", "2.1.3-20261004"))
    }
}
