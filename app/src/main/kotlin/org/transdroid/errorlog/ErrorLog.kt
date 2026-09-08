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
package org.transdroid.errorlog

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small capped on-device error log, backed by a flat file so recent entries survive process
 * death (crashes included). Each [append] is one "entry" - a multi-line crash dump still only
 * costs one slot, so a single crash can't evict the rest of the history. No Android dependency,
 * so this is unit-testable with a plain [File].
 */
internal class ErrorLogStore(private val file: File, private val maxEntries: Int = 200) {

    private val entries = ArrayDeque<String>()

    init {
        try {
            if (file.exists()) {
                // Entries are separated by a blank line; a plain "\n" split would break multi-line
                // crash dumps back into several entries.
                file.readText().split(ENTRY_SEPARATOR).filter { it.isNotBlank() }.forEach {
                    entries.addLast(it)
                }
                trim()
            }
        } catch (e: Exception) {
            // A missing/corrupt log file must never prevent startup - just start empty.
        }
    }

    @Synchronized
    fun append(entry: String) {
        entries.addLast(entry)
        trim()
        try {
            file.writeText(entries.joinToString(ENTRY_SEPARATOR))
        } catch (e: Exception) {
            // Logging must never itself throw.
        }
    }

    @Synchronized
    fun entries(): List<String> = entries.toList()

    private fun trim() {
        while (entries.size > maxEntries) entries.removeFirst()
    }

    private companion object {
        const val ENTRY_SEPARATOR = "\n\n"
    }
}

/**
 * Global logging facade for on-device error capture, feeding the "Send error report" flow in
 * Settings. Deliberately a global object rather than threaded through [org.transdroid.AppContainer]:
 * [org.transdroid.ui.torrents.toUiError] is a pure extension function called from every ViewModel's
 * catch blocks with no receiver access to a container, so a shared logging facade (the same pattern
 * Timber/android.util.Log use) is the smaller, more idiomatic choice than adding a dependency
 * parameter to every call site.
 *
 * Never pass a whole ServerProfile/SearchProviderConfig/RssFeed into [log] - only pre-vetted safe
 * fields (a host, a display name), matching how [org.transdroid.ui.torrents.toUiError] already only
 * receives a bare host string. Usernames, passwords, API keys, custom headers, and feed/indexer URLs
 * (which can embed passkeys) must never reach this log.
 */
object ErrorLog {

    private var store: ErrorLogStore? = null
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        if (store != null) return
        store = ErrorLogStore(File(context.filesDir, "errorlog.txt"))
    }

    /** One compact entry for a regular caught exception - no stack trace. */
    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val suffix = throwable?.let { " (${it.javaClass.simpleName}: ${it.message})" }.orEmpty()
        store?.append("${timestamp()} $tag: $message$suffix")
    }

    /** One entry for an uncaught crash: a header plus every stack frame, mirroring Transdroid 2's crash log. */
    fun logCrash(throwable: Throwable) {
        val frames = throwable.stackTrace.joinToString("\n") { "  at $it" }
        store?.append("${timestamp()} FATAL: $throwable\n$frames")
    }

    fun entries(): List<String> = store?.entries().orEmpty()

    private fun timestamp(): String = timestampFormat.format(Date())
}
