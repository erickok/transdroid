package org.transdroid.core.app.search;

import org.transdroid.core.R;
import org.transdroid.core.gui.TorrentsActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BarcodeHelper {

	public static final int ACTIVITY_BARCODE = 0x0000c0de; // A 'random' ID to identify scan intents
	public static final Uri SCANNER_MARKET_URI = Uri.parse("market://search?q=pname:com.google.zxing.client.android");

	/**
	 * Call this to start a bar code scanner intent. The calling activity will receive an Intent result with ID
	 * {@link #ACTIVITY_BARCODE}. From there {@link #handleScanResult(int, Intent)} should be called to parse the result
	 * into a search query.
	 * @param activity The calling activity, to which the result is returned or a dialog is bound that asks to install
	 *            the bar code scanner
	 */
	public static void startBarcodeScanner(final SherlockFragmentActivity activity) {
		try {
			// Start a bar code scanner that can handle the SCAN intent (specifically ZXing)
			activity.startActivityForResult(new Intent("com.google.zxing.client.android.SCAN"), ACTIVITY_BARCODE);
		} catch (Exception e) {
			// Can't start the bar code scanner, for example with a SecurityException or when ZXing is not present
			new DialogFragment() {
				public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
					return new AlertDialog.Builder(activity).setIcon(android.R.drawable.ic_dialog_alert)
							.setMessage(activity.getString(R.string.search_barcodescannernotfound))
							.setPositiveButton(android.R.string.yes, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									activity.startActivity(new Intent(Intent.ACTION_VIEW, SCANNER_MARKET_URI));
								}
							}).setNegativeButton(android.R.string.no, null).create();
				};
			}.show(activity.getSupportFragmentManager(), "installscanner");
		}
	}

	/**
	 * The activity that called {@link #startBarcodeScanner(SherlockFragmentActivity)} should call this after the scan
	 * result was returned. This will parse the scan data and return a query search query appropriate to the bar code.
	 * @param resultCode The raw result code as returned by the bar code scanner
	 * @param data The raw data as returned from the bar code scanner
	 * @return A String that can be used as new search query, or null if the bar code could not be scanned or no query
	 *         can be constructed for it
	 */
	public static String handleScanResult(TorrentsActivity activity, int resultCode, Intent data) {
		String contents = data.getStringExtra("SCAN_RESULT");
		String formatName = data.getStringExtra("SCAN_RESULT_FORMAT");
		if (formatName != null && formatName.equals("QR_CODE")) {
			// Scanned barcode was a QR code: return the contents directly
			return contents;
		} else {
			if (TextUtils.isEmpty(contents))
				return null;
			// Get a meaningful search query based on a Google Search product lookup
			return GoogleWebSearchBarcodeResolver.resolveBarcode(contents);
		}
	}

}
