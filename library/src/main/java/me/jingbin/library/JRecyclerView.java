package me.jingbin.library;


import android.content.Context;
import android.util.AttributeSet;
import android.util.DebugUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;

import me.jingbin.library.adapter.BaseRecyclerViewAdapter;

/**
 * @author jingbin
 * link to https://github.com/youlookwhat/JRecyclerView
 */
public class JRecyclerView extends RecyclerView {

    /**
     * 下面的ItemViewType是保留值(ReservedItemViewType),如果用户的adapter与它们重复将会强制抛出异常。
     * 不过为了简化,我们检测到重复时对用户的提示是ItemViewType必须小于10000
     * 设置一个很大的数字,尽可能避免和用户的adapter冲突
     */
    private static final int TYPE_REFRESH_HEADER = 10000;
    private static final int TYPE_FOOTER = 10001;
    private static final int HEADER_INIT_INDEX = 10002;
    /**
     * 每个header必须有不同的type,不然滚动的时候顺序会变化（不要随便加静态，改了一天！！！！！！！！！！）
     */
    private List<Integer> sHeaderTypes = new ArrayList<>();
    /**
     * HeaderView 数组
     */
    private ArrayList<View> mHeaderViews = new ArrayList<>();

    private WrapAdapter mWrapAdapter;
    /**
     * 是否正在加载更多
     */
    private boolean isLoadingData = false;
    /**
     * 是否没有更多数据了
     */
    private boolean isNoMore = false;
    /**
     * 设置是否能 下拉刷新
     */
    private boolean pullRefreshEnabled = false;
    /**
     * 设置是否能 加载更多
     */
    private boolean loadingMoreEnabled = true;
    /**
     * 首页列表增加一个tabhost的高度
     */
    private boolean isFooterMoreHeight = false;

    private LoadingListener mLoadingListener;
    private BaseRefreshHeader mRefreshHeader;
    private View mFootView;
    private AppBarStateChangeListener.State appbarState = AppBarStateChangeListener.State.EXPANDED;
    private final RecyclerView.AdapterDataObserver mDataObserver = new DataObserver();
    private float mLastY = -1;
    private static final float DRAG_RATE = 3;

    public JRecyclerView(Context context) {
        this(context, null);
    }

