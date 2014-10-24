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

public class SetTransferRatesTask extends DaemonTask {
	protected SetTransferRatesTask(IDaemonAdapter adapter, Bundle data) {
		super(adapter, DaemonMethod.SetTransferRates, null, data);
	}
	public static SetTransferRatesTask create(IDaemonAdapter adapter, Integer uploadRate, Integer downloadRate) {
		Bundle data = new Bundle();
		data.putInt("UPLOAD_RATE", (uploadRate == null? -1: uploadRate.intValue()));
		data.putInt("DOWNLOAD_RATE", (downloadRate == null? -1: downloadRate.intValue()));
		return new SetTransferRatesTask(adapter, data);
	}
	public Integer getUploadRate() {
		int uploadRate = extras.getInt("UPLOAD_RATE");
		return (uploadRate == -1? null: new Integer(uploadRate));
	}
	public Integer getDownloadRate() {
		int downloadRate = extras.getInt("DOWNLOAD_RATE");
		return (downloadRate == -1? null: new Integer(downloadRate));
	}
}
