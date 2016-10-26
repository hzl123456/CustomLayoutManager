package cn.xmrk.layoutmanager;

import android.graphics.PointF;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Au61 on 2016/10/10.
 */

public class CustomLayoutManager extends RecyclerView.LayoutManager {

    /**
     * 最大放大倍数
     **/
    private static final float SCALE_RATE = 1.4f;

    /**
     * 默认每个item之间的角度
     **/
    private static float INTERVAL_ANGLE = 30f;


    /**
     * 滑动距离和角度的一个比例
     **/
    private static float DISTANCE_RATIO = 10f;

    /**
     * 默认的半径长度
     **/
    private static final int DEFAULT_RADIO = 100;
    /**
     * 滑动的方向
     */
    private static int SCROLL_LEFT = 1;
    private static int SCROLL_RIGHT = 2;

    /**
     * 半径默认为100
     **/
    private int mRadius;
    /**
     * 当前旋转的角度
     **/
    private float offsetRotate;

    private int startLeft;
    private int startTop;
    /**
     * 第一个的角度是为0
     **/
    private int firstChildRotate = 0;

    //放大的倍数
    private float maxScale;

    //在什么角度变化之内
    private float minScaleRotate = 40;

    //每个item之间的角度间隔
    private float intervalAngle;

    //最大和最小的移除角度
    private int minRemoveDegree;
    private int maxRemoveDegree;

    //记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
    private SparseBooleanArray itemAttached = new SparseBooleanArray();
    //保存所有的Item的上下左右的偏移量信息
    private SparseArray<Float> itemsRotate = new SparseArray<>();


    // 这里的每个item的大小都是一样的
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;


    public CustomLayoutManager() {
        this(DEFAULT_RADIO);
    }


    public CustomLayoutManager(int mRadius) {
        this.mRadius = mRadius;
        offsetRotate = 0;
        intervalAngle = INTERVAL_ANGLE;
        maxScale = SCALE_RATE;
        minRemoveDegree = -120;
        maxRemoveDegree = 120;
    }


    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //如果没有item，直接返回
        //跳过preLayout，preLayout主要用于支持动画
        if (getItemCount() <= 0 || state.isPreLayout()) {
            offsetRotate = 0;
            return;
        }
        //得到子view的宽和高，这边的item的宽高都是一样的，所以只需要进行一次测量
        View scrap = recycler.getViewForPosition(0);
        addView(scrap);
        measureChildWithMargins(scrap, 0, 0);
        //计算测量布局的宽高
        mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
        mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
        //确定起始位置，在最上方的中心处
        startLeft = (getHorizontalSpace() - mDecoratedChildWidth) / 2;
        startTop = 0;

        //记录每个item旋转的角度
        float rotate = firstChildRotate;
        for (int i = 0; i < getItemCount(); i++) {
            itemsRotate.put(i, rotate);
            itemAttached.put(i, false);
            rotate += intervalAngle;
        }

