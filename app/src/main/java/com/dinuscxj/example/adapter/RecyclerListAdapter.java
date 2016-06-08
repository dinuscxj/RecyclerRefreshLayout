package com.dinuscxj.example.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

public abstract class RecyclerListAdapter<T, VH extends RecyclerListAdapter.ViewHolder<T>> extends BaseRecyclerListAdapter<T, VH> {
  public static final int UNKNOWN_VIEW_TYPE = Integer.MIN_VALUE;

  private final HashMap<Class<?>, Integer> mViewHolderTypeRegistry = new HashMap<>();
  private final HashMap<Integer, ViewHolderFactory> mViewHolderFactoryRegistry = new HashMap<>();

  public <F> void addViewType(Class<? extends F> clazz, ViewHolderFactory<? extends ViewHolder<? extends F>> factory) {
    int id = mViewHolderFactoryRegistry.size();
    mViewHolderTypeRegistry.put(clazz, id);
    mViewHolderFactoryRegistry.put(id, factory);
  }

  public <F> void addViewType(int viewType, ViewHolderFactory<? extends ViewHolder<? extends F>> factory) {
    mViewHolderFactoryRegistry.put(viewType, factory);
  }

  @Override
  @SuppressWarnings("unchecked")
  public VH onCreateViewHolder(ViewGroup parent, int viewType) {
    if (mViewHolderFactoryRegistry.size() > 0) {
      return (VH) mViewHolderFactoryRegistry.get(viewType).onCreateViewHolder(parent);
    } else {
      return onCreateViewHolder(parent);
    }
  }

  public VH onCreateViewHolder(ViewGroup parent) {
    throw new RuntimeException("onCreateViewHolder(ViewGroup, int) is not implemented.");
  }

  @Override
  public int getItemViewType(int position) {
    if (mViewHolderTypeRegistry.size() > 0) {
      int viewType = getItemViewType(getItem(position).getClass());
      if (viewType != UNKNOWN_VIEW_TYPE) {
        return viewType;
      } else {
        return getItemViewType(getItem(position));
      }
    } else {
      return getItemViewType(getItem(position));
    }
  }

  public int getItemViewType(Class<?> clazz) {
    if (mViewHolderTypeRegistry.size() > 0) {
      while (clazz != Object.class) {
        if (mViewHolderTypeRegistry.containsKey(clazz)) {
          return mViewHolderTypeRegistry.get(clazz);
        }
        clazz = clazz.getSuperclass();
      }
      return UNKNOWN_VIEW_TYPE;
    } else {
      return UNKNOWN_VIEW_TYPE;
    }
  }

  public int getItemViewType(T item) {
    throw new RuntimeException("Cannot parse view type for (" + item.getClass() + ")");
  }

  @Override
  public void onBindViewHolder(VH holder, int position) {
    holder.bind(getItem(position), position);
  }

  public static abstract class ViewHolder<T> extends RecyclerView.ViewHolder {
    public ViewHolder(@NonNull View parent) {
      super(parent);
    }

    public abstract void bind(T item, int position);

  }

  public interface ViewHolderFactory<VH extends ViewHolder> {
    VH onCreateViewHolder(ViewGroup parent);
  }
}
