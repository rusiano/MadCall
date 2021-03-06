package com.example.rusia.madcall.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.rusia.madcall.MapsActivity;
import com.example.rusia.madcall.R;

/**
 * Created by rusia on 24/11/2017.
 * -
 */

public class AdvancedSearchFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tab_advanced_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SlidingPaneLayout mSlidingPaneLayout = view.getRootView()
                .findViewById(R.id.sliding_pane_layout);

        // set the behavior of the closing button
        view.findViewById(R.id.advanced_search_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSlidingPaneLayout.closePane();
            }
        });
    }

}
