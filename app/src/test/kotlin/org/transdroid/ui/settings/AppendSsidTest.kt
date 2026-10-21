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
package org.transdroid.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppendSsidTest {

    @Test
    fun `adds a trimmed network to the end`() {
        assertEquals(listOf("HomeNet", "HomeNet-5G"), appendSsid(listOf("HomeNet"), "  HomeNet-5G "))
    }

    @Test
    fun `ignores a network that is already listed`() {
        assertEquals(listOf("HomeNet"), appendSsid(listOf("HomeNet"), "HomeNet "))
    }

    @Test
    fun `ignores blank input, such as an empty draft on save`() {
        assertEquals(listOf("HomeNet"), appendSsid(listOf("HomeNet"), "   "))
    }

    @Test
    fun `network names are case sensitive`() {
        assertEquals(listOf("HomeNet", "homenet"), appendSsid(listOf("HomeNet"), "homenet"))
    }
}
