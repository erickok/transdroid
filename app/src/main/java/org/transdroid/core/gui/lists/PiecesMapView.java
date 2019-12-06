package org.transdroid.core.gui.lists;

import org.transdroid.R;

import android.content.Context;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

class PiecesMapView extends View {

    private final float scale = getContext().getResources().getDisplayMetrics().density;
    private final int MINIMUM_HEIGHT = (int) (25 * scale);
    private final int MINIMUM_PIECE_WIDTH = (int) (2 * scale);

    private ArrayList<Integer> pieces = null;

    private final Paint downloadingPaint = new Paint();
    private final Paint donePaint = new Paint();
    private final Paint partialDonePaint = new Paint();

    public PiecesMapView(Context context) {
        super(context);
        initPaints();
    }

    private void initPaints() {
        downloadingPaint.setColor(getResources().getColor(R.color.torrent_downloading));
        donePaint.setColor(getResources().getColor(R.color.torrent_seeding));
        partialDonePaint.setColor(getResources().getColor(R.color.file_low));
    }

    public void setPieces(List<Integer> pieces) {
        this.pieces = new ArrayList<Integer>(pieces);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int ws = MeasureSpec.getSize(widthMeasureSpec);
            int hs = Math.max(getHeight(), MINIMUM_HEIGHT);
            setMeasuredDimension(ws, hs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (this.pieces == null) {
            return;
        }

        int height = getHeight();
        int width = getWidth();

        // downscale
        ArrayList<Integer> piecesScaled;
        int pieceWidth;

        pieceWidth = MINIMUM_PIECE_WIDTH;
        piecesScaled = new ArrayList<Integer>();

        int bucketCount = (int) Math.ceil((double) width / (double) pieceWidth);
        int bucketSize = (int) Math.floor((double)this.pieces.size() / (double) bucketCount);

        // loop buckets
        for (int i = 0; i < bucketCount; i++) {

            // Get segment of pieces that fall into bucket
            int start = i * bucketSize;

            // If this is the last bucket, throw the remainder of the pieces array into it
            int end = (i == bucketCount-1) ? this.pieces.size() : (i+1) * bucketSize;

            ArrayList<Integer> bucket = new ArrayList<Integer>(this.pieces.subList(start, end));

            int doneCount = 0;
            int downloadingCount = 0;

            // loop pieces in bucket
            for(int j = 0; j < bucket.size(); j++) {
                // Count downloading pieces
                if (bucket.get(j) == 1) {
                    downloadingCount++;
                }
                // Count finished pieces
                else if (bucket.get(j) == 2) {
                    doneCount++;
                }
            }

            int state;
            // If a piece is downloading show bucket as downloading
            if (downloadingCount > 0) {
                state = 1;
            }
            // If all pieces are done, show bucket as done
            else if (doneCount == bucket.size()) {
                state = 2;
            }
            // Some done pieces, show bucket as partially done
            else if (doneCount > 0) {
                state = 3;
            }
            // bucket is not downloaded
            else {
                state = 0;
            }

            piecesScaled.add(state);
        }

        String scaledPiecesString = "";
        for (int s : piecesScaled)
        {
            scaledPiecesString += s;
        }

        // Draw downscaled peices
        for (int i = 0; i < piecesScaled.size(); i++) {
            int piece = piecesScaled.get(i);

            if (piece == 0) {
                continue;
            }

            Paint paint = new Paint();
            switch (piece) {
                case 1:
                    paint = downloadingPaint;
                    break;
                case 2:
                    paint = donePaint;
                    break;
                case 3:
                    paint = partialDonePaint;
                    break;
            }
            int x = i * pieceWidth;

            canvas.drawRect(x, 0, x + pieceWidth, height, paint);
        }
    }

}

