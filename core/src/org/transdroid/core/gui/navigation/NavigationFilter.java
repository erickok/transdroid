package org.transdroid.core.gui.navigation;

import org.transdroid.daemon.Torrent;

import android.os.Parcelable;

/**
 * Represents a filter, used in the app navigation, that can check if some torrent matches the user-set filter
 * @author Eric Kok
 */
public interface NavigationFilter extends Parcelable {

	/**
	 * Implementations should check if the supplied torrent matches the filter; for example a label filter should return
	 * true if the torrent's label equals this items label name.
	 * @param torrent The torrent to check for matches
	 * @return True if the torrent matches the filter and should be shown in the current screen, false otherwise
	 */
	boolean matches(Torrent torrent);

	/**
	 * Implementations should return a name that can be shown to indicate the active filter
	 * @return The name of the filter item as string
	 */
	String getName();
	
}
