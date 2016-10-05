package com.dinuscxj.example.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dinuscxj.example.R;
import com.dinuscxj.example.adapter.RecyclerListAdapter;
import com.dinuscxj.example.model.OpenProjectFactory;
import com.dinuscxj.example.model.OpenProjectModel;
import com.dinuscxj.example.utils.DensityUtil;
import com.dinuscxj.refresh.RecyclerRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenProjectFloatFragment extends RecyclerFragment<OpenProjectModel> {
    private static final int SIMULATE_UNSPECIFIED = 0;
    private static final int SIMULATE_FRESH_FIRST = 1;
    private static final int SIMULATE_FRESH_NO_DATA = 2;
    private static final int SIMULATE_FRESH_FAILURE = 3;

    private static final int REQUEST_DURATION = 800;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<OpenProjectModel> mItemList = new ArrayList<>();

    private int mSimulateStatus;

    public static OpenProjectFloatFragment newInstance() {
        return new OpenProjectFloatFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSimulateStatus = SIMULATE_UNSPECIFIED;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_simple_item, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_first:
                mSimulateStatus = SIMULATE_FRESH_FIRST;
                break;
            case R.id.refresh_no_data:
                mSimulateStatus = SIMULATE_FRESH_NO_DATA;
                break;
            case R.id.refresh_failure:
                mSimulateStatus = SIMULATE_FRESH_FAILURE;
                break;
        }

        mItemList.clear();
        getHeaderAdapter().notifyDataSetChanged();
        getHeaderAdapter().removeAllFooterView();

        refresh();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getOriginAdapter().setItemList(mItemList);
        getHeaderAdapter().notifyDataSetChanged();

        getRecyclerRefreshLayout().setRefreshStyle(RecyclerRefreshLayout.RefreshStyle.FLOAT);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                (int)DensityUtil.dip2px(getActivity(), 40), (int)DensityUtil.dip2px(getActivity(), 40));
        getRecyclerRefreshLayout().setRefreshView(new MaterialRefreshView(getActivity()), layoutParams);
    }

    @Override
    protected RecyclerView.LayoutManager onCreateLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @NonNull
    @Override
    public RecyclerListAdapter createAdapter() {
        return new RecyclerListAdapter() {
            {
                addViewType(OpenProjectModel.class, new ViewHolderFactory<ViewHolder>() {
                    @Override
                    public ViewHolder onCreateViewHolder(ViewGroup parent) {
                        return new ItemViewHolder(parent);
                    }
                });
            }
        };
    }

    @Override
    protected InteractionListener createInteraction() {
        return new ItemInteractionListener();
    }

    private void simulateNetworkRequest(final RequestListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(REQUEST_DURATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isAdded()) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mSimulateStatus == SIMULATE_FRESH_FAILURE) {
                                listener.onFailed();
                            } else if (mSimulateStatus == SIMULATE_FRESH_NO_DATA) {
                                listener.onSuccess(Collections.EMPTY_LIST);
                            } else {
                                listener.onSuccess(OpenProjectFactory.createOpenProjects());
                            }

                            mSimulateStatus = SIMULATE_UNSPECIFIED;
                        }
                    });
                }
            }
        }).start();
    }

    private interface RequestListener {
        void onSuccess(List<OpenProjectModel> openProjectModels);

        void onFailed();
    }

    private class ItemInteractionListener extends InteractionListener {
        @Override
        public void requestRefresh() {
            simulateNetworkRequest(new RequestListener() {
                @Override
                public void onSuccess(List<OpenProjectModel> openProjectModels) {
                    mItemList.clear();
                    mItemList.addAll(openProjectModels);
                    getHeaderAdapter().notifyDataSetChanged();
                    OpenProjectFloatFragment.ItemInteractionListener.super.requestRefresh();
                }

                @Override
                public void onFailed() {
                    OpenProjectFloatFragment.ItemInteractionListener.super.requestFailure();
                }
            });
        }

        @Override
        public void requestMore() {
            simulateNetworkRequest(new RequestListener() {
                @Override
                public void onSuccess(List<OpenProjectModel> openProjectModels) {
                    mItemList.addAll(openProjectModels);
                    getHeaderAdapter().notifyDataSetChanged();
                    OpenProjectFloatFragment.ItemInteractionListener.super.requestMore();
                }

                @Override
                public void onFailed() {
                    OpenProjectFloatFragment.ItemInteractionListener.super.requestFailure();
                }
            });
        }
    }

    private class ItemViewHolder extends RecyclerListAdapter.ViewHolder<OpenProjectModel> {
        private final TextView mTvTitle;
        private final TextView mTvContent;
        private final TextView mTvAuthor;

        private final LinearLayout mLlContentPanel;

        public ItemViewHolder(@NonNull ViewGroup parent) {
            super(LayoutInflater.from(getActivity()).inflate(R.layout.simple_list_item, parent, false));

            mTvTitle = (TextView) itemView.findViewById(R.id.title);
            mTvContent = (TextView) itemView.findViewById(R.id.content);
            mTvAuthor = (TextView) itemView.findViewById(R.id.author);

            mLlContentPanel = (LinearLayout) itemView.findViewById(R.id.content_panel);
        }

        @Override
        public void bind(OpenProjectModel item, int position) {
            mTvTitle.setText(item.getTitle());
            mTvContent.setText(item.getContent());
            mTvAuthor.setText(item.getAuthor());

            mLlContentPanel.setBackgroundColor(Color.parseColor(item.getColor()));
        }
    }
}
