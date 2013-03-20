package org.transdroid.core.gui.log;

import java.sql.SQLException;
import java.util.Date;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.OrmLiteDao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

/**
 * Application-wide logging class that registers entries in the database (for a certain time).
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class Log {

	public static final String LOG_NAME = "Transdroid";
	private static final long MAX_LOG_AGE = 15 * 60 * 1000; // 15 minutes
	
	@OrmLiteDao(helper = DatabaseHelper.class, model = ErrorLogEntry.class)
	Dao<ErrorLogEntry, Integer> errorLogDao;

	protected void log(String logName, int priority, String message) {
		android.util.Log.println(priority, LOG_NAME, message);
		try {
			// Store this log message to the database
			errorLogDao.create(new ErrorLogEntry(priority, logName, message));
			// Truncate the error log
			DeleteBuilder<ErrorLogEntry, Integer> db = errorLogDao.deleteBuilder();
			db.setWhere(db.where().le(ErrorLogEntry.DATEANDTIME, new Date(new Date().getTime() - MAX_LOG_AGE)));
			errorLogDao.delete(db.prepare());
		} catch (SQLException e) {
			android.util.Log.e(LOG_NAME, "Cannot write log message to database: " + e.toString());
		}
	}
	
	public static void e(Context caller, String message) {
		Log_.getInstance_(caller).log(caller.getClass().toString(), android.util.Log.ERROR, message);
	}
	
	public static void i(Context caller, String message) {
		Log_.getInstance_(caller).log(caller.getClass().toString(), android.util.Log.INFO, message);
	}
	
	public static void d(Context caller, String message) {
		Log_.getInstance_(caller).log(caller.getClass().toString(), android.util.Log.DEBUG, message);
	}

}
