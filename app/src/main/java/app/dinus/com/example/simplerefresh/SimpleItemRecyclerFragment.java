package app.dinus.com.example.simplerefresh;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import app.dinus.com.example.R;
import app.dinus.com.example.adapter.RecyclerListAdapter;
import app.dinus.com.example.model.ItemModel;

public class SimpleItemRecyclerFragment extends SimpleRecyclerFragment<ItemModel> {
    private static final int MAX_ITEM_COUNT     = 50;
    private static final int REQUEST_DURATION   = 2000;
    private static final int RESPONSE_THRESHOLD = 20;

    private final Handler mHandler = new Handler();
    private final List<ItemModel> mItemList = new ArrayList<>();

    public static SimpleItemRecyclerFragment newInstance() {
        return new SimpleItemRecyclerFragment();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getOriginAdapter().setItemList(mItemList);
        getHeaderAdapter().notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerListAdapter onCreateAdapter() {
        return new RecyclerListAdapter() {
            {
                addViewType(ItemModel.class, new ViewHolderFactory<ViewHolder>() {
                    @Override
                    public ViewHolder onCreateViewHolder(ViewGroup parent) {
                        return new ItemViewHolder(parent);
                    }
                });
            }
        };
    }

    @Override
    protected InteractionListener onCreateInteraction() {
        return new InteractionListener() {
            @Override
            public void requestRefresh() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mItemList.clear();
                        mItemList.addAll(responseItemList());
                        getHeaderAdapter().notifyDataSetChanged();

                        requestComplete();
                    }
                }, REQUEST_DURATION);
            }

            @Override
            public void requestMore() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mItemList.addAll(responseItemList());
                        if (mItemList.size() > MAX_ITEM_COUNT) {
                            mItemList.add(new ItemModel(getString(R.string.no_more),
                                    getString(R.string.no_more), null));
                        }
                        getHeaderAdapter().notifyDataSetChanged();

                        requestComplete();
                    }
                }, REQUEST_DURATION);
            }
        };
    }

    private List<ItemModel> responseItemList() {
        List<ItemModel> itemList = new ArrayList<>();

        String title = getString(R.string.title);
        String content = getString(R.string.content);

        for (int i = 0; i < RESPONSE_THRESHOLD; i++) {
            ItemModel itemModel = new ItemModel(title + i, content);
            itemList.add(itemModel);
        }

        return itemList;
    }

    private class ItemViewHolder extends RecyclerListAdapter.ViewHolder<ItemModel> {
        private TextView mTvTitle;
        private TextView mTvContent;

        public ItemViewHolder(@NonNull ViewGroup parent) {
            super(LayoutInflater.from(getActivity()).inflate(R.layout.simple_list_item, parent, false));

            mTvTitle = (TextView) itemView.findViewById(R.id.title);
            mTvContent = (TextView) itemView.findViewById(R.id.content);
        }

        @Override
        public void bind(ItemModel item, int position) {
            mTvTitle.setText(item.getTitle());
            mTvContent.setText(item.getContent());
        }
    }
}
