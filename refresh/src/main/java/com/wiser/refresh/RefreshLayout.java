package com.wiser.refresh;

import java.lang.ref.WeakReference;

import com.wiser.refresh.listener.OnRefreshFooterStateListener;
import com.wiser.refresh.listener.OnRefreshListener;
import com.wiser.refresh.listener.OnRefreshScrollChangedListener;
import com.wiser.refresh.listener.OnRefreshHeaderStateListener;
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
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * @author Wiser
 *         <p>
 *         RecycleView刷新布局
 */
public class RefreshLayout extends LinearLayout {

	private View							mContentView;									// 内容控件

	private View							mHeaderView;									// 头部布局

	private View							mFooterView;									// 底部布局

	private float							mOffset;										// 偏移量

	private float							mOffsetFooter;									// 偏移量

	private float							headerHeight;									// 头部高度

	private float							footerHeight;									// 头部高度

	private float							downY;											// 主动触摸滑动时按下的Y轴位置

	private float							downPressY;										// 主动触摸滑动时按下的Y轴位置

	private boolean							isSlidingTop			= true;					// 是否滑动到顶部

	private boolean							isSlidingBottom			= true;					// 是否滑动到底部

	private boolean							isRunningPullDown		= true;					// 是否进行到达顶部下拉动作

	private boolean							isRunningPullUp			= true;					// 是否进行到达底部下拉动作

	public static final int					REFRESH_HEADER_NO		= 0;					// 不刷新

	public static final int					REFRESH_HEADER_PREPARE	= 1;					// 准备刷新

	public static final int					REFRESH_HEADER_RUNNING	= 2;					// 正在刷新

	public static final int					REFRESH_HEADER_END		= 3;					// 刷新完成

	private int								refreshHeaderState		= REFRESH_HEADER_NO;	// 刷新状态

	public static final int					REFRESH_FOOTER_NO		= 4;					// 不刷新

	public static final int					REFRESH_FOOTER_PREPARE	= 5;					// 准备刷新

	public static final int					REFRESH_FOOTER_RUNNING	= 6;					// 正在刷新

	public static final int					REFRESH_FOOTER_END		= 7;					// 刷新完成

	private int								refreshFooterState		= REFRESH_FOOTER_NO;	// 刷新状态

	private OnRefreshListener				onRefreshListener;								// 刷新监听

	private OnRefreshHeaderStateListener	onRefreshHeaderStateListener;					// 刷新状态监听

	private OnRefreshFooterStateListener	onRefreshFooterStateListener;					// 刷新状态监听

	private RefreshHandler					refreshHandler;

	private boolean							isIntercept				= true;					// 是否拦截(为了处理子控件触发的触摸事件，例如子控件点击事件会同时进行)

	private boolean							isPullDownRefreshEnd	= true;					// 是否彻底刷新完成

	private boolean							isPullUpRefreshEnd		= true;					// 是否彻底刷新完成

	private boolean							isDefaultRefresh;								// 是否默认刷新

	private int								headerLayoutId;									// 头部布局

	private int								footerLayoutId;									// 底部布局

	private boolean							isLoadMore;										// 是否有加载更多

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
		footerLayoutId = typedArray.getResourceId(R.styleable.RefreshLayout_refreshFooterLayoutId, -1);
		isDefaultRefresh = typedArray.getBoolean(R.styleable.RefreshLayout_refreshIsDefault, isDefaultRefresh);
		isLoadMore = typedArray.getBoolean(R.styleable.RefreshLayout_refreshIsLoadMore, isLoadMore);
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

