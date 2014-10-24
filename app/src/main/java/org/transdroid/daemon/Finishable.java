package org.transdroid.daemon;

/**
 * Represents an object that has a notion of being able to start, being in progress and being able to finish, such as
 * something that can be downloaded (like a torrent or a torrent file).
 * @author erickok
 */
public interface Finishable {

	/**
	 * Whether the represented object has been started, but is not yet finished.
	 * @return True iif the object (like the data transfer) was started and is in progress, but was not finished yet.
	 */
	public boolean isStarted();

	/**
	 * Whether the represented object has been finished.
	 * @return True iif the object (like the data transfer) was finished, which means it is no longer started or in
	 *         progress.
	 */
	public boolean isFinished();
	
}
