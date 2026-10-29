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
package org.transdroid.data.legacy

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.transdroid.data.ServerProfilesRepository
import org.transdroid.data.SettingsRepository
import org.transdroid.errorlog.ErrorLog

/**
 * Brings Transdroid 2 settings into v3. When v3 is installed as an update over v2 (same package
 * and signing key), v2's default SharedPreferences file is still on disk; [migrateOnUpgrade] imports
 * it once and then deletes it, since it holds server passwords in plain text. Users who can't
 * upgrade in place (a new phone, or a different install source) can import v2's exported
 * `settings.json` with [importExportFile] instead.
 */
class LegacyMigration(
    private val context: Context,
    private val profiles: ServerProfilesRepository,
    private val settings: SettingsRepository,
) {

    /** v2 used PreferenceManager.getDefaultSharedPreferences, which is named after the package. */
    private val prefsName = "${context.packageName}_preferences"

    suspend fun migrateOnUpgrade() = withContext(Dispatchers.IO) {
        val file = File(context.dataDir, "shared_prefs/$prefsName.xml")
        if (!file.exists()) return@withContext
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).all
            apply(LegacySettingsConverter.convert(prefs))
        } catch (e: Exception) {
            // Most likely the Keystore was briefly unavailable: keep the v2 file and retry on
            // the next start. Repeating is safe, since already imported servers are skipped.
            ErrorLog.log("Migration", "Importing Transdroid 2 settings failed", e)
            return@withContext
        }
        // Only now that everything is safely in the encrypted store
        if (!context.deleteSharedPreferences(prefsName)) {
            ErrorLog.log("Migration", "Could not delete the Transdroid 2 settings file")
        }
    }

    /**
     * Imports a Transdroid 2 `settings.json` export. Throws [IllegalArgumentException] if [text]
     * isn't one.
     */
    suspend fun importExportFile(text: String): LegacyImportSummary {
        val import = withContext(Dispatchers.Default) {
            LegacySettingsConverter.convert(LegacySettingsConverter.fromExportJson(text))
        }
        return apply(import)
    }

    private suspend fun apply(import: LegacyImport): LegacyImportSummary {
        val result = profiles.importLegacy(import)
        // Open on the server v2 would have, unless the user already picked one in v3
        val activeId = settings.activeServerId.first()
        if (result.activeProfileId != null && result.data.profiles.none { it.id == activeId }) {
            settings.setActiveServer(result.activeProfileId)
        }
        if (!result.summary.isEmpty) settings.setLegacyImportSummary(result.summary)
        return result.summary
    }
}
