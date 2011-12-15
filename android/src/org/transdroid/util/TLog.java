package org.transdroid.util;

import org.transdroid.daemon.util.ITLogger;

import android.util.Log;



/**
 * Universal logger class (as srop-in replacement for the default Log).
 * 
 * @author erickok
 *
 */
public class TLog {

	private static final String LOG_TAG = "Transdroid";

	/**
	 * Send a DEBUG log message.
	 * @param self Unique source tag, identifying the part of Transdroid it happens in
	 * @param msg The debug message to log
	 */
	public static void d(String self, String msg) {
		Log.d(LOG_TAG, self + ": " + msg);
	}

	/**
	 * Send an ERROR log message.
	 * @param self Unique source tag, identifying the part of Transdroid it happens in
	 * @param msg The error message to log
	 */
	public static void e(String self, String msg) {
		Log.e(LOG_TAG, self + ": " + msg);
	}

	/**
	 * Returns an IDLogger instance that redirects all calls to the 
	 * static methods of TLog - to be used to use TLog as an instance
	 * in DLog (logging daemon messages).
	 * @return An Android ITLogger instance
	 */
	public static ITLogger getInstance() {
		return new ITLogger() {
			@Override
			public void d(String self, String msg) {
				TLog.d(self, msg);
			}
			@Override
			public void e(String self, String msg) {
				TLog.e(self, msg);
			}
		};
	}
	
}
