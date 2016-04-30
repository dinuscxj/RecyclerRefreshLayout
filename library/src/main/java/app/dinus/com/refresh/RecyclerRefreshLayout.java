package app.dinus.com.refresh;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * NOTE: the class based on the {@link android.support.v4.widget.SwipeRefreshLayout} source code
 *
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
 * the function {@link #setRefreshView(View, LayoutParams)},
 * you may also need to define the refresh status control components,
 * can be implemented by call the function {@link #setRefreshStateView(View)}
 * </p>
 */
public class RecyclerRefreshLayout extends ViewGroup
        implements NestedScrollingParent, NestedScrollingChild {

    private static final int INVALID_INDEX = -1;
    private static final int INVALID_POINTER = -1;
    //the default height of the RefreshView
    private static final int DEFAULT_REFRESH_SIZE_DP = 30;
    //the animation duration of the RefreshView scroll to the refresh point or the start point
    private static final int DEFAULT_ANIMATE_DURATION = 200;
    // the threshold of the trigger to refresh
    private static final int DEFAULT_REFRESH_TARGET_OFFSET_DP = 50;

    private static final float DRAG_RATE = .5f;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;

    // NestedScroll
    private float mTotalUnconsumed;
    private final int[] mParentScrollConsumed = new int[2];
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;

    //whether to remind the callback listener(OnRefreshListener)
    private boolean mNotify;
    private boolean mRefreshing;
    private boolean mIsBeingDragged;
    private boolean mIsFitRefreshing;
    private boolean mReturningToStart;

    private int mRefreshViewIndex = INVALID_INDEX;
    private int mActivePointerId = INVALID_POINTER;
    private int mAnimateDuration = DEFAULT_ANIMATE_DURATION;

    private int mFrom;
    private int mTouchSlop;
    private int mSpinnerSize;
    private int mCurrentScrollOffset;

    private float mInitialDownY;
    private float mInitialMotionY;
    private float mRefreshTargetOffset;

    private View mTarget;
    private View mRefreshView;
    private View mRefreshStateView;

    private IRefreshStatus mIRefreshStatus;
    private OnRefreshListener mOnRefreshListener;

    private Interpolator mInterpolator;

    private final Animation mAnimateToRefreshingAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int targetEnd = (int) mRefreshTargetOffset;
            int targetTop = (int) (mFrom + (targetEnd - mFrom) * interpolatedTime);

            scrollTargetOffset(0, -mCurrentScrollOffset - targetTop);
        }
    };

    private final Animation mAnimateToStartAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int targetEnd = 0;
            int targetTop = (int) (mFrom + (targetEnd - mFrom) * interpolatedTime);

            scrollTargetOffset(0, -mCurrentScrollOffset - targetTop);
        }
    };

    private final Animation.AnimationListener mRefreshingListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mIRefreshStatus.refreshing();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mNotify) {
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }
            }
        }
    };

    private final Animation.AnimationListener mResetListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mIRefreshStatus.reset();
        }
    };

    public RecyclerRefreshLayout(Context context) {
        this(context, null);
    }

    public RecyclerRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mSpinnerSize = (int) (DEFAULT_REFRESH_SIZE_DP * metrics.density);

        mRefreshTargetOffset = DEFAULT_REFRESH_TARGET_OFFSET_DP * metrics.density;

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

        setWillNotDraw(false);
        onCreateRefreshView();
        onCreateRefreshStateView();
        setNestedScrollingEnabled(true);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    }

    protected void onCreateRefreshView() {
        mRefreshView = new RefreshView(getContext());

        if (mRefreshView instanceof IRefreshStatus) {
            mIRefreshStatus = (IRefreshStatus) mRefreshView;
        } else {
            throw new ClassCastException("the refreshView must implement the interface IRefreshStatus");
        }

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, mSpinnerSize);
        addView(mRefreshView, layoutParams);
    }

    protected void onCreateRefreshStateView() {
        if (mRefreshStateView == null) {
            return;
        }

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mRefreshStateView, layoutParams);
    }

    public void setRefreshStateView(View refreshStateView) {
        if (mRefreshStateView == refreshStateView) {
            return;
        }

        if (mRefreshStateView != null && mRefreshStateView.getParent() != null) {
            ((ViewGroup) mRefreshStateView.getParent()).removeView(mRefreshStateView);
        }

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mRefreshStateView, layoutParams);
    }

    /**
     * Note
     * @param refreshView must implements the interface IRefreshStatus
     * @param layoutParams the with is always the match_parent， no matter how you set
     *                     the height you need to set a specific value
     */
    public void setRefreshView(View refreshView, LayoutParams layoutParams) {
        if (mRefreshView == refreshView) {
            return;
        }

        if (mRefreshView != null && mRefreshView.getParent() != null) {
            ((ViewGroup) mRefreshView.getParent()).removeView(mRefreshView);
        }

        mRefreshView = refreshView;

        if (mRefreshView instanceof IRefreshStatus) {
            mIRefreshStatus = (IRefreshStatus) mRefreshView;
        } else {
            throw new ClassCastException("the refreshView must implement the interface IRefreshStatus");
        }
        addView(mRefreshView, layoutParams);
    }

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void setAnimateDuration(int duration) {
        mAnimateDuration = duration;
    }

    public void setRefreshTargetOffset(float refreshTargetOffset) {
        mRefreshTargetOffset = refreshTargetOffset;
        requestLayout();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mRefreshViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            return mRefreshViewIndex;
        } else if (i >= mRefreshViewIndex) {
            return i + 1;
        } else {
            return i;
        }
    }

    // NestedScrollingParent
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        if (isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0) {
            startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
            return true;
        }
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        mTotalUnconsumed = 0;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinner(mTotalUnconsumed);
        }

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
        if (mTotalUnconsumed > 0) {
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed) {
        if (dyUnconsumed < 0) {
            dyUnconsumed = Math.abs(dyUnconsumed);
            mTotalUnconsumed += dyUnconsumed;
            moveSpinner(mTotalUnconsumed);
        }
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dxConsumed, null);
    }

    // NestedScrollingChild
    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

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
        if (mTarget == null) {
            ensureTarget();
        }
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

        if (mRefreshStateView != null) {
            mRefreshStateView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }

        if (mRefreshTargetOffset < mRefreshView.getHeight()) {
            mRefreshTargetOffset = mRefreshView.getHeight();
        }

        int offsetTop = (int) -(mRefreshTargetOffset - (mRefreshTargetOffset - mRefreshView.getMeasuredHeight()) / 2);

        mRefreshView.layout((width / 2 - mRefreshView.getMeasuredWidth() / 2), offsetTop,
                (width / 2 + mRefreshView.getMeasuredWidth() / 2), offsetTop + mRefreshView.getMeasuredHeight());
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }

        if (mRefreshStateView != null) {
            mRefreshStateView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        }

        mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mRefreshView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mRefreshView.getLayoutParams().height, MeasureSpec.EXACTLY));

        mRefreshViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mRefreshView) {
                mRefreshViewIndex = index;
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        if (mRefreshing) {
            return true;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp(mTarget) || canChildScrollUp(mRefreshStateView)) {
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                scrollTargetOffset(0, 0);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;

                float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }

                mInitialDownY = initialDownY;
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
        if (mRefreshing) {
            return true;
        }

        if (mReturningToStart && ev.getAction() == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp(mTarget) || canChildScrollUp(mRefreshStateView)) {
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

                final float overScrollY = (activeMoveY - mInitialMotionY) * DRAG_RATE;

                if (mIsBeingDragged) {
                    if (overScrollY > 0) {
                        moveSpinner(overScrollY);
                    } else {
                        return false;
                    }
                } else {
                    initDragStatus(activeMoveY);
                }
                break;
            }

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final float activeMoveY = getMotionEventY(ev, mActivePointerId);
                if (activeMoveY == -1) {
                    mIsBeingDragged = false;
                    mActivePointerId = INVALID_POINTER;
                    return false;
                }

                if (!mIsBeingDragged) {
                    return false;
                }

                final float overScrollTop = (activeMoveY - mInitialMotionY) * DRAG_RATE;

                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;

                finishSpinner(overScrollTop);
                return false;
            }
            default:
                break;
        }

        return true;
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            mRefreshing = refreshing;
            scrollTargetOffset(0, 0);
            mNotify = false;

            animateToRefreshingPosition(-mCurrentScrollOffset, mRefreshingListener);
        } else {
            setRefreshing(refreshing, false);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            mRefreshing = refreshing;
            if (refreshing) {
                animateToRefreshingPosition(-mCurrentScrollOffset, mRefreshingListener);
            } else {
                animateOffsetToStartPosition(-mCurrentScrollOffset, mResetListener);
            }
        }
    }

    private void initDragStatus(float activeMoveY) {
        float diff = activeMoveY - mInitialDownY;
        if (!mIsBeingDragged && diff > mTouchSlop) {
            mInitialMotionY = mInitialDownY + diff;
            mIsBeingDragged = true;
        }
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mAnimateToStartAnimation.reset();
        mAnimateToStartAnimation.setDuration(mAnimateDuration);
        mAnimateToStartAnimation.setInterpolator(mInterpolator);
        if (listener != null) {
            mAnimateToStartAnimation.setAnimationListener(listener);
        }
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mAnimateToStartAnimation);
    }

    private void animateToRefreshingPosition(int from, Animation.AnimationListener listener) {
        mFrom = from;

        mAnimateToRefreshingAnimation.reset();
        mAnimateToRefreshingAnimation.setDuration(mAnimateDuration);
        mAnimateToRefreshingAnimation.setInterpolator(mInterpolator);

        if (listener != null) {
            mAnimateToRefreshingAnimation.setAnimationListener(listener);
        }

        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mAnimateToRefreshingAnimation);
    }

    private void moveSpinner(float overScrollTop) {
        float originalDragPercent = overScrollTop / mRefreshTargetOffset;
        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float slingshotDist = mRefreshTargetOffset;
        float extraOS = Math.abs(overScrollTop) - mRefreshTargetOffset;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2.5f) / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) -
                Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = (int) ((slingshotDist * dragPercent) + extraMove);

        if (mRefreshView.getVisibility() != View.VISIBLE) {
            mRefreshView.setVisibility(View.VISIBLE);
        }

        if (overScrollTop > mRefreshTargetOffset && !mIsFitRefreshing) {
            mIsFitRefreshing = true;
            mIRefreshStatus.pullToRefresh();
        } else if (overScrollTop <= mRefreshTargetOffset && mIsFitRefreshing) {
            mIsFitRefreshing = false;
            mIRefreshStatus.releaseToRefresh();
        }

        scrollTargetOffset(0, -mCurrentScrollOffset - targetY);
    }

    private void finishSpinner(float overScrollTop) {
        if (overScrollTop > mRefreshTargetOffset) {
            setRefreshing(true, true);
        } else {
            mRefreshing = false;
            animateOffsetToStartPosition(-mCurrentScrollOffset, mResetListener);
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = MotionEventCompat.getActionIndex(ev);
        int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    private void scrollTargetOffset(int offsetX, int offsetY) {
        mRefreshView.bringToFront();
        scrollBy(offsetX, offsetY);
        mCurrentScrollOffset = getScrollY();

        mIRefreshStatus.pullProgress(-mCurrentScrollOffset, -mCurrentScrollOffset / mRefreshTargetOffset);
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    public boolean canChildScrollUp(View mTarget) {
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

        return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
    }

    private void ensureTarget() {
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mRefreshView) && !child.equals(mRefreshStateView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }
}
