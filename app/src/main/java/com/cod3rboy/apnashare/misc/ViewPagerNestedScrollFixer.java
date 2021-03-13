package com.cod3rboy.apnashare.misc;

import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.reflect.Field;

public class ViewPagerNestedScrollFixer extends RecyclerView.OnScrollListener implements RecyclerView.OnItemTouchListener {
    private static final String LOG_TAG = ViewPagerNestedScrollFixer.class.getSimpleName();

    private int scrollState;
    private int scrollPointerId;
    private int initialTouchX;
    private int initialTouchY;
    private int dx;
    private int dy;

    public ViewPagerNestedScrollFixer() {
        scrollState = RecyclerView.SCROLL_STATE_IDLE;
        scrollPointerId = -1;
        initialTouchX = 0;
        initialTouchY = 0;
        dx = 0;
        dy = 0;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                scrollPointerId = e.getPointerId(0);
                initialTouchX = (int) (e.getX() + 0.5f);
                initialTouchY = (int) (e.getY() + 0.5f);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int actionIndex = e.getActionIndex();
                scrollPointerId = e.getPointerId(actionIndex);
                initialTouchX = (int) (e.getX(actionIndex) + 0.5f);
                initialTouchY = (int) (e.getY(actionIndex) + 0.5f);
                break;
            case MotionEvent.ACTION_MOVE:
                int index = e.findPointerIndex(scrollPointerId);
                if (index >= 0 && scrollState != RecyclerView.SCROLL_STATE_DRAGGING) {
                    int x = (int) (e.getX(index) + 0.5f);
                    int y = (int) (e.getY(index) + 0.5f);
                    dx = x - initialTouchX;
                    dy = y - initialTouchY;
                }
                break;
        }
        return false;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        int oldState = scrollState;
        scrollState = newState;
        if (oldState == RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            boolean canScrollHorizontally = layoutManager.canScrollHorizontally();
            boolean canScrollVertically = layoutManager.canScrollVertically();
            if (canScrollHorizontally != canScrollVertically) { // Single direction
                if ((canScrollHorizontally && Math.abs(dy) > Math.abs(dx))
                        || (canScrollVertically && Math.abs(dx) > Math.abs(dy))) {
                    recyclerView.stopScroll();
                }
            }
        }
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public static void enforceSingleScrollDirection(ViewPager2 viewPager2) {
        // Use java reflection api to access private mRecyclerView field of ViewPager2 instance
        // and add register above listener as OnItemTouchListener and OnScrollListener on mRecyclerView
        ViewPagerNestedScrollFixer scrollFixer = new ViewPagerNestedScrollFixer();
        try {
            Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
            recyclerViewField.setAccessible(true);
            RecyclerView mRecyclerView = (RecyclerView) recyclerViewField.get(viewPager2);
            mRecyclerView.addOnItemTouchListener(scrollFixer);
            mRecyclerView.addOnScrollListener(scrollFixer);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }
}
