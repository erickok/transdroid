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

import java.sql.SQLException;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.ormlite.annotations.OrmLiteDao;
import org.transdroid.R;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;

import com.j256.ormlite.dao.Dao;

@EBean
public class ErrorLogSender {

	@Bean
	protected Log log;
	@Bean
	protected NavigationHelper navigationHelper;
	@OrmLiteDao(helper = DatabaseHelper.class)
	protected Dao<ErrorLogEntry, Integer> errorLogDao;

	public void collectAndSendLog(final Activity callingActivity, final ServerSetting serverSetting) {

		try {

			// Prepare an email with error logging information
			StringBuilder body = new StringBuilder();
			body.append("Please describe your problem:\n\n\n");
			body.append("\n");
			body.append(navigationHelper.getAppNameAndVersion());
			body.append("\n");
			if (serverSetting == null) {
				body.append("(No server settings)");
			} else {
				body.append(serverSetting.getType().toString());
				body.append(" settings: ");
				body.append(serverSetting.getHumanReadableIdentifier());
			}
			body.append("\n\nConnection and error log:");

			// Print the individual error log messages as stored in the database
			List<ErrorLogEntry> all = errorLogDao.queryBuilder().orderBy(ErrorLogEntry.ID, true).query();
			for (ErrorLogEntry errorLogEntry : all) {
				body.append("\n");
				body.append(errorLogEntry.getLogId());
				body.append(" -- ");
				body.append(errorLogEntry.getDateAndTime());
				body.append(" -- ");
				body.append(errorLogEntry.getPriority());
				body.append(" -- ");
				body.append(errorLogEntry.getMessage());
			}

			Intent target = new Intent(Intent.ACTION_SEND);
			target.setType("message/rfc822");
			target.putExtra(Intent.EXTRA_EMAIL, new String[] { "transdroid@2312.nl" });
			target.putExtra(Intent.EXTRA_SUBJECT, "Transdroid error report");
			target.putExtra(Intent.EXTRA_TEXT, body.toString());
			try {
				callingActivity.startActivity(Intent.createChooser(target,
						callingActivity.getString(R.string.pref_sendlog)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			} catch (ActivityNotFoundException e) {
				log.i(callingActivity, "Tried to send error log, but there is no email app installed.");
			}

		} catch (SQLException e) {
			log.e(callingActivity, "Cannot read the error log to build an error report to send: " + e.toString());
		}

	}

}
