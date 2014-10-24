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

public class AddByFileTask extends DaemonTask {
	protected AddByFileTask(IDaemonAdapter adapter, Bundle data) {
		super(adapter, DaemonMethod.AddByFile, null, data);
	}
	public static AddByFileTask create(IDaemonAdapter adapter, String file) {
		Bundle data = new Bundle();
		data.putString("FILE", file);
		return new AddByFileTask(adapter, data);
	}
	public String getFile() {
		return extras.getString("FILE");
	}
}
