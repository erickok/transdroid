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
package org.transdroid.core.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class ScheduledJobCreator implements JobCreator {

	@Nullable
	@Override
	public Job create(@NonNull String tag) {
		switch (tag) {
			case AppUpdateJob.TAG:
				return new AppUpdateJob();
			case RssCheckerJob.TAG:
				return new RssCheckerJob();
			case ServerCheckerJob.TAG:
				return new ServerCheckerJob();
			default:
				return null;
		}
	}

}
