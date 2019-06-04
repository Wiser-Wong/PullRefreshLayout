package com.wiser.refresh.listener;

/**
 * @author Wiser
 * 
 *         ScrollView 滚动监听
 */
public interface OnRefreshScrollChangedListener {

	void onScrollChanged(int l, int t, int oldl, int oldt);

	void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY);
}
