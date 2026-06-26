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
package org.transdroid.core.gui.lists;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.daemon.Peer;
import org.transdroid.daemon.util.FileSizeConverter;

/**
 * View that represents a single {@link Peer}, showing its client, address, country flag, transfer
 * speeds and progress, with a left-edge bar indicating connection encryption.
 *
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_peer)
public class PeerView extends StatusBarLayout {

    // Padlock shown after the address for an encrypted peer connection
    private static final String PADLOCK = "🔒"; // 🔒

    @ViewById
    protected TextView clientText, addressText, progressText, speedsText;

    public PeerView(Context context) {
        super(context, null);
    }

    public void bind(Peer peer, GeoIpHelper geoIpHelper) {

        clientText.setText(peer.getClient());

        // Build the address line: optional country flag prefix, the address, and a padlock when encrypted
        String country = geoIpHelper == null ? peer.getCountryCode() : geoIpHelper.resolveCountry(peer);
        String flag = GeoIpHelper.flagEmoji(country);
        StringBuilder address = new StringBuilder();
        if (flag != null) {
            address.append(flag).append(' ');
        } else if (country != null && !country.isEmpty()) {
            address.append(country).append(' ');
        }
        address.append(peer.getAddress());
        Boolean encrypted = peer.isEncrypted();
        if (encrypted != null && encrypted) {
            address.append(' ').append(PADLOCK);
        }
        addressText.setText(address);

        if (peer.getProgress() >= 0) {
            progressText.setText(getResources().getString(R.string.status_percent, (int) (peer.getProgress() * 100)));
            progressText.setVisibility(View.VISIBLE);
        } else {
            progressText.setVisibility(View.GONE);
        }

        speedsText.setText(getResources().getString(R.string.status_peer_speeds,
                FileSizeConverter.getSize(peer.getDownSpeed()) + "/s",
                FileSizeConverter.getSize(peer.getUpSpeed()) + "/s"));

        // Left-edge bar reflects the peer's progress: green when it is a seed (100%), grey while it is
        // still downloading, and hidden when the client does not report progress
        float progress = peer.getProgress();
        if (progress < 0) {
            clearBarColor();
        } else if (progress >= 1f) {
            setBarColor(getResources().getColor(R.color.green));
        } else {
            setBarColor(getResources().getColor(R.color.grey));
        }

    }

}
