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
package org.transdroid.protocol

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomHeadersTest {

    @Test
    fun `custom headers never clobber the adapter's own session and auth headers`() {
        val request = Request.Builder()
            .url("http://example/")
            .header("Cookie", "SID=abc")
            .header("Authorization", "Basic xyz")
            .build()

        val merged = DaemonAdapterFactory.withCustomHeaders(
            request,
            mapOf(
                "Cookie" to "CF_Authorization=tok",
                "Authorization" to "Bearer proxy",
                "CF-Access-Client-Id" to "id.access",
            ),
        )

        assertEquals("SID=abc; CF_Authorization=tok", merged.header("Cookie"))
        assertEquals("Basic xyz", merged.header("Authorization"))
        assertEquals("id.access", merged.header("CF-Access-Client-Id"))
    }

    @Test
    fun `custom headers apply when the adapter set none`() {
        val request = Request.Builder().url("http://example/").build()

        val merged = DaemonAdapterFactory.withCustomHeaders(
            request,
            mapOf("Cookie" to "CF_Authorization=tok", "Authorization" to "Bearer proxy"),
        )

        assertEquals("CF_Authorization=tok", merged.header("Cookie"))
        assertEquals("Bearer proxy", merged.header("Authorization"))
    }
}
