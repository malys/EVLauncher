package com.evsuite.launcher;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * An icon that takes the largest square its parent allows, up to {@code android:maxWidth}.
 *
 * <p>A favourite tile's height follows the number of rows, so a fixed icon size is either
 * clipped on a three-row page or lost in a one-row tile. A square measured from the parent
 * keeps every icon of a page the same size whatever each drawable's intrinsic size is; the
 * cap keeps a launcher icon from being upscaled past the resolution it ships with.
 */
public class SquareIconView extends AppCompatImageView {
    public SquareIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // An UNSPECIFIED side imposes nothing; it must not shrink the square to 0.
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE : MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE : MeasureSpec.getSize(heightMeasureSpec);
        int side = Math.min(Math.min(width, height), getMaxWidth());
        if (side == Integer.MAX_VALUE) {
            side = 0;
        }
        setMeasuredDimension(side, side);
    }
}
