package app.dinus.com.example.simplerefresh;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import app.dinus.com.example.R;
import app.dinus.com.example.adapter.HeaderViewRecyclerAdapter;
import app.dinus.com.example.adapter.RecyclerListAdapter;
import app.dinus.com.example.model.CursorModel;
import app.dinus.com.refresh.RecyclerRefreshLayout;

public abstract class SimpleRecyclerFragment<MODEL extends CursorModel> extends Fragment {
  private boolean mIsLoading;

  private View mLoadingMoreView;
  private RecyclerView mRecyclerView;
  private RecyclerRefreshLayout mRecyclerRefreshLayout;

  private HeaderViewRecyclerAdapter mHeaderAdapter;
  private RecyclerListAdapter<MODEL, ?> mOriginAdapter;

  private InteractionListener mInteractionListener;

  private RefreshEventDetector mRefreshEventDetector = new RefreshEventDetector();
  private AutoLoadEventDetector mAutoLoadEventDetector = new AutoLoadEventDetector();

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

    refresh();
  }

  private void initRecyclerView(View view) {
    mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

    mRecyclerView.addOnScrollListener(mAutoLoadEventDetector);

    RecyclerView.LayoutManager layoutManager = onCreateLayoutManager();
    if (layoutManager != null) {
      mRecyclerView.setLayoutManager(layoutManager);
    }

    mInteractionListener = onCreateInteraction();

    mOriginAdapter = onCreateAdapter();
    mHeaderAdapter = new HeaderViewRecyclerAdapter(mOriginAdapter);

    mLoadingMoreView = onCreateLoadMoreView();

    mRecyclerView.setAdapter(mHeaderAdapter);
  }

  private void initRecyclerRefreshLayout(View view) {
    mRecyclerRefreshLayout = (RecyclerRefreshLayout) view.findViewById(R.id.refresh_layout);

    if (mRecyclerRefreshLayout == null) {
      return ;
    }

    if (allowPullToRefresh()) {
      mRecyclerRefreshLayout.setNestedScrollingEnabled(true);
      mRecyclerRefreshLayout.setOnRefreshListener(mRefreshEventDetector);
    } else {
      mRecyclerRefreshLayout.setEnabled(false);
    }
  }

  @NonNull
  public abstract RecyclerListAdapter onCreateAdapter();

  protected RecyclerView.LayoutManager onCreateLayoutManager() {
    return new LinearLayoutManager(getActivity());
  }

  protected InteractionListener onCreateInteraction() {
    return null;
  }

  protected View onCreateLoadMoreView() {
    return LayoutInflater.from(getActivity())
            .inflate(R.layout.footer_loading_layout, new FrameLayout(getActivity()), false);
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

  protected RecyclerRefreshLayout findRecyclerRefreshLayout() {
    return mRecyclerRefreshLayout ;
  }

  protected RecyclerView findRecyclerView() {
    return mRecyclerView;
  }

  protected boolean allowPullToRefresh() {
    return true;
  }

  public void refresh() {
    if (mRecyclerRefreshLayout.getHeight() == 0) {
      mRecyclerRefreshLayout.postDelayed(new Runnable() {
        @Override
        public void run() {
          requestRefresh();
          mRecyclerRefreshLayout.setRefreshing(true);
        }
      }, 100);
    } else {
      requestRefresh();
    }
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
    }

    public void requestMore() {
      requestComplete();
    }

    protected void requestComplete() {
      mIsLoading = false;

      if (mRecyclerRefreshLayout != null) {
        mRecyclerRefreshLayout.setRefreshing(false);
      }

      if (hasMore() && !mHeaderAdapter.containsFooterView(mLoadingMoreView)) {
        mHeaderAdapter.addFooterView(mLoadingMoreView);
      } else if (!hasMore() && mHeaderAdapter.containsFooterView(mLoadingMoreView)){
        mHeaderAdapter.removeFooter(mLoadingMoreView);
      }
    }

    protected boolean hasMore() {
      return mOriginAdapter.getItem(mOriginAdapter.getItemCount() - 1).hasMore();
    }
  }
}
