package com.example.rusia.madcall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.rusia.madcall.fragment.AdvancedSearchFragment;
import com.example.rusia.madcall.fragment.NearMeFragment;
import com.example.rusia.madcall.fragment.SettingsFragment;

/**
 * Created by rusia on 26/11/2017.
 * Listener for (option) buttons.
 */

public class ButtonListener
        implements  View.OnClickListener,
                    View.OnLongClickListener,
                    View.OnTouchListener {

    private Activity activity;
    private FragmentManager mFragmentManager;
    private boolean isMenuOpen,
            isNearMeButtonPressed, isSearchButtonPressed, isSettingsButtonPressed;

    ButtonListener(Activity activity, FragmentManager mFragmentManager) {
        this.activity = activity;
        this.mFragmentManager = mFragmentManager;

        this.isMenuOpen = false;
        this.isNearMeButtonPressed = false;
        this.isSearchButtonPressed = false;
        this.isSettingsButtonPressed = false;
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        SlidingPaneLayout mSlidingPaneLayout = activity.findViewById(R.id.sliding_pane_layout);
        RelativeLayout leftIcons = activity.findViewById(R.id.left_icons);
        switch (viewId) {
            case (R.id.fab_menu):
                if (isMenuOpen) {
                    isMenuOpen = false;
                    view.setRotation(0);
                    leftIcons.setVisibility(View.GONE);
                } else {
                    isMenuOpen = true;
                    view.setRotation(90);
                    leftIcons.setVisibility(View.VISIBLE);
                }
                break;

            case (R.id.fab_near_me):
                mSlidingPaneLayout.openPane();
                mFragmentManager.beginTransaction()
                        .replace(R.id.master_pane, new NearMeFragment()).commit();
                //TODO: change master pane layout accordingly
                break;

            case (R.id.fab_search):
                mSlidingPaneLayout.openPane();
                mFragmentManager.beginTransaction()
                        .replace(R.id.master_pane, new AdvancedSearchFragment()).commit();
                //TODO: change master pane layout accordingly
                break;

            case (R.id.fab_settings):
                mSlidingPaneLayout.openPane();
                mFragmentManager.beginTransaction()
                        .replace(R.id.master_pane, new SettingsFragment()).commit();
                //TODO: change master pane layout accordingly
                break;

            case (R.id.fab_location):

        }
    }


    @Override
    public boolean onLongClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            case (R.id.fab_near_me):
                // Show the description of the button and save the state
                activity.findViewById(R.id.fab_near_me_description).setVisibility(View.VISIBLE);
                isNearMeButtonPressed = true;
                break;
            case (R.id.fab_search):
                // Show the description of the button and save the state
                activity.findViewById(R.id.fab_search_description).setVisibility(View.VISIBLE);
                isSearchButtonPressed = true;
                break;
            case (R.id.fab_settings):
                // Show the description of the button and save the state
                activity.findViewById(R.id.fab_settings_description).setVisibility(View.VISIBLE);
                isSettingsButtonPressed = true;
                break;
        }
        // Return true to consume the event and not trigger the simple click, too
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int viewId = view.getId();
        switch (viewId) {
            case (R.id.fab_near_me):
                // If search button is pressed and the user releases it, hide the button description
                if (isNearMeButtonPressed && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    activity.findViewById(R.id.fab_near_me_description).setVisibility(View.GONE);
                    isNearMeButtonPressed = false;
                }
                break;
            case (R.id.fab_search):
                // If search button is pressed and the user releases it, hide the button description
                if (isSearchButtonPressed && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    activity.findViewById(R.id.fab_search_description).setVisibility(View.GONE);
                    isSearchButtonPressed = false;
                }
                break;
            case (R.id.fab_settings):
                // If search button is pressed and the user releases it, hide the button description
                if (isSettingsButtonPressed && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    activity.findViewById(R.id.fab_settings_description).setVisibility(View.GONE);
                    isSettingsButtonPressed = false;
                }
                break;
        }
        // Return false to consume the event
        return false;
    }
}
