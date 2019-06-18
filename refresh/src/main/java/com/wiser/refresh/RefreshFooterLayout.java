package com.wiser.refresh;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.wiser.refresh.listener.OnRefreshFooterStateListener;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * @author Wiser
 *         <p>
 *         刷新头部布局
 */
public class RefreshFooterLayout extends FrameLayout implements OnRefreshFooterStateListener {

	private AppCompatImageView	ivLoading;

	private TextView			tvPullUpTip;

	private int					refreshState;	// 刷新状态

	private ObjectAnimator		loadingAnimator;

	private RefreshLayout		refreshLayout;

	public RefreshFooterLayout(@NonNull Context context) {
		super(context);
		initView(context);
	}

	public RefreshFooterLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	private void initView(Context context) {
		View view = LayoutInflater.from(context).inflate(R.layout.refresh_footer, this, false);
		ivLoading = view.findViewById(R.id.iv_footer_loading);
		tvPullUpTip = view.findViewById(R.id.tv_pull_up_tip);

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		addView(view);
	}

	public void setRefreshLayout(RefreshLayout refreshLayout) {
		if (this.refreshLayout != null) return;
		this.refreshLayout = refreshLayout;
		if (this.refreshLayout != null) this.refreshLayout.setOnRefreshFooterStateListener(this);
	}

	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (getParent() instanceof RefreshLayout) {
			this.refreshLayout = (RefreshLayout) getParent();
			if (this.refreshLayout != null) this.refreshLayout.setOnRefreshFooterStateListener(this);
		}
	}

	/**
	 * 更新状态提示
	 *
	 * @param refreshState
	 */
	public void updateStateTip(int refreshState) {
		switch (refreshState) {
			case RefreshLayout.REFRESH_FOOTER_NO:
			case RefreshLayout.REFRESH_FOOTER_PREPARE:// 准备刷新
				tvPullUpTip.setText(getResources().getString(R.string.refresh_footer_more_tip));
				if (ivLoading.getVisibility() == View.VISIBLE) {
					ivLoading.setVisibility(View.GONE);
				}
				stopLoadingAnim();
				break;
			case RefreshLayout.REFRESH_FOOTER_RUNNING:// 刷新中
				if (ivLoading.getVisibility() == View.GONE) {
					ivLoading.setVisibility(View.VISIBLE);
				}
				tvPullUpTip.setText(getResources().getString(R.string.refresh_footer_being_tip));
				startLoadingAnim();
				break;
			case RefreshLayout.REFRESH_FOOTER_END:// 刷新结束
				tvPullUpTip.setText(getResources().getString(R.string.refresh_footer_finish_tip));
				break;
		}
	}

	public void startLoadingAnim() {
		if (loadingAnimator == null) {
			loadingAnimator = ObjectAnimator.ofFloat(ivLoading, "rotation", 0f, 360f).setDuration(1500);
			loadingAnimator.setRepeatCount(-1);
			loadingAnimator.setInterpolator(new LinearInterpolator());
		}
		loadingAnimator.start();
	}

	public void stopLoadingAnim() {
		if (loadingAnimator != null && loadingAnimator.isRunning()) loadingAnimator.cancel();
	}

	public void detach() {
		if (loadingAnimator != null) loadingAnimator.cancel();
		loadingAnimator = null;
		ivLoading = null;
		tvPullUpTip = null;
	}

	/**
	 * 根据long类型转时分秒类型字符串
	 *
	 * @param mill
	 * @return
	 */
	public static String getHourMinuteSecondStrForLong(long mill) {
		Date date = new Date(mill);
		String dateStr = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
			// 进行格式化
			dateStr = sdf.format(date);
			return dateStr;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dateStr;
	}

	@Override protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		detach();
	}

	@Override public void onRefreshFooterState(int state) {
		updateStateTip(state);
	}
}
