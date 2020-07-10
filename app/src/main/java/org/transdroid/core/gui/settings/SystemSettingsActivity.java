/*
 * Copyright 2010-2018 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceManager;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.json.JSONException;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.SettingsPersistence;
import org.transdroid.core.gui.log.ErrorLogSender;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.search.BarcodeHelper;
import org.transdroid.core.gui.search.SearchHistoryProvider;
import org.transdroid.core.service.AppUpdateJob;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@EActivity
public class SystemSettingsActivity extends PreferenceCompatActivity {

    protected static final int DIALOG_IMPORTSETTINGS = 0;
    private OnPreferenceClickListener onImportSettingsClick = new OnPreferenceClickListener() {
        @SuppressWarnings("deprecation")
        @Override
        public boolean onPreferenceClick(Preference preference) {
            showDialog(DIALOG_IMPORTSETTINGS);
            return true;
        }
    };
    protected static final int DIALOG_EXPORTSETTINGS = 1;
    private OnPreferenceClickListener onExportSettingsClick = new OnPreferenceClickListener() {
        @SuppressWarnings("deprecation")
        @Override
        public boolean onPreferenceClick(Preference preference) {
            showDialog(DIALOG_EXPORTSETTINGS);
            return true;
        }
    };
    protected static final int ACTIVITY_IMPORT_SETTINGS = 1;
    protected static final int ACTIVITY_EXPORT_SETTINGS = 2;

    @Bean
    protected NavigationHelper navigationHelper;
    @Bean
    protected ApplicationSettings applicationSettings;
    @Bean
    protected ErrorLogSender errorLogSender;
    @Bean
    protected SettingsPersistence settingsPersistence;

    private OnPreferenceClickListener onCheckUpdatesClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AppUpdateJob.schedule(getApplicationContext());
            return true;
        }
    };
    private OnPreferenceClickListener onClearSearchClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            SearchHistoryProvider.clearHistory(getApplicationContext());
            SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.pref_clearsearch_success));
            return true;
        }
    };
    private OnClickListener importSettingsFromFile = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
                    && !navigationHelper.checkSettingsReadPermission(SystemSettingsActivity.this))
                return; // We are requesting permission to access file storage
            importSettingsFromFile();
        }
    };

    private OnClickListener importSettingsFromQr = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            BarcodeHelper.startBarcodeScanner(SystemSettingsActivity.this, BarcodeHelper.ACTIVITY_BARCODE_QRSETTINGS);
        }
    };
    private OnClickListener exportSettingsToFile = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
                    && !navigationHelper.checkSettingsWritePermission(SystemSettingsActivity.this))
                return; // We are requesting permission to access file storage
            exportSettingsToFile();
        }
    };

    private OnClickListener exportSettingsToQr = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
            try {
                String settings = settingsPersistence.exportSettingsAsString(prefs);
                BarcodeHelper.shareContentBarcode(SystemSettingsActivity.this, settings);
            } catch (JSONException e) {
                SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.error_cant_write_settings_file)
                        .colorResource(R.color.red));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Just load the system-related preferences from XML
        addPreferencesFromResource(R.xml.pref_system);

        // Handle outgoing links and preference changes
        if (navigationHelper.enableUpdateChecker()) {
            findPreference("system_checkupdates").setOnPreferenceClickListener(onCheckUpdatesClick);
        } else {
            getPreferenceScreen().removePreference(findPreference("system_checkupdates"));
        }
        findPreference("system_clearsearch").setOnPreferenceClickListener(onClearSearchClick);
        findPreference("system_importsettings").setOnPreferenceClickListener(onImportSettingsClick);
        findPreference("system_exportsettings").setOnPreferenceClickListener(onExportSettingsClick);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (Boolean.TRUE.equals(navigationHelper.handleSettingsReadPermissionResult(requestCode, grantResults))) {
            importSettingsFromFile();
        } else if (Boolean.TRUE.equals(navigationHelper.handleSettingsWritePermissionResult(requestCode, grantResults))) {
            exportSettingsToFile();
        }
    }

    private void importSettingsFromFile() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
                settingsPersistence.importSettingsFromFile(prefs, SettingsPersistence.DEFAULT_SETTINGS_FILE);
                SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.pref_import_success));
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, ACTIVITY_IMPORT_SETTINGS);
            }
        } catch (FileNotFoundException e) {
            SnackbarManager
                    .show(Snackbar.with(SystemSettingsActivity.this).text(R.string.error_file_not_found).colorResource(R.color.red));
        } catch (JSONException e) {
            SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this)
                    .text(getString(R.string.error_no_valid_settings_file, getString(R.string.app_name))).colorResource(R.color.red));
        }
    }

    @OnActivityResult(ACTIVITY_IMPORT_SETTINGS)
    public void importSettingsFilePicked(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream fis = getContentResolver().openInputStream(data.getData());
                if (fis != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
                    settingsPersistence.importSettingsFromStream(prefs, fis);
                    SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.pref_import_success));
                }
            } catch (IOException | JSONException e) {
                SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.error_file_not_found)
                        .colorResource(R.color.red));
            }
        }
    }

    private void exportSettingsToFile() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
                settingsPersistence.exportSettingsToFile(prefs, SettingsPersistence.DEFAULT_SETTINGS_FILE);
                SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.pref_export_success));
            } else {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, SettingsPersistence.DEFAULT_SETTINGS_FILENAME);
                startActivityForResult(intent, ACTIVITY_EXPORT_SETTINGS);
            }
        } catch (JSONException | IOException e) {
            SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.error_cant_write_settings_file)
                    .colorResource(R.color.red));
        }
    }

    @OnActivityResult(ACTIVITY_EXPORT_SETTINGS)
    public void exportSettingsFilePicked(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                OutputStream fos = getContentResolver().openOutputStream(data.getData());
                if (fos != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
                    settingsPersistence.exportSettingsToStream(prefs, fos);
                    SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.pref_export_success));
                }
            } catch (IOException | JSONException e) {
                SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.error_cant_write_settings_file)
                        .colorResource(R.color.red));
            }
        }
    }

    @OnActivityResult(BarcodeHelper.ACTIVITY_BARCODE_QRSETTINGS)
    public void onQrCodeScanned(@SuppressWarnings("UnusedParameters") int resultCode, Intent data) {
        // We should have received Intent extras with the QR-decoded data representing Transdroid settings
        if (data == null || !data.hasExtra("SCAN_RESULT"))
            return; // Cancelled scan; ignore
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
        String contents = data.getStringExtra("SCAN_RESULT");
        String formatName = data.getStringExtra("SCAN_RESULT_FORMAT");
        if (formatName != null && formatName.equals("QR_CODE") && !TextUtils.isEmpty(contents)) {
            try {
                settingsPersistence.importSettingsAsString(prefs, contents);
                SnackbarManager.show(Snackbar.with(SystemSettingsActivity.this).text(R.string.pref_import_success));
            } catch (JSONException e) {
                SnackbarManager
                        .show(Snackbar.with(SystemSettingsActivity.this).text(R.string.error_file_not_found).colorResource(R.color.red));
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_IMPORTSETTINGS:
                // @formatter:off
                return new AlertDialog.Builder(this)
                        .setMessage(
                                getString(
                                        Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
                                                ? R.string.pref_import_dialog : R.string.pref_import_dialog_android10,
                                        getString(R.string.app_name),
                                        SettingsPersistence.DEFAULT_SETTINGS_FILE.toString()))
                        .setPositiveButton(R.string.pref_import_fromfile, importSettingsFromFile)
                        .setNeutralButton(R.string.pref_import_fromqr, importSettingsFromQr)
                        .setNegativeButton(android.R.string.cancel, null).create();
            // @formatter:on
            case DIALOG_EXPORTSETTINGS:
                // @formatter:off
                return new AlertDialog.Builder(this)
                        .setMessage(
                                getString(
                                        Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
                                                ? R.string.pref_export_dialog : R.string.pref_export_dialog_android10,
                                        getString(R.string.app_name),
                                        SettingsPersistence.DEFAULT_SETTINGS_FILE.toString()))
                        .setPositiveButton(R.string.pref_export_tofile, exportSettingsToFile)
                        .setNeutralButton(R.string.pref_export_toqr, exportSettingsToQr)
                        .setNegativeButton(android.R.string.cancel, null).create();
            // @formatter:on
        }
        return null;
    }

}
