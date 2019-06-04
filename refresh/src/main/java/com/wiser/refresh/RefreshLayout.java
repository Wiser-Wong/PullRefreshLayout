package com.wiser.refresh;

import java.lang.ref.WeakReference;

import com.wiser.refresh.listener.OnRefreshListener;
import com.wiser.refresh.listener.OnRefreshScrollChangedListener;
import com.wiser.refresh.listener.OnRefreshStateListener;
import com.wiser.refresh.view.RefreshNestedScrollView;
import com.wiser.refresh.view.RefreshScrollView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * @author Wiser
 *         <p>
 *         RecycleView刷新布局
 */
public class RefreshLayout extends LinearLayout {

	private View					mContentView;						// 内容控件

	private View					mHeaderView;						// 头部布局

	private float					mOffset;							// 偏移量

	private float					headerHeight;						// 头部高度

	private float					downY;								// 主动触摸滑动时按下的Y轴位置

	private float					downPressY;							// 主动触摸滑动时按下的Y轴位置

	private boolean					isSlidingTop		= true;			// 是否滑动到顶部

	private boolean					isRunningPullDown	= true;			// 是否进行到达顶部下拉动作

	public static final int			REFRESH_NO			= 0;			// 不刷新

	public static final int			REFRESH_PREPARE		= 1;			// 准备刷新

	public static final int			REFRESH_RUNNING		= 2;			// 正在刷新

	public static final int			REFRESH_END			= 3;			// 刷新完成

	private int						refreshState		= REFRESH_NO;	// 刷新状态

	private OnRefreshListener		onRefreshListener;					// 刷新监听

	private OnRefreshStateListener	onRefreshStateListener;				// 刷新状态监听

	private RefreshHandler			refreshHandler;

	private boolean					isIntercept			= true;			// 是否拦截(为了处理子控件触发的触摸事件，例如子控件点击事件会同时进行)

	private boolean					isRefreshEnd		= true;			// 是否彻底刷新完成

	private boolean					isDefaultRefresh;					// 是否默认刷新

	int								headerLayoutId;						// 头部布局

	public RefreshLayout(Context context) {
		super(context);
		init(context, null);
	}

	public RefreshLayout(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		if (attrs == null) return;
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
		headerLayoutId = typedArray.getResourceId(R.styleable.RefreshLayout_refreshHeaderLayoutId, -1);
		isDefaultRefresh = typedArray.getBoolean(R.styleable.RefreshLayout_refreshIsDefault, isDefaultRefresh);
		typedArray.recycle();

		setOrientation(LinearLayout.VERTICAL);

		refreshHandler = new RefreshHandler(this);

		initView(context);
	}

	private void initView(Context context) {
		if (headerLayoutId > 0) {
			mHeaderView = LayoutInflater.from(context).inflate(headerLayoutId, this, false);
		}

		if (mHeaderView != null) addView(mHeaderView);

		defaultHeaderView();

	}

	public View headerView() {
		return mHeaderView;
	}

	public View contentView() {
		return mContentView;
	}

	// 默认header
	private void defaultHeaderView() {
		if (mHeaderView == null) {
			mHeaderView = new RefreshHeaderLayout(getContext());
			addView(mHeaderView, 0);
			((RefreshHeaderLayout) mHeaderView).setRefreshLayout(this);
		}
	}

