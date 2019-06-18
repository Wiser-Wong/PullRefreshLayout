package com.wiser.refreshlayout;

import java.util.ArrayList;
import java.util.List;

import com.wiser.refresh.RefreshFooterLayout;
import com.wiser.refresh.RefreshHeaderLayout;
import com.wiser.refresh.RefreshLayout;
import com.wiser.refresh.listener.OnRefreshListener;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements OnRefreshListener {

	private List<String>		list	= new ArrayList<>();

	private RefreshHeaderLayout	headerLayout;

	private RefreshFooterLayout footerLayout;

	private RecyclerView		recyclerView;

	// private RefreshScrollView scrollView;
	// private RefreshNestedScrollView scrollView;

	// private Button btn;

	private RefreshLayout		refreshLayout;

	private MAdapter			adapter;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initData();
	}

	private void initData() {

		headerLayout = findViewById(R.id.header_view);
		footerLayout = findViewById(R.id.footer_view);

		recyclerView = findViewById(R.id.rlv_list);

		refreshLayout = findViewById(R.id.refresh_layout);

		// scrollView = findViewById(R.id.scrollView);

		// btn = findViewById(R.id.btn);

		headerLayout.setRefreshLayout(refreshLayout);
		footerLayout.setRefreshLayout(refreshLayout);

		refreshLayout.setOnRefreshListener(this);

		for (int i = 0; i < 20; i++) {
			list.add("我是第" + i + "条数据");
		}

		recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(adapter = new MAdapter(this, list));

		// btn.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// Toast.makeText(MainActivity.this,"点击",Toast.LENGTH_LONG).show();
		// }
		// });
	}

	@Override public void pullDownRefreshing() {
		new Handler().postDelayed(new Runnable() {

			@Override public void run() {
				adapter.addHeadData("我是新增数据");
				refreshLayout.refreshHeaderState(RefreshLayout.REFRESH_HEADER_END);
			}
		}, 2500);
	}

	@Override public void pullUpRefreshing() {
		new Handler().postDelayed(new Runnable() {

			@Override public void run() {
				adapter.addFooterData("我是底部新增数据");
				refreshLayout.refreshFooterState(RefreshLayout.REFRESH_FOOTER_END);
			}
		}, 2500);
	}
}
