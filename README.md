
## RecyclerRefreshLayout
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RecyclerRefreshLayout-green.svg?style=true)](https://android-arsenal.com/details/1/3383)

 [RecyclerRefreshLayout](https://github.com/dinuscxj/RecyclerRefreshLayout) based on the {@link android.support.v4.widget.SwipeRefreshLayout}
 The `RecyclerRefreshLayout` should be used whenever the user can refresh the
 contents of a `view` via a vertical swipe gesture. The `activity` that
 instantiates this view should add an `OnRefreshListener` to be notified
 whenever the swipe to refresh gesture is completed. The `RecyclerRefreshLayout`
 will notify the listener each and every time the gesture is completed again;
 the listener is responsible for correctly determining when to actually
 initiate a refresh of its content. If the listener determines there should
 not be a refresh, it must call `setRefreshing(false)` to cancel any visual
 indication of a refresh. If an `activity` wishes to show just the progress
 animation, it should call `setRefreshing(true)`. To disable the gesture and
 progress animation, call `setEnabled(false)` on the `view`.
 
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutNormal.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutNoData.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutFailure.gif?width=300)<br/>

## Features
 * Supports all of the views: `ListView`, `GridView`, `ScrollView`, `FrameLayout`, or Even a single `TextView`.
 * Custom refresh view: You can customize a `View` and implements `IRefreshStatus`. And then call the method `setRefreshView(View, LayoutParams)`. 
 * Custom refresh tips: [tips] (https://github.com/dinuscxj/RecyclerRefreshLayout/tree/master/app/src/main/java/com/dinuscxj/example/tips).

## Usage
 Add dependency
 ```gradle
 dependencies {
    compile 'com.dinuscxj:recyclerrefreshlayout:1.0.1'
 }
 ```

 Config in xml
 ``` xml
 <?xml version="1.0" encoding="utf-8"?>
 <com.dinuscxj.refresh.RecyclerRefreshLayout xmlns:android="http://schemas.android.com/apk/res/android"
     android:id="@+id/refresh_layout"
     android:layout_width="match_parent"
     android:layout_height="match_parent">
     <android.support.v7.widget.RecyclerView
         android:id="@+id/recycler_view"
         android:layout_width="match_parent"
         android:layout_height="match_parent" />
 </app.dinus.com.refresh.RecyclerRefreshLayout>
 ```

 Set the `OnRefreshListener` in java
 ```java
 mRecyclerRefreshLayout.setOnRefreshListener(OnRefreshListener);
 ```
 
## Customize
 You can add a refresh view (need to implements `IRefreshStatus`) to `RecyclerRefreshLayout` to implement any UI effect you want.
 ```java
 public interface IRefreshStatus {
   /**
    * When the content view has reached top and refresh has been completed, view will be reset.
    */
   void reset();
   /**
    * Refresh View is refreshing
    */
   void refreshing();
   /**
    * Refresh View is dropped down to the refresh point
    */
   void pullToRefresh();
   /**
    * Refresh View is released into the refresh point
    */
   void releaseToRefresh();
   /**
    * @param pullDistance The drop-down distance of the refresh View
    * @param pullProgress The drop-down progress of the refresh View and the pullProgress may be more than 1.0f
    */
   void pullProgress(float pullDistance, float pullProgress);
 }
 ```
 
 Set the refresh view
 ```java 
 mRecyclerRefreshLayout.setRefreshView(View, LayoutParams);
 ```
 If necessary, Maybe you need to reference [RefreshViewEg](https://github.com/dinuscxj/RecyclerRefreshLayout/tree/master/app/src/main/java/com/dinuscxj/example/demo/RefreshViewEg.java) 
 
## Misc
  ***QQ Group:*** **342748245**
  
## License
    Copyright 2015-2019 dinus

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
