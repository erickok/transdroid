package org.transdroid.core.gui.rss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import fr.marvinlabs.widget.CheckableRelativeLayout;

/**
 * A relative layout that that is checkable (to be used in a contextual action bar) and shows a coloured bar in the far
 * left indicating the view status, that is, if the item is new to the user or was viewed earlier.
 * @author Eric Kok
 */
public class RssitemStatusLayout extends CheckableRelativeLayout {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final int WIDTH = (int) (6 * scale + 0.5f);

	private Boolean isNew = null;
	private final Paint oldPaint = new Paint();
	private final Paint newPaint = new Paint();
	private final RectF fullRect = new RectF();

	public RssitemStatusLayout(Context context) {
		super(context);
		initPaints();
		setWillNotDraw(false);
	}

	public RssitemStatusLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaints();
		setWillNotDraw(false);
	}

	private void initPaints() {
		oldPaint.setColor(0xFF9E9E9E); // Grey
		newPaint.setColor(0xFF8ACC12); // Normal green
	}

	public void setIsNew(Boolean isNew) {
		this.isNew = isNew;
		this.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int height = getHeight();
		int width = WIDTH;
		fullRect.set(0, 0, width, height);

		if (isNew == null) {
			return;
		}

		canvas.drawRect(fullRect, isNew? newPaint: oldPaint);

	}

}
