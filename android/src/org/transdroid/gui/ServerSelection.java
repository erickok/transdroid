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

import java.util.List;

import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.PreferencesAdapter;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

public class ServerSelection extends ListActivity {

    private SharedPreferences prefs;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_serverselection);
        
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
        // List the daemons
		List<DaemonSettings> daemons = Preferences.readAllDaemonSettings(prefs);
        setListAdapter(new PreferencesAdapter(this, daemons));
		
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// Perform click action depending on the clicked list item (note that dividers are ignored)
    	Object item = getListAdapter().getItem(position);
    
    	if (item instanceof DaemonSettings) { 
    		
    		// Get selected server
    		DaemonSettings daemon = (DaemonSettings) item;
    		Intent startIntent = new Intent(this, Torrents.class);
    		startIntent.putExtra(Transdroid.INTENT_OPENDAEMON, daemon.getIdString());

    		// Return the a shortcut intent for the selected server
    		Intent i = new Intent();
    		ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
    		i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, startIntent);
    		i.putExtra(Intent.EXTRA_SHORTCUT_NAME, daemon.getName());
    		i.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
    		setResult(RESULT_OK, i);
    		finish();
    	}
    }
    
}
