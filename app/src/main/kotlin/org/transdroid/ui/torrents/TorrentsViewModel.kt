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
package org.transdroid.ui.torrents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.transdroid.AppContainer
import org.transdroid.appContainer
import org.transdroid.data.ServerProfile
import org.transdroid.errorlog.ErrorLog
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentFile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.Tracker

/** User-facing error kinds; mapped to localized strings in the UI layer. */
sealed class UiError {
    data class Connection(val host: String) : UiError()
    data class Authentication(val detail: String? = null) : UiError()
    data object Ssl : UiError()
    data class Unexpected(val detail: String? = null) : UiError()
}

internal fun Throwable.toUiError(host: String): UiError {
    ErrorLog.log("Daemon", "Error for $host", this)
    return when (this) {
        is DaemonException.Connection -> UiError.Connection(host)
        is DaemonException.Authentication -> UiError.Authentication(message)
        is DaemonException.UntrustedServer -> UiError.Ssl
        is DaemonException.UnexpectedResponse -> UiError.Unexpected(message)
        else -> UiError.Unexpected()
    }
}

/** Sentinel [TorrentsUiState.labelFilters] entry for the drawer's "No label" bucket; never a real torrent label. */
const val NO_LABEL = "\u0000no-label"

enum class TorrentFilter {
    ALL, DOWNLOADING, SEEDING, COMPLETED, PAUSED, ERROR;

    fun matches(torrent: Torrent): Boolean = when (this) {
        ALL -> true
        DOWNLOADING -> torrent.status == TorrentStatus.DOWNLOADING || torrent.status == TorrentStatus.QUEUED ||
            torrent.status == TorrentStatus.CHECKING
        SEEDING -> torrent.status == TorrentStatus.SEEDING
        COMPLETED -> torrent.isFinished
        PAUSED -> torrent.status == TorrentStatus.PAUSED
        ERROR -> torrent.status == TorrentStatus.ERROR
    }
}

enum class TorrentSort {
    DATE_ADDED, NAME, DOWNLOAD_SPEED, RATIO;

    /**
     * The direction a field starts in the first time it's selected - matches Transdroid 2's
     * per-field defaults (newest/fastest/highest first; name A-to-Z).
     */
    val defaultDescending: Boolean
        get() = this != NAME

    private fun ascending(): Comparator<Torrent> = when (this) {
        DATE_ADDED -> compareBy { it.addedTimestamp ?: Long.MIN_VALUE }
        NAME -> compareBy { it.name.lowercase() }
        DOWNLOAD_SPEED -> compareBy { it.downloadRate }
        RATIO -> compareBy { it.ratio }
    }

    fun comparator(descending: Boolean): Comparator<Torrent> =
        ascending().let { if (descending) it.reversed() else it }
}

data class TorrentsUiState(
    val activeProfile: ServerProfile? = null,
    /** All configured servers, for the drawer's server switcher. */
    val allProfiles: List<ServerProfile> = emptyList(),
    /** False until the profile store has emitted, so we don't flash the welcome screen. */
    val profilesLoaded: Boolean = false,
    /** Number of configured servers; single-server flows can skip confirmation steps. */
    val profileCount: Int = 0,
    val torrents: List<Torrent> = emptyList(),
    /** True after the first successful load for the active profile. */
    val hasLoaded: Boolean = false,
    val refreshing: Boolean = false,
    val error: UiError? = null,
    val filter: TorrentFilter = TorrentFilter.ALL,
    /** OR semantics: a torrent matches if it carries any of the selected labels. */
    val labelFilters: Set<String> = emptySet(),
    /** Free-text filter over torrent names, from the drawer's filter field. */
    val nameQuery: String = "",
    val sort: TorrentSort = TorrentSort.DATE_ADDED,
    val sortDescending: Boolean = TorrentSort.DATE_ADDED.defaultDescending,
    val selectedTorrentId: String? = null,
    val files: Map<String, List<TorrentFile>> = emptyMap(),
    val trackers: Map<String, List<Tracker>> = emptyMap(),
    /** Whether the active server's client exposes an alternative ("turtle") speed limits toggle. */
    val altSpeedSupported: Boolean = false,
    val altSpeedEnabled: Boolean = false,
) {
    val availableLabels: List<String>
        get() = torrents.flatMap { it.labels }.distinct().sorted()

    /** Whether any torrent carries no label at all, i.e. whether the drawer's "No label" bucket applies. */
    val hasUnlabeledTorrents: Boolean
        get() = torrents.any { it.labels.isEmpty() }

    val visibleTorrents: List<Torrent>
        get() = torrents
            .filter {
                filter.matches(it) &&
                    (
                        labelFilters.isEmpty() ||
                            it.labels.any { label -> label in labelFilters } ||
                            (NO_LABEL in labelFilters && it.labels.isEmpty())
                        ) &&
                    (nameQuery.isBlank() || it.name.contains(nameQuery, ignoreCase = true))
            }
            .sortedWith(sort.comparator(sortDescending).thenBy { it.name.lowercase() })

    val selectedTorrent: Torrent?
        get() = torrents.firstOrNull { it.id == selectedTorrentId }

    fun countFor(filter: TorrentFilter): Int = torrents.count { filter.matches(it) }

    fun countForLabel(label: String): Int =
        if (label == NO_LABEL) torrents.count { it.labels.isEmpty() } else torrents.count { label in it.labels }

    val totalDownloadRate: Long
        get() = torrents.sumOf { it.downloadRate }

    val totalUploadRate: Long
        get() = torrents.sumOf { it.uploadRate }

    val activeCount: Int
        get() = torrents.count { it.status == TorrentStatus.DOWNLOADING }

    val sharingCount: Int
        get() = torrents.count { it.status == TorrentStatus.SEEDING }
}

class TorrentsViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(TorrentsUiState())
    val ui: StateFlow<TorrentsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(container.activeProfile, container.profilesRepository.profiles) { active, all ->
                active to all
            }.collect { (profile, all) ->
                _ui.update { state ->
                    val switched = profile?.id != state.activeProfile?.id
                    state.copy(
                        activeProfile = profile,
                        allProfiles = all,
                        profilesLoaded = true,
                        profileCount = all.size,
                        torrents = if (switched) emptyList() else state.torrents,
                        hasLoaded = if (switched) false else state.hasLoaded,
                        files = if (switched) emptyMap() else state.files,
                        trackers = if (switched) emptyMap() else state.trackers,
                        error = if (switched) null else state.error,
                    )
                }
                if (profile != null) refresh(showSpinner = false)
            }
        }
    }

    /** Runs while the torrents UI is started; cancellation stops the polling. */
    suspend fun pollLoop() {
        while (currentCoroutineContext().isActive) {
            refreshNow(showSpinner = false)
            delay(container.settingsRepository.pollIntervalSeconds.first() * 1000L)
        }
    }

    fun refresh(showSpinner: Boolean = true) {
        viewModelScope.launch { refreshNow(showSpinner) }
    }

    private suspend fun refreshNow(showSpinner: Boolean) {
        val profile = _ui.value.activeProfile ?: return
        if (showSpinner) _ui.update { it.copy(refreshing = true) }
        try {
            val adapter = container.adapterFor(profile)
            val torrents = adapter.listTorrents()
            val altSpeedEnabled = if (adapter.supportsAltSpeedLimits) {
                // A transient failure here shouldn't blank the whole list; just keep the last-known state.
                try {
                    adapter.isAltSpeedLimitsEnabled()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _ui.value.altSpeedEnabled
                }
            } else {
                false
            }
            _ui.update {
                if (it.activeProfile?.id != profile.id) it
                else it.copy(
                    torrents = torrents,
                    hasLoaded = true,
                    refreshing = false,
                    error = null,
                    altSpeedSupported = adapter.supportsAltSpeedLimits,
                    altSpeedEnabled = altSpeedEnabled,
                )
            }
            container.widgetStateRepository.update(profile.displayName, torrents)
            // Keep an open torrent-details screen's Files/Trackers tabs current too, not just the
            // main list - they're otherwise only ever loaded once, when the screen first opens.
            _ui.value.selectedTorrentId
                ?.takeIf { id -> torrents.any { it.id == id } }
                ?.let { refreshDetails(adapter, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update {
                // A late failure from a server the user already switched away from is stale
                if (it.activeProfile?.id != profile.id) it
                else it.copy(refreshing = false, error = e.toUiError(profile.host))
            }
        }
    }

    private suspend fun refreshDetails(adapter: org.transdroid.protocol.DaemonAdapter, torrentId: String) {
        try {
            val files = adapter.listFiles(torrentId)
            _ui.update { it.copy(files = it.files + (torrentId to files)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep showing the last-known files rather than clearing them on a transient failure
        }
        try {
            val trackers = adapter.listTrackers(torrentId)
            _ui.update { it.copy(trackers = it.trackers + (torrentId to trackers)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep showing the last-known trackers rather than clearing them on a transient failure
        }
    }

    fun setFilter(filter: TorrentFilter) {
        _ui.update { it.copy(filter = filter) }
    }

    /** Picking the already-active sort field again reverses its direction instead of no-op'ing. */
    fun setSort(sort: TorrentSort) {
        _ui.update {
            if (it.sort == sort) {
                it.copy(sortDescending = !it.sortDescending)
            } else {
                it.copy(sort = sort, sortDescending = sort.defaultDescending)
            }
        }
    }

    fun toggleLabelFilter(label: String) {
        _ui.update {
            it.copy(labelFilters = if (label in it.labelFilters) it.labelFilters - label else it.labelFilters + label)
        }
    }

    fun setNameQuery(query: String) {
        _ui.update { it.copy(nameQuery = query) }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch { container.settingsRepository.setActiveServer(profileId) }
    }

    fun setFilePriority(torrentId: String, file: TorrentFile, priority: FilePriority) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                val adapter = container.adapterFor(profile)
                adapter.setFilePriority(torrentId, file.index, priority)
                val files = adapter.listFiles(torrentId)
                _ui.update { it.copy(files = it.files + (torrentId to files)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.toUiError(profile.host)) }
            }
        }
    }

    fun select(torrentId: String?) {
        _ui.update { it.copy(selectedTorrentId = torrentId) }
    }

    fun toggleStartPause(torrent: Torrent) {
        runAction { adapter ->
            if (torrent.status == TorrentStatus.PAUSED) adapter.start(torrent.id) else adapter.pause(torrent.id)
        }
    }

    fun remove(torrent: Torrent, deleteData: Boolean) {
        runAction { adapter -> adapter.remove(torrent.id, deleteData) }
    }

    /** Flips alternative ("turtle") speed limits; optimistic, since a poll cycle would confirm it anyway. */
    fun toggleAltSpeed() {
        val profile = _ui.value.activeProfile ?: return
        val target = !_ui.value.altSpeedEnabled
        _ui.update { it.copy(altSpeedEnabled = target) }
        viewModelScope.launch {
            try {
                container.adapterFor(profile).setAltSpeedLimitsEnabled(target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update {
                    if (it.activeProfile?.id == profile.id) it.copy(altSpeedEnabled = !target, error = e.toUiError(profile.host))
                    else it
                }
            }
        }
    }

    fun loadFiles(torrentId: String) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                val files = container.adapterFor(profile).listFiles(torrentId)
                _ui.update { it.copy(files = it.files + (torrentId to files)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Leave the files section empty; the list-level error banner covers connectivity
            }
        }
    }

    fun loadTrackers(torrentId: String) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                val trackers = container.adapterFor(profile).listTrackers(torrentId)
                _ui.update { it.copy(trackers = it.trackers + (torrentId to trackers)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Leave the trackers section empty; the list-level error banner covers connectivity
            }
        }
    }

    /** Adds a torrent by magnet/URL; invokes [onResult] with null on success. */
    fun add(url: String, startPaused: Boolean = false, onResult: (UiError?) -> Unit) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                container.adapterFor(profile).addByUrl(url, startPaused)
                refreshNow(showSpinner = false)
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(e.toUiError(profile.host))
            }
        }
    }

    /** Adds a torrent from the raw contents of a .torrent file. */
    fun addFile(fileName: String, contents: ByteArray, startPaused: Boolean = false, onResult: (UiError?) -> Unit) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                container.adapterFor(profile).addByFile(fileName, contents, startPaused)
                refreshNow(showSpinner = false)
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(e.toUiError(profile.host))
            }
        }
    }

    private fun runAction(action: suspend (org.transdroid.protocol.DaemonAdapter) -> Unit) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                action(container.adapterFor(profile))
                refreshNow(showSpinner = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.toUiError(profile.host)) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TorrentsViewModel(checkNotNull(this[APPLICATION_KEY]).appContainer) }
        }
    }
}
