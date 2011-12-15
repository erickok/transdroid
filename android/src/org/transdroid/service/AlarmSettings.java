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
package org.transdroid.service;

/**
 * The settings for the Alarm service
 * 
 * @author erickok
 */

public class AlarmSettings{

	private boolean enableAlarm;
	private int alarmInterval;
	private boolean checkRssFeeds;
	private boolean alarmPlaySound;
	private String alarmSoundURI;
	private boolean alarmVibrate;
	private boolean adwNotify;
	private boolean adwOnlyDl;
		
	public AlarmSettings(boolean enableAlarm, int alarmInterval, boolean checkRssFeeds, boolean alarmPlaySound, String alarmSoundURI, boolean alarmVibrate, boolean adwNotify, boolean adwOnlyDl) {
		this.enableAlarm = enableAlarm;
		this.alarmInterval = alarmInterval;
		this.checkRssFeeds = checkRssFeeds;
		this.alarmPlaySound = alarmPlaySound;
		this.alarmSoundURI = alarmSoundURI;
		this.alarmVibrate = alarmVibrate;
		this.adwNotify = adwNotify;
		this.adwOnlyDl = adwOnlyDl;
	}
	
	public boolean isAlarmEnabled() {
		return enableAlarm;
	}
	
	public int getAlarmIntervalInSeconds() {
		return alarmInterval;
	}

	public long getAlarmIntervalInMilliseconds() {
		return getAlarmIntervalInSeconds() * 1000;
	}

	public boolean shouldCheckRssFeeds() {
		return checkRssFeeds;
	}
	
	public boolean getAlarmPlaySound() {
		return alarmPlaySound;
	}
	
	public String getAlarmSoundURI() {
		return alarmSoundURI;
	}
	
	public boolean getAlarmVibrate() {
		return alarmVibrate;
	}

	public boolean showAdwNotifications() {
		return adwNotify;
	}

	public boolean showOnlyDownloadsInAdw() {
		return adwOnlyDl;
	}
	
}

