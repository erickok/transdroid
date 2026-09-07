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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.transdroid.AppContainer
import org.transdroid.appContainer
import org.transdroid.data.RssFeed
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
    /** Stable enough to track "already added this session" without a real item id. */
    val key: String
        get() = item.torrentUrl ?: item.link ?: (feed.id + item.title)
}

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
) {
    val visibleEntries: List<RssEntry>
        get() = entries.filter { (selectedFeedId == null || it.feed.id == selectedFeedId) && (!newOnly || it.isNew) }

    val newCount: Int
        get() = visibleEntries.count { it.isNew }
}

class RssViewModel(private val container: AppContainer) : ViewModel() {

    val feeds: StateFlow<List<RssFeed>> = container.profilesRepository.feeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(RssUiState())
    val ui: StateFlow<RssUiState> = _ui.asStateFlow()

    private var fetchJob: Job? = null

    fun newFeedId(): String = UUID.randomUUID().toString()

    fun saveFeed(feed: RssFeed) {
        viewModelScope.launch { container.profilesRepository.saveFeed(feed) }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch { container.profilesRepository.deleteFeed(feedId) }
    }

    fun setSelectedFeed(feedId: String?) {
        _ui.update { it.copy(selectedFeedId = feedId) }
    }

    fun setNewOnly(newOnly: Boolean) {
        _ui.update { it.copy(newOnly = newOnly) }
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
            val entries = perFeedResults.filterNotNull().flatten()
                .sortedByDescending { it.item.timestamp ?: Long.MIN_VALUE }
            _ui.update {
                it.copy(
                    loading = false,
                    entries = entries,
                    // Only surface an error if every feed failed - a partial fetch still has
                    // something useful to show, so a single broken feed shouldn't blank the screen.
                    error = if (entries.isEmpty() && perFeedResults.all { r -> r == null }) UiError.Unexpected() else null,
                    lastUpdatedTimestamp = System.currentTimeMillis() / 1000,
                )
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
    fun addItem(entry: RssEntry) {
        val url = entry.item.torrentUrl ?: return
        viewModelScope.launch {
            val profile = container.activeProfile.first()
            if (profile == null) {
                _ui.update { it.copy(addError = UiError.Unexpected()) }
                return@launch
            }
            try {
                container.adapterFor(profile).addByUrl(url)
                _ui.update {
                    it.copy(addedKeys = it.addedKeys + entry.key, addedItemTitle = entry.item.title, addError = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(addError = e.toUiError(profile.host), addedItemTitle = null) }
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
