## RecyclerRefreshLayout

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RecyclerRefreshLayout-green.svg?style=true)](https://android-arsenal.com/details/1/3383)

English | [中文版](https://github.com/dinuscxj/RecyclerRefreshLayout/blob/master/README-ZH.md)<br/>

[RecyclerRefreshLayout](https://github.com/dinuscxj/RecyclerRefreshLayout) 
based on the {@link android.support.v4.widget.SwipeRefreshLayout}
The `RecyclerRefreshLayout` should be used whenever the user can refresh the
contents of a `view` via a vertical swipe gesture. The activity that
instantiates this view should add an `OnRefreshListener` to be notified
whenever the swipe to refresh gesture is completed. The `RecyclerRefreshLayout`
will notify the listener each and every time the gesture is completed again;
the listener is responsible for correctly determining when to actually
initiate a refresh of its content. If the listener determines there should
not be a refresh, it must call `setRefreshing(false)` to cancel any visual
indication of a refresh. If an activity wishes to show just the progress
animation, it should call `setRefreshing(true)`. To disable the gesture and
progress animation, call `setEnabled(false)` on the `view`.

> Note: The `RecyclerRefreshLayout` supports all of the views: `ListView`, `GridView`, `ScrollView`, `FrameLayout`, or Even a single `TextView`
  
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutNormal.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutFloat.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutPinned.gif?width=300)<br/>
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutLoadMore.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutNoData.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutLoadError.gif?width=300)<br/>

## Installation

Add the following dependency to your build.gradle file:

```gradle
    dependencies {
        compile 'com.dinuscxj:recyclerrefreshlayout:2.0.5'
    }
```

## Usage

#### Config in xml
```xml
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
 
#### Configure the attributes(* must)
Set the listener to be notified when a refresh is triggered via the swipe gesture. 
```java
RecyclerRefreshLayout.setOnRefreshListener(OnRefreshListener);
```

Notify the widget that refresh state has changed. Do not call this 
when refresh is triggered by a swipe gesture.
```java
RecyclerRefreshLayout.setRefreshing(boolean);
``` 

#### Configure the attributes(optional)
Set the interpolator used by the animation that move the refresh view 
the release point to the refreshing point.
```java
RecyclerRefreshLayout.setAnimateToRefreshInterpolator(Interpolator);
```

Set the interpolator used by the animation that move the refresh view 
from the refreshing point or (the release point) to the start point.
```java
RecyclerRefreshLayout.setAnimateToStartInterpolator(Interpolator);
```

Set the duration used by the animation that move the refresh view 
the release point to the refreshing point.
```java
RecyclerRefreshLayout.setAnimateToRefreshDuration(int);
```

Set the duration used by the animation that move the refresh view 
from the refreshing point or (the release point) to the start point.
```java
RecyclerRefreshLayout.setAnimateToStartDuration(int);
```

Set the top position of the RefreshView relative to its parent.
```java
RecyclerRefreshLayoutsetRefreshInitialOffset(float)
```

Set The minimum distance that trigger refresh
```java
RecyclerRefreshLayout.setRefreshTargetOffset(float)
```

Set the style of the RefreshView
```java
RecyclerRefreshLayout.setRefreshStyle(@NonNull RefreshStyle) 
```

## Customize

Customize a refresh view (need to implements `IRefreshStatus`) for `RecyclerRefreshLayout`.
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
*                     pullProgress = pullDistance / refreshTargetOffset
*/
void pullProgress(float pullDistance, float pullProgress);
}
```
```java 
RecyclerRefreshLayout.setRefreshView(View, LayoutParams);
```
Eg. [RefreshView](https://github.com/dinuscxj/RecyclerRefreshLayout/blob/master/recyclerrefreshlayout/src/main/java/com/dinuscxj/refresh/RefreshView.java) or [RefreshViewEg](https://github.com/dinuscxj/RecyclerRefreshLayout/tree/master/app/src/main/java/com/dinuscxj/example/demo/RefreshViewEg.java) 

Customize a drag distance converter (need to implements `IDragDistanceConverter`) for `RecyclerRefreshLayout`.
```java
public interface IDragDistanceConverter {
 /**
  * @param scrollDistance the distance between the ACTION_DOWN point and the ACTION_MOVE point
  * @param refreshDistance the distance between the refresh point and the start point
  * @return the real distance of the refresh view moved
  */
 float convert(float scrollDistance, float refreshDistance);
}
```
```java
RecyclerRefreshLayout.setDragDistanceConverter(@NonNull IDragDistanceConverter) 
```
Eg. [MaterialDragDistanceConverter](https://github.com/dinuscxj/RecyclerRefreshLayout/blob/master/recyclerrefreshlayout/src/main/java/com/dinuscxj/refresh/MaterialDragDistanceConverter.java) or [DragDistanceConverterEg](https://github.com/dinuscxj/RecyclerRefreshLayout/tree/master/app/src/main/java/com/dinuscxj/example/demo/DragDistanceConverterEg.java) 

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
