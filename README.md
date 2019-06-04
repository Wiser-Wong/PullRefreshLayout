# PullRefreshLayout
自定义下拉刷新组件

## 使用说明

    * 默认样式
    <com.wiser.refresh.RefreshLayout
        android:id="@+id/refresh_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@mipmap/background"
        app:refreshHeaderLayoutId="@layout/header"
        app:refreshIsDefault="true">
        
        <android.support.v7.widget.RecyclerView
            android:id="@+id/rlv_list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="#ffffff" />
        
    </com.wiser.refresh.RefreshLayout>
    * 自定义样式需要设置状态监听 OnRefreshStateListener
    * OnRefreshStateListener是自己刷新监听
    
## 支持

* 可配置自己的布局
* 支持RecycleView下拉刷新
* 支持ScrollView以及NestedScrollView下拉刷新 需要继承RefreshScrollView或者RefreshNestedScrollView类
* 支持布局下拉刷新
* 上拉加载暂不支持 可自己在RecycleView 最后一条添加上拉加载功能

## 截图
![images](https://github.com/Wiser-Wong/PullRefreshLayout/blob/master/images/pull_refresh.gif)
