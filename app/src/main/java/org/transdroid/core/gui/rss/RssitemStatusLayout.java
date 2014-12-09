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
package org.transdroid.core.gui.rss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import org.transdroid.R;

/**
 * A relative layout that that is checkable (to be used in a contextual action bar) and shows a coloured bar in the far
 * left indicating the view status, that is, if the item is new to the user or was viewed earlier.
 * @author Eric Kok
 */
public class RssitemStatusLayout extends RelativeLayout {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final int WIDTH = (int) (6 * scale + 0.5f);
	private final Paint oldPaint = new Paint();
	private final Paint newPaint = new Paint();
	private final RectF fullRect = new RectF();
	private Boolean isNew = null;

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
		oldPaint.setColor(getResources().getColor(R.color.file_off)); // Grey
		newPaint.setColor(getResources().getColor(R.color.file_normal)); // Normal green
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

		canvas.drawRect(fullRect, isNew ? newPaint : oldPaint);

	}

}