	// 添加滚动监听 判断滑动位置
	private void addScrollListener() {
		if (mContentView == null) return;
		// RecycleView 控件
		if (mContentView instanceof RecyclerView) ((RecyclerView) mContentView).addOnScrollListener(new RecyclerView.OnScrollListener() {

			@Override public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				// isSlidingTop = recyclerView.getLayoutManager() != null &&
				// ((LinearLayoutManager)
				// recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition() ==
				// 0;
			}

			@Override public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				// 判断是否滚动到顶部
				isSlidingTop = !recyclerView.canScrollVertically(-1);
			}
		});
		// ScrollView 控件
		if (mContentView instanceof RefreshScrollView) ((RefreshScrollView) mContentView).setOnRefreshScrollChangedListener(new OnRefreshScrollChangedListener() {

			@Override public void onScrollChanged(int l, int t, int oldl, int oldt) {
				isSlidingTop = mContentView.getScrollY() == 0;
			}

			@Override public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
				isSlidingTop = scrollY == 0 && clampedY;
			}
		});
		// NestedScrollView 控件
		if (mContentView instanceof RefreshNestedScrollView) ((RefreshNestedScrollView) mContentView).setOnRefreshScrollChangedListener(new OnRefreshScrollChangedListener() {

			@Override public void onScrollChanged(int l, int t, int oldl, int oldt) {
				isSlidingTop = mContentView.getScrollY() == 0;
			}

			@Override public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
				isSlidingTop = scrollY == 0 && clampedY;
			}
		});
	}

	@Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (getChildCount() > 2) throw new IllegalStateException("刷新组件内部只能添加一个内容布局");
		if (mHeaderView != null) {
			headerHeight = mHeaderView.getMeasuredHeight();
			mHeaderView.layout(0, -mHeaderView.getMeasuredHeight(), mHeaderView.getMeasuredWidth(), 0);
		}
		if (mContentView != null) mContentView.layout(0, 0, mContentView.getMeasuredWidth(), getBottom());
	}

	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (getChildCount() > 2) throw new IllegalStateException("刷新组件内部只能添加一个内容布局");
		mContentView = getChildAt(1);
		addScrollListener();
		if (isDefaultRefresh) {
			if (mHeaderView != null) pullRefreshReleaseScroll(true, -mHeaderView.getMeasuredHeight());
		}
	}

	@Override public boolean dispatchTouchEvent(MotionEvent ev) {
		if (mContentView == null || mHeaderView == null) return super.dispatchTouchEvent(ev);
		// 正在刷新 不可处理其他事件
		// if (refreshState == REFRESH_RUNNING) return true;
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (mOffset == 0 && refreshState != REFRESH_RUNNING) refreshState(REFRESH_NO);
				downY = downPressY = ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				// 判断滑动方向
				if (isSlidingTop) {// 顶部 开始下拉进入刷新模式
					if (isRunningPullDown) {
						isRunningPullDown = false;
						downY = ev.getY();
					}
					if (mContentView != null)
						// 加该方法是为了处理子View滑动到顶部时候继续滑出头部 然后没有抬起往回滑动隐藏头部时 子View会处理自己滑动事件
						mContentView.getParent().requestDisallowInterceptTouchEvent(false);
					float moveY = ev.getY() - downY;
					moveY /= 2.4f;
					mOffset -= moveY;
					downY = ev.getY();
					// 更新刷新头tip
					if (refreshState != REFRESH_RUNNING && refreshState != REFRESH_END) {
						if (Math.abs(mOffset) < headerHeight) refreshState(REFRESH_NO);
						else refreshState(REFRESH_PREPARE);
					}
					if (mOffset >= 0) {// 刷新头完全隐藏
						mOffset = 0;
						scrollTo(0, 0);
						if (refreshState == REFRESH_END) {// 当正在刷新中的时候 再次触摸屏幕 这个时候刷新完成了但是动画未完成，这个时候将头部隐藏，触摸未结束，再次滑动出头部，应该变成下拉刷新样式 所以做此处理
							refreshState(REFRESH_END);
						}
						return super.dispatchTouchEvent(ev);
					}
					scrollTo(0, (int) mOffset);
					return super.dispatchTouchEvent(ev);
				} else {
					isRunningPullDown = true;
					mOffset = 0;
				}
				if (mContentView != null) mContentView.getParent().requestDisallowInterceptTouchEvent(true);
			case MotionEvent.ACTION_UP:
				if (isSlidingTop && mOffset < 0 && Math.abs(ev.getY() - downPressY) > 20) handleActionUp();
				break;
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override public boolean onInterceptTouchEvent(MotionEvent event) {
		if (mContentView == null || mHeaderView == null) return super.onInterceptTouchEvent(event);
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isIntercept = false;
				break;
			case MotionEvent.ACTION_MOVE:
				isIntercept = mOffset < 0;
				break;
			case MotionEvent.ACTION_UP:
				isIntercept = false;
				break;
		}
		return isIntercept;
	}

	// 处理抬起事件
	private void handleActionUp() {

		// 当下拉已经超过刷新头的时候 释放刷新会先滚动到刷新头所有内容展示出来的位置
		if (Math.abs(mOffset) > headerHeight) pullRefreshReleaseScroll(true, -headerHeight);
		else pullRefreshReleaseScroll(true, 0);

	}

	// 拉动刷新释放滚动
	private void pullRefreshReleaseScroll(final boolean isRefreshing, float offset) {
		ValueAnimator animator = ValueAnimator.ofFloat(mOffset, offset);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			@Override public void onAnimationUpdate(ValueAnimator animation) {
				mOffset = (float) animation.getAnimatedValue();
				scrollTo(0, (int) mOffset);
			}
		});
		animator.addListener(new AnimatorListenerAdapter() {

			@Override public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (mOffset < 0 && refreshState != REFRESH_RUNNING && isRefreshEnd) {
					refreshState(REFRESH_RUNNING);// 刷新中
				}
				if (!isRefreshing) {// 刷新完成处理
					refreshState(REFRESH_END);
				}
			}
		});
		animator.setDuration(350);
		animator.start();
	}

	// 刷新状态
	public void refreshState(int refreshState) {
		this.refreshState = refreshState;
		switch (refreshState) {
			case REFRESH_NO:// 未刷新
			case REFRESH_PREPARE:// 准备释放刷新
				isRefreshEnd = true;
				refreshHandler.removeMessages(REFRESH_END);
				break;
			case REFRESH_RUNNING:// 刷新中
				// 刷新中
				if (onRefreshListener != null) onRefreshListener.pullDownRefreshing();
				break;
			case REFRESH_END:// 刷新结束
				if (refreshHandler != null) {
					if (mOffset >= 0) {// 如果刷新完成了 并且头部已经隐藏了 直接更改刷新状态
						isRefreshEnd = true;
						refreshHandler.removeMessages(REFRESH_END);
						refreshState(REFRESH_NO);
					} else if (mOffset < 0) {// 否则头部未隐藏 继续执行延时隐藏
						isRefreshEnd = false;
						refreshHandler.removeMessages(REFRESH_END);
						refreshHandler.sendEmptyMessageDelayed(REFRESH_END, 600);
					}
				}
				break;
		}
		if (onRefreshStateListener != null) onRefreshStateListener.onRefreshState(refreshState);
	}

	// 刷新状态
	public int refreshState() {
		return refreshState;
	}

	private static class RefreshHandler extends Handler {

		WeakReference<RefreshLayout> reference;

		RefreshHandler(RefreshLayout refreshLayout) {
			reference = new WeakReference<>(refreshLayout);
		}

		@Override public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (reference != null && reference.get() != null) {
				if (msg.what == REFRESH_END) {
					reference.get().pullRefreshReleaseScroll(false, 0);
				}
			}
		}
	}

	public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
		this.onRefreshListener = onRefreshListener;
	}

	public void setOnRefreshStateListener(OnRefreshStateListener onRefreshStateListener) {
		this.onRefreshStateListener = onRefreshStateListener;
	}

	@Override protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (refreshHandler != null) refreshHandler.removeMessages(REFRESH_END);
		refreshHandler = null;
		mContentView = null;
		mHeaderView = null;
		onRefreshListener = null;
	}
}
