package com.dinuscxj.example.tips;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


public class Tips {

  public final View mView;
  public final boolean mHideTarget;
  public final FrameLayout.LayoutParams mLayoutParams;

  View mTargetView;

  public Tips(Context context, int layoutRes) {
    this(context, layoutRes, true);
  }

  public Tips(View view) {
    this(view, true);
  }

  public Tips(Context context, int layoutRes, boolean hideTarget) {
    this(LayoutInflater.from(context).inflate(layoutRes, new FrameLayout(context), false), hideTarget);
  }

  public Tips(View view, boolean hideTarget) {
    this.mHideTarget = hideTarget;
    this.mView = view;
    // add tag to this view.
    this.mView.setTag(this);
    ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof FrameLayout.LayoutParams) {
      this.mLayoutParams = (FrameLayout.LayoutParams) lp;
    } else {
      this.mLayoutParams =
          new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT);
    }
  }

  /**
   * Apply the tips view to the target view.
   *
   * @param target target view to show at.
   * @param tipsId tips view id.
   */
  public View applyTo(View target, int tipsId) {
    mTargetView = target;
    ViewGroup parent = (ViewGroup) target.getParent();
    if (parent == null) {
      return null;
    }
    View tipsView;
    if (parent instanceof TipsContainer) {
      tipsView = addTipsViewToContainer(target, parent, tipsId);
    } else {
      TipsContainer tipsContainerView = new TipsContainer(target.getContext());
      ViewGroup.LayoutParams targetParams = target.getLayoutParams();
      int index = parent.indexOfChild(target);
      parent.removeViewAt(index);
      parent.addView(tipsContainerView, index, targetParams);
      Drawable background = target.getBackground();
      if (background != null) {
        tipsContainerView.setBackgroundDrawable(background);
      }
      tipsView = addTipsViewToContainer(target, tipsContainerView, tipsId);
    }
    return tipsView;
  }

  private View addTipsViewToContainer(View target, ViewGroup tipContainer, int tipsId) {
    View tipsView = TipsUtils.findChildViewById(tipContainer, tipsId);
    if (tipsView != null) {
      tipsView.bringToFront();
      return tipsView;
    } else {
      mView.setId(tipsId);
      FrameLayout.LayoutParams targetViewLayoutParams = new FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      if (mHideTarget) {
        target.setVisibility(View.INVISIBLE);
      }
      if (tipContainer.indexOfChild(target) == -1) {
        tipContainer.addView(target, targetViewLayoutParams);
      } else {
        target.setLayoutParams(targetViewLayoutParams);
      }
      tipContainer.addView(mView, mLayoutParams);
      return mView;
    }
  }
}
