package me.jingbin.jrecyclerview.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import me.jingbin.jrecyclerview.R;
import me.jingbin.jrecyclerview.adapter.HomeAdapter;
import me.jingbin.jrecyclerview.bean.HomeItemBean;
import me.jingbin.jrecyclerview.databinding.LayoutFooterViewBinding;
import me.jingbin.jrecyclerview.databinding.LayoutHeaderViewBinding;
import me.jingbin.library.JRecyclerView;

public class SimpleActivity extends AppCompatActivity {

    private int page = 1;
    private HomeAdapter homeAdapter;
    private JRecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);

        recyclerView = findViewById(R.id.recyclerView);
        initAdapter();
    }

    private void initAdapter() {
        final LayoutHeaderViewBinding header1 = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.layout_header_view, (ViewGroup) recyclerView.getParent(), false);
        final LayoutHeaderViewBinding header2 = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.layout_header_view, (ViewGroup) recyclerView.getParent(), false);
        final LayoutFooterViewBinding footerViewBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.layout_footer_view, (ViewGroup) recyclerView.getParent(), false);
        final LayoutFooterViewBinding footerViewBinding2 = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.layout_footer_view, (ViewGroup) recyclerView.getParent(), false);

        homeAdapter = new HomeAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);


        footerViewBinding2.tvText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                recyclerView.removeFooterView(footerViewBinding2.getRoot());
                recyclerView.setHeaderViewEnabled(false);
            }
        });
        recyclerView.addHeaderView(header1.getRoot());
        recyclerView.addHeaderView(header2.getRoot());
        recyclerView.addFooterView(footerViewBinding2.getRoot());
        recyclerView.setAdapter(homeAdapter);
        homeAdapter.addAll(get());
        recyclerView.loadMoreComplete();
        recyclerView.setOnLoadMoreListener(new JRecyclerView.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

            }
        });
        recyclerView.setLoadMoreEnabled(false);
        recyclerView.setOnRefreshListener(new JRecyclerView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                recyclerView.refreshComplete();
            }
        });

    }

    private ArrayList<HomeItemBean> get() {
        ArrayList<HomeItemBean> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            HomeItemBean bean = new HomeItemBean();
            bean.setTitle("jingbin:" + i);
            list.add(bean);
        }
        return list;
    }
}
