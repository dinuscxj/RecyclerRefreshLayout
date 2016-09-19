package com.dinuscxj.example.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dinuscxj.example.R;
import com.dinuscxj.example.adapter.HeaderViewRecyclerAdapter;
import com.dinuscxj.example.adapter.RecyclerListAdapter;
import com.dinuscxj.example.model.CursorModel;
import com.dinuscxj.example.tips.DefaultTipsHelper;
import com.dinuscxj.example.tips.TipsHelper;
import com.dinuscxj.refresh.RecyclerRefreshLayout;

public abstract class RecyclerFragment<MODEL extends CursorModel> extends Fragment {
    private boolean mIsLoading;

    private RecyclerView mRecyclerView;
    private RecyclerRefreshLayout mRecyclerRefreshLayout;

    private TipsHelper mTipsHelper;
    private HeaderViewRecyclerAdapter mHeaderAdapter;
    private RecyclerListAdapter<MODEL, ?> mOriginAdapter;

    private InteractionListener mInteractionListener;

    private final RefreshEventDetector mRefreshEventDetector = new RefreshEventDetector();
    private final AutoLoadEventDetector mAutoLoadEventDetector = new AutoLoadEventDetector();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.base_refresh_recycler_list_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView(view);
        initRecyclerRefreshLayout(view);

        mInteractionListener = createInteraction();
        mTipsHelper = createTipsHelper();

        refresh();
    }

    private void initRecyclerView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        mRecyclerView.addOnScrollListener(mAutoLoadEventDetector);

        RecyclerView.LayoutManager layoutManager = onCreateLayoutManager();
        if (layoutManager != null) {
            mRecyclerView.setLayoutManager(layoutManager);
        }

        mOriginAdapter = createAdapter();
        mHeaderAdapter = new HeaderViewRecyclerAdapter(mOriginAdapter);
        mRecyclerView.setAdapter(mHeaderAdapter);
        mHeaderAdapter.adjustSpanSize(mRecyclerView);
    }

    private void initRecyclerRefreshLayout(View view) {
        mRecyclerRefreshLayout = (RecyclerRefreshLayout) view.findViewById(R.id.refresh_layout);

        if (mRecyclerRefreshLayout == null) {
            return;
        }

        if (allowPullToRefresh()) {
            mRecyclerRefreshLayout.setNestedScrollingEnabled(true);
            mRecyclerRefreshLayout.setOnRefreshListener(mRefreshEventDetector);
        } else {
            mRecyclerRefreshLayout.setEnabled(false);
        }
    }

    @NonNull
    public abstract RecyclerListAdapter createAdapter();

    protected RecyclerView.LayoutManager onCreateLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    protected TipsHelper createTipsHelper() {
        return new DefaultTipsHelper(this);
    }

    protected InteractionListener createInteraction() {
        return null;
    }

    @Override
    public void onDestroyView() {
        mRecyclerView.removeOnScrollListener(mAutoLoadEventDetector);
        super.onDestroyView();
    }

    public HeaderViewRecyclerAdapter getHeaderAdapter() {
        return mHeaderAdapter;
    }

    public RecyclerListAdapter<MODEL, ?> getOriginAdapter() {
        return mOriginAdapter;
    }

    public RecyclerRefreshLayout getRecyclerRefreshLayout() {
        return mRecyclerRefreshLayout;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public boolean allowPullToRefresh() {
        return true;
    }

    public void refresh() {
        if (isFirstPage()) {
            mTipsHelper.showLoading(true);
        } else {
            mRecyclerRefreshLayout.setRefreshing(true);
        }

        requestRefresh();
    }

    public boolean isFirstPage() {
        return mOriginAdapter.getItemCount() <= 0;
    }

    public class RefreshEventDetector implements RecyclerRefreshLayout.OnRefreshListener {

        @Override
        public void onRefresh() {
            requestRefresh();
        }
    }

    public class AutoLoadEventDetector extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView view, int dx, int dy) {
            RecyclerView.LayoutManager manager = view.getLayoutManager();
            if (manager.getChildCount() > 0) {
                int count = manager.getItemCount();
                int last = ((RecyclerView.LayoutParams) manager
                        .getChildAt(manager.getChildCount() - 1).getLayoutParams()).getViewAdapterPosition();

                if (last == count - 1 && !mIsLoading && mInteractionListener != null) {
                    requestMore();
                }
            }
        }
    }

    private void requestRefresh() {
        if (mInteractionListener != null && !mIsLoading) {
            mIsLoading = true;
            mInteractionListener.requestRefresh();
        }
    }

    private void requestMore() {
        if (mInteractionListener != null && mInteractionListener.hasMore() && !mIsLoading) {
            mIsLoading = true;
            mInteractionListener.requestMore();
        }
    }

    public abstract class InteractionListener {
        public void requestRefresh() {
            requestComplete();

            if (mOriginAdapter.isEmpty()) {
                mTipsHelper.showEmpty();
            } else if (hasMore()) {
                mTipsHelper.showHasMore();
            } else {
                mTipsHelper.hideHasMore();
            }
        }

        public void requestMore() {
            requestComplete();
        }

        public void requestFailure() {
            requestComplete();
            mTipsHelper.showError(isFirstPage(), new Exception("net error"));
        }

        protected void requestComplete() {
            mIsLoading = false;

            if (mRecyclerRefreshLayout != null) {
                mRecyclerRefreshLayout.setRefreshing(false);
            }

            mTipsHelper.hideError();
            mTipsHelper.hideEmpty();
            mTipsHelper.hideLoading();
        }

        protected boolean hasMore() {
            return mOriginAdapter.getItem(mOriginAdapter.getItemCount() - 1).hasMore();
        }
    }
}
