package com.dinuscxj.example.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public abstract class BaseRecyclerListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
  protected List<T> mItemList = new ArrayList<>();

  public BaseRecyclerListAdapter() {
  }

  public BaseRecyclerListAdapter(@NonNull T[] arrays) {
    if (arrays == null) {
      throw new IllegalArgumentException("don't pass null in");
    }
    mItemList.addAll(Arrays.asList(arrays));
  }

  public BaseRecyclerListAdapter(@NonNull Collection<T> collection) {
    if (collection == null) {
      throw new IllegalArgumentException("don't pass null in");
    }
    mItemList.addAll(collection);
  }

  public BaseRecyclerListAdapter(@NonNull List<T> list) {
    if (list == null) {
      throw new IllegalArgumentException("don't pass null in");
    }
    mItemList = list;
  }

  @Override
  public int getItemCount() {
    return mItemList.size();
  }

  public List<T> getItemList() {
    return mItemList;
  }

  @UiThread
  public void setItemList(List<T> itemList) {
    mItemList.clear();
    mItemList = itemList;

    notifyDataSetChanged();
  }

  public
  T getItem(int position) {
    return mItemList.get(position);
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> add(@NonNull T item) {
    mItemList.add(item);
    notifyItemInserted(mItemList.size() - 1);
    return this;
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> addAll(@NonNull T[] items) {
    return addAll(Arrays.asList(items));
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> addAll(@NonNull Collection<T> items) {
    mItemList.addAll(items);
    notifyItemRangeInserted(mItemList.size() - items.size(), items.size());
    return this;
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> add(int position, @NonNull T item) {
    mItemList.add(position, item);
    notifyItemInserted(position);
    return this;
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> remove(int position) {
    mItemList.remove(position);
    notifyItemRemoved(position);
    return this;
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> remove(@NonNull T item) {
    return remove(mItemList.indexOf(item));
  }

  @UiThread
  public BaseRecyclerListAdapter<T, VH> clear() {
    mItemList.clear();
    notifyDataSetChanged();
    return this;
  }

  public boolean isEmpty() {
    return mItemList == null || mItemList.isEmpty();
  }

}