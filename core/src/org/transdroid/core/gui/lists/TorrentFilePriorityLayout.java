package org.transdroid.core.gui.lists;

import org.transdroid.daemon.Priority;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import fr.marvinlabs.widget.CheckableRelativeLayout;

public class TorrentFilePriorityLayout extends CheckableRelativeLayout {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final int WIDTH = (int) (5 * scale + 0.5f);

	private Priority priority = null;
	private final Paint offPaint = new Paint();
	private final Paint lowPaint = new Paint();
	private final Paint highPaint = new Paint();
	private final Paint normalPaint = new Paint();
	private final RectF fullRect = new RectF();

	public TorrentFilePriorityLayout(Context context) {
		super(context);
		initPaints();
		setWillNotDraw(false);
	}

	public TorrentFilePriorityLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaints();
		setWillNotDraw(false);
	}

	private void initPaints() {
		offPaint.setColor(0xFF9E9E9E);		// Grey
		lowPaint.setColor(0x778ACC12);		// Very Light green
		normalPaint.setColor(0xBB8ACC12);		// Light green
		highPaint.setColor(0xFF8ACC12);			// Green
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
		this.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int height = getHeight();
		int width = WIDTH;
		fullRect.set(0, 0, width, height);

		if (priority == null) {
			return;
		}
		
		switch (priority) {
		case Low:
			canvas.drawRect(fullRect, lowPaint);
			break;
		case Normal:
			canvas.drawRect(fullRect, normalPaint);
			break;
		case High:
			canvas.drawRect(fullRect, highPaint);
			break;
		default: // Off
			canvas.drawRect(fullRect, offPaint);
			break;
		}

	}

}
