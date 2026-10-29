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
package org.transdroid.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.json.Json
import org.transdroid.data.legacy.LegacyImport
import org.transdroid.data.legacy.LegacyMergeResult
import org.transdroid.data.legacy.LegacySettingsConverter

/**
 * Stores server profiles as one JSON document, encrypted at rest with a Keystore-bound
 * AES-GCM key (see [KeystoreProfilesCipher]) via a custom DataStore [Serializer]. The store
 * file is excluded from auto-backup because the key never leaves this device.
 */
class ServerProfilesRepository(context: Context, cipher: ProfilesCipher = KeystoreProfilesCipher) {

    private val dataStore: DataStore<ProfilesData> = DataStoreFactory.create(
        serializer = EncryptedProfilesSerializer(cipher),
        corruptionHandler = ReplaceFileCorruptionHandler { ProfilesData() },
        produceFile = { context.dataStoreFile(FILE_NAME) },
    )

    /**
     * The store's data, retrying briefly when the Keystore is transiently unavailable.
     * If it stays broken past the retries, fall back to an empty view instead of letting
     * the exception crash every collector — the data on disk is untouched and reappears
     * once the Keystore recovers (typically next app start).
     */
    private val data: Flow<ProfilesData> = dataStore.data
        .retryWhen { cause, attempt ->
            (cause is IOException && attempt < 3).also { retrying -> if (retrying) delay(250 * (attempt + 1)) }
        }
        .catch { cause ->
            if (cause is IOException) emit(ProfilesData()) else throw cause
        }

    val profiles: Flow<List<ServerProfile>> = data.map { it.profiles }

    /** A snapshot of everything in the store, for backup export. */
    suspend fun currentData(): ProfilesData = data.first()

    /** Replaces the entire store with restored backup contents. */
    suspend fun replaceData(newData: ProfilesData) {
        dataStore.updateData { newData }
    }

    /**
     * Adds imported Transdroid 2 settings to the store (see [LegacySettingsConverter.merge]).
     * Merges inside the store's own transaction, so it throws rather than overwriting the
     * existing data when that can't be read.
     */
    suspend fun importLegacy(import: LegacyImport): LegacyMergeResult {
        lateinit var result: LegacyMergeResult
        dataStore.updateData { existing ->
            result = LegacySettingsConverter.merge(existing, import)
            result.data
        }
        return result
    }

    suspend fun save(profile: ServerProfile) {
        dataStore.updateData { data ->
            val existing = data.profiles.indexOfFirst { it.id == profile.id }
            val updated = if (existing >= 0) {
                data.profiles.toMutableList().also { it[existing] = profile }
            } else {
                data.profiles + profile
            }
            data.copy(profiles = updated)
        }
    }

    suspend fun delete(profileId: String) {
        dataStore.updateData { data ->
            data.copy(profiles = data.profiles.filterNot { it.id == profileId })
        }
    }

    val feeds: Flow<List<RssFeed>> = data.map { it.feeds }

    suspend fun saveFeed(feed: RssFeed) {
        dataStore.updateData { data ->
            val existing = data.feeds.indexOfFirst { it.id == feed.id }
            val updated = if (existing >= 0) {
                data.feeds.toMutableList().also { it[existing] = feed }
            } else {
                data.feeds + feed
            }
            data.copy(feeds = updated)
        }
    }

    suspend fun deleteFeed(feedId: String) {
        dataStore.updateData { data ->
            data.copy(feeds = data.feeds.filterNot { it.id == feedId })
        }
    }

    /** Updates a feed's last-viewed marker without resurrecting it if it was deleted. */
    suspend fun markFeedViewed(feedId: String, timestamp: Long) {
        dataStore.updateData { data ->
            data.copy(
                feeds = data.feeds.map {
                    if (it.id == feedId) it.copy(lastViewedTimestamp = timestamp) else it
                }
            )
        }
    }

    val searchProviders: Flow<List<SearchProviderConfig>> = data.map { it.searchProviders }

    suspend fun saveSearchProvider(provider: SearchProviderConfig) {
        dataStore.updateData { data ->
            val existing = data.searchProviders.indexOfFirst { it.id == provider.id }
            val updated = if (existing >= 0) {
                data.searchProviders.toMutableList().also { it[existing] = provider }
            } else {
                data.searchProviders + provider
            }
            data.copy(searchProviders = updated)
        }
    }

    suspend fun deleteSearchProvider(providerId: String) {
        dataStore.updateData { data ->
            data.copy(searchProviders = data.searchProviders.filterNot { it.id == providerId })
        }
    }

    private companion object {
        // Referenced in data_extraction_rules.xml / full_backup_content.xml
        const val FILE_NAME = "server_profiles.bin"
    }
}

internal class EncryptedProfilesSerializer(private val cipher: ProfilesCipher) : Serializer<ProfilesData> {

    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = ProfilesData()

    override suspend fun readFrom(input: InputStream): ProfilesData {
        val blob = input.readBytes()
        if (blob.isEmpty()) return defaultValue
        // Only unrecoverable data problems may become CorruptionException — the
        // corruption handler WIPES the store. A transient Keystore failure (which
        // Android is known to produce right after unlock or under system pressure)
        // must surface as a retryable IOException instead, never as corruption.
        val plaintext = try {
            cipher.decrypt(blob)
        } catch (e: AEADBadTagException) {
            throw CorruptionException("Profiles cannot be decrypted with the current key", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Profiles blob is malformed", e)
        } catch (e: Exception) {
            throw IOException("Keystore unavailable while reading server profiles", e)
        }
        return try {
            json.decodeFromString(plaintext.decodeToString())
        } catch (e: Exception) {
            throw CorruptionException("Cannot parse server profiles", e)
        }
    }

    override suspend fun writeTo(t: ProfilesData, output: OutputStream) {
        output.write(cipher.encrypt(json.encodeToString(ProfilesData.serializer(), t).encodeToByteArray()))
    }
}
