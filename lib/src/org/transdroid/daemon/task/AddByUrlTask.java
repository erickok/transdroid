/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.daemon.task;

import org.transdroid.daemon.DaemonMethod;
import org.transdroid.daemon.IDaemonAdapter;

import android.os.Bundle;

public class AddByUrlTask extends DaemonTask {
	protected AddByUrlTask(IDaemonAdapter adapter, Bundle data) {
		super(adapter, DaemonMethod.AddByUrl, null, data);
	}
	public static AddByUrlTask create(IDaemonAdapter adapter, String url, String title) {
		Bundle data = new Bundle();
		data.putString("URL", url);
		data.putString("TITLE", title);
		return new AddByUrlTask(adapter, data);
	}
	public String getUrl() {
		return extras.getString("URL");
	}
	public String getTitle() {
		return extras.getString("TITLE");
	}
}
