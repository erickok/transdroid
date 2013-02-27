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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Activity that loads the torrents fragment and, on tablet interfaces, hosts
 * the details fragment.
 * 
 * @author erickok
 */
public class Torrents extends SherlockFragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_torrents);

		ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		if (savedInstanceState == null) {

			// Start the fragment for this torrent
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			TorrentsFragment fragment = new TorrentsFragment();
			ft.replace(R.id.torrents, fragment);
			ft.commit();

		}

	}

	@Override
	protected void onNewIntent(Intent i) {
		loadData(i);
	}

	private void loadData(Intent i) {
		((TorrentsFragment)getSupportFragmentManager().findFragmentById(R.id.torrents)).handleIntent(i);
	}

}
