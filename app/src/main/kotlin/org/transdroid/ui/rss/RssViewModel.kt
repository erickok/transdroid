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
package org.transdroid.ui.rss

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.transdroid.AppContainer
import org.transdroid.appContainer
import org.transdroid.data.RssFeed
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.rss.RssItem
import org.transdroid.ui.torrents.UiError
import org.transdroid.ui.torrents.toUiError

/** One feed item merged into the all-feeds timeline, tagged with the feed it came from. */
data class RssEntry(
    val feed: RssFeed,
    val item: RssItem,
    /** Published after [feed]'s last-viewed marker, from before this fetch updated it. */
    val isNew: Boolean,
) {
    /**
     * Stable enough to track "already added this session" without a real item id. Scoped to the
     * feed because the same torrent often shows up in several feeds (e.g. overlapping searches on
     * one site), and this doubles as the LazyColumn key, which must be unique.
     */
    val key: String
        get() = feed.id + "|" + (item.torrentUrl ?: item.link ?: item.title)
}

/**
 * Merges per-feed fetch results into one newest-first timeline. Null results are failed feeds
 * and are skipped. Repeated items within one feed are dropped, keeping the first, so every
 * [RssEntry.key] is unique - a duplicate key crashes the RSS list.
 */
internal fun mergeFeedEntries(perFeedResults: List<List<RssEntry>?>): List<RssEntry> =
    perFeedResults.filterNotNull().flatten()
        .distinctBy { it.key }
        .sortedByDescending { it.item.timestamp ?: Long.MIN_VALUE }

/** Whether [entry] can be picked in selection mode: it has a torrent link and isn't added yet. */
internal fun canSelect(entry: RssEntry, addedKeys: Set<String>): Boolean =
    entry.item.torrentUrl != null && entry.key !in addedKeys

/**
 * The torrents a bulk add sends to the server: one per distinct link, so the same torrent
 * selected under two feeds is only added once.
 */
internal fun bulkAddTargets(selected: List<RssEntry>, addedKeys: Set<String>): List<RssEntry> =
    selected.filter { canSelect(it, addedKeys) }.distinctBy { it.item.torrentUrl }

/**
 * Adds each target's torrent link with [add], one by one; one failing doesn't stop the others.
 * Returns the links that were added.
 */
internal suspend fun addEach(targets: List<RssEntry>, add: suspend (url: String) -> Unit): Set<String> {
    val added = mutableSetOf<String>()
    for (target in targets) {
        val url = target.item.torrentUrl ?: continue
        try {
            add(url)
            added += url
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Counted as failed by the caller: targets minus what was added
        }
    }
    return added
}

/** The keys of every row showing one of [urls], so a torrent added from one feed shows as added under the others too. */
internal fun keysForUrls(entries: List<RssEntry>, urls: Set<String>): Set<String> =
    entries.filter { it.item.torrentUrl in urls }.mapTo(mutableSetOf()) { it.key }

/** How a bulk add went: [failed] of [total] torrents couldn't be added. */
data class RssBulkAddResult(val total: Int, val failed: Int)

