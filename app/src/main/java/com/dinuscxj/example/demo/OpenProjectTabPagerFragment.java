package com.dinuscxj.example.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.dinuscxj.example.R;

public class OpenProjectTabPagerFragment extends TabPagerFragment {

    public static OpenProjectTabPagerFragment newInstance() {
        return new OpenProjectTabPagerFragment();
    }

    @Override
    public void onBuildTabPager(@NonNull Builder builder) {
        FragmentEntry.buildTabPager(builder, getActivity());
    }

    private enum FragmentEntry {
        NORMAL(
                R.string.tab_normal,
                OpenProjectNormalFragment.class),
        FLOAT(
                R.string.tab_float,
                OpenProjectFloatFragment.class),
        PINNED(
                R.string.tab_pinned,
                OpenProjectPinnedFragment.class);

        final int titleResource;
        final Class<? extends Fragment> fragmentClass;

        FragmentEntry(int indicatorResource, Class<? extends Fragment> fragmentClass) {
            this.titleResource = indicatorResource;
            this.fragmentClass = fragmentClass;
        }

        static void buildTabPager(Builder builder, Context context) {
            for (FragmentEntry e : FragmentEntry.values()) {
                builder.addTab(context.getString(e.titleResource), e.fragmentClass, null);
            }
        }
    }


}
