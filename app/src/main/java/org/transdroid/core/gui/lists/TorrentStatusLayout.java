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
import org.transdroid.daemon.TorrentStatus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * A relative layout that is checkable (to be used in a contextual action bar) and shows a coloured bar in the far left
 * indicating the status of the represented torrent. Active downloads are blue, seeding torrents are green, errors are
 * red, etc.
 * @author Eric Kok
 */
public class TorrentStatusLayout extends RelativeLayout {

	private final float scale = getContext().getResources().getDisplayMetrics().density;
	private final int WIDTH = (int) (6 * scale + 0.5f);

	private TorrentStatus status = null;
	private final Paint pausedPaint = new Paint();
	private final Paint otherPaint = new Paint();
	private final Paint downloadingPaint = new Paint();
	private final Paint seedingPaint = new Paint();
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
		pausedPaint.setColor(getResources().getColor(R.color.torrent_paused));
		otherPaint.setColor(getResources().getColor(R.color.torrent_other));
		downloadingPaint.setColor(getResources().getColor(R.color.torrent_downloading));
		seedingPaint.setColor(getResources().getColor(R.color.torrent_seeding));
		errorPaint.setColor(getResources().getColor(R.color.torrent_error));
	}

	/**
	 * Registers the status of the represented torrent and invalidates the view so the status colour will be updated
	 * accordingly.
	 * @param status The updated torrent status to show
	 */
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
			canvas.drawRect(fullRect, downloadingPaint);
			break;
		case Paused:
			canvas.drawRect(fullRect, pausedPaint);
			break;
		case Seeding:
			canvas.drawRect(fullRect, seedingPaint);
			break;
		case Error:
			canvas.drawRect(fullRect, errorPaint);
			break;
		default: // Checking, Waiting, Queued, Unknown
			canvas.drawRect(fullRect, otherPaint);
			break;
		}

	}

}
