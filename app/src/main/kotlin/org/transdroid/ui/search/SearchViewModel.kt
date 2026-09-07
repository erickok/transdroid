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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.transdroid.AppContainer
import org.transdroid.appContainer
import org.transdroid.data.SearchProviderConfig
import org.transdroid.protocol.search.SearchResult
import org.transdroid.protocol.search.TorznabProvider
import org.transdroid.ui.torrents.UiError
import org.transdroid.ui.torrents.toUiError

data class SearchUiState(
    val query: String = "",
    val selectedProviderId: String? = null,
    val searching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val searched: Boolean = false,
    val error: UiError? = null,
    val addedTitle: String? = null,
    val addError: UiError? = null,
)

class SearchViewModel(private val container: AppContainer) : ViewModel() {

    val providers: StateFlow<List<SearchProviderConfig>> = container.profilesRepository.searchProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    private var searchJob: Job? = null

    fun setQuery(query: String) {
        _ui.update { it.copy(query = query) }
    }

    fun selectProvider(providerId: String) {
        _ui.update { it.copy(selectedProviderId = providerId) }
    }

    fun search() {
        val query = _ui.value.query.trim()
        if (query.isEmpty()) return
        val provider = providers.value.firstOrNull { it.id == _ui.value.selectedProviderId }
            ?: providers.value.firstOrNull()
            ?: return
        searchJob?.cancel()
        _ui.update { it.copy(searching = true, error = null) }
        searchJob = viewModelScope.launch {
            try {
                val results = TorznabProvider(
                    endpointUrl = provider.url,
                    apiKey = provider.apiKey,
                    httpClient = container.httpClient,
                    username = provider.username,
                    password = provider.password,
                ).search(query)
                    .sortedByDescending { it.seeders ?: -1 }
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

    fun addResult(result: SearchResult) {
        viewModelScope.launch {
            val profile = container.activeProfile.first()
            if (profile == null) {
                _ui.update { it.copy(addError = UiError.Unexpected()) }
                return@launch
            }
            try {
                container.adapterFor(profile).addByUrl(result.torrentUrl)
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
