/* 
 * Copyright 2010-2013 Eric Kok et al.
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

import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.Item;

/**
 * A container class that holds RSS feed settings and, after they have been retrieved, the contents as {@link Channel},
 * the number of new items and an indication of a connection error.
 * @author Eric Kok
 */
public class RssfeedLoader {

	private final RssfeedSetting setting;
	private Channel channel = null;
	private int newCount = -1;
	private boolean hasError = false;
	
	public RssfeedLoader(RssfeedSetting setting) {
		this.setting = setting;
	}
	
	public void update(Channel channel, boolean hasError) {
		this.channel = channel;
		this.hasError = hasError;
		if (channel == null || hasError) {
			hasError = true;
			newCount = -1;
			return;
		}
		// Count the number of new items, based on the date that this RSS feed was last viewed by the user
		newCount = 0;
		for (Item item : channel.getItems()) {
			if (item.getPubdate() == null || setting.getLastViewed() == null
					|| item.getPubdate().after(setting.getLastViewed())) {
				newCount++;
				item.setIsNew(true);
			} else {
				item.setIsNew(false);
			}
		}
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public RssfeedSetting getSetting() {
		return setting;
	}
	
	public int getNewCount() {
		return newCount;
	}

	public boolean hasError() {
		return hasError ;
	}

}
