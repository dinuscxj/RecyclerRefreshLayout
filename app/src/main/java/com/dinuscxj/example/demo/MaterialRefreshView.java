package com.dinuscxj.example.demo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.dinuscxj.refresh.IRefreshStatus;

/**
 * the default implementation class of the interface IRefreshStatus, and the class should always be rewritten
 */
public class MaterialRefreshView extends View implements IRefreshStatus {
    private static final int KEY_SHADOW_COLOR = 0x1E000000;
    private static final int FILL_SHADOW_COLOR = 0x3D000000;
    // PX
    private static final float X_OFFSET = 0f;
    private static final float Y_OFFSET = 1.75f;
    private static final float SHADOW_RADIUS = 3.5f;
    private static final int SHADOW_ELEVATION = 4;


    private static final int MAX_ARC_DEGREE = 330;
    private static final int ANIMATION_DURATION = 888;
    private static final int DEFAULT_START_DEGREES = 285;
    private static final int DEFAULT_STROKE_WIDTH = 2;

    private final RectF mArcBounds = new RectF();
    private final Paint mPaint = new Paint();

    private float mStartDegrees;
    private float mSwipeDegrees;

    private float mStrokeWidth;
    private int mShadowRadius;

    private boolean mHasTriggeredRefresh;

    private ValueAnimator mRotateAnimator;

    public MaterialRefreshView(Context context) {
        this(context, null);
    }

    public MaterialRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
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

    private void initBackground(float radius) {
        final float density = getContext().getResources().getDisplayMetrics().density;
        final int diameter = (int) (radius * density * 2);
        final int shadowYOffset = (int) (density * Y_OFFSET);
        final int shadowXOffset = (int) (density * X_OFFSET);

        final int color = Color.parseColor("#FFFAFAFA");

        mShadowRadius = (int) (density * SHADOW_RADIUS);

        ShapeDrawable circle;
        if (elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
        } else {
            OvalShape oval = new OvalShadow(mShadowRadius, diameter);
            circle = new ShapeDrawable(oval);
            ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, circle.getPaint());
            circle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset,
                    KEY_SHADOW_COLOR);
            final int padding = mShadowRadius;
            // set padding so the inner image sits correctly within the shadow.
            setPadding(padding, padding, padding, padding);
        }
        circle.getPaint().setColor(color);
        setBackgroundDrawable(circle);
    }

    private boolean elevationSupported() {
        return android.os.Build.VERSION.SDK_INT >= 21;
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
        super.onDetachedFromWindow();
        resetAnimator();
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
        mArcBounds.inset(radius * 0.333f, radius * 0.333f);

        initBackground(radius);
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

    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private Paint mShadowPaint;
        private int mCircleDiameter;

        public OvalShadow(int shadowRadius, int circleDiameter) {
            super();
            mShadowPaint = new Paint();
            mShadowRadius = shadowRadius;
            mCircleDiameter = circleDiameter;
            mRadialGradient = new RadialGradient(mCircleDiameter / 2, mCircleDiameter / 2,
                    mShadowRadius, new int[] {
                    FILL_SHADOW_COLOR, Color.TRANSPARENT
            }, null, Shader.TileMode.CLAMP);
            mShadowPaint.setShader(mRadialGradient);
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            final int viewWidth = MaterialRefreshView.this.getWidth();
            final int viewHeight = MaterialRefreshView.this.getHeight();
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2 + mShadowRadius),
                    mShadowPaint);
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2), paint);
        }
    }
}
