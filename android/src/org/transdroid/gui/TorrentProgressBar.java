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
 package org.transdroid.gui;

import org.transdroid.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class TorrentProgressBar extends View {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final float ROUND_SIZE = 3.3f * scale;
	private final int MINIMUM_HEIGHT = (int)(8 * scale + 0.5f);
	private final int RIGHT_MARGIN = (int)(3 * scale + 0.5f);
	
	private int progress;
	private boolean isActive;
	private boolean isError;
	private final Paint notdonePaint = new Paint();
	private final Paint inactiveDonePaint = new Paint();
	private final Paint inactivePaint = new Paint();
	private final Paint progressPaint = new Paint();
	private final Paint donePaint = new Paint();
	private final Paint errorPaint = new Paint();
	
	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}

	public TorrentProgressBar(Context context) {
		super(context);
		initPaints();
	}
	
	public TorrentProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaints();
		
		// Parse any set attributes from XML
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TorrentProgressBar);
		if (a.hasValue(R.styleable.TorrentProgressBar_progress)) {
			this.progress = a.getIndex(R.styleable.TorrentProgressBar_progress);
			this.isActive = a.getBoolean(R.styleable.TorrentProgressBar_isActive, false);
		}
		a.recycle();
	}

    private void initPaints() {
		notdonePaint.setColor(0xFFEEEEEE);
		inactiveDonePaint.setColor(0xFFA759D4);
		inactivePaint.setColor(0xFF9E9E9E);
		progressPaint.setColor(0xFF42A8FA);
		donePaint.setColor(0xFF8CCF29);
		errorPaint.setColor(0xFFDE3939);
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int ws = MeasureSpec.getSize(widthMeasureSpec) - RIGHT_MARGIN;
    	int hs = Math.max(getHeight(), MINIMUM_HEIGHT);
    	setMeasuredDimension(ws, hs);
    }

	@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = getHeight();
        int width = getWidth();
        RectF fullRect = new RectF(0, 0, width, height);
        
        // Error?
        if (isError) {
        	canvas.drawRoundRect(fullRect, ROUND_SIZE, ROUND_SIZE, errorPaint);
        } else {
	        // Background rounded rectangle
	        canvas.drawRoundRect(fullRect, ROUND_SIZE, ROUND_SIZE, notdonePaint);
	        
	        // Foreground progress indicator
	        if (progress > 0) {
	        	RectF progressRect = new RectF(0, 0, width * ((float)progress / 100), height);
	            canvas.drawRoundRect(progressRect, ROUND_SIZE, ROUND_SIZE, 
	            		(isActive? (progress == 100? donePaint: progressPaint): (progress == 100? inactiveDonePaint: inactivePaint)));
	        }
        }
        
    }

}
