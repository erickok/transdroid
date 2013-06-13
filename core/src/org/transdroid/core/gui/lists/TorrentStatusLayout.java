package org.transdroid.core.gui.lists;

import org.transdroid.daemon.TorrentStatus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import fr.marvinlabs.widget.CheckableRelativeLayout;

public class TorrentStatusLayout extends CheckableRelativeLayout {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final int WIDTH = (int) (5 * scale + 0.5f);

	private TorrentStatus status = null;
	private final Paint inactiveDonePaint = new Paint();
	private final Paint inactivePaint = new Paint();
	private final Paint progressPaint = new Paint();
	private final Paint donePaint = new Paint();
	private final Paint errorPaint = new Paint();
	private final RectF fullRect = new RectF();

	public TorrentStatusLayout(Context context) {
		super(context);
		initPaints();
		setWillNotDraw(false);
	}

	public TorrentStatusLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaints();
		setWillNotDraw(false);
	}

	private void initPaints() {
		inactiveDonePaint.setColor(0xFFA759D4);	// Purple
		inactivePaint.setColor(0xFF9E9E9E);		// Grey
		progressPaint.setColor(0xFF42A8FA);		// Blue
		donePaint.setColor(0xFF8ACC12);			// Green
		errorPaint.setColor(0xFFDE3939);		// Red
	}

	public void setStatus(TorrentStatus status) {
		this.status = status;
		this.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int height = getHeight();
		int width = WIDTH;
		fullRect.set(0, 0, width, height);

		if (status == null) {
			return;
		}
		
		switch (status) {
		case Downloading:
			canvas.drawRect(fullRect, progressPaint);
			break;
		case Paused:
			canvas.drawRect(fullRect, inactiveDonePaint);
			break;
		case Seeding:
			canvas.drawRect(fullRect, donePaint);
			break;
		case Error:
			canvas.drawRect(fullRect, errorPaint);
			break;
		default: // Checking, Waiting, Queued, Unknown
			canvas.drawRect(fullRect, inactivePaint);
			break;
		}

	}

}
