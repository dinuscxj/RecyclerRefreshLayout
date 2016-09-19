package com.dinuscxj.example.demo;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dinuscxj.example.R;
import com.dinuscxj.refresh.IRefreshStatus;

public class RefreshViewEg extends LinearLayout implements IRefreshStatus {
    private static final int ANIMATION_DURATION = 150;
    private static final Interpolator ANIMATION_INTERPOLATOR = new DecelerateInterpolator();

    private Animation mRotateAnimation;
    private Animation mResetRotateAnimation;
    private Animation mRefreshingRotateAnimation;

    private TextView mTextRefreshView;
    private ImageView mImageRefreshView;

    public RefreshViewEg(Context context) {
        this(context, null);
    }

    public RefreshViewEg(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAnimation();

        LayoutInflater.from(context).inflate(R.layout.layout_refresh, this);

        mTextRefreshView = (TextView) findViewById(R.id.refresh_text);
        mImageRefreshView = (ImageView) findViewById(R.id.refresh_image);

        reset();
    }

    private void initAnimation() {
        mRotateAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mRotateAnimation.setDuration(ANIMATION_DURATION);
        mRotateAnimation.setFillAfter(true);

        mResetRotateAnimation = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mResetRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mResetRotateAnimation.setDuration(ANIMATION_DURATION);
        mResetRotateAnimation.setFillAfter(true);

        mRefreshingRotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRefreshingRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mRefreshingRotateAnimation.setDuration(ANIMATION_DURATION * 2);
        mRefreshingRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRefreshingRotateAnimation.setRepeatMode(Animation.RESTART);
        mRefreshingRotateAnimation.setFillAfter(true);
    }

    @Override
    public void reset() {
        mTextRefreshView.setText("pull to refresh");
        mTextRefreshView.setTextColor(Color.RED);

        mImageRefreshView.clearAnimation();
        mImageRefreshView.setImageDrawable(getResources().getDrawable(R.drawable.simple_ptr_flip));
    }

    @Override
    public void refreshing() {
        mTextRefreshView.setText("is loading");

        mImageRefreshView.clearAnimation();
        mImageRefreshView.setImageResource(R.drawable.simple_ptr_rotate);
        mImageRefreshView.startAnimation(mRefreshingRotateAnimation);
    }

    @Override
    public void pullToRefresh() {
        mTextRefreshView.setText("release to refresh");

        mImageRefreshView.clearAnimation();
        if (mImageRefreshView.getAnimation() == null
                || mImageRefreshView.getAnimation() == mResetRotateAnimation) {
            mImageRefreshView.startAnimation(mRotateAnimation);
        }
    }

    @Override
    public void releaseToRefresh() {
        mTextRefreshView.setText("pull to refresh");

        mImageRefreshView.clearAnimation();
        if (mRotateAnimation == mImageRefreshView.getAnimation()) {
            mImageRefreshView.startAnimation(mResetRotateAnimation);
        }
    }

    @Override
    public void pullProgress(float pullDistance, float pullProgress) {
        if (pullProgress > 1.0f) {
            pullProgress = 1.0f;
        }
        pullProgress *= pullProgress * pullProgress;

        mTextRefreshView.setTextColor(evaluateColorChange(pullProgress, Color.RED, Color.GREEN));
    }

    private int evaluateColorChange(float fraction, int startValue, int endValue) {
        int startInt = startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }
}