    public JRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mFootView = new LoadingMoreFooter(getContext());
        mFootView.setVisibility(GONE);
    }

    public void addHeaderView(View view) {
        sHeaderTypes.add(HEADER_INIT_INDEX + mHeaderViews.size());
        mHeaderViews.add(view);
//        if (mWrapAdapter != null) {
//            mWrapAdapter.notifyDataSetChanged();
//        }
    }

    /**
     * 根据header的ViewType判断是哪个header
     */
    private View getHeaderViewByType(int itemType) {
        if (!isHeaderType(itemType)) {
            return null;
        }
        return mHeaderViews.get(itemType - HEADER_INIT_INDEX);
    }

    /**
     * 判断一个type是否为HeaderType
     */
    private boolean isHeaderType(int itemViewType) {
        return mHeaderViews.size() > 0 && sHeaderTypes.contains(itemViewType);
    }

    /**
     * 判断是否是JRecyclerView保留的itemViewType
     */
    private boolean isReservedItemViewType(int itemViewType) {
        if (itemViewType == TYPE_REFRESH_HEADER || itemViewType == TYPE_FOOTER || sHeaderTypes.contains(itemViewType)) {
            return true;
        } else {
            return false;
        }
    }

    public void setFootView(final View view) {
        mFootView = view;
    }

    public void loadMoreComplete() {
        isLoadingData = false;
        if (mFootView instanceof LoadingMoreFooter) {
            ((LoadingMoreFooter) mFootView).setState(LoadingMoreFooter.STATE_COMPLETE);
        } else {
            mFootView.setVisibility(View.GONE);
        }
    }

    public void setNoMore(boolean noMore) {
        isLoadingData = false;
        isNoMore = noMore;
        if (mFootView instanceof LoadingMoreFooter) {
            ((LoadingMoreFooter) mFootView).setState(isNoMore ? LoadingMoreFooter.STATE_NOMORE : LoadingMoreFooter.STATE_COMPLETE);
        } else {
            mFootView.setVisibility(View.GONE);
        }
    }

    public void refresh() {
        if (pullRefreshEnabled && mLoadingListener != null) {
            mRefreshHeader.setState(BaseRefreshHeader.STATE_REFRESHING);
            mLoadingListener.onRefresh();
        }
    }

    public void reset() {
        setNoMore(false);
        loadMoreComplete();
        refreshComplete();
    }

    public void refreshComplete() {
        if (pullRefreshEnabled) {
            mRefreshHeader.refreshComplete();
        }
        setNoMore(false);
    }

    /**
     * 没有更多内容
     */
    public void noMoreLoading() {
        isLoadingData = false;
        isNoMore = true;
        if (mFootView instanceof LoadingMoreFooter) {
            ((LoadingMoreFooter) mFootView).setState(LoadingMoreFooter.STATE_NOMORE);
        } else {
            mFootView.setVisibility(View.GONE);
        }
    }

    /**
     * 不显示"没有更多内容了"且不能继续刷新
     */
    public void noRefresh() {
        isLoadingData = false;
        isNoMore = true;
    }

    public void setRefreshHeader(BaseRefreshHeader refreshHeader) {
        mRefreshHeader = refreshHeader;
    }

    public void setPullRefreshEnabled(boolean enabled) {
        pullRefreshEnabled = enabled;
        if (mRefreshHeader == null) {
            mRefreshHeader = new YunRefreshHeader(getContext());
        }
    }

    /**
     * 首页和修行社：设置列表底部增加一个tabhost的高度
     *
     * @param enabled 默认为不增加，正常
     */
    public void setFooterMoreHeightEnabled(boolean enabled) {
        isFooterMoreHeight = enabled;
        if (mFootView instanceof LoadingMoreFooter) {
            ((LoadingMoreFooter) mFootView).setFooterMoreHeight(true);
        }
    }

    public void setLoadingMoreEnabled(boolean enabled) {
        loadingMoreEnabled = enabled;
        if (!enabled) {
            if (mFootView instanceof LoadingMoreFooter) {
                ((LoadingMoreFooter) mFootView).setState(LoadingMoreFooter.STATE_COMPLETE);
            }
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mWrapAdapter = new WrapAdapter(adapter);
        super.setAdapter(mWrapAdapter);
        adapter.registerAdapterDataObserver(mDataObserver);
        mDataObserver.onChanged();
    }

    /**
     * 避免用户自己调用getAdapter() 引起的ClassCastException
     */
    @Override
    public Adapter getAdapter() {
        if (mWrapAdapter != null) {
            return mWrapAdapter.getOriginalAdapter();
        } else {
            return null;
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (mWrapAdapter != null) {
            if (layout instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) layout);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (mWrapAdapter.isHeader(position) || mWrapAdapter.isFooter(position) || mWrapAdapter.isRefreshHeader(position))
                                ? gridManager.getSpanCount() : 1;
                    }
                });

            }
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE && mLoadingListener != null && !isLoadingData && loadingMoreEnabled) {
            LayoutManager layoutManager = getLayoutManager();
            int lastVisibleItemPosition;
            if (layoutManager instanceof GridLayoutManager) {
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition = findMax(into);
            } else {
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
//            Log.e("-----11", "--" + (lastVisibleItemPosition >= layoutManager.getItemCount() - 1));
//            Log.e("-----22", "--" + (layoutManager.getItemCount() > layoutManager.getChildCount()));
//            Log.e("-----22-list1``", "--" + (layoutManager.getItemCount()));
//            Log.e("-----22-list2``", "--" + (layoutManager.getChildCount()));
//            Log.e("-----33", "--" + (!isNoMore));
//            Log.e("-----44", "--" + (mRefreshHeader.getState() < YunRefreshHeader.STATE_REFRESHING));
            // 取消那条后，只有一条信息也可以刷新
            if (layoutManager.getChildCount() > 0
                    && lastVisibleItemPosition >= layoutManager.getItemCount() - 1
//                    && layoutManager.getItemCount() > layoutManager.getChildCount()
                    && !isNoMore
                    && (!pullRefreshEnabled || mRefreshHeader.getState() < YunRefreshHeader.STATE_REFRESHING)) {
                isLoadingData = true;
                if (mFootView instanceof LoadingMoreFooter) {
                    ((LoadingMoreFooter) mFootView).setState(LoadingMoreFooter.STATE_LOADING);
                } else {
                    mFootView.setVisibility(View.VISIBLE);
                }
                mLoadingListener.onLoadMore();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = ev.getRawY() - mLastY;
                mLastY = ev.getRawY();
                if (pullRefreshEnabled && isOnTop() && appbarState == AppBarStateChangeListener.State.EXPANDED) {
                    mRefreshHeader.onMove(deltaY / DRAG_RATE);
                    if (mRefreshHeader.getVisibleHeight() > 0 && mRefreshHeader.getState() < YunRefreshHeader.STATE_REFRESHING) {
                        return false;
                    }
                }
                break;
            default:
                mLastY = -1;
                if (pullRefreshEnabled && isOnTop() && appbarState == AppBarStateChangeListener.State.EXPANDED) {
                    if (mRefreshHeader.releaseAction()) {
                        if (mLoadingListener != null) {
                            mLoadingListener.onRefresh();
                        }
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private boolean isOnTop() {
        if (mRefreshHeader != null
                && mRefreshHeader instanceof View
                && ((View) mRefreshHeader).getParent() != null) {
            return true;
        } else {
            return false;
        }
    }

    private class DataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            if (mWrapAdapter != null) {
                mWrapAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
        }
    }

    private class WrapAdapter extends RecyclerView.Adapter<ViewHolder> {

        private RecyclerView.Adapter adapter;

        WrapAdapter(RecyclerView.Adapter adapter) {
            this.adapter = adapter;
        }

        RecyclerView.Adapter getOriginalAdapter() {
            return this.adapter;
        }

        /**
         * 是否是 HeaderView 布局
         */
        boolean isHeader(int position) {
            return position >= getPullHeaderSize() && position < mHeaderViews.size() + getPullHeaderSize();
        }

        /**
         * 是否是 上拉加载 footer 布局
         */
        boolean isFooter(int position) {
            if (loadingMoreEnabled) {
                return position == getItemCount() - 1;
            } else {
                return false;
            }
        }

        /**
         * 是否是 头部刷新布局
         */
        boolean isRefreshHeader(int position) {
            if (pullRefreshEnabled) {
                return position == 0;
            } else {
                return false;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_REFRESH_HEADER) {
                return new SimpleViewHolder((View) mRefreshHeader);
            } else if (isHeaderType(viewType)) {
                return new SimpleViewHolder(getHeaderViewByType(viewType));
            } else if (viewType == TYPE_FOOTER) {
                return new SimpleViewHolder(mFootView);
            }
            ViewHolder viewHolder = adapter.onCreateViewHolder(parent, viewType);
            bindViewClickListener(viewHolder);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (isHeader(position) || isRefreshHeader(position)) {
                return;
            }
            // 如果可以下拉刷新，就需要 +1
            int adjPosition = position - (getHeadersCount() + getPullHeaderSize());
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    adapter.onBindViewHolder(holder, adjPosition);
                }
            }
        }

        /**
         * some times we need to override this
         */
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> objectList) {
            if (isHeader(position) || isRefreshHeader(position)) {
                return;
            }
            if (adapter != null) {
                // 如果可以下拉刷新，就需要+1
                int adjPosition = position - (getHeadersCount() + getPullHeaderSize());
                int adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    if (objectList.isEmpty()) {
                        adapter.onBindViewHolder(holder, adjPosition);
                    } else {
                        adapter.onBindViewHolder(holder, adjPosition, objectList);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            if (adapter != null) {
                return getPullHeaderSize() + getHeadersCount() + adapter.getItemCount() + getLoadingMoreSize();
            } else {
                return getPullHeaderSize() + getHeadersCount() + getLoadingMoreSize();
            }
        }

        /**
         * 获取 item 类型
         */
        @Override
        public int getItemViewType(int position) {
            if (isRefreshHeader(position)) {
                return TYPE_REFRESH_HEADER;
            }
            if (isHeader(position)) {
                position = position - getPullHeaderSize();
                return sHeaderTypes.get(position);
            }
            if (isFooter(position)) {
                return TYPE_FOOTER;
            }
            int adapterCount;
            if (adapter != null) {
                int adjPosition = position - (getHeadersCount() + getPullHeaderSize());
                adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    int type = adapter.getItemViewType(adjPosition);
                    if (isReservedItemViewType(type)) {
                        throw new IllegalStateException("JRecyclerView require itemViewType in adapter should be less than 10000 !");
                    }
                    return type;
                }
            }
            return 0;
        }

        @Override
        public long getItemId(int position) {
            if (adapter != null && position >= getHeadersCount() + getPullHeaderSize()) {
                int adjPosition = position - (getHeadersCount() + getPullHeaderSize());
                if (adjPosition < adapter.getItemCount()) {
                    return adapter.getItemId(adjPosition);
                }
            }
            return -1;
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (isHeader(position) || isFooter(position) || isRefreshHeader(position))
                                ? gridManager.getSpanCount() : 1;
                    }
                });
            }
            adapter.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            adapter.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null
                    && lp instanceof StaggeredGridLayoutManager.LayoutParams
                    && (isHeader(holder.getLayoutPosition()) || isRefreshHeader(holder.getLayoutPosition()) || isFooter(holder.getLayoutPosition()))) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
            adapter.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            adapter.onViewRecycled(holder);
        }

        @Override
        public boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
            return adapter.onFailedToRecycleView(holder);
        }

        @Override
        public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            adapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            adapter.registerAdapterDataObserver(observer);
        }

        private class SimpleViewHolder extends RecyclerView.ViewHolder {
            SimpleViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    /**
     * 获取 HeaderView的个数
     */
    int getHeadersCount() {
        return mHeaderViews.size();
    }

    /**
     * 给itemView设置点击事件和长按事件
     */
    private void bindViewClickListener(final ViewHolder viewHolder) {
        if (viewHolder == null) {
            return;
        }
        final View view = viewHolder.itemView;
        if (onItemClickListener != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onClick(v, viewHolder.getLayoutPosition() - (getHeadersCount() + getPullHeaderSize()));
                }
            });
        }
        if (onItemLongClickListener != null) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return onItemLongClickListener.onLongClick(v, viewHolder.getLayoutPosition() - (getHeadersCount() + getPullHeaderSize()));
                }
            });
        }
    }

    public void setLoadingListener(LoadingListener listener) {
        mLoadingListener = listener;
    }

    public interface LoadingListener {

        void onRefresh();

        void onLoadMore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 解决和CollapsingToolbarLayout冲突的问题
        AppBarLayout appBarLayout = null;
        ViewParent p = getParent();
        while (p != null) {
            if (p instanceof CoordinatorLayout) {
                break;
            }
            p = p.getParent();
        }
        if (p != null) {
            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) p;
            final int childCount = coordinatorLayout.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = coordinatorLayout.getChildAt(i);
                if (child instanceof AppBarLayout) {
                    appBarLayout = (AppBarLayout) child;
                    break;
                }
            }
            if (appBarLayout != null) {
                appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
                    @Override
                    public void onStateChanged(AppBarLayout appBarLayout, State state) {
                        appbarState = state;
                    }
                });
            }
        }
    }

    /**
     * 区别是否需要算上刷新布局
     * 如果使用控件自带的下拉刷新，则计算position时需要算上
     */
    private int getPullHeaderSize() {
        if (pullRefreshEnabled) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 如果使用上拉刷新，则计算position时需要算上
     */
    private int getLoadingMoreSize() {
        if (loadingMoreEnabled) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * call it when you finish the activity,
     */
    public void destroy() {
        if (mHeaderViews != null) {
            mHeaderViews.clear();
            mHeaderViews = null;
        }
        if (sHeaderTypes != null) {
            sHeaderTypes.clear();
            mHeaderViews = null;
        }
    }

    /**
     * Register a callback to be invoked when an item in this RecyclerView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }


    /**
     * Register a callback to be invoked when an item in this RecyclerView has
     * been long clicked and held
     *
     * @param listener The callback that will run
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public interface OnItemClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param v        The view that was clicked.
         * @param position The position of the view in the adapter.
         */
        void onClick(View v, int position);
    }

    public interface OnItemLongClickListener {

        /**
         * Called when a view has been clicked and held.
         *
         * @param v        The view that was clicked and held.
         * @param position The position of the view in the adapter.
         * @return true if the callback consumed the long click, false otherwise.
         */
        boolean onLongClick(View v, int position);
    }

    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
}
