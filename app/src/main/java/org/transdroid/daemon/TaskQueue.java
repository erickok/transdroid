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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.util.DLog;

public class TaskQueue implements Runnable {

	private static final String LOG_NAME = "Queue";
	
	private List<DaemonTask> queue;	
	private Thread worker;
	private IDaemonCallback callback;
	private boolean paused;;
	
	public TaskQueue(IDaemonCallback callback) {
		queue = Collections.synchronizedList(new LinkedList<DaemonTask>());
		paused = true;
		this.callback = callback;
		worker = new Thread(this);
		worker.start();
	}
	
	/**
	 * Enqueue a single new task to later perform.
	 * @param task The task to add to the queue
	 */
	public synchronized void enqueue(DaemonTask task) {
		queue.add(task);
		notifyAll();
	}
	
	/**
	 * Queues an old set of tasks again in the queue. This for example can be 
	 * used to restore on old queue after an Activity was destroyed and 
	 * is restored again.
	 * @param tasks A list of daemon tasks to queue
	 */
	public synchronized void requeue(List<DaemonTask> tasks) {
		queue.addAll(tasks);
		notifyAll();
	}

	/**
	 * Removes all remaining tasks from the queue. Existing operations are still
	 * continued and their results posted back.
	 */
	public synchronized void clear() {
		queue.clear();
		notifyAll();
	}
	
	/**
	 * Removes all remaining tasks from the queue that are of some specific type.
	 * Other remaining tasks will still be executed and running operations are 
	 * still continued and their results posted back.
	 * @param class1 
	 */
	public synchronized void clear(DaemonMethod ofType) {
		Iterator<DaemonTask> task = queue.iterator();
		while (task.hasNext()) {
			if (task.next().getMethod() == ofType) {
				task.remove();
			}
		}
		notifyAll();
	}
	
	/**
	 * Returns a copy of the queue with all remaining tasks. This can be used
	 * to save them on an Activity destroy and restore them later using
	 * requeue().
	 * @return A list containing all remaining tasks
	 */
	public Queue<DaemonTask> getRemainingTasks() {
		return new LinkedList<DaemonTask>(queue);
	}
	
	/**
	 * Request the task perfoming thread to stop all activity
	 */
	public synchronized void requestStop() {
		paused = true;
	}
	
	/**
	 * Request
	 */
	public synchronized void start() {
		paused = false;
		notify();
	}

	@Override
	public void run() {
		
		while (true) {
			
			if (this.paused) {
				// We are going to pause
				DLog.d(LOG_NAME, "Task queue pausing");
			}
			synchronized (this) {
				while (this.paused || queue.isEmpty()) {
					try {
						// We are going to run again if wait() succeeded (on notify())
						wait();
						DLog.d(LOG_NAME, "Task queue resuming");
					} catch (Exception e) {
					}
				}
			}

			processTask();
			
			if (queue.isEmpty()) {
				callback.onQueueEmpty();
				// We are going to pause
				DLog.d(LOG_NAME, "Task queue pausing (queue empty)");
			}
			
		}
		
	}
	
	private void processTask() {
		
		// Get the task to execute
		DaemonTask task = queue.remove(0);
		if (task == null) {
			return;
		}

		if (callback.isAttached())
			callback.onQueuedTaskStarted(task);
		
		// Ask the daemon adapter to perform the task (which does it synchronously)
		DLog.d(LOG_NAME, "Starting task: " + task.toString());
		DaemonTaskResult result = task.execute();

		if (callback.isAttached())
			callback.onQueuedTaskFinished(task);
		
		// Return the result (to the UI thread)
		DLog.d(LOG_NAME, "Task result: " + (result == null? "null": result.toString()));
		if (result != null && !this.paused && callback.isAttached()) {
			if (result.wasSuccessful()) {
				callback.onTaskSuccess((DaemonTaskSuccessResult) result);
			} else {
				callback.onTaskFailure((DaemonTaskFailureResult) result);
			}
		}

	}
	
}
