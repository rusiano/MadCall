package com.example.rusia.madcall.design;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by rusia on 24/11/2017.
 * Motivation: need to override onInterceptTouchEvent.
 */

public class CustomSlidingPaneLayout extends SlidingPaneLayout {

    public CustomSlidingPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /*
     * We are overriding this method because normally if you swipe right you open the master pane
     * but as we deal with a map, most of the times we swipe right is just to move around with the
     * map and we do not want to open any pane. On the contrary, we want the pane to open only
     * when we click on the corresponding buttons.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If the master pane is closed just consume the event without action
        if(!isOpen()) return false;

        // otherwise "do what you gotta do"
        return super.onInterceptTouchEvent(ev);
    }
}
