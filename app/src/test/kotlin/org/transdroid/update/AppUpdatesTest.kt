/*
 * Copyright 2010-2026 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid. If not, see <https://www.gnu.org/licenses/>.
 */
package org.transdroid.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatesTest {

    @Test
    fun `parses version code and name`() {
        assertEquals(LatestRelease(3000014, "3.0.0"), AppUpdates.parse("3000014|3.0.0"))
        assertEquals(LatestRelease(249, "2.5.29"), AppUpdates.parse(" 249 | 2.5.29 \n"))
    }

    @Test
    fun `rejects anything that isn't the one-line format`() {
        assertNull(AppUpdates.parse(""))
        assertNull(AppUpdates.parse("3.0.0"))
        assertNull(AppUpdates.parse("abc|3.0.0"))
        assertNull(AppUpdates.parse("0|3.0.0"))
        assertNull(AppUpdates.parse("3000014|"))
        assertNull(AppUpdates.parse("1|2|3"))
        assertNull(AppUpdates.parse("<html><body>404|Not found</body></html>"))
    }

    @Test
    fun `notifies once per newer version`() {
        val release = LatestRelease(3000014, "3.0.0")
        assertTrue(AppUpdates.shouldNotify(release, installedVersionCode = 3000013, notifiedVersionCode = null))
        assertFalse(AppUpdates.shouldNotify(release, installedVersionCode = 3000013, notifiedVersionCode = 3000014))
        // An even newer release notifies again
        assertTrue(AppUpdates.shouldNotify(LatestRelease(3000015, "3.0.1"), 3000013, notifiedVersionCode = 3000014))
    }

    @Test
    fun `never notifies about the installed or an older version`() {
        assertFalse(AppUpdates.shouldNotify(LatestRelease(3000013, "3.0.0-beta1"), 3000013, null))
        // The file on master still announces Transdroid 2 during the betas
        assertFalse(AppUpdates.shouldNotify(LatestRelease(249, "2.5.29"), 3000013, null))
    }

    @Test
    fun `the published file parses`() {
        // Keeps the announcement on master readable by both Transdroid 2 and 3
        val file = File("../latest-app.html").takeIf { it.exists() } ?: File("latest-app.html")
        assertNotNull(AppUpdates.parse(file.readText()))
    }
}
