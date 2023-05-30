package com.file.productx.sendfile.scan;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;

import me.dm7.barcodescanner.core.ViewFinderView;

public class CustomViewFinderView extends ViewFinderView {

    private final Paint mPaint = new Paint();

    public CustomViewFinderView(Context context) {
        super(context);
        this.init();
    }

    public CustomViewFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    private void init() {
        this.mPaint.setColor(Color.WHITE);
        this.mPaint.setAntiAlias(true);
        this.setBorderColor(Color.WHITE);
        this.setLaserColor(Color.WHITE);
        this.mPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40, this.getResources().getDisplayMetrics()));
        this.setSquareViewFinder(true);
    }

}
