/*
 * Copyright 2010-2024 Eric Kok et al.
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;

/**
 * A relative layout that paints a coloured bar along its far-left edge, used to indicate a status
 * (e.g. a peer's encryption or a tracker's connection state). This generalises the colour-bar drawing
 * of {@link TorrentFilePriorityLayout} to an arbitrary colour.
 *
 * @author Eric Kok
 */
public class StatusBarLayout extends RelativeLayout {

    private final float scale = getContext().getResources().getDisplayMetrics().density;
    private final int WIDTH = (int) (6 * scale + 0.5f);
    private final Paint barPaint = new Paint();
    private final RectF fullRect = new RectF();
    private boolean hasColor = false;

    public StatusBarLayout(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public StatusBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    /**
     * Sets the colour of the left-edge status bar. Use {@link #clearBarColor()} to hide it.
     *
     * @param color The colour to paint
     */
    public void setBarColor(@ColorInt int color) {
        this.hasColor = true;
        this.barPaint.setColor(color);
        this.invalidate();
    }

    public void clearBarColor() {
        this.hasColor = false;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!hasColor) {
            return;
        }
        fullRect.set(0, 0, WIDTH, getHeight());
        canvas.drawRect(fullRect, barPaint);
    }

}
