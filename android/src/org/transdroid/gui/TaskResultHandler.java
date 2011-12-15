package org.transdroid.gui;

import org.transdroid.daemon.IDaemonCallback;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;

import android.os.Handler;
import android.os.Message;

/**
 * The Task result handler is a mediator between the worker and UI threads. It post
 * back results from the executed tasks to itself (using the Handler class) and 
 * post this results (now on the UI thread) back to the original IDaemonCallback.
 * 
 * @author erickok
 *
 */
public class TaskResultHandler extends Handler implements IDaemonCallback {

	private static final int QUEUE_EMPTY = 0;
	private static final int TASK_FINISHED = 1;
	private static final int TASK_STARTED = 2;
	private static final int TASK_FAILURE = 3;
	private static final int TASK_SUCCESS = 4;
	
	private IDaemonCallback callback;
	
	public TaskResultHandler(IDaemonCallback callback) {
		this.callback = callback;
	}

	@Override
	public void handleMessage(Message msg) {
		// We are now on the UI thread again, call the original method on the IDaemonCallback
		switch (msg.what) {
		case QUEUE_EMPTY:
			callback.onQueueEmpty();
			break;
		case TASK_FINISHED:
			callback.onQueuedTaskFinished((DaemonTask) msg.obj);
			break;
		case TASK_STARTED:
			callback.onQueuedTaskStarted((DaemonTask) msg.obj);
			break;
		case TASK_FAILURE:
			callback.onTaskFailure((DaemonTaskFailureResult) msg.obj);
			break;
		case TASK_SUCCESS:
			callback.onTaskSuccess((DaemonTaskSuccessResult) msg.obj);
			break;
		}
	}
	
	@Override
	public void onQueueEmpty() {
		Message msg = Message.obtain(this);
		msg.what = QUEUE_EMPTY;
		sendMessage(msg);
	}

	@Override
	public void onQueuedTaskFinished(DaemonTask finished) {
		Message msg = Message.obtain(this);
		msg.what = TASK_FINISHED;
		msg.obj = finished;
		sendMessage(msg);
	}

	@Override
	public void onQueuedTaskStarted(DaemonTask started) {
		Message msg = Message.obtain(this);
		msg.what = TASK_STARTED;
		msg.obj = started;
		sendMessage(msg);
	}

	@Override
	public void onTaskFailure(DaemonTaskFailureResult result) {
		Message msg = Message.obtain(this);
		msg.what = TASK_FAILURE;
		msg.obj = result;
		sendMessage(msg);
	}

	@Override
	public void onTaskSuccess(DaemonTaskSuccessResult result) {
		Message msg = Message.obtain(this);
		msg.what = TASK_SUCCESS;
		msg.obj = result;
		sendMessage(msg);
	}
	
}
