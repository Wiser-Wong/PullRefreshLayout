package com.wiser.refresh.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.wiser.refresh.listener.OnRefreshScrollChangedListener;

/**
 * @author Wiser
 * 
 *         想进行ScrollView 刷新 必须继承该RefreshScrollView
 */
public class RefreshScrollView extends ScrollView {

	private OnRefreshScrollChangedListener onRefreshScrollChangedListener;

	public RefreshScrollView(Context context) {
		super(context);
	}

	public RefreshScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
		super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
		if (onRefreshScrollChangedListener != null) onRefreshScrollChangedListener.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
	}

	@Override protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (onRefreshScrollChangedListener != null) onRefreshScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
	}

	public void setOnRefreshScrollChangedListener(OnRefreshScrollChangedListener onRefreshScrollChangedListener) {
		this.onRefreshScrollChangedListener = onRefreshScrollChangedListener;
	}

	@Override protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		onRefreshScrollChangedListener = null;
	}
}
