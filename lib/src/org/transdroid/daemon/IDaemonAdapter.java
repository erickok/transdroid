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
 package org.transdroid.daemon;

import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskResult;

/**
 * The required methods that a server daemon adapter should implement, to
 * support all client operations.
 *
 * @author erickok
 *
 */
public interface IDaemonAdapter {

	public DaemonTaskResult executeTask(DaemonTask task);

	public Daemon getType();

	public DaemonSettings getSettings();

}
