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

import org.transdroid.daemon.DaemonException;

/**
 * 
 * The result daemon task that failed. Use getException() to see what went wrong.
 * 
 * @author erickok
 *
 */
public class DaemonTaskFailureResult extends DaemonTaskResult {
	
	private DaemonException e;
	
	public DaemonTaskFailureResult(DaemonTask executedTask, DaemonException e) {
		super(executedTask, false);
		this.e = e;
	}
	
	/**
	 * Return the exception that occurred during the task execution.
	 * @return A daemon exception object with string res ID or fixed string message
	 */
	public DaemonException getException() {
		return e;
	}

	@Override
	public String toString() {
		return "Failure on " + executedTask.toString() + ": " + getException().toString();
	}
	
}
