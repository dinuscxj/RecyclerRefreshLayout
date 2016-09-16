package com.dinuscxj.example.demo;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dinuscxj.example.R;

import java.util.ArrayList;

public abstract class TabPagerFragment extends Fragment {
    private ViewPager mMainViewPager;
    private TabLayout mTabLayout;

    private final static class TabInfo implements Parcelable {
        private final String title;
        private final Bundle arguments;
        private final Class<?> fragmentClass;

        public TabInfo(@NonNull String title, @NonNull Class<? extends Fragment> clazz, @Nullable Bundle arguments) {
            this.title = title;
            this.arguments = arguments;
            this.fragmentClass = clazz;
        }

        private TabInfo(Parcel parcel) throws ParcelFormatException {
            try {
                this.title = parcel.readString();
                this.arguments = parcel.readBundle();
                this.fragmentClass = getClass().getClassLoader().loadClass(parcel.readString());
            } catch (Exception e) {
                throw new ParcelFormatException();
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(title);
            parcel.writeString(fragmentClass.getCanonicalName());
            parcel.writeBundle(arguments);
        }

        public static final Creator<TabInfo> CREATOR = new Creator<TabInfo>() {
            @Override
            public TabInfo createFromParcel(Parcel parcel) {
                return new TabInfo(parcel);
            }

            @Override
            public TabInfo[] newArray(int i) {
                return new TabInfo[i];
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.base_tabpager_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        mMainViewPager = (ViewPager) view.findViewById(R.id.view_pager);

        setupToolbar(view);
        setupViewPager();
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }
    }

    private void setupViewPager() {
        mMainViewPager.setAdapter(buildPagerAdapter());
        mMainViewPager.setOffscreenPageLimit(mMainViewPager.getAdapter().getCount());
        mTabLayout.setupWithViewPager(mMainViewPager);
    }

    private FragmentStatePagerAdapter buildPagerAdapter() {
        BuilderImplement builder = new BuilderImplement();
        onBuildTabPager(builder);
        return builder.build();
    }

    private class TabPagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<TabInfo> tabInfos = new ArrayList<>();

        public TabPagerAdapter(ArrayList<TabInfo> tabInfos) {
            super(getChildFragmentManager());
            this.tabInfos = tabInfos;
        }

        @Override
        public Fragment getItem(int position) {
            try {
                Class<?> clazz = tabInfos.get(position).fragmentClass;
                Fragment fragment = (Fragment) clazz.newInstance();

                if (tabInfos.get(position).arguments != null) {
                    fragment.setArguments(tabInfos.get(position).arguments);
                }

                return fragment;
            } catch (Exception e) {
                throw new RuntimeException("Cannot construct fragment", e);
            }
        }

        @Override
        public int getCount() {
            return tabInfos.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabInfos.get(position).title;
        }
    }

    public interface Builder {
        void addTab(@NonNull String title, @NonNull Class<? extends Fragment> clazz, @Nullable Bundle arguments);
    }

    private final class BuilderImplement implements Builder {
        private ArrayList<TabInfo> tabs = new ArrayList<>();

        protected TabPagerAdapter build() {
            if (tabs == null)
                throw new IllegalStateException("This builder should not be reused");
            TabPagerAdapter adapter = new TabPagerAdapter(tabs);
            tabs = null;
            return adapter;
        }

        @Override
        public void addTab(@NonNull String title, @NonNull Class<? extends Fragment> clazz, @Nullable Bundle arguments) {
            tabs.add(new TabInfo(title, clazz, arguments));
        }
    }

    public abstract void onBuildTabPager(@NonNull Builder builder);

}
