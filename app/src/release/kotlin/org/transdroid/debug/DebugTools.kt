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

import org.transdroid.data.ServerProfile
import org.transdroid.protocol.DaemonAdapter

/**
 * Release-build twin of the debug-only `DebugTools` (app/src/debug/.../DebugTools.kt) - same
 * package and API, so shared app/src/main code can reference `DebugTools` unconditionally and
 * this is the no-op implementation Gradle actually compiles into every release variant.
 * DummyDaemonAdapter itself lives only under app/src/debug and is never compiled here at all.
 */
object DebugTools {
    val dummyServerAvailable: Boolean = false

    fun newDummyServerProfile(): Nothing = error("Dummy test servers are not available in release builds")

    fun isDummyServer(profile: ServerProfile): Boolean = false

    fun adapterFor(profile: ServerProfile): DaemonAdapter? = null
}
