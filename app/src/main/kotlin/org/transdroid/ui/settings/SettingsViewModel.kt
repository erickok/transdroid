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
package org.transdroid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.util.UUID
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.transdroid.AppContainer
import org.transdroid.appContainer
import org.transdroid.background.FinishedTorrentsWorker
import org.transdroid.data.BackupCrypto
import org.transdroid.data.ProfilesData
import org.transdroid.data.RssFeed
import org.transdroid.data.SearchProviderConfig
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.CertificateFingerprint
import org.transdroid.protocol.Tls
import org.transdroid.protocol.Xirvik
import org.transdroid.protocol.discovery.DiscoveredDaemon
import org.transdroid.ui.torrents.UiError
import org.transdroid.ui.torrents.toUiError

sealed class TestState {
    data object Idle : TestState()
    data object Testing : TestState()
    data class Success(val versionInfo: String) : TestState()
    data class Failure(val error: UiError) : TestState()
}

sealed class CertificateState {
    data object Idle : CertificateState()
    data object Fetching : CertificateState()
    data class Fetched(val fingerprint: CertificateFingerprint) : CertificateState()
    data class Failed(val error: UiError) : CertificateState()
}

data class DiscoveryState(
    val scanning: Boolean = false,
    val scanned: Boolean = false,
    val found: List<DiscoveredDaemon> = emptyList(),
)

sealed class XirvikState {
    data object Idle : XirvikState()
    data object Detecting : XirvikState()
    data class Success(val path: String) : XirvikState()
    /** The mount path couldn't be found; the caller falls back to the adapter's own /RPC2 default. */
    data object Failed : XirvikState()
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val profiles: StateFlow<List<ServerProfile>> = container.profilesRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeServerId: StateFlow<String?> = container.settingsRepository.activeServerId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    fun newProfileId(): String = UUID.randomUUID().toString()

    fun save(profile: ServerProfile) {
        viewModelScope.launch {
            // Read from the repository, not the StateFlow, which may not have emitted yet
            val firstServer = container.profilesRepository.profiles.first().isEmpty()
            container.profilesRepository.save(profile)
            if (firstServer) container.settingsRepository.setActiveServer(profile.id)
        }
    }

    fun delete(profileId: String) {
        viewModelScope.launch {
            val wasActive = container.settingsRepository.activeServerId.first() == profileId
            container.profilesRepository.delete(profileId)
            if (wasActive) container.settingsRepository.setActiveServer(null)
        }
    }

    fun testConnection(profile: ServerProfile) {
        _testState.value = TestState.Testing
        viewModelScope.launch {
            _testState.value = try {
                TestState.Success(container.adapterForTest(profile).testConnection())
            } catch (e: Exception) {
                TestState.Failure(e.toUiError(profile.host))
            }
        }
    }

    fun resetTestState() {
        _testState.value = TestState.Idle
        _certificateState.value = CertificateState.Idle
        _xirvikState.value = XirvikState.Idle
    }

    private val _certificateState = MutableStateFlow<CertificateState>(CertificateState.Idle)
    val certificateState: StateFlow<CertificateState> = _certificateState.asStateFlow()

    /** Reads the server's certificate fingerprint so the user can decide to trust it. */
    fun fetchCertificate(host: String, port: Int) {
        _certificateState.value = CertificateState.Fetching
        viewModelScope.launch {
            _certificateState.value = try {
                CertificateState.Fetched(Tls.fetchCertificate(host, port))
            } catch (e: Exception) {
                CertificateState.Failed(e.toUiError(host))
            }
        }
    }

    fun dismissCertificate() {
        _certificateState.value = CertificateState.Idle
    }

    private val _xirvikState = MutableStateFlow<XirvikState>(XirvikState.Idle)
    val xirvikState: StateFlow<XirvikState> = _xirvikState.asStateFlow()

