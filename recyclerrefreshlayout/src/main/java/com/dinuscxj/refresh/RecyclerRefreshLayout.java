package com.dinuscxj.refresh;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

import java.lang.reflect.Field;

/**
 * NOTE: the class based on the {@link android.support.v4.widget.SwipeRefreshLayout} source code
 * <p>
 * The RecyclerRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The RecyclerRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * Maybe you need a custom refresh components, can be implemented by call
 * the function {@link #setRefreshView(View, ViewGroup.LayoutParams)}
 * </p>
 */
public class RecyclerRefreshLayout extends ViewGroup
        implements NestedScrollingParent, NestedScrollingChild {

    private static final int INVALID_INDEX = -1;
    private static final int INVALID_POINTER = -1;
    //the default height of the RefreshView
    private static final int DEFAULT_REFRESH_SIZE_DP = 30;
    //the animation duration of the RefreshView scroll to the refresh point or the start point
    private static final int DEFAULT_ANIMATE_DURATION = 300;
    // the threshold of the trigger to refresh
    private static final int DEFAULT_REFRESH_TARGET_OFFSET_DP = 50;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2.0f;

    // NestedScroll
    private float mTotalUnconsumed;
    private boolean mNestedScrollInProgress;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;

    //whether to remind the callback listener(OnRefreshListener)
    private boolean mIsAnimatingToStart;
    private boolean mIsRefreshing;
    private boolean mIsFitRefresh;
    private boolean mIsBeingDragged;
    private boolean mNotifyListener;
    private boolean mDispatchTargetTouchDown;

    private int mRefreshViewIndex = INVALID_INDEX;
    private int mActivePointerId = INVALID_POINTER;
    private int mAnimateToStartDuration = DEFAULT_ANIMATE_DURATION;
    private int mAnimateToRefreshDuration = DEFAULT_ANIMATE_DURATION;

    private int mFrom;
    private int mTouchSlop;
    private int mRefreshViewSize;

    private float mInitialDownY;
    private float mInitialScrollY;
    private float mInitialMotionY;
    private float mCurrentTouchOffsetY;
    private float mTargetOrRefreshViewOffsetY;
    private float mRefreshInitialOffset;
    private float mRefreshTargetOffset;

    // Whether the client has set a custom refreshing position;
    private boolean mUsingCustomRefreshTargetOffset = false;
    // Whether the client has set a custom starting position;
    private boolean mUsingCustomRefreshInitialOffset = false;
    // Whether or not the RefreshView has been measured.
    private boolean mRefreshViewMeasured = false;

    private RefreshStyle mRefreshStyle = RefreshStyle.NORMAL;

    private View mTarget;
    private View mRefreshView;

    private IDragDistanceConverter mDragDistanceConverter;

    private IRefreshStatus mIRefreshStatus;
    private OnRefreshListener mOnRefreshListener;

    private Interpolator mAnimateToStartInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
    private Interpolator mAnimateToRefreshInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

    private final Animation mAnimateToRefreshingAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            switch (mRefreshStyle) {
                case FLOAT:
                    float refreshTargetOffset = mRefreshTargetOffset + mRefreshInitialOffset;
                    animateToTargetOffset(refreshTargetOffset, mRefreshView.getTop(), interpolatedTime);
                    break;
                default:
                    animateToTargetOffset(mRefreshTargetOffset, mTarget.getTop(), interpolatedTime);
                    break;
            }
        }
    };

    private final Animation mAnimateToStartAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            switch (mRefreshStyle) {
                case FLOAT:
                    animateToTargetOffset(mRefreshInitialOffset, mRefreshView.getTop(), interpolatedTime);
                    break;
                default:
                    animateToTargetOffset(0.0f, mTarget.getTop(), interpolatedTime);
                    break;
            }
        }
    };

    private void animateToTargetOffset(float targetEnd, float currentOffset, float interpolatedTime) {
        int targetOffset = (int) (mFrom + (targetEnd - mFrom) * interpolatedTime);

        setTargetOrRefreshViewOffsetY((int) (targetOffset - currentOffset));
    }

    private final Animation.AnimationListener mRefreshingListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimatingToStart = true;
            mIRefreshStatus.refreshing();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mNotifyListener) {
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }
            }

            mIsAnimatingToStart = false;
        }
    };

    private final Animation.AnimationListener mResetListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimatingToStart = true;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            reset();
        }
    };

    public RecyclerRefreshLayout(Context context) {
        this(context, null);
    }

    public RecyclerRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mRefreshViewSize = (int) (DEFAULT_REFRESH_SIZE_DP * metrics.density);

        mRefreshTargetOffset = DEFAULT_REFRESH_TARGET_OFFSET_DP * metrics.density;

        mTargetOrRefreshViewOffsetY = 0.0f;
        mRefreshInitialOffset = 0.0f;

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

        initRefreshView();
        initDragDistanceConverter();
        setNestedScrollingEnabled(true);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    }

    @Override
    protected void onDetachedFromWindow() {
        reset();
        clearAnimation();
        super.onDetachedFromWindow();
    }

    private void reset() {
        setTargetOrRefreshViewToInitial();

        mCurrentTouchOffsetY = 0.0f;

        mIRefreshStatus.reset();
        mRefreshView.setVisibility(View.GONE);

        mIsRefreshing = false;
        mIsAnimatingToStart = false;
    }

    private void setTargetOrRefreshViewToInitial() {
        switch (mRefreshStyle) {
            case FLOAT:
                setTargetOrRefreshViewOffsetY((int) (mRefreshInitialOffset - mTargetOrRefreshViewOffsetY));
                break;
            default:
                setTargetOrRefreshViewOffsetY((int) (0 - mTargetOrRefreshViewOffsetY));
                break;
        }
    }

    private void initRefreshView() {
        mRefreshView = new RefreshView(getContext());
        mRefreshView.setVisibility(View.GONE);
        if (mRefreshView instanceof IRefreshStatus) {
            mIRefreshStatus = (IRefreshStatus) mRefreshView;
        } else {
            throw new ClassCastException("the refreshView must implement the interface IRefreshStatus");
        }

        LayoutParams layoutParams = new LayoutParams(mRefreshViewSize, mRefreshViewSize);
        addView(mRefreshView, layoutParams);
    }

    private void initDragDistanceConverter() {
        mDragDistanceConverter = new MaterialDragDistanceConverter();
    }

    /**
     * @param refreshView  must implements the interface IRefreshStatus
     * @param layoutParams the with is always the match_parent， no matter how you set
     *                     the height you need to set a specific value
     */
    public void setRefreshView(View refreshView, ViewGroup.LayoutParams layoutParams) {
        if (mRefreshView == refreshView) {
            return;
        }

        if (mRefreshView != null && mRefreshView.getParent() != null) {
            ((ViewGroup) mRefreshView.getParent()).removeView(mRefreshView);
        }

        if (refreshView instanceof IRefreshStatus) {
            mIRefreshStatus = (IRefreshStatus) refreshView;
        } else {
            throw new ClassCastException("the refreshView must implement the interface IRefreshStatus");
        }
        refreshView.setVisibility(View.GONE);
        addView(refreshView, layoutParams);

        mRefreshView = refreshView;
    }

    public void setDragDistanceConverter(@NonNull IDragDistanceConverter dragDistanceConverter) {
        if (dragDistanceConverter == null) {
            throw new NullPointerException("the dragDistanceConverter can't be null");
        }
        this.mDragDistanceConverter = dragDistanceConverter;
    }

    /**
     * @param animateToStartInterpolator The interpolator used by the animation that
     *                                   move the refresh view from the refreshing point or
     *                                   (the release point) to the start point.
     */
    public void setAnimateToStartInterpolator(Interpolator animateToStartInterpolator) {
        mAnimateToStartInterpolator = animateToStartInterpolator;
    }

    /**
     * @param animateToRefreshInterpolator The interpolator used by the animation that
     *                                     move the refresh view the release point to the refreshing point.
     */
    public void setAnimateToRefreshInterpolator(Interpolator animateToRefreshInterpolator) {
        mAnimateToRefreshInterpolator = animateToRefreshInterpolator;
    }

    /**
     * @param animateToStartDuration The duration used by the animation that
     *                               move the refresh view from the refreshing point or
     *                               (the release point) to the start point.
     */
    public void setAnimateToStartDuration(int animateToStartDuration) {
        mAnimateToStartDuration = animateToStartDuration;
    }

    /**
     * @param animateToRefreshDuration The duration used by the animation that
     *                                 move the refresh view the release point to the refreshing point.
     */
    public void setAnimateToRefreshDuration(int animateToRefreshDuration) {
        mAnimateToRefreshDuration = animateToRefreshDuration;
    }

    /**
     * @param refreshTargetOffset The minimum distance that trigger refresh.
     */
    public void setRefreshTargetOffset(float refreshTargetOffset) {
        mRefreshTargetOffset = refreshTargetOffset;
        mUsingCustomRefreshTargetOffset = true;
        requestLayout();
    }

    /**
     * @param refreshInitialOffset the top position of the {@link #mRefreshView} relative to its parent.
     */
    public void setRefreshInitialOffset(float refreshInitialOffset) {
        mRefreshInitialOffset = refreshInitialOffset;
        mUsingCustomRefreshInitialOffset = true;
        requestLayout();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        switch (mRefreshStyle) {
            case FLOAT:
                if (mRefreshViewIndex < 0) {
                    return i;
                } else if (i == childCount - 1) {
                    // Draw the selected child last
                    return mRefreshViewIndex;
                } else if (i >= mRefreshViewIndex) {
                    // Move the children after the selected child earlier one
                    return i + 1;
                } else {
                    // Keep the children before the selected child the same
                    return i;
                }
            default:
                if (mRefreshViewIndex < 0) {
                    return i;
                } else if (i == 0) {
                    // Draw the selected child first
                    return mRefreshViewIndex;
                } else if (i <= mRefreshViewIndex) {
                    // Move the children before the selected child earlier one
                    return i - 1;
                } else {
                    return i;
                }
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        switch (mRefreshStyle) {
            case FLOAT:
                return isEnabled() && canChildScrollUp(mTarget) && !mIsRefreshing
                        && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
            default:
                return isEnabled() && canChildScrollUp(mTarget)
                        && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        }
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;

            }
            Log.i("debug", "pre scroll");
            moveSpinner(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishSpinner();
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0) {
            mTotalUnconsumed += Math.abs(dy);
            Log.i("debug", "nested scroll");
            moveSpinner(mTotalUnconsumed);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            return;
        }

        ensureTarget();
        if (mTarget == null) {
            return;
        }

        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        final int childTop = getPaddingTop();
        final int childLeft = getPaddingLeft();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();

        mTarget.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        mRefreshView.layout((width / 2 - mRefreshView.getMeasuredWidth() / 2), (int) mRefreshInitialOffset,
                (width / 2 + mRefreshView.getMeasuredWidth() / 2), (int) (mRefreshInitialOffset + mRefreshView.getMeasuredHeight()));
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ensureTarget();
        if (mTarget == null) {
            return;
        }

        measureTarget();
        measureRefreshView(widthMeasureSpec, heightMeasureSpec);

        if (!mRefreshViewMeasured && !mUsingCustomRefreshInitialOffset) {
            switch (mRefreshStyle) {
                case PINNED:
                    mTargetOrRefreshViewOffsetY = mRefreshInitialOffset = 0.0f;
                    break;
                case FLOAT:
                    mTargetOrRefreshViewOffsetY = mRefreshInitialOffset = -mRefreshView.getMeasuredHeight();
                    break;
                default:
                    mTargetOrRefreshViewOffsetY = 0.0f;
                    mRefreshInitialOffset = -mRefreshView.getMeasuredHeight();
                    break;
            }
        }

        if (!mRefreshViewMeasured && !mUsingCustomRefreshTargetOffset) {
            if (mRefreshTargetOffset < mRefreshView.getMeasuredHeight()) {
                mRefreshTargetOffset = mRefreshView.getMeasuredHeight();
            }
        }

        mRefreshViewMeasured = true;

        mRefreshViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mRefreshView) {
                mRefreshViewIndex = index;
                break;
            }
        }

    }

    private void measureTarget() {
        mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
    }

    private void measureRefreshView(int widthMeasureSpec, int heightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) mRefreshView.getLayoutParams();

        final int childWidthMeasureSpec;
        if (lp.width == LayoutParams.MATCH_PARENT) {
            final int width = Math.max(0, getMeasuredWidth() - getPaddingLeft() - getPaddingRight()
                    - lp.leftMargin - lp.rightMargin);
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
        }

        final int childHeightMeasureSpec;
        if (lp.height == LayoutParams.MATCH_PARENT) {
            final int height = Math.max(0, getMeasuredHeight()
                    - getPaddingTop() - getPaddingBottom()
                    - lp.topMargin - lp.bottomMargin);
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    height, MeasureSpec.EXACTLY);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() +
                            lp.topMargin + lp.bottomMargin,
                    lp.height);
        }

        mRefreshView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // support compile sdk version < 23
                onStopNestedScroll(this);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();
        if (mTarget == null) {
            return false;
        }

        switch (mRefreshStyle) {
            case FLOAT:
                if (!isEnabled() || canChildScrollUp(mTarget)
                        || mIsRefreshing || mNestedScrollInProgress) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false;
                }
                break;
            default:
                if ((!isEnabled() || (canChildScrollUp(mTarget) && !mDispatchTargetTouchDown))) {
                    return false;
                }
                break;
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;

                float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }

                // Animation.AnimationListener.onAnimationEnd() can't be ensured to be called
                if (mAnimateToRefreshingAnimation.hasEnded() && mAnimateToStartAnimation.hasEnded()) {
                    mIsAnimatingToStart = false;
                }

                mInitialDownY = initialDownY;
                mInitialScrollY = mTargetOrRefreshViewOffsetY;
                mDispatchTargetTouchDown = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                float activeMoveY = getMotionEventY(ev, mActivePointerId);
                if (activeMoveY == -1) {
                    return false;
                }

                initDragStatus(activeMoveY);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
            default:
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        ensureTarget();
        if (mTarget == null) {
            return false;
        }

        switch (mRefreshStyle) {
            case FLOAT:
                if (!isEnabled() || canChildScrollUp(mTarget) || mNestedScrollInProgress) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false;
                }
                break;
            default:
                if ((!isEnabled() || (canChildScrollUp(mTarget) && !mDispatchTargetTouchDown))) {
                    return false;
                }
                break;
        }

        if (mRefreshStyle == RefreshStyle.FLOAT && (canChildScrollUp(mTarget) || mNestedScrollInProgress)) {
            return false;
        }

        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final float activeMoveY = getMotionEventY(ev, mActivePointerId);
                if (activeMoveY == -1) {
                    return false;
                }

                float overScrollY;
                if (mIsAnimatingToStart) {
                    overScrollY = getTargetOrRefreshViewTop();

                    mInitialMotionY = activeMoveY;
                    mInitialScrollY = overScrollY;

                    Log.i("debug", "animatetostart overscrolly " + overScrollY + " -- " + mInitialMotionY);
                } else {
                    overScrollY = activeMoveY - mInitialMotionY + mInitialScrollY;
                    Log.i("debug", "overscrolly " + overScrollY + " --" + mInitialMotionY + " -- " + mInitialScrollY);
                }

                if (mIsRefreshing) {
                    //note: float style will not come here
                    if (overScrollY <= 0) {
                        if (mDispatchTargetTouchDown) {
                            mTarget.dispatchTouchEvent(ev);
                        } else {
                            MotionEvent obtain = MotionEvent.obtain(ev);
                            obtain.setAction(MotionEvent.ACTION_DOWN);
                            mDispatchTargetTouchDown = true;
                            mTarget.dispatchTouchEvent(obtain);
                        }
                    } else if (overScrollY > 0 && overScrollY < mRefreshTargetOffset) {
                        if (mDispatchTargetTouchDown) {
                            MotionEvent obtain = MotionEvent.obtain(ev);
                            obtain.setAction(MotionEvent.ACTION_CANCEL);
                            mDispatchTargetTouchDown = false;
                            mTarget.dispatchTouchEvent(obtain);
                        }
                    }
                    Log.i("debug", "moveSpinner refreshing -- " + mInitialScrollY + " -- " + (activeMoveY - mInitialMotionY));
                    moveSpinner(overScrollY);
                } else {
                    if (mIsBeingDragged) {
                        if (overScrollY > 0) {
                            moveSpinner(overScrollY);
                        } else {
                            return false;
                        }
                    } else {
                        initDragStatus(activeMoveY);
                    }
                }
                break;
            }

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                onNewerPointerDown(ev);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER
                        || getMotionEventY(ev, mActivePointerId) == -1) {
                    resetTouchEvent();
                    return false;
                }

                if (mIsRefreshing || mIsAnimatingToStart) {
                    if (mDispatchTargetTouchDown) {
                        mTarget.dispatchTouchEvent(ev);
                    }
                    resetTouchEvent();
                    return false;
                }

                resetTouchEvent();
                finishSpinner();
                return false;
            }
            default:
                break;
        }

        return true;
    }

    private void resetTouchEvent() {
        mInitialScrollY = 0.0f;

        mIsBeingDragged = false;
        mDispatchTargetTouchDown = false;
        mActivePointerId = INVALID_POINTER;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mIsRefreshing != refreshing) {
            mIsRefreshing = refreshing;
            mNotifyListener = false;

            animateToRefreshingPosition((int) mTargetOrRefreshViewOffsetY, mRefreshingListener);
        } else {
            setRefreshing(refreshing, false);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mIsRefreshing != refreshing) {
            mNotifyListener = notify;
            mIsRefreshing = refreshing;
            if (refreshing) {
                animateToRefreshingPosition((int) mTargetOrRefreshViewOffsetY, mRefreshingListener);
            } else {
                animateOffsetToStartPosition((int) mTargetOrRefreshViewOffsetY, mResetListener);
            }
        }
    }

    private void initDragStatus(float activeMoveY) {
        float diff = activeMoveY - mInitialDownY;
        if (mIsRefreshing && (diff > mTouchSlop || mTargetOrRefreshViewOffsetY > 0)) {
            mIsBeingDragged = true;
            mInitialMotionY = mInitialDownY + mTouchSlop;
            //scroll direction: from up to down
        } else if (!mIsBeingDragged && diff > mTouchSlop) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
        }
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        clearAnimation();

        if (computeAnimateToStartDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }

        mFrom = from;
        mAnimateToStartAnimation.reset();
        mAnimateToStartAnimation.setDuration(computeAnimateToStartDuration(from));
        mAnimateToStartAnimation.setInterpolator(mAnimateToStartInterpolator);
        if (listener != null) {
            mAnimateToStartAnimation.setAnimationListener(listener);
        }

        startAnimation(mAnimateToStartAnimation);
    }

    private void animateToRefreshingPosition(int from, Animation.AnimationListener listener) {
        clearAnimation();

        if (computeAnimateToRefreshingDuration(from) <= 0) {
            listener.onAnimationStart(null);
            listener.onAnimationEnd(null);
            return;
        }

        mFrom = from;
        mAnimateToRefreshingAnimation.reset();
        mAnimateToRefreshingAnimation.setDuration(computeAnimateToRefreshingDuration(from));
        mAnimateToRefreshingAnimation.setInterpolator(mAnimateToRefreshInterpolator);

        if (listener != null) {
            mAnimateToRefreshingAnimation.setAnimationListener(listener);
        }

        startAnimation(mAnimateToRefreshingAnimation);
    }

    private int computeAnimateToRefreshingDuration(float from) {
        Log.i("debug", "from -- refreshing " + from);

        if (from < mRefreshInitialOffset) {
            return 0;
        }

        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mRefreshInitialOffset - mRefreshTargetOffset) / mRefreshTargetOffset))
                        * mAnimateToRefreshDuration);
            default:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mRefreshTargetOffset) / mRefreshTargetOffset))
                        * mAnimateToRefreshDuration);
        }
    }

    private int computeAnimateToStartDuration(float from) {
        Log.i("debug", "from -- start " + from);

        if (from < mRefreshInitialOffset) {
            return 0;
        }

        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from - mRefreshInitialOffset) / mRefreshTargetOffset))
                        * mAnimateToStartDuration);
            default:
                return (int) (Math.max(0.0f, Math.min(1.0f, Math.abs(from) / mRefreshTargetOffset))
                        * mAnimateToStartDuration);
        }
    }

    /**
     * @param targetOrRefreshViewOffsetY the top position of the target
     *                                   or the RefreshView relative to its parent.
     */
    private void moveSpinner(float targetOrRefreshViewOffsetY) {
        mCurrentTouchOffsetY = targetOrRefreshViewOffsetY;

        float convertScrollOffset;
        float refreshTargetOffset;
        if (!mIsRefreshing) {
            switch (mRefreshStyle) {
                case FLOAT:
                    convertScrollOffset = mRefreshInitialOffset
                            + mDragDistanceConverter.convert(targetOrRefreshViewOffsetY, mRefreshTargetOffset);
                    refreshTargetOffset = mRefreshTargetOffset;
                    break;
                default:
                    convertScrollOffset = mDragDistanceConverter.convert(targetOrRefreshViewOffsetY, mRefreshTargetOffset);
                    refreshTargetOffset = mRefreshTargetOffset;
                    break;
            }
        } else {
            //The Float style will never come here
            if (targetOrRefreshViewOffsetY > mRefreshTargetOffset) {
                convertScrollOffset = mRefreshTargetOffset;
            } else {
                convertScrollOffset = targetOrRefreshViewOffsetY;
            }

            if (convertScrollOffset < 0.0f) {
                convertScrollOffset = 0.0f;
            }

            refreshTargetOffset = mRefreshTargetOffset;
        }

        if (!mIsRefreshing) {
            if (convertScrollOffset > refreshTargetOffset && !mIsFitRefresh) {
                mIsFitRefresh = true;
                mIRefreshStatus.pullToRefresh();
            } else if (convertScrollOffset <= refreshTargetOffset && mIsFitRefresh) {
                mIsFitRefresh = false;
                mIRefreshStatus.releaseToRefresh();
            }
        }

        Log.i("debug", targetOrRefreshViewOffsetY + " -- " + refreshTargetOffset + " -- "
                + convertScrollOffset + " -- " + mTargetOrRefreshViewOffsetY + " -- " + mRefreshTargetOffset);

        setTargetOrRefreshViewOffsetY((int) (convertScrollOffset - mTargetOrRefreshViewOffsetY));
    }

    private void finishSpinner() {
        if (mIsRefreshing || mIsAnimatingToStart) {
            return;
        }

        float scrollY = getTargetOrRefreshViewOffset();
        if (scrollY > mRefreshTargetOffset) {
            setRefreshing(true, true);
        } else {
            mIsRefreshing = false;
            animateOffsetToStartPosition((int) mTargetOrRefreshViewOffsetY, mResetListener);
        }
    }

    private void onNewerPointerDown(MotionEvent ev) {
        final int index = MotionEventCompat.getActionIndex(ev);
        mActivePointerId = MotionEventCompat.getPointerId(ev, index);

        mInitialMotionY = getMotionEventY(ev, mActivePointerId) - mCurrentTouchOffsetY;

        Log.i("debug", " onDown " + mInitialMotionY);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = MotionEventCompat.getActionIndex(ev);
        int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }

        mInitialMotionY = getMotionEventY(ev, mActivePointerId) - mCurrentTouchOffsetY;

        Log.i("debug", " onUp " + mInitialMotionY);
    }

    private void setTargetOrRefreshViewOffsetY(int offsetY) {
        if (mTarget == null) {
          return;
        }

        switch (mRefreshStyle) {
            case FLOAT:
                mRefreshView.offsetTopAndBottom(offsetY);
                mTargetOrRefreshViewOffsetY = mRefreshView.getTop();
                break;
            case PINNED:
                mTarget.offsetTopAndBottom(offsetY);
                mTargetOrRefreshViewOffsetY = mTarget.getTop();
                break;
            default:
                mTarget.offsetTopAndBottom(offsetY);
                mRefreshView.offsetTopAndBottom(offsetY);
                mTargetOrRefreshViewOffsetY = mTarget.getTop();
                break;
        }

        Log.i("debug", "current offset" + mTargetOrRefreshViewOffsetY);

        switch (mRefreshStyle) {
            case FLOAT:
                mIRefreshStatus.pullProgress(mTargetOrRefreshViewOffsetY,
                        (mTargetOrRefreshViewOffsetY - mRefreshInitialOffset) / mRefreshTargetOffset);
                break;
            default:
                mIRefreshStatus.pullProgress(mTargetOrRefreshViewOffsetY, mTargetOrRefreshViewOffsetY / mRefreshTargetOffset);
                break;
        }

        if (mRefreshView.getVisibility() != View.VISIBLE) {
            mRefreshView.setVisibility(View.VISIBLE);
        }

        invalidate();
    }

    private int getTargetOrRefreshViewTop() {
        switch (mRefreshStyle) {
            case FLOAT:
                return mRefreshView.getTop();
            default:
                return mTarget.getTop();
        }
    }

    private int getTargetOrRefreshViewOffset() {
        switch (mRefreshStyle) {
            case FLOAT:
                return (int) (mRefreshView.getTop() - mRefreshInitialOffset);
            default:
                return mTarget.getTop();
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    private boolean canChildScrollUp(View mTarget) {
        if (mTarget == null) {
            return false;
        }

        if (android.os.Build.VERSION.SDK_INT < 14 && mTarget instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mTarget;
            return absListView.getChildCount() > 0
                    && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                    .getTop() < absListView.getPaddingTop());
        }

        if (mTarget instanceof ViewGroup) {
            int childCount = ((ViewGroup) mTarget).getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = ((ViewGroup) mTarget).getChildAt(i);
                if (canChildScrollUp(child)) {
                    return true;
                }
            }
        }

        return ViewCompat.canScrollVertically(mTarget, -1);
    }

    private void ensureTarget() {
        if (!isTargetValid()) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mRefreshView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    private boolean isTargetValid() {
        for (int i = 0; i < getChildCount(); i++) {
            if (mTarget == getChildAt(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Set the style of the RefreshView.
     *
     * @param refreshStyle One of {@link RefreshStyle#NORMAL}
     *                     , {@link RefreshStyle#PINNED}, or {@link RefreshStyle#FLOAT}
     */
    public void setRefreshStyle(@NonNull RefreshStyle refreshStyle) {
        mRefreshStyle = refreshStyle;
    }

    public enum RefreshStyle {
        NORMAL,
        PINNED,
        FLOAT
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    /**
     * Per-child layout information for layouts that support margins.
     */
    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }
}