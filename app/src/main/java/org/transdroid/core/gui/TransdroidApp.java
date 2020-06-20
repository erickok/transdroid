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
package org.transdroid.core.gui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.evernote.android.job.JobConfig;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.util.JobLogger;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.service.ScheduledJobCreator;

@EApplication
public class TransdroidApp extends Application {

	@Bean
	protected Log log;

	@Override
	public void onCreate() {
		super.onCreate();

		// Configure Android-Job
		JobConfig.addLogger(new JobLogger() {
			@Override
			public void log(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable t) {
				log.d(tag, message);
			}
		});
		JobManager.create(this).addJobCreator(new ScheduledJobCreator());
	}

}
