package com.dinuscxj.refresh;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * the default implementation class of the interface IRefreshStatus, and the class should always be rewritten
 */
public class RefreshView extends View implements IRefreshStatus {
    private static final int MAX_ARC_DEGREE = 330;
    private static final int ANIMATION_DURATION = 888;
    private static final int DEFAULT_START_DEGREES = 285;
    private static final int DEFAULT_STROKE_WIDTH = 2;

    private final RectF mArcBounds = new RectF();
    private final Paint mPaint = new Paint();

    private float mStartDegrees;
    private float mSwipeDegrees;

    private float mStrokeWidth;

    private boolean mHasTriggeredRefresh;

    private ValueAnimator mRotateAnimator;

    public RefreshView(Context context) {
        this(context, null);
    }

    public RefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initData();
        initPaint();
    }

    private void initData() {
        float density = getResources().getDisplayMetrics().density;
        mStrokeWidth = density * DEFAULT_STROKE_WIDTH;

        mStartDegrees = DEFAULT_START_DEGREES;
        mSwipeDegrees = 0.0f;
    }

    private void initPaint() {
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(Color.parseColor("#FFD72263"));
    }

    private void startAnimator() {
        mRotateAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mRotateAnimator.setInterpolator(new LinearInterpolator());
        mRotateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float rotateProgress = (float) animation.getAnimatedValue();
                setStartDegrees(DEFAULT_START_DEGREES + rotateProgress * 360);
            }
        });
        mRotateAnimator.setRepeatMode(ValueAnimator.RESTART);
        mRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRotateAnimator.setDuration(ANIMATION_DURATION);

        mRotateAnimator.start();
    }

    private void resetAnimator() {
        if (mRotateAnimator != null) {
            mRotateAnimator.cancel();
            mRotateAnimator.removeAllUpdateListeners();

            mRotateAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        resetAnimator();
        super.onDetachedFromWindow();
    }

    private void drawArc(Canvas canvas) {
        canvas.drawArc(mArcBounds, mStartDegrees, mSwipeDegrees, false, mPaint);
    }

    private void setStartDegrees(float startDegrees) {
        mStartDegrees = startDegrees;
        postInvalidate();
    }

    public void setSwipeDegrees(float swipeDegrees) {
        this.mSwipeDegrees = swipeDegrees;
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float radius = Math.min(w, h) / 2.0f;
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        mArcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        mArcBounds.inset(mStrokeWidth / 2.0f, mStrokeWidth / 2.0f);
    }

    @Override
    public void reset() {
        resetAnimator();

        mHasTriggeredRefresh = false;
        mStartDegrees = DEFAULT_START_DEGREES;
        mSwipeDegrees = 0.0f;
    }

    @Override
    public void refreshing() {
        mHasTriggeredRefresh = true;
        mSwipeDegrees = MAX_ARC_DEGREE;

        startAnimator();
    }

    @Override
    public void refreshComplete() {

    }

    @Override
    public void pullToRefresh() {

    }

    @Override
    public void releaseToRefresh() {
    }

    @Override
    public void pullProgress(float pullDistance, float pullProgress) {
        if (!mHasTriggeredRefresh) {
            float swipeProgress = Math.min(1.0f, pullProgress);
            setSwipeDegrees(swipeProgress * MAX_ARC_DEGREE);
        }
    }
}
