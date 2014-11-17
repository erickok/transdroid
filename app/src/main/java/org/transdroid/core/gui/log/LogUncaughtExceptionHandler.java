/*
 * Copyright 2010-2013 Eric Kok et al.
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
package org.transdroid.core.gui.log;

import android.content.Context;

public class LogUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	private final Context context;
	private final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

	public LogUncaughtExceptionHandler(Context context, Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler) {
		this.context = context;
		this.defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {

		// Write exception stack trace to the log
		String prefix = "E: ";
		Log_ log = Log_.getInstance_(context);
		log.e(this, prefix + ex.toString());
		if (ex.getCause() != null) {
			for (StackTraceElement e : ex.getCause().getStackTrace()) {
				log.e(this, prefix + e.toString());
			}
		}
		for (StackTraceElement e : ex.getStackTrace()) {
			log.e(this, prefix + e.toString());
		}

		// Rely on default Android exception handling
		defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
	}

}
