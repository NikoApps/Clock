package io.github.nikoapps.clock.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DeviceStorageTest {
    @get:Rule val dirs = TemporaryFolder()

    private fun file(dir: String) = File(dirs.root, "$dir/datastore/settings.preferences_pb")

    @Test fun `moves the old file into device storage`() {
        val old = file("ce").apply { parentFile.mkdirs(); writeText("helper settings") }
        val new = file("de")
        moveStoreFile(from = old, to = new)
        assertEquals("helper settings", new.readText())
        assertFalse(old.exists())
    }

    @Test fun `the old file wins over defaults written first`() {
        val old = file("ce").apply { parentFile.mkdirs(); writeText("helper settings") }
        val new = file("de").apply { parentFile.mkdirs(); writeText("defaults") }
        moveStoreFile(from = old, to = new)
        assertEquals("helper settings", new.readText())
    }

    @Test fun `nothing to move leaves device storage alone`() {
        val new = file("de").apply { parentFile.mkdirs(); writeText("current") }
        moveStoreFile(from = file("ce"), to = new)
        assertEquals("current", new.readText())
    }
}
