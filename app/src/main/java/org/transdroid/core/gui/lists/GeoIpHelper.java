/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.transdroid.core.gui.lists;

import android.content.Context;

import androidx.annotation.Nullable;

import com.maxmind.db.Reader;
import com.maxmind.db.Reader.FileMode;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.daemon.Peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

/**
 * Helper that manages an optional, locally downloaded GeoIP database (in MaxMind .mmdb format) used
 * to resolve the country of a peer's IP address. When no database has been downloaded, callers
 * should fall back to the country code provided by the torrent client (if any).
 *
 * <p>NOTE: the MaxMind reader 4.x requires Java 17, which is incompatible with this project's Java 8
 * target; we use the last Java-8 compatible line (2.x), whose {@code Reader.get(InetAddress, Class)}
 * decodes a record into a nested {@link Map} with a {@code country.iso_code} field (matching the
 * ip66.dev schema).
 */
@EBean(scope = EBean.Scope.Singleton)
public class GeoIpHelper {

    public static final String DB_URL = "https://downloads.ip66.dev/db/ip66.mmdb";
    private static final String DB_FILENAME = "ip66.mmdb";

    @RootContext
    protected Context context;

    private Reader reader;
    private boolean readerInitialised = false;

    private File getDbFile() {
        return new File(context.getFilesDir(), DB_FILENAME);
    }

    /**
     * @return Whether a usable GeoIP database has been downloaded.
     */
    public boolean isDownloaded() {
        File db = getDbFile();
        return db.exists() && db.length() > 0;
    }

    /**
     * Downloads the GeoIP database to the app's internal storage, replacing any existing copy. Runs
     * synchronously; callers should invoke this off the UI thread.
     *
     * @throws IOException On any network or file error
     */
    public synchronized void download() throws IOException {
        File target = getDbFile();
        File temp = new File(context.getFilesDir(), DB_FILENAME + ".tmp");

        HttpURLConnection connection = (HttpURLConnection) new URL(DB_URL).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code " + status);
            }
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            // Swap the freshly downloaded file into place
            closeReader();
            //noinspection ResultOfMethodCallIgnored
            target.delete();
            if (!temp.renameTo(target)) {
                throw new IOException("Could not move the downloaded database into place");
            }
        } finally {
            connection.disconnect();
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    /**
     * Deletes the downloaded GeoIP database; subsequent lookups fall back to client-provided data.
     */
    public synchronized void clear() {
        closeReader();
        //noinspection ResultOfMethodCallIgnored
        getDbFile().delete();
    }

    private synchronized Reader getReader() {
        if (!readerInitialised) {
            readerInitialised = true;
            if (isDownloaded()) {
                try {
                    // Use MEMORY (not MEMORY_MAPPED) to avoid java.nio.file.Path APIs (API 26+)
                    reader = new Reader(getDbFile(), FileMode.MEMORY);
                } catch (IOException e) {
                    reader = null;
                }
            }
        }
        return reader;
    }

    private synchronized void closeReader() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // Ignore
            }
            reader = null;
        }
        readerInitialised = false;
    }

    /**
     * Looks up the 2-letter ISO country code for the given IP (or host:port) string using the
     * downloaded database, or null when unavailable.
     */
    @Nullable
    public String lookupCountry(String address) {
        Reader r = getReader();
        if (r == null || address == null) {
            return null;
        }
        try {
            InetAddress ip = InetAddress.getByName(stripPort(address));
            @SuppressWarnings("unchecked")
            Map<String, Object> record = r.get(ip, Map.class);
            if (record == null) {
                return null;
            }
            Object country = record.get("country");
            if (!(country instanceof Map)) {
                return null;
            }
            Object iso = ((Map<?, ?>) country).get("iso_code");
            if (iso == null) {
                return null;
            }
            String code = iso.toString();
            return code.isEmpty() ? null : code.toUpperCase(Locale.US);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolves the country code for a peer, preferring the locally downloaded database (if present)
     * and otherwise falling back to whatever the torrent client reported.
     */
    @Nullable
    public String resolveCountry(Peer peer) {
        if (isDownloaded()) {
            String resolved = lookupCountry(peer.getAddress());
            if (resolved != null) {
                return resolved;
            }
        }
        return peer.getCountryCode();
    }

    /**
     * Extracts the host part from a "host", "host:port" or "[ipv6]:port" address string.
     */
    private static String stripPort(String address) {
        if (address == null) {
            return null;
        }
        address = address.trim();
        if (address.startsWith("[")) {
            // [ipv6] or [ipv6]:port
            int end = address.indexOf(']');
            if (end > 0) {
                return address.substring(1, end);
            }
            return address;
        }
        int colon = address.indexOf(':');
        int lastColon = address.lastIndexOf(':');
        if (colon >= 0 && colon == lastColon) {
            // Exactly one colon: treat as host:port (IPv4 or hostname)
            return address.substring(0, colon);
        }
        // No colon, or multiple colons (bare IPv6) -> use as-is
        return address;
    }

    /**
     * Converts a 2-letter ISO country code into the corresponding Unicode regional-indicator flag
     * emoji (e.g. "US" -> 🇺🇸), or returns null for an invalid code.
     */
    @Nullable
    public static String flagEmoji(String iso2) {
        if (iso2 == null) {
            return null;
        }
        String code = iso2.trim().toUpperCase(Locale.US);
        if (code.length() != 2 || !code.matches("[A-Z]{2}")) {
            return null;
        }
        int first = 0x1F1E6 + (code.charAt(0) - 'A');
        int second = 0x1F1E6 + (code.charAt(1) - 'A');
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

}
