package org.transdroid.core.gui.log;

import java.sql.SQLException;
import java.util.Date;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.OrmLiteDao;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.daemon.util.ITLogger;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

/**
 * Application-wide logging class that registers entries in the database (for a certain time).
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class Log implements ITLogger {

	public static final String LOG_NAME = "Transdroid";
	private static final long MAX_LOG_AGE = 15 * 60 * 1000; // 15 minutes
	
	// Access to resources and database in local singleton instance
	private Context context;
	@OrmLiteDao(helper = DatabaseHelper.class, model = ErrorLogEntry.class)
	Dao<ErrorLogEntry, Integer> errorLogDao;
	@Bean
	protected NavigationHelper navigationHelper;

	protected Log(Context context) {
		this.context = context;
	}
	
	protected void log(String logName, int priority, String message) {
		if (navigationHelper.inDebugMode())
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

	@Override
	public void d(String self, String msg) {
		Log.d(context, msg);
	}

	@Override
	public void e(String self, String msg) {
		Log.e(context, msg);
	}

}
