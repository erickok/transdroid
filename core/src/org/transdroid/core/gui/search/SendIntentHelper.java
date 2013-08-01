package org.transdroid.core.gui.search;

import android.content.Intent;

/**
 * Used to clean up text as received from a generic ACTION_SEND intent. This class is highly custom-based for known
 * applications, i.e. the EXTRA_TEXT send by some known applications.
 * @author Eric Kok
 */
public class SendIntentHelper {

	private static final String SOUNDHOUND1 = "Just used #SoundHound to find ";
	private static final String SOUNDHOUND1_END = " http://";
	private static final String SHAZAM = "I just used Shazam to discover ";
	private static final String SHAZAM_END = ". http://";
	private static final String YOUTUBE_ID = "Watch \"";
	private static final String YOUTUBE_START = "\"";
	private static final String YOUTUBE_END = "\"";

	/**
	 * Cleans a SEND intent text string by removing irrelevant parts, so that the remaining text can be used as search
	 * string. Typically deals with specific known applications such as Shazam and YouTube's SEND intents.
	 * @param intent The original SEND intent that was received
	 * @return A cleaned string to be used as search query
	 */
	public static String cleanUpText(Intent intent) {

		if (intent == null || !intent.hasExtra(Intent.EXTRA_TEXT)) {
			return null;
		}
		String text = intent.getStringExtra(Intent.EXTRA_TEXT);
		try {

			// Soundhound song/artist share
			if (text.startsWith(SOUNDHOUND1)) {
				return cutOut(text, SOUNDHOUND1, SOUNDHOUND1_END).replace(" by ", " ");
			}
			// Shazam song share
			if (text.startsWith(SHAZAM)) {
				return cutOut(text, SHAZAM, SHAZAM_END).replace(" by ", " ");
			}
			// YouTube app share (stores title in EXTRA_SUBJECT)
			if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
				String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
				if (subject.startsWith(YOUTUBE_ID)) {
					return cutOut(subject, YOUTUBE_START, YOUTUBE_END);
				}
			}

		} catch (Exception e) {
			// Ignore any errors in parsing; just return the raw text
		}
		return text;
	}

	private static String cutOut(String text, String start, String end) {
		int startAt = text.indexOf(start) + start.length();
		return text.substring(startAt, text.indexOf(end, startAt));
	}

}
