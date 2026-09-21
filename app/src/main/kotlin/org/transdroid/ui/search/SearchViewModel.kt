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
package org.transdroid.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
import org.transdroid.data.SearchProviderConfig
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.search.SearchResult
import org.transdroid.protocol.search.TorznabProvider
import org.transdroid.ui.torrents.UiError
import org.transdroid.ui.torrents.toUiError

/** Client-side sort over an already-fetched result set; each field has one fixed "best first" direction. */
enum class SearchSort {
    SEEDERS, LEECHERS, SIZE, NEWEST;

    val comparator: Comparator<SearchResult>
        get() = when (this) {
            SEEDERS -> compareByDescending { it.seeders ?: -1 }
            LEECHERS -> compareByDescending { it.leechers ?: -1 }
            SIZE -> compareByDescending { it.sizeBytes ?: -1 }
            NEWEST -> compareByDescending { it.timestamp ?: -1 }
        }
}

data class SearchUiState(
    val query: String = "",
    val selectedProviderId: String? = null,
    val searching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val searched: Boolean = false,
    val error: UiError? = null,
    val addedTitle: String? = null,
    val addError: UiError? = null,
    val sort: SearchSort = SearchSort.SEEDERS,
    /** Null means "all indexers"; otherwise a [SearchResult.indexerName] to filter down to. */
    val indexerFilter: String? = null,
    val activeProfile: ServerProfile? = null,
    /** Download locations previously used on the active server; see [org.transdroid.ui.components.AddTorrentOptionsSheet]. */
    val recentDownloadLocations: List<String> = emptyList(),
) {
    /** Distinct tracker names found in [results] (via Jackett's per-item indexer tag), sorted. */
    val availableIndexers: List<String>
        get() = results.mapNotNull { it.indexerName }.distinct().sorted()

    val visibleResults: List<SearchResult>
        get() = results
            .filter { indexerFilter == null || it.indexerName == indexerFilter }
            .sortedWith(sort.comparator)
}

class SearchViewModel(private val container: AppContainer) : ViewModel() {

    val providers: StateFlow<List<SearchProviderConfig>> = container.profilesRepository.searchProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    private var searchJob: Job? = null

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

    fun setQuery(query: String) {
        _ui.update { it.copy(query = query) }
    }

    fun selectProvider(providerId: String) {
        _ui.update { it.copy(selectedProviderId = providerId) }
    }

    fun setSort(sort: SearchSort) {
        _ui.update { it.copy(sort = sort) }
    }

    fun setIndexerFilter(indexerName: String?) {
        _ui.update { it.copy(indexerFilter = indexerName) }
    }

    fun search() {
        val query = _ui.value.query.trim()
        if (query.isEmpty()) return
        val provider = providers.value.firstOrNull { it.id == _ui.value.selectedProviderId }
            ?: providers.value.firstOrNull()
            ?: return
        searchJob?.cancel()
        // A new search's results carry their own set of indexer names; a filter picked for the
        // previous search's results wouldn't necessarily mean anything for this one.
        _ui.update { it.copy(searching = true, error = null, indexerFilter = null) }
        searchJob = viewModelScope.launch {
            try {
                val results = TorznabProvider(
                    endpointUrl = provider.url,
                    apiKey = provider.apiKey,
                    httpClient = container.httpClient,
                    username = provider.username,
                    password = provider.password,
                ).search(query)
                _ui.update { it.copy(searching = false, results = results, searched = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update {
                    it.copy(searching = false, results = emptyList(), searched = true, error = e.toUiError(provider.displayName))
                }
            }
        }
    }

    fun addResult(result: SearchResult, startPaused: Boolean = false, downloadLocation: String? = null) {
        viewModelScope.launch {
            val profile = container.activeProfile.first()
            if (profile == null) {
                _ui.update { it.copy(addError = UiError.Unexpected()) }
                return@launch
            }
            try {
                val adapter = container.adapterFor(profile)
                val provider = providers.value.firstOrNull { it.id == _ui.value.selectedProviderId }
                    ?: providers.value.firstOrNull()
                val authedProvider = provider
                    ?.takeIf { it.username.isNotBlank() && it.password.isNotBlank() }
                    ?.takeIf { !result.torrentUrl.startsWith("magnet:") }
                if (authedProvider != null) {
                    // The download link commonly sits behind the exact same member-area login as
                    // the Torznab endpoint itself (common on Jackett-fronted seedboxes) - handing
                    // it straight to the daemon as a URL fails there, silently, since the daemon
                    // has no way to authenticate to it. Fetch the bytes here instead, where the
                    // indexer's own credentials are already known, then upload them directly.
                    val bytes = TorznabProvider(
                        endpointUrl = authedProvider.url,
                        apiKey = authedProvider.apiKey,
                        httpClient = container.httpClient,
                        username = authedProvider.username,
                        password = authedProvider.password,
                    ).fetchTorrentBytes(result.torrentUrl)
                    adapter.addByFile(torrentFileName(result.title), bytes, startPaused, downloadLocation)
                } else {
                    adapter.addByUrl(result.torrentUrl, startPaused, downloadLocation)
                }
                if (!downloadLocation.isNullOrBlank()) {
                    container.settingsRepository.addRecentDownloadLocation(profile.id, downloadLocation.trim())
                }
                _ui.update { it.copy(addedTitle = result.title, addError = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(addError = e.toUiError(profile.host), addedTitle = null) }
            }
        }
    }

    fun clearAddResult() {
        _ui.update { it.copy(addedTitle = null, addError = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(checkNotNull(this[APPLICATION_KEY]).appContainer) }
        }
    }
}

/** A filesystem-safe .torrent file name derived from a search result's title. */
private fun torrentFileName(title: String): String =
    title.trim().ifBlank { "torrent" }.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".torrent"