    /** Looks up a Xirvik seedbox's per-account SCGI mount path from its auto-config endpoint. */
    fun detectXirvik(server: String, username: String, password: String) {
        _xirvikState.value = XirvikState.Detecting
        viewModelScope.launch {
            val path = Xirvik.detectRpcPath(container.httpClient, server, username, password)
            _xirvikState.value = if (path != null) XirvikState.Success(path) else XirvikState.Failed
        }
    }

    private val _discovery = MutableStateFlow(DiscoveryState())
    val discovery: StateFlow<DiscoveryState> = _discovery.asStateFlow()

    /** Scans the local network once per settings session; no-op while already scanning. */
    fun startLanScan() {
        if (_discovery.value.scanning) return
        _discovery.value = DiscoveryState(scanning = true)
        viewModelScope.launch {
            val found = try {
                container.lanDiscovery.scan()
            } catch (e: Exception) {
                emptyList()
            }
            _discovery.value = DiscoveryState(scanning = false, scanned = true, found = found)
        }
    }

    val searchProviders: StateFlow<List<SearchProviderConfig>> = container.profilesRepository.searchProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveSearchProvider(provider: SearchProviderConfig) {
        viewModelScope.launch { container.profilesRepository.saveSearchProvider(provider) }
    }

    fun deleteSearchProvider(providerId: String) {
        viewModelScope.launch { container.profilesRepository.deleteSearchProvider(providerId) }
    }

    val feeds: StateFlow<List<RssFeed>> = container.profilesRepository.feeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun newFeedId(): String = UUID.randomUUID().toString()

    fun saveFeed(feed: RssFeed) {
        viewModelScope.launch { container.profilesRepository.saveFeed(feed) }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch { container.profilesRepository.deleteFeed(feedId) }
    }

    val notifyFinished: StateFlow<Boolean> = container.settingsRepository.notifyFinished
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pollIntervalSeconds: StateFlow<Int> = container.settingsRepository.pollIntervalSeconds
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            org.transdroid.data.SettingsRepository.DEFAULT_POLL_INTERVAL_SECONDS,
        )

    fun setPollInterval(seconds: Int) {
        viewModelScope.launch { container.settingsRepository.setPollIntervalSeconds(seconds) }
    }

    /** Persists the toggle and (un)schedules the background check accordingly. */
    fun setNotifyFinished(context: android.content.Context, enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setNotifyFinished(enabled)
            if (enabled) {
                FinishedTorrentsWorker.schedule(context.applicationContext)
            } else {
                FinishedTorrentsWorker.cancel(context.applicationContext)
            }
        }
    }

    /** Serializes and encrypts the whole settings store with the given passphrase. */
    fun createBackup(passphrase: String, onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            val data = container.profilesRepository.currentData()
            val bytes = withContext(Dispatchers.Default) {
                BackupCrypto.encrypt(
                    backupJson.encodeToString(ProfilesData.serializer(), data).encodeToByteArray(),
                    passphrase.toCharArray(),
                )
            }
            onReady(bytes)
        }
    }

    fun restoreBackup(bytes: ByteArray, passphrase: String, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = try {
                val plaintext = withContext(Dispatchers.Default) {
                    BackupCrypto.decrypt(bytes, passphrase.toCharArray())
                }
                val data = backupJson.decodeFromString(ProfilesData.serializer(), plaintext.decodeToString())
                container.profilesRepository.replaceData(data)
                // The restored profiles carry their own ids; reset the selection if stale
                val activeId = container.settingsRepository.activeServerId.first()
                if (data.profiles.none { it.id == activeId }) {
                    container.settingsRepository.setActiveServer(data.profiles.firstOrNull()?.id)
                }
                RestoreResult.Success(data.profiles.size)
            } catch (e: AEADBadTagException) {
                RestoreResult.WrongPassphrase
            } catch (e: Exception) {
                RestoreResult.InvalidFile
            }
            onResult(result)
        }
    }

    sealed class RestoreResult {
        data class Success(val serverCount: Int) : RestoreResult()
        data object WrongPassphrase : RestoreResult()
        data object InvalidFile : RestoreResult()
    }

    companion object {
        private val backupJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(checkNotNull(this[APPLICATION_KEY]).appContainer) }
        }
    }
}
