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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.transdroid.AppContainer
import org.transdroid.appContainer
import org.transdroid.data.PeerSort
import org.transdroid.data.ServerProfile
import org.transdroid.errorlog.ErrorLog
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.Peer
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

    companion object {
        /** Tolerates a stored name that no longer exists, e.g. after a constant is renamed. */
        fun fromName(name: String?): TorrentFilter = entries.firstOrNull { it.name == name } ?: ALL
    }
}

enum class BulkAction { START, PAUSE, CHECK, REMOVE, LABEL, MOVE }

/** How a bulk action went: [failed] of [total] torrents couldn't be updated. */
data class BulkResult(val action: BulkAction, val total: Int, val failed: Int)

/** The drawer filters in use on one server; remembered per server across app starts. */
data class TorrentFilters(
    val status: TorrentFilter = TorrentFilter.ALL,
    val labels: Set<String> = emptySet(),
    val trackers: Set<String> = emptySet(),
)

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

    companion object {
        val DEFAULT = DATE_ADDED

        /** Tolerates a stored name that no longer exists, e.g. after a constant is renamed. */
        fun fromName(name: String?): TorrentSort = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
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
    /** OR semantics: a torrent matches if any of its [Torrent.trackers] is selected. */
    val trackerFilters: Set<String> = emptySet(),
    /** Free-text filter over torrent names, from the drawer's filter field. */
    val nameQuery: String = "",
    val sort: TorrentSort = TorrentSort.DATE_ADDED,
    val sortDescending: Boolean = TorrentSort.DATE_ADDED.defaultDescending,
    val selectedTorrentId: String? = null,
    /** Torrents picked in selection mode (long-press); empty when not selecting. */
    val selection: Set<String> = emptySet(),
    /** The outcome of the last bulk action, until the screen has shown it. */
    val bulkResult: BulkResult? = null,
    /** Whether to show the one-time "long-press to select" tip above the list. */
    val showSelectionHint: Boolean = false,
    val files: Map<String, List<TorrentFile>> = emptyMap(),
    val trackers: Map<String, List<Tracker>> = emptyMap(),
    val peers: Map<String, List<Peer>> = emptyMap(),
    /** The order the details screen lists [peers] in; a preference, see [org.transdroid.data.SettingsRepository.peerSort]. */
    val peerSort: PeerSort = PeerSort.DEFAULT,
    /** Whether the active server's client exposes an alternative ("turtle") speed limits toggle. */
    val altSpeedSupported: Boolean = false,
    val altSpeedEnabled: Boolean = false,
    /** Download locations previously used on the active server, most-recent first; see [org.transdroid.data.SettingsRepository.recentDownloadLocations]. */
    val recentDownloadLocations: List<String> = emptyList(),
) {
    val availableLabels: List<String>
        get() = torrents.flatMap { it.labels }.distinct().sorted()

    /** Whether any torrent carries no label at all, i.e. whether the drawer's "No label" bucket applies. */
    val hasUnlabeledTorrents: Boolean
        get() = torrents.any { it.labels.isEmpty() }

    /** Distinct tracker hostnames across all loaded torrents; empty for a client with no bulk tracker data (rTorrent). */
    val availableTrackers: List<String>
        get() = torrents.flatMap { it.trackers }.distinct().sorted()

    /**
     * The drawer's labels: [availableLabels] plus any selected label no torrent carries (anymore),
     * e.g. a filter restored from a previous session, so it can still be turned off.
     */
    val drawerLabels: List<String>
        get() = (availableLabels + (labelFilters - NO_LABEL)).distinct().sorted()

    /**
     * Whether the drawer shows its Labels section, which then always includes "No label" - also
     * when every torrent has a label, so users who label everything can check that nothing is
     * left unlabeled. Hidden when the server uses no labels at all (or a restored filter needs it).
     */
    val drawerShowsLabels: Boolean
        get() = drawerLabels.isNotEmpty() || NO_LABEL in labelFilters

    /** The drawer's trackers: [availableTrackers] plus any selected one, see [drawerLabels]. */
    val drawerTrackers: List<String>
        get() = (availableTrackers + trackerFilters).distinct().sorted()

    val visibleTorrents: List<Torrent>
        get() = torrents
            .filter {
                filter.matches(it) &&
                    (
                        labelFilters.isEmpty() ||
                            it.labels.any { label -> label in labelFilters } ||
                            (NO_LABEL in labelFilters && it.labels.isEmpty())
                        ) &&
                    (trackerFilters.isEmpty() || it.trackers.any { tracker -> tracker in trackerFilters }) &&
                    (nameQuery.isBlank() || it.name.contains(nameQuery, ignoreCase = true))
            }
            .sortedWith(sort.comparator(sortDescending).thenBy { it.name.lowercase() })

    val selecting: Boolean
        get() = selection.isNotEmpty()

    val selectedTorrents: List<Torrent>
        get() = torrents.filter { it.id in selection }

    val selectionCanStart: Boolean
        get() = selectedTorrents.any { it.status.canStart }

    val selectionCanPause: Boolean
        get() = selectedTorrents.any { !it.status.canStart }

    val selectedTorrent: Torrent?
        get() = torrents.firstOrNull { it.id == selectedTorrentId }

    fun countFor(filter: TorrentFilter): Int = torrents.count { filter.matches(it) }

    fun countForLabel(label: String): Int =
        if (label == NO_LABEL) torrents.count { it.labels.isEmpty() } else torrents.count { label in it.labels }

    fun countForTracker(tracker: String): Int = torrents.count { tracker in it.trackers }

    val totalDownloadRate: Long
        get() = torrents.sumOf { it.downloadRate }

    val totalUploadRate: Long
        get() = torrents.sumOf { it.uploadRate }

    val activeCount: Int
        get() = torrents.count { it.status == TorrentStatus.DOWNLOADING }

    val sharingCount: Int
        get() = torrents.count { it.status == TorrentStatus.SEEDING }
}

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class TorrentsViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(TorrentsUiState())
    val ui: StateFlow<TorrentsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(container.activeProfile, container.profilesRepository.profiles) { active, all ->
                active to all
            }.collect { (profile, all) ->
                // Restore the filters last used on this server, on a cold start or a server switch
                val restored = profile?.takeIf { it.id != _ui.value.activeProfile?.id }
                    ?.let { container.settingsRepository.torrentFilters(it.id).first() }
                _ui.update { state ->
                    val switched = profile?.id != state.activeProfile?.id
                    state.copy(
                        filter = restored?.status ?: state.filter,
                        labelFilters = restored?.labels ?: state.labelFilters,
                        trackerFilters = restored?.trackers ?: state.trackerFilters,
                        activeProfile = profile,
                        allProfiles = all,
                        profilesLoaded = true,
                        profileCount = all.size,
                        torrents = if (switched) emptyList() else state.torrents,
                        hasLoaded = if (switched) false else state.hasLoaded,
                        files = if (switched) emptyMap() else state.files,
                        trackers = if (switched) emptyMap() else state.trackers,
                        peers = if (switched) emptyMap() else state.peers,
                        error = if (switched) null else state.error,
                        selection = if (switched) emptySet() else state.selection,
                    )
                }
                if (profile != null) refresh(showSpinner = false)
            }
        }
        viewModelScope.launch {
            container.activeProfile.flatMapLatest { profile ->
                profile?.let { container.settingsRepository.recentDownloadLocations(it.id) } ?: flowOf(emptyList())
            }.collect { locations -> _ui.update { it.copy(recentDownloadLocations = locations) } }
        }
        viewModelScope.launch {
            container.settingsRepository.peerSort.collect { sort -> _ui.update { it.copy(peerSort = sort) } }
        }
        viewModelScope.launch {
            container.settingsRepository.selectionHintSeen.collect { seen -> _ui.update { it.copy(showSelectionHint = !seen) } }
        }
        viewModelScope.launch {
            combine(
                container.settingsRepository.torrentSort,
                container.settingsRepository.torrentSortDescending,
            ) { sort, descending -> sort to descending }
                .collect { (sort, descending) -> _ui.update { it.copy(sort = sort, sortDescending = descending) } }
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
                    // Torrents removed elsewhere drop out of the selection
                    selection = it.selection.intersect(torrents.mapTo(HashSet()) { t -> t.id }),
                    hasLoaded = true,
                    refreshing = false,
                    error = null,
                    altSpeedSupported = adapter.supportsAltSpeedLimits,
                    altSpeedEnabled = altSpeedEnabled,
                )
            }
            container.widgetStateRepository.update(profile.id, profile.displayName, torrents)
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
        try {
            val peers = fetchPeers(adapter, torrentId)
            _ui.update { it.copy(peers = it.peers + (torrentId to peers)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep showing the last-known peers rather than clearing them on a transient failure
        }
    }

    /** The daemon's peer list, with countries filled in from the local GeoIP database when one is downloaded. */
    private suspend fun fetchPeers(adapter: org.transdroid.protocol.DaemonAdapter, torrentId: String): List<Peer> =
        container.geoIpDatabase.withCountries(adapter.listPeers(torrentId))

    fun setFilter(filter: TorrentFilter) {
        _ui.update { it.copy(filter = filter) }
        saveFilters()
    }

    /** Remembers the active server's drawer filters, so a cold start shows the same list. */
    private fun saveFilters() {
        val state = _ui.value
        val profileId = state.activeProfile?.id ?: return
        val filters = TorrentFilters(state.filter, state.labelFilters, state.trackerFilters)
        viewModelScope.launch { container.settingsRepository.setTorrentFilters(profileId, filters) }
    }

    /**
     * Picking the already-active sort field again reverses its direction instead of no-op'ing.
     * Persisted via [org.transdroid.data.SettingsRepository.setTorrentSort] and fed back into
     * [ui] through the [init] collector above, so it survives a cold app start.
     */
    fun setSort(sort: TorrentSort) {
        val current = _ui.value
        val descending = if (current.sort == sort) !current.sortDescending else sort.defaultDescending
        viewModelScope.launch { container.settingsRepository.setTorrentSort(sort, descending) }
    }

    fun toggleLabelFilter(label: String) {
        _ui.update {
            it.copy(labelFilters = if (label in it.labelFilters) it.labelFilters - label else it.labelFilters + label)
        }
        saveFilters()
    }

    fun toggleTrackerFilter(tracker: String) {
        _ui.update {
            it.copy(trackerFilters = if (tracker in it.trackerFilters) it.trackerFilters - tracker else it.trackerFilters + tracker)
        }
        saveFilters()
    }

    /** Back to the full torrent list: no status, label, tracker or name filter. */
    fun clearFilters() {
        _ui.update {
            it.copy(filter = TorrentFilter.ALL, labelFilters = emptySet(), trackerFilters = emptySet(), nameQuery = "")
        }
        saveFilters()
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

    /** Long-press: starts selection mode with [torrentId] selected (or adds it). */
    fun startSelection(torrentId: String) {
        _ui.update { it.copy(selection = it.selection + torrentId) }
        if (_ui.value.showSelectionHint) dismissSelectionHint()
    }

    fun dismissSelectionHint() {
        _ui.update { it.copy(showSelectionHint = false) }
        viewModelScope.launch { container.settingsRepository.setSelectionHintSeen() }
    }

    /** A tap while selecting; deselecting the last torrent ends selection mode. */
    fun toggleSelected(torrentId: String) {
        _ui.update {
            it.copy(selection = if (torrentId in it.selection) it.selection - torrentId else it.selection + torrentId)
        }
    }

    /** Selects every torrent the current filters show. */
    fun selectAllVisible() {
        _ui.update { it.copy(selection = it.selection + it.visibleTorrents.map(Torrent::id)) }
    }

    fun clearSelection() {
        _ui.update { it.copy(selection = emptySet()) }
    }

    fun consumeBulkResult() {
        _ui.update { it.copy(bulkResult = null) }
    }

    fun startSelected() = runBulk(BulkAction.START, { it.status.canStart }) { adapter, t -> adapter.start(t.id) }

    fun pauseSelected() = runBulk(BulkAction.PAUSE, { !it.status.canStart }) { adapter, t -> adapter.pause(t.id) }

    fun checkSelected() = runBulk(BulkAction.CHECK) { adapter, t -> adapter.checkData(t.id) }

    fun removeSelected(deleteData: Boolean) = runBulk(BulkAction.REMOVE) { adapter, t -> adapter.remove(t.id, deleteData) }

    /** [label] null or blank clears it. */
    fun setLabelSelected(label: String?) =
        runBulk(BulkAction.LABEL) { adapter, t -> adapter.setLabel(t.id, label.orEmpty()) }

    fun setLocationSelected(location: String) {
        _ui.value.activeProfile?.let { profile -> viewModelScope.launch { rememberDownloadLocation(profile.id, location) } }
        runBulk(BulkAction.MOVE) { adapter, t -> adapter.setDownloadLocation(t.id, location) }
    }

    /**
     * Runs [action] on each selected torrent that [applies] to, one by one, then ends selection
     * mode and reports how many succeeded. One failing torrent doesn't stop the others.
     */
    private fun runBulk(
        kind: BulkAction,
        applies: (Torrent) -> Boolean = { true },
        action: suspend (org.transdroid.protocol.DaemonAdapter, Torrent) -> Unit,
    ) {
        val profile = _ui.value.activeProfile ?: return
        val targets = _ui.value.selectedTorrents.filter(applies)
        _ui.update { it.copy(selection = emptySet()) }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val adapter = container.adapterFor(profile)
            var failed = 0
            for (torrent in targets) {
                try {
                    action(adapter, torrent)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed++
                }
            }
            _ui.update { it.copy(bulkResult = BulkResult(kind, total = targets.size, failed = failed)) }
            refreshNow(showSpinner = false)
            delay(FOLLOW_UP_REFRESH_MILLIS)
            refreshNow(showSpinner = false)
        }
    }

    fun toggleStartPause(torrent: Torrent) {
        runAction { adapter ->
            if (torrent.status.canStart) adapter.start(torrent.id) else adapter.pause(torrent.id)
        }
    }

    fun remove(torrent: Torrent, deleteData: Boolean) {
        runAction { adapter -> adapter.remove(torrent.id, deleteData) }
    }

    /** [label] null or blank clears it. */
    fun setLabel(torrent: Torrent, label: String?) {
        runAction { adapter -> adapter.setLabel(torrent.id, label.orEmpty()) }
    }

    fun checkData(torrent: Torrent) {
        runAction { adapter -> adapter.checkData(torrent.id) }
    }

    /** Also remembered as a recent location, same as [add]/[addFile], so it offers itself back as a chip next time. */
    fun setDownloadLocation(torrent: Torrent, location: String) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                container.adapterFor(profile).setDownloadLocation(torrent.id, location)
                rememberDownloadLocation(profile.id, location)
                refreshNow(showSpinner = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.toUiError(profile.host)) }
            }
        }
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

    fun loadPeers(torrentId: String) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                val peers = fetchPeers(container.adapterFor(profile), torrentId)
                _ui.update { it.copy(peers = it.peers + (torrentId to peers)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Leave the peers section empty; the list-level error banner covers connectivity
            }
        }
    }

    /** Adds a torrent by magnet/URL; invokes [onResult] with null on success. */
    fun add(url: String, startPaused: Boolean = false, downloadLocation: String? = null, onResult: (UiError?) -> Unit) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                container.adapterFor(profile).addByUrl(url, startPaused, downloadLocation)
                rememberDownloadLocation(profile.id, downloadLocation)
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
    fun addFile(
        fileName: String,
        contents: ByteArray,
        startPaused: Boolean = false,
        downloadLocation: String? = null,
        onResult: (UiError?) -> Unit,
    ) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                container.adapterFor(profile).addByFile(fileName, contents, startPaused, downloadLocation)
                rememberDownloadLocation(profile.id, downloadLocation)
                refreshNow(showSpinner = false)
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(e.toUiError(profile.host))
            }
        }
    }

    private suspend fun rememberDownloadLocation(profileId: String, downloadLocation: String?) {
        if (!downloadLocation.isNullOrBlank()) {
            container.settingsRepository.addRecentDownloadLocation(profileId, downloadLocation.trim())
        }
    }

    private fun runAction(action: suspend (org.transdroid.protocol.DaemonAdapter) -> Unit) {
        val profile = _ui.value.activeProfile ?: return
        viewModelScope.launch {
            try {
                action(container.adapterFor(profile))
                refreshNow(showSpinner = false)
                // Daemons apply some actions asynchronously (qBittorrent only recalculates a
                // torrent's state on its next status update), so the immediate refresh can still
                // show the old state; a second one soon after beats waiting for the next poll
                delay(FOLLOW_UP_REFRESH_MILLIS)
                refreshNow(showSpinner = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.toUiError(profile.host)) }
            }
        }
    }

    companion object {
        private const val FOLLOW_UP_REFRESH_MILLIS = 2000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TorrentsViewModel(checkNotNull(this[APPLICATION_KEY]).appContainer) }
        }
    }
}
