## RecyclerRefreshLayout

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RecyclerRefreshLayout-green.svg?style=true)](https://android-arsenal.com/details/1/3383)

[English](https://github.com/dinuscxj/RecyclerRefreshLayout) | 中文版<br/>

[RecyclerRefreshLayout](https://github.com/dinuscxj/RecyclerRefreshLayout) 
基于 {@link android.support.v4.widget.SwipeRefreshLayout}
`RecyclerRefreshLayout` 可以被用于用户通过一个垂直滑动的手势刷新当前 `view` 
内容的场景. 实例化这个视图的操作中总应该添加一个OnRefreshListener监听刷新动作完成后通知。
`RecyclerRefreshLayout` 每次手势操作完成后都会通知监听器（OnRefreshListener），
监听器负责决定何时来发起一个刷新操作。 如果监听器决定不触发刷新操作则需要调用
`setRefreshing(false)` 取消刷新的任何可视指示. 如果用户想要强制执行一次刷新动画， 
则需要调用 `setRefreshing(true)`. 如果想要禁用刷新动画，则可以调用 `setEnabled(false)`。

> 注意: `RecyclerRefreshLayout` 支持所有的View: `ListView`, `GridView`, `ScrollView`, `FrameLayout`, 甚至一个 `TextView`
  
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutNormal.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutFloat.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutPinned.gif?width=300)<br/>
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutLoadMore.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutNoData.gif?width=300)
![](https://raw.githubusercontent.com/dinuscxj/RecyclerRefreshLayout/master/Preview/RecyclerRefreshLayoutLoadError.gif?width=300)<br/>

## 注入

在你的build.gradle文件里添加下面依赖:

```gradle
    dependencies {
        compile 'com.dinuscxj:recyclerrefreshlayout:2.0.5'
    }
```

## 用法

#### 在xml中配置
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
 
#### 配置下面的属性(* 必选)
设置监听器监听手势操作是否触发刷新操作
```java
RecyclerRefreshLayout.setOnRefreshListener(OnRefreshListener);
```

通知刷新状态已经改变. 当刷新由滑动手势触发不要调用它。
```java
RecyclerRefreshLayout.setRefreshing(boolean);
``` 

#### 配置下面的属性(可选)
设置将刷新动画从手势释放的位置移动到刷新位置的动画所需要的Interpolator
```java
RecyclerRefreshLayout.setAnimateToRefreshInterpolator(Interpolator);
```

设置将刷新动画从手势释放的位置（或刷新位置）移动到起始位置的动画所需要的Interpolator
```java
RecyclerRefreshLayout.setAnimateToStartInterpolator(Interpolator);
```

设置将刷新动画从手势释放的位置移动到刷新位置的动画所需要的时间
```java
RecyclerRefreshLayout.setAnimateToRefreshDuration(int);
```

设置将刷新动画从手势释放的位置（或刷新位置）移动到起始位置的动画所需要的时间
```java
RecyclerRefreshLayout.setAnimateToStartDuration(int);
```

设置RefreshView相对父组件的初始位置
```java
RecyclerRefreshLayoutsetRefreshInitialOffset(float)
```

设置触发刷新需要滑动的最小距离
```java
RecyclerRefreshLayout.setRefreshTargetOffset(float)
```

设置RefreshView的样式
```java
RecyclerRefreshLayout.setRefreshStyle(@NonNull RefreshStyle) 
```

## 自定义

为 `RecyclerRefreshLayout` 自定义刷新动画 (需要实现接口 `IRefreshStatus`)
```java
public interface IRefreshStatus {
/**
* 当手势操作完成且刷新动画滑动到起始位置
*/
void reset();
/**
* 正在刷新
*/
void refreshing();
/**
* 刷新动画被下拉到刷新位置
*/
void pullToRefresh();
/**
* 刷新动画被释放到刷新位置
*/
void releaseToRefresh();
/**
* @param pullDistance 刷新动画被拖动的距离
* @param pullProgress 刷新动画被拖动的进度，当大于触发刷新的距离会大于1.0f
*                     pullProgress = pullDistance / refreshTargetOffset
*/
void pullProgress(float pullDistance, float pullProgress);
}
```
```java 
RecyclerRefreshLayout.setRefreshView(View, LayoutParams);
```
例如. [RefreshView](https://github.com/dinuscxj/RecyclerRefreshLayout/blob/master/recyclerrefreshlayout/src/main/java/com/dinuscxj/refresh/RefreshView.java) or [RefreshViewEg](https://github.com/dinuscxj/RecyclerRefreshLayout/tree/master/app/src/main/java/com/dinuscxj/example/demo/RefreshViewEg.java) 

为 `RecyclerRefreshLayout` 定义拖动距离的转换器（需要实现 `IDragDistanceConverter`） 
```java
public interface IDragDistanceConverter {
 /**
  * @param scrollDistance ACTION_DOWN触发的Y坐标和当前ACTION_MOVE所在Y坐标的距离
  * @param refreshDistance 刷新点和起始点之间的距离
  * @return 返回下拉刷新动画实际滑动的距离
  */
 float convert(float scrollDistance, float refreshDistance);
}
```
```java
RecyclerRefreshLayout.setDragDistanceConverter(@NonNull IDragDistanceConverter) 
```
例如. [MaterialDragDistanceConverter](https://github.com/dinuscxj/RecyclerRefreshLayout/blob/master/recyclerrefreshlayout/src/main/java/com/dinuscxj/refresh/MaterialDragDistanceConverter.java) or [DragDistanceConverterEg](https://github.com/dinuscxj/RecyclerRefreshLayout/tree/master/app/src/main/java/com/dinuscxj/example/demo/DragDistanceConverterEg.java) 

## 杂谈

  ***QQ 群:*** **342748245**
  
## 开源协议

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
