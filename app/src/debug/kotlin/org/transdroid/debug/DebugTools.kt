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
package org.transdroid.debug

import java.util.UUID
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.DaemonAdapter
import org.transdroid.protocol.DaemonType

/**
 * The seam between shared app code (app/src/main) and debug-only tooling. This is the debug
 * build's version of this object; app/src/release/.../DebugTools.kt is a same-named, same-API
 * no-op twin. Because the two live in different build-type source sets with identical package
 * and class names, shared code can reference `DebugTools` unconditionally - Gradle compiles
 * whichever twin matches the variant being built - so a release build never even compiles this
 * file or [DummyDaemonAdapter], let alone ships them.
 */
object DebugTools {
    private const val DUMMY_MARKER_KEY = "dummy"
    private const val DUMMY_MARKER_VALUE = "true"

    val dummyServerAvailable: Boolean = true

    /** A ready-to-save profile for a new dummy test server; see [DummyDaemonAdapter]. */
    fun newDummyServerProfile(): ServerProfile = ServerProfile(
        id = UUID.randomUUID().toString(),
        name = "Dummy",
        type = DaemonType.TRANSMISSION,
        host = "dummy",
        port = 0,
        extras = mapOf(DUMMY_MARKER_KEY to DUMMY_MARKER_VALUE),
    )

    fun isDummyServer(profile: ServerProfile): Boolean = profile.extras[DUMMY_MARKER_KEY] == DUMMY_MARKER_VALUE

    /** A fresh [DummyDaemonAdapter] for [profile] if it's a dummy server, else null. */
    fun adapterFor(profile: ServerProfile): DaemonAdapter? = if (isDummyServer(profile)) DummyDaemonAdapter() else null
}
