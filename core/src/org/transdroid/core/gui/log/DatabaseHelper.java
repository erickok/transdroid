package org.transdroid.core.gui.log;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Helper to access the database to access persisting objects.
 * @author Eric Kok
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

	private static final String DATABASE_NAME = "transdroid.db";
	private static final int DATABASE_VERSION = 1;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
		try {
			TableUtils.createTable(connectionSource, ErrorLogEntry.class);
		} catch (SQLException e) {
			Log.e(org.transdroid.core.gui.log.Log.LOG_NAME, "Could not create new table for ErrorLogEntry", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int oldVersion,
			int newVersion) {
		try {
			switch (oldVersion) {
			case 1:
				TableUtils.createTable(connectionSource, ErrorLogEntry.class);
			/*case 1:
				etc...*/
			}

		} catch (SQLException e) {
			Log.e(org.transdroid.core.gui.log.Log.LOG_NAME, "Could not upgrade the table for ErrorLogEntry", e);
		}
	}

}