data class RssUiState(
    val loading: Boolean = false,
    val entries: List<RssEntry> = emptyList(),
    val error: UiError? = null,
    /** Null means "all feeds". */
    val selectedFeedId: String? = null,
    val newOnly: Boolean = false,
    val lastUpdatedTimestamp: Long? = null,
    val addedKeys: Set<String> = emptySet(),
    val addedItemTitle: String? = null,
    val addError: UiError? = null,
    val activeProfile: ServerProfile? = null,
    /** Download locations previously used on the active server; see [org.transdroid.ui.components.AddTorrentOptionsSheet]. */
    val recentDownloadLocations: List<String> = emptyList(),
    /** Keys ([RssEntry.key]) picked in selection mode (long-press); empty when not selecting. */
    val selection: Set<String> = emptySet(),
    /** The outcome of the last bulk add, until the screen has shown it. */
    val bulkAddResult: RssBulkAddResult? = null,
    /** A bulk add is sending its torrents; rows turn "Added" one by one meanwhile. */
    val bulkAdding: Boolean = false,
) {
    val visibleEntries: List<RssEntry>
        get() = entries.filter { (selectedFeedId == null || it.feed.id == selectedFeedId) && (!newOnly || it.isNew) }

    val newCount: Int
        get() = visibleEntries.count { it.isNew }

    val selecting: Boolean
        get() = selection.isNotEmpty()

    /** The torrents "Download" will add: the selected rows, minus repeats of the same link. */
    val bulkTargets: List<RssEntry>
        get() = bulkAddTargets(visibleEntries.filter { it.key in selection }, addedKeys)

    /** Drops selected keys that are no longer visible, after a refresh or a filter change. */
    fun withPrunedSelection(): RssUiState {
        if (selection.isEmpty()) return this
        val visibleKeys = visibleEntries.mapTo(mutableSetOf()) { it.key }
        return copy(selection = selection intersect visibleKeys)
    }
}

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class RssViewModel(private val container: AppContainer) : ViewModel() {

    val feeds: StateFlow<List<RssFeed>> = container.profilesRepository.feeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(RssUiState())
    val ui: StateFlow<RssUiState> = _ui.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            container.activeProfile.collect { profile -> _ui.update { it.copy(activeProfile = profile) } }
        }
        viewModelScope.launch {
            container.activeProfile.flatMapLatest { profile ->
                profile?.let { container.settingsRepository.recentDownloadLocations(it.id) } ?: flowOf(emptyList())
            }.collect { locations -> _ui.update { it.copy(recentDownloadLocations = locations) } }
        }
    }

    fun newFeedId(): String = UUID.randomUUID().toString()

    fun saveFeed(feed: RssFeed) {
        viewModelScope.launch { container.profilesRepository.saveFeed(feed) }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch { container.profilesRepository.deleteFeed(feedId) }
    }

    fun setSelectedFeed(feedId: String?) {
        _ui.update { it.copy(selectedFeedId = feedId).withPrunedSelection() }
    }

    fun setNewOnly(newOnly: Boolean) {
        _ui.update { it.copy(newOnly = newOnly).withPrunedSelection() }
    }

    /** Long-press: starts selection mode with [entry] selected (or adds it). */
    fun startSelection(entry: RssEntry) {
        // Not while a bulk add runs: its rows aren't all marked as added yet, so they could be sent twice
        _ui.update { if (!it.bulkAdding && canSelect(entry, it.addedKeys)) it.copy(selection = it.selection + entry.key) else it }
    }

    /** A tap while selecting; deselecting the last item ends selection mode. */
    fun toggleSelected(entry: RssEntry) {
        _ui.update {
            when {
                entry.key in it.selection -> it.copy(selection = it.selection - entry.key)
                canSelect(entry, it.addedKeys) -> it.copy(selection = it.selection + entry.key)
                else -> it
            }
        }
    }

    /** Selects every item the current feed and "new only" filters show that can still be added. */
    fun selectAllVisible() {
        _ui.update { state ->
            state.copy(selection = state.selection + state.visibleEntries.filter { canSelect(it, state.addedKeys) }.map { it.key })
        }
    }

    fun clearSelection() {
        _ui.update { it.copy(selection = emptySet()) }
    }

    fun consumeBulkAddResult() {
        _ui.update { it.copy(bulkAddResult = null) }
    }

    /** Fetches every configured feed in parallel and merges their items into one timeline. */
    fun refresh() {
        fetchJob?.cancel()
        _ui.update { it.copy(loading = true, error = null) }
        fetchJob = viewModelScope.launch {
            // `feeds` is a stateIn StateFlow that may not have emitted yet on the very first
            // collection (e.g. right after navigating here); fall back to the repository
            // directly rather than racing it and treating a real feed list as empty.
            val configuredFeeds = feeds.value.ifEmpty { container.profilesRepository.feeds.first() }
            if (configuredFeeds.isEmpty()) {
                _ui.update { it.copy(entries = emptyList(), loading = false, error = null) }
                return@launch
            }
            val perFeedResults = coroutineScope {
                configuredFeeds.map { feed ->
                    async { fetchFeed(feed) }
                }.map { it.await() }
            }
            val entries = mergeFeedEntries(perFeedResults)
            _ui.update {
                it.copy(
                    loading = false,
                    entries = entries,
                    // Only surface an error if every feed failed - a partial fetch still has
                    // something useful to show, so a single broken feed shouldn't blank the screen.
                    error = if (entries.isEmpty() && perFeedResults.all { r -> r == null }) UiError.Unexpected() else null,
                    lastUpdatedTimestamp = System.currentTimeMillis() / 1000,
                ).withPrunedSelection()
            }
        }
    }

    /** Null on failure - the caller treats that feed as skipped rather than failing the whole refresh. */
    private suspend fun fetchFeed(feed: RssFeed): List<RssEntry>? {
        val sinceTimestamp = feed.lastViewedTimestamp
        return try {
            val channel = container.rssFetcher.fetch(feed.url)
            val newest = channel.items.firstNotNullOfOrNull { it.timestamp }
            if (newest != null && newest != feed.lastViewedTimestamp) {
                container.profilesRepository.markFeedViewed(feed.id, newest)
            }
            channel.items.map { item ->
                val itemTimestamp = item.timestamp
                RssEntry(
                    feed = feed,
                    item = item,
                    isNew = sinceTimestamp != null && itemTimestamp != null && itemTimestamp > sinceTimestamp,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Sends an item's torrent link to the active server. */
    fun addItem(entry: RssEntry, startPaused: Boolean = false, downloadLocation: String? = null) {
        val url = entry.item.torrentUrl ?: return
        viewModelScope.launch {
            val profile = container.activeProfile.first()
            if (profile == null) {
                _ui.update { it.copy(addError = UiError.Unexpected()) }
                return@launch
            }
            try {
                container.adapterFor(profile).addByUrl(url, startPaused, downloadLocation)
                if (!downloadLocation.isNullOrBlank()) {
                    container.settingsRepository.addRecentDownloadLocation(profile.id, downloadLocation.trim())
                }
                _ui.update {
                    it.copy(
                        addedKeys = it.addedKeys + keysForUrls(it.entries, setOf(url)),
                        addedItemTitle = entry.item.title,
                        addError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(addError = e.toUiError(profile.host), addedItemTitle = null) }
            }
        }
    }

    /**
     * Sends every selected item's torrent link to the active server, one by one, then ends
     * selection mode and reports how many were added. One failing doesn't stop the others.
     */
    fun addSelected(startPaused: Boolean = false, downloadLocation: String? = null) {
        if (_ui.value.bulkAdding) return
        val targets = _ui.value.bulkTargets
        _ui.update { it.copy(selection = emptySet(), bulkAdding = targets.isNotEmpty()) }
        if (targets.isEmpty()) return
        // viewModelScope outlives the RSS screen (the view model belongs to the activity), so
        // leaving the screen mid-way doesn't stop the remaining adds
        viewModelScope.launch {
            try {
                val profile = container.activeProfile.first()
                if (profile == null) {
                    _ui.update { it.copy(addError = UiError.Unexpected()) }
                    return@launch
                }
                try {
                    val adapter = container.adapterFor(profile)
                    val addedUrls = addEach(targets) { url ->
                        adapter.addByUrl(url, startPaused, downloadLocation)
                        _ui.update { it.copy(addedKeys = it.addedKeys + keysForUrls(it.entries, setOf(url))) }
                    }
                    _ui.update {
                        it.copy(bulkAddResult = RssBulkAddResult(total = targets.size, failed = targets.size - addedUrls.size))
                    }
                    if (addedUrls.isNotEmpty() && !downloadLocation.isNullOrBlank()) {
                        container.settingsRepository.addRecentDownloadLocation(profile.id, downloadLocation.trim())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // No adapter for this server (bad settings), or saving the recent location failed
                    _ui.update { it.copy(addError = e.toUiError(profile.host)) }
                }
            } finally {
                _ui.update { it.copy(bulkAdding = false) }
            }
        }
    }

    fun clearAddResult() {
        _ui.update { it.copy(addedItemTitle = null, addError = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RssViewModel(checkNotNull(this[APPLICATION_KEY]).appContainer) }
        }
    }
}
