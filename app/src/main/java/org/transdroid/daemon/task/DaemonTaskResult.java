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
 package org.transdroid.daemon.task;

import org.transdroid.daemon.DaemonMethod;
import org.transdroid.daemon.Torrent;

/**
 * The result of a task that was executed on the daemon. This is always either a success or failure and hence
 * only these in-line classes are publicly used.
 *  
 * @author erickok
 *
 */
public class DaemonTaskResult {

	protected DaemonTask executedTask;
	protected boolean success;	
	
	/**
	 * Protected constructor. This class must be used only via the DaemonTaskSuccessResult or DaemonTaskFailureResult.
	 */
	protected DaemonTaskResult(DaemonTask executedTask, boolean wasSuccessful) {
		this.executedTask = executedTask;
		this.success = wasSuccessful;
	}

	/**
	 * Returns the original task that we were executing on the daemon, so all extra data is also available.
	 * @return The task as it was originally queued
	 */
	public DaemonTask getTask() {
		return executedTask;
	}
	
	/**
	 * Returns the original method that we were executing on the daemon.
	 * @return The method type of the executed task
	 */
	public DaemonMethod getMethod() {
		return executedTask.getMethod();
	}
	
	/**
	 * The torrent to that was the target of the executed task.
	 * @return The targeted torrent object, or null if it was torrent-independent
	 */
	public Torrent getTargetTorrent() {
		return executedTask.getTargetTorrent();
	}
	
	/**
	 * Whether the task executed successfully.
	 * @return True if the task executed as expected, false if some error occurred
	 */
	public boolean wasSuccessful() {
		return success;
	}

	@Override
	public String toString() {
		return (success? "Success on ": "Failure on ") + executedTask.toString();
	}
	
}