	public View footerView() {
		return mFooterView;
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

	// 默认footer
	private void defaultFooterView() {
		if (mFooterView == null) {
			mFooterView = new RefreshFooterLayout(getContext());
			measureView(mFooterView);
			footerHeight = mFooterView.getMeasuredHeight();
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) footerHeight);
			addView(mFooterView, params);
			((RefreshFooterLayout) mFooterView).setRefreshLayout(this);
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
				// 判断底部
				isSlidingBottom = !recyclerView.canScrollVertically(1);
			}
		});
		// ScrollView 控件
		if (mContentView instanceof RefreshScrollView) ((RefreshScrollView) mContentView).setOnRefreshScrollChangedListener(new OnRefreshScrollChangedListener() {

			@Override public void onScrollChanged(int l, int t, int oldl, int oldt) {
				isSlidingTop = mContentView.getScrollY() == 0;
				isSlidingBottom = ((RefreshScrollView) mContentView).getChildAt(0) != null
						&& ((RefreshScrollView) mContentView).getChildAt(0).getMeasuredHeight() == (mContentView.getScrollY() + mContentView.getHeight());
			}

			@Override public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
				isSlidingTop = scrollY == 0 && clampedY;
				isSlidingBottom = ((RefreshScrollView) mContentView).getChildAt(0) != null
						&& ((RefreshScrollView) mContentView).getChildAt(0).getMeasuredHeight() == (mContentView.getScrollY() + mContentView.getHeight());
			}
		});
		// NestedScrollView 控件
		if (mContentView instanceof RefreshNestedScrollView) ((RefreshNestedScrollView) mContentView).setOnRefreshScrollChangedListener(new OnRefreshScrollChangedListener() {

			@Override public void onScrollChanged(int l, int t, int oldl, int oldt) {
				isSlidingTop = mContentView.getScrollY() == 0;
				isSlidingBottom = ((RefreshNestedScrollView) mContentView).getChildAt(0) != null
						&& ((RefreshNestedScrollView) mContentView).getChildAt(0).getMeasuredHeight() == (mContentView.getScrollY() + mContentView.getHeight());
			}

			@Override public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
				isSlidingTop = scrollY == 0 && clampedY;
				isSlidingBottom = ((RefreshNestedScrollView) mContentView).getChildAt(0) != null
						&& ((RefreshNestedScrollView) mContentView).getChildAt(0).getMeasuredHeight() == (mContentView.getScrollY() + mContentView.getHeight());
			}
		});
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	@Override protected void onFinishInflate() {
		super.onFinishInflate();
		// footer view 在此添加保证添加到linearlayout中的最后
		if (footerLayoutId > 0) {
			mFooterView = LayoutInflater.from(getContext()).inflate(footerLayoutId, this, false);
		}

		if (mFooterView != null) {
			measureView(mFooterView);
			footerHeight = mFooterView.getMeasuredHeight();
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) footerHeight);
			addView(mFooterView, params);
		}

		defaultFooterView();

		if (!isLoadMore) mFooterView.setVisibility(View.GONE);

	}

	@Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (getChildCount() > 3) throw new IllegalStateException("刷新组件内部只能添加一个内容布局");
		if (mHeaderView != null) {
			headerHeight = mHeaderView.getMeasuredHeight();
			mHeaderView.layout(0, -mHeaderView.getMeasuredHeight(), mHeaderView.getMeasuredWidth(), 0);
		}
		if (mContentView != null) mContentView.layout(0, 0, mContentView.getMeasuredWidth(), getBottom());

		if (isSlidingTop && isSlidingBottom) mFooterView.setVisibility(View.GONE);

	}

	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (getChildCount() > 3) throw new IllegalStateException("刷新组件内部只能添加一个内容布局");
		mContentView = getChildAt(1);
		addScrollListener();
		// 默认刷新
		if (isDefaultRefresh) {
			if (mHeaderView != null) pullDownRefreshReleaseScroll(true, -mHeaderView.getMeasuredHeight());
		}

		// // 同时顶部和底部时 footer隐藏
		// if (isSlidingTop && isSlidingBottom) {
		// mFooterView.setVisibility(View.GONE);
		// }
	}

	@Override public boolean dispatchTouchEvent(MotionEvent ev) {
		if (mContentView == null) return super.dispatchTouchEvent(ev);
		// 正在刷新 不可处理其他事件
		// if (refreshState == REFRESH_RUNNING) return true;
		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (mOffset == 0 && refreshHeaderState != REFRESH_HEADER_RUNNING) refreshHeaderState(REFRESH_HEADER_NO);
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
					if (refreshHeaderState != REFRESH_HEADER_RUNNING && refreshHeaderState != REFRESH_HEADER_END) {
						if (Math.abs(mOffset) < headerHeight) refreshHeaderState(REFRESH_HEADER_NO);
						else refreshHeaderState(REFRESH_HEADER_PREPARE);
					}
					if (mOffset >= 0) {// 刷新头完全隐藏
						mOffset = 0;
						scrollTo(0, 0);
						if (refreshHeaderState == REFRESH_HEADER_END) {// 当正在刷新中的时候 再次触摸屏幕 这个时候刷新完成了但是动画未完成，这个时候将头部隐藏，触摸未结束，再次滑动出头部，应该变成下拉刷新样式 所以做此处理
							refreshHeaderState(REFRESH_HEADER_END);
						}
						return super.dispatchTouchEvent(ev);
					}
					scrollTo(0, (int) mOffset);
					return super.dispatchTouchEvent(ev);
				} else {
					isRunningPullDown = true;
					mOffset = 0;
				}
				// 底部 开始上拉进入刷新模式
				if (isSlidingBottom && isLoadMore) {
					if (isRunningPullUp) {
						isRunningPullUp = false;
						downY = ev.getY();
					}
					if (mContentView != null)
						// 加该方法是为了处理子View滑动到顶部时候继续滑出头部 然后没有抬起往回滑动隐藏头部时 子View会处理自己滑动事件
						mContentView.getParent().requestDisallowInterceptTouchEvent(false);
					float moveY = downY - ev.getY();
					moveY /= 1.2f;
					mOffsetFooter += moveY;
					downY = ev.getY();
					// 更新刷新头tip
					if (refreshFooterState != REFRESH_FOOTER_RUNNING && refreshFooterState != REFRESH_FOOTER_END) {
						if (Math.abs(mOffsetFooter) < footerHeight) refreshFooterState(REFRESH_FOOTER_NO);
						else refreshFooterState(REFRESH_FOOTER_PREPARE);
					}
					if (mOffsetFooter <= 0) {// 刷新头完全隐藏
						mOffsetFooter = 0;
						scrollTo(0, 0);
						if (refreshFooterState == REFRESH_FOOTER_END) {// 当正在刷新中的时候 再次触摸屏幕 这个时候刷新完成了但是动画未完成，这个时候将头部隐藏，触摸未结束，再次滑动出头部，应该变成下拉刷新样式 所以做此处理
							refreshFooterState(REFRESH_FOOTER_END);
						}
						return super.dispatchTouchEvent(ev);
					}
					scrollTo(0, (int) mOffsetFooter);
					return super.dispatchTouchEvent(ev);
				} else {
					isRunningPullUp = true;
					mOffsetFooter = 0;
				}
				if (mContentView != null) mContentView.getParent().requestDisallowInterceptTouchEvent(true);
			case MotionEvent.ACTION_UP:
				if (isSlidingTop && mOffset < 0 && Math.abs(ev.getY() - downPressY) > 20) handleActionUp();
				else if (isLoadMore && isSlidingBottom && mOffsetFooter > 0 && Math.abs(ev.getY() - downPressY) > 20) handleActionUp();
				break;
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override public boolean onInterceptTouchEvent(MotionEvent event) {
		if (mContentView == null) return super.onInterceptTouchEvent(event);
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isIntercept = false;
				break;
			case MotionEvent.ACTION_MOVE:
				isIntercept = mOffset < 0 || mOffsetFooter > 0;
				break;
			case MotionEvent.ACTION_UP:
				isIntercept = false;
				break;
		}
		return isIntercept;
	}

	// 处理抬起事件
	private void handleActionUp() {

		if (isSlidingTop) {
			// 当下拉已经超过刷新头的时候 释放刷新会先滚动到刷新头所有内容展示出来的位置
			if (Math.abs(mOffset) > headerHeight) pullDownRefreshReleaseScroll(true, -headerHeight);
			else pullDownRefreshReleaseScroll(true, 0);
		} else if (isSlidingBottom) {
			// 当下拉已经超过刷新头的时候 释放刷新会先滚动到刷新头所有内容展示出来的位置
			if (Math.abs(mOffsetFooter) > footerHeight) pullUpRefreshReleaseScroll(true, footerHeight);
			else pullUpRefreshReleaseScroll(true, 0);
		}

	}

	// 拉动刷新释放滚动
	private void pullDownRefreshReleaseScroll(final boolean isRefreshing, float offset) {
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
				if (mOffset < 0 && refreshHeaderState != REFRESH_HEADER_RUNNING && isPullDownRefreshEnd) {
					refreshHeaderState(REFRESH_HEADER_RUNNING);// 刷新中
				}
				if (!isRefreshing) {// 刷新完成处理
					refreshHeaderState(REFRESH_HEADER_END);
				}
			}
		});
		animator.setDuration(350);
		animator.start();
	}

	// 拉动刷新释放滚动
	private void pullUpRefreshReleaseScroll(final boolean isRefreshing, float offset, long... duration) {
		ValueAnimator animator = ValueAnimator.ofFloat(mOffsetFooter, offset);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			@Override public void onAnimationUpdate(ValueAnimator animation) {
				mOffsetFooter = (float) animation.getAnimatedValue();
				scrollTo(0, (int) mOffsetFooter);
			}
		});
		animator.addListener(new AnimatorListenerAdapter() {

			@Override public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (mOffsetFooter > 0 && refreshFooterState != REFRESH_FOOTER_RUNNING && isPullUpRefreshEnd) {
					refreshFooterState(REFRESH_FOOTER_RUNNING);// 刷新中
				}
				if (!isRefreshing) {// 刷新完成处理
					refreshFooterState(REFRESH_FOOTER_END);
				}
			}
		});
		if (duration != null && duration.length > 0) animator.setDuration(duration[0]);
		else animator.setDuration(350);
		animator.start();
	}

	// 刷新状态
	public void refreshHeaderState(int refreshHeaderState) {
		this.refreshHeaderState = refreshHeaderState;
		switch (refreshHeaderState) {
			case REFRESH_HEADER_NO:// 未刷新
			case REFRESH_HEADER_PREPARE:// 准备释放刷新
				isPullDownRefreshEnd = true;
				refreshHandler.removeMessages(REFRESH_HEADER_END);
				break;
			case REFRESH_HEADER_RUNNING:// 刷新中
				// 刷新中
				if (onRefreshListener != null) onRefreshListener.pullDownRefreshing();
				break;
			case REFRESH_HEADER_END:// 刷新结束
				if (refreshHandler != null) {
					if (mOffset >= 0) {// 如果刷新完成了 并且头部已经隐藏了 直接更改刷新状态
						isPullDownRefreshEnd = true;
						refreshHandler.removeMessages(REFRESH_HEADER_END);
						refreshHeaderState(REFRESH_HEADER_NO);
					} else if (mOffset < 0) {// 否则头部未隐藏 继续执行延时隐藏
						isPullDownRefreshEnd = false;
						refreshHandler.removeMessages(REFRESH_HEADER_END);
						refreshHandler.sendEmptyMessageDelayed(REFRESH_HEADER_END, 200);
					}
				}
				break;
		}
		if (onRefreshHeaderStateListener != null) onRefreshHeaderStateListener.onRefreshHeaderState(refreshHeaderState);
	}

	// 刷新状态
	public void refreshFooterState(int refreshFooterState) {
		this.refreshFooterState = refreshFooterState;
		switch (refreshFooterState) {
			case REFRESH_FOOTER_NO:// 未刷新
			case REFRESH_FOOTER_PREPARE:// 准备释放刷新
				isPullUpRefreshEnd = true;
				refreshHandler.removeMessages(REFRESH_FOOTER_END);
				break;
			case REFRESH_FOOTER_RUNNING:// 刷新中
				// 刷新中
				if (onRefreshListener != null) onRefreshListener.pullUpRefreshing();
				break;
			case REFRESH_FOOTER_END:// 刷新结束
				if (refreshHandler != null) {
					if (mOffsetFooter <= 0) {// 如果刷新完成了 并且底部已经隐藏了 直接更改刷新状态
						isPullUpRefreshEnd = true;
						refreshHandler.removeMessages(REFRESH_FOOTER_END);
						refreshFooterState(REFRESH_FOOTER_NO);
					} else if (mOffsetFooter > 0) {// 否则底部未隐藏 继续执行延时隐藏
						isPullUpRefreshEnd = false;
						refreshHandler.removeMessages(REFRESH_FOOTER_END);
						refreshHandler.sendEmptyMessageDelayed(REFRESH_FOOTER_END, 1);
					}
				}
				break;
		}
		if (onRefreshFooterStateListener != null) onRefreshFooterStateListener.onRefreshFooterState(refreshFooterState);
	}

	// 刷新状态
	public int refreshHeaderState() {
		return refreshHeaderState;
	}

	private static class RefreshHandler extends Handler {

		WeakReference<RefreshLayout> reference;

		RefreshHandler(RefreshLayout refreshLayout) {
			reference = new WeakReference<>(refreshLayout);
		}

		@Override public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (reference != null && reference.get() != null) {
				if (msg.what == REFRESH_HEADER_END) {
					reference.get().pullDownRefreshReleaseScroll(false, 0);
				}
				if (msg.what == REFRESH_FOOTER_END) {
					reference.get().pullUpRefreshReleaseScroll(false, 0);
				}
			}
		}
	}

	public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
		this.onRefreshListener = onRefreshListener;
	}

	public void setOnRefreshHeaderStateListener(OnRefreshHeaderStateListener onRefreshHeaderStateListener) {
		this.onRefreshHeaderStateListener = onRefreshHeaderStateListener;
	}

	public void setOnRefreshFooterStateListener(OnRefreshFooterStateListener onRefreshFooterStateListener) {
		this.onRefreshFooterStateListener = onRefreshFooterStateListener;
	}

	@Override protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (refreshHandler != null) {
			refreshHandler.removeMessages(REFRESH_HEADER_END);
			refreshHandler.removeMessages(REFRESH_FOOTER_END);
		}
		refreshHandler = null;
		mContentView = null;
		mHeaderView = null;
		mFooterView = null;
		onRefreshListener = null;
		onRefreshHeaderStateListener = null;
		onRefreshFooterStateListener = null;
	}
}
