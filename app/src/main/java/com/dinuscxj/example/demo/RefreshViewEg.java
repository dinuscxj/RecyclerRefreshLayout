package com.dinuscxj.example.demo;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.dinuscxj.example.R;
import com.dinuscxj.refresh.IRefreshStatus;

/**
 * the default implementation class of the interface IRefreshStatus, and the class should always be rewritten
 */
public class RefreshViewEg extends android.support.v7.widget.AppCompatImageView implements IRefreshStatus {
    private static final int ANIMATION_DURATION = 150;
    private static final Interpolator ANIMATION_INTERPOLATOR = new DecelerateInterpolator();

    private Animation mRotateAnimation;
    private Animation mResetRotateAnimation;

    public RefreshViewEg(Context context) {
        this(context, null);
    }

    public RefreshViewEg(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        initAnimation();
    }

    private void initView() {
        this.setScaleType(ScaleType.CENTER);
        this.setImageResource(R.drawable.default_ptr_flip);
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
    }

    @Override
    public void reset() {
        clearAnimation();

        this.setImageResource(R.drawable.default_ptr_flip);
    }

    @Override
    public void refreshing() {
        clearAnimation();

        AnimationDrawable drawable = (AnimationDrawable) getResources().getDrawable(R.drawable.spinner);
        this.setImageDrawable(drawable);
        drawable.start();
    }

    @Override
    public void refreshComplete() {

    }

    @Override
    public void pullToRefresh() {
        clearAnimation();

        if (getAnimation() == null || getAnimation() == mResetRotateAnimation) {
            startAnimation(mRotateAnimation);
        }
    }

    @Override
    public void releaseToRefresh() {
        clearAnimation();

        if (getAnimation() == null || getAnimation() == mRotateAnimation) {
            startAnimation(mResetRotateAnimation);
        }
    }

    @Override
    public void pullProgress(float pullDistance, float pullProgress) {

    }
}
