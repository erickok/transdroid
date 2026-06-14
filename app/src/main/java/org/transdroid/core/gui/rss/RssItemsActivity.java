/*
 * Copyright 2010-2024 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.rss;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.SettingsUtils;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.rssparser.Channel;

@EActivity(R.layout.activity_rssitems)
public class RssItemsActivity extends AppCompatActivity {

    @Extra
    protected Channel rssfeed = null;
    @Extra
    protected String rssfeedName;
    @Extra
    protected boolean requiresExternalAuthentication;

    @FragmentById(R.id.rssitems_fragment)
    protected RssItemsFragment fragmentItems;
    @ViewById
    protected Toolbar rssfeedsToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SettingsUtils.applyDayNightTheme(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @AfterViews
    protected void init() {

        // We require an RSS feed to be specified; otherwise close the activity
        if (rssfeed == null) {
            finish();
            return;
        }

        setSupportActionBar(rssfeedsToolbar);
        getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(rssfeedName));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Extend toolbar into status bar; pad content above nav bar for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(rssfeedsToolbar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            LayerDrawable bg = new LayerDrawable(new Drawable[]{
                    new ColorDrawable(ContextCompat.getColor(v.getContext(), R.color.green_dark)),
                    new ColorDrawable(ContextCompat.getColor(v.getContext(), R.color.green))
            });
            bg.setLayerInset(1, 0, statusBarHeight, 0, 0);
            v.setBackground(bg);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom);
            return insets;
        });

        // Get the intent extras and show them to the already loaded fragment
        fragmentItems.update(rssfeed, false, requiresExternalAuthentication);
    }

    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

}
