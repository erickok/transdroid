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

import java.net.MalformedURLException;
import java.net.URL;

import org.transdroid.R;
import org.transdroid.gui.util.ActivityUtil;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Provides an activity in which the user can input a URL using a text box. 
 * Alternatively a file selector can be started for a local .torrent file. The 
 * URL or file location will then be forwarded to the Transdroid application.
 * 
 * @author erickok
 *
 */
public class Add extends Activity {

	//private static final String LOG_NAME = "Add";
	private final static String PICK_FILE_INTENT = "org.openintents.action.PICK_FILE";
	private final static Uri OIFM_MARKET_URI = Uri.parse("market://search?q=pname:org.openintents.filemanager");
	public static final int FILE_REQUEST_CODE = 1;

	private static final int DIALOG_INSTALLFILEMANAGER = 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_add);
        
        // Add button click handlers
        findViewById(R.id.ok).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				parseInput();
			}        	
        });
        findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
		    	setResult(RESULT_CANCELED);
		    	finish();
			}        	
        });
    	findViewById(R.id.selectfile).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				StartSelectorIntent();
			}        	
        });
    }
    
    /**
     * Parse the text input and return the given URL
     */
    private void parseInput() {

    	// Get the URL text
        EditText url = (EditText) findViewById(R.id.url);
		String urlText = url.getText().toString();

		// Check if no URL given
		if (urlText == null || urlText.length() <= 0) {
			Toast.makeText(this, R.string.no_valid_url, Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Check URL structure
		try {
			new URL(urlText); // Nothing is actually done with it; only for parsing
		} catch (MalformedURLException e) {
			Toast.makeText(this, R.string.no_valid_url, Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Create a result for the calling activity
		Intent i = new Intent(this, Torrents.class);
		i.setData(Uri.parse(urlText));
		startActivity(i);
		setResult(RESULT_OK);
		finish();
		
    }

    /**
     * Starts an Intent to pick a local .torrent file (usually form the SD card)
     */
    private void StartSelectorIntent() {

    	// Test to see if a file manager is available that can handle the PICK_FILE intent, such as IO File Manager
		Intent pick = new Intent(PICK_FILE_INTENT);
    	if (ActivityUtil.isIntentAvailable(this, pick)) {
    		// Ask the file manager to allow the user to pick a file
    		startActivityForResult(pick, FILE_REQUEST_CODE);
    	} else {
    		// Show a message if the user should install OI File Manager for this feature
    		showDialog(DIALOG_INSTALLFILEMANAGER);
    	}
    	
    }

    @Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_INSTALLFILEMANAGER:
			return ActivityUtil.buildInstallDialog(this, R.string.oifm_not_found, OIFM_MARKET_URI);		
		}
		return null;
	}

    /**
     * A result was returned from the on of the intents
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
    	switch (requestCode) {
    	case FILE_REQUEST_CODE:
	    	// Did we receive a file name?
	    	if (data != null && data.getData() != null && data.getData().toString() != "") {
	
	    		// Create a result for the calling activity
	    		Intent i = new Intent(this, Torrents.class);
	    		i.setData(data.getData());
	    		startActivity(i);
	    		setResult(RESULT_OK);
	    		finish();
	    		
	    	}
	    	break;

    	}
		super.onActivityResult(requestCode, resultCode, data);
	}
    
}
