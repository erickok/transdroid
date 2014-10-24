/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.lists;

import org.transdroid.R;
import org.transdroid.daemon.Priority;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * A relative layout that that is checkable (to be used in a contextual action bar) and shows a coloured bar in the far
 * left indicating the priority of the represented file. The darker the green, the higher the priority, while grey means
 * the file isn't downloaded at all.
 * @author Eric Kok
 */
public class TorrentFilePriorityLayout extends RelativeLayout {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final int WIDTH = (int) (6 * scale + 0.5f);

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
		offPaint.setColor(getResources().getColor(R.color.file_off));
		lowPaint.setColor(getResources().getColor(R.color.file_low));
		normalPaint.setColor(getResources().getColor(R.color.file_normal));
		highPaint.setColor(getResources().getColor(R.color.file_high));
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
