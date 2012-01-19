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
package org.transdroid.gui;

import org.transdroid.R;
import org.transdroid.daemon.Torrent;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class Details extends FragmentActivity {

	//private static final String LOG_NAME = "Details";

	public static final String STATE_DAEMON = "transdroid_state_details_daemon";
	public static final String STATE_LABELS = "transdroid_state_details_labels";
	public static final String STATE_TORRENT = "transdroid_state_details_torrent";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details);

		getSupportActionBar().setDisplayShowTitleEnabled(true);
			
		if (savedInstanceState == null) {
			Intent i = getIntent();
			// Get torrent and daemon form the new intent
			int daemonNumber = i.getIntExtra(STATE_DAEMON, 0);
			String[] existingLabels = i.getStringArrayExtra(STATE_LABELS);
			Torrent torrent = i.getParcelableExtra(STATE_TORRENT);

			// Start the fragment for this torrent
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.details, new DetailsFragment(null, daemonNumber, torrent, existingLabels));
			if (getSupportFragmentManager().findFragmentById(R.id.details) != null) {
				ft.addToBackStack(null);
			}
			ft.commit();
		}

	}

}