        //在布局之前，将所有的子View先Detach掉，放入到Scrap缓存中
        detachAndScrapAttachedViews(recycler);
        fixRotateOffset();
        layoutItems(recycler, state);
    }

    /**
     * 进行view的回收和显示
     **/
    private void layoutItems(RecyclerView.Recycler recycler, RecyclerView.State state) {
        layoutItems(recycler, state, SCROLL_RIGHT);
    }

    /**
     * 进行view的回收和显示的具体实现
     **/
    private void layoutItems(RecyclerView.Recycler recycler,
                             RecyclerView.State state, int oritention) {
        if (state.isPreLayout()) return;

        //移除界面之外的view
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (itemsRotate.get(position) - offsetRotate > maxRemoveDegree
                    || itemsRotate.get(position) - offsetRotate < minRemoveDegree) {
                itemAttached.put(position, false);
                removeAndRecycleView(view, recycler);
            }
        }

        //将要显示的view进行显示出来
        for (int i = 0; i < getItemCount(); i++) {
            if (itemsRotate.get(i) - offsetRotate <= maxRemoveDegree
                    && itemsRotate.get(i) - offsetRotate >= minRemoveDegree) {
                if (!itemAttached.get(i)) {
                    View scrap = recycler.getViewForPosition(i);
                    measureChildWithMargins(scrap, 0, 0);
                    if (oritention == SCROLL_LEFT)
                        addView(scrap, 0);
                    else
                        addView(scrap);
                    float rotate = itemsRotate.get(i) - offsetRotate;

                    int left = calLeftPosition(rotate);
                    int top = calTopPosition(rotate);

                    scrap.setRotation(rotate);

                    layoutDecorated(scrap, startLeft + left, startTop + top,
                            startLeft + left + mDecoratedChildWidth, startTop + top + mDecoratedChildHeight);

                    itemAttached.put(i, true);

                    //计算角度然后进行放大
                    float scale = calculateScale(rotate);
                    scrap.setScaleX(scale);
                    scrap.setScaleY(scale);
                }
            }
        }
    }


    /**
     * 根据角度计算大小，0度的时候最大，minScaleRotate度的时候最小，然后其他时候变小
     **/
    private float calculateScale(float rotate) {
        rotate = Math.abs(rotate) > minScaleRotate ? minScaleRotate : Math.abs(rotate);
        return (1 - rotate / minScaleRotate) * (maxScale - 1) + 1;
    }


    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int willScroll = dx;

        //每个item x方向上的移动距离
        float theta = dx / DISTANCE_RATIO;
        float targetRotate = offsetRotate + theta;

        //目标角度
        if (targetRotate < 0) {
            willScroll = (int) (-offsetRotate * DISTANCE_RATIO);
        } else if (targetRotate > getMaxOffsetDegree()) {
            willScroll = (int) ((getMaxOffsetDegree() - offsetRotate) * DISTANCE_RATIO);
        }
        theta = willScroll / DISTANCE_RATIO;

        //当前移动的总角度
        offsetRotate += theta;

        //重新设置每个item的x和y的坐标
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            float newRotate = view.getRotation() - theta;
            int offsetX = calLeftPosition(newRotate);
            int offsetY = calTopPosition(newRotate);

            layoutDecorated(view, startLeft + offsetX, startTop + offsetY,
                    startLeft + offsetX + mDecoratedChildWidth, startTop + offsetY + mDecoratedChildHeight);
            view.setRotation(newRotate);

            //计算角度然后进行放大
            float scale = calculateScale(newRotate);
            view.setScaleX(scale);
            view.setScaleY(scale);
        }

        //根据dx的大小判断是左滑还是右滑
        if (dx < 0)
            layoutItems(recycler, state, SCROLL_LEFT);
        else
            layoutItems(recycler, state, SCROLL_RIGHT);
        return willScroll;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    /**
     * 当前item的x的坐标
     **/
    private int calLeftPosition(float rotate) {
        return (int) (mRadius * Math.cos(Math.toRadians(90 - rotate)));
    }

    /**
     * 当前item的y的坐标
     **/
    private int calTopPosition(float rotate) {
        return (int) ( mRadius * Math.sin(Math.toRadians(90 - rotate)));
    }

    /**
     * 设置滚动时候的角度
     **/
    private void fixRotateOffset() {
        if (offsetRotate < 0) {
            offsetRotate = 0;
        }
        if (offsetRotate > getMaxOffsetDegree()) {
            offsetRotate = getMaxOffsetDegree();
        }

    }

    /**
     * 最大的角度
     **/
    private float getMaxOffsetDegree() {
        return (getItemCount() - 1) * intervalAngle;
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }


    private PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final int direction = targetPosition < firstChildPos ? -1 : 1;
        return new PointF(direction, 0);
    }


    @Override
    public void scrollToPosition(int position) {//移动到某一项
        if (position < 0 || position > getItemCount() - 1) return;
        float targetRotate = position * intervalAngle;
        if (targetRotate == offsetRotate) return;
        offsetRotate = targetRotate;
        fixRotateOffset();
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {//平滑的移动到某一项
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return CustomLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }

        };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {//adapter进行改变的时候
        removeAllViews();
        offsetRotate = 0;
    }

}
