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
package org.transdroid.core.gui.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import org.transdroid.R;

import java.lang.ref.WeakReference;

public class BarcodeHelper {

    // A 'random' ID to identify QR-encoded settings scan intents
    public static final int ACTIVITY_BARCODE_QRSETTINGS = 0x0000c0df;
    public static final int ACTIVITY_BARCODE_ADDSERVER = 0x0000c0e0;
    private static final Uri SCANNER_MARKET_URI = Uri.parse("market://search?q=pname:com.google.zxing.client.android");

    /**
     * Call this to start a bar code scanner intent. The calling activity will receive an Intent result with  the given
     * request code.
     *
     * @param activity    The calling activity, to which the result is returned or a dialog is bound that asks to install
     *                    the bar code scanner
     * @param requestCode {@link #ACTIVITY_BARCODE_QRSETTINGS}
     */
    public static void startBarcodeScanner(final Activity activity, int requestCode) {
        // Start a bar code scanner that can handle the SCAN intent (specifically ZXing)
        startBarcodeIntent(activity, new Intent("com.google.zxing.client.android.SCAN"), requestCode);
    }

    /**
     * Call this to share content encoded in a QR code, specially used to share settings. The calling activity will
     * receive an Intent result with ID {@link #ACTIVITY_BARCODE_QRSETTINGS}. From there the returned intent will
     * contain the data as SCAN_RESULT String extra.
     *
     * @param activity The calling activity, to which the result is returned or a dialog is bound that asks to install
     *                 the bar code scanner
     * @param content  The content to share, that is, the raw data (Transdroid settings encoded as JSON data structure)
     *                 to share as QR code
     */
    public static void shareContentBarcode(final Activity activity, final String content) {
        // Start a bar code encoded that can handle the ENCODE intent (specifically ZXing)
        Intent encodeIntent = new Intent("com.google.zxing.client.android.ENCODE");
        encodeIntent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
        encodeIntent.putExtra("ENCODE_DATA", content);
        encodeIntent.putExtra("ENCODE_SHOW_CONTENTS", false);
        startBarcodeIntent(activity, encodeIntent, -1);
    }

    @SuppressLint("ValidFragment")
    private static void startBarcodeIntent(final Activity activity, final Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            // Can't start the bar code scanner, for example with a SecurityException or when ZXing is not present
            final WeakReference<Context> intentStartContext = new WeakReference<>(activity);
            new AlertDialog.Builder(activity)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(activity.getString(R.string.search_barcodescannernotfound))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (intentStartContext.get() != null)
                            intentStartContext.get().startActivity(new Intent(Intent.ACTION_VIEW, SCANNER_MARKET_URI));
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

}
