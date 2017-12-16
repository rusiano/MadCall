package com.example.rusia.madcall.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.rusia.madcall.R;

/**
 * Created by rusiano on 24/11/2017.
 * -
 */

public class NearMeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.tab_near_me, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View rootView = view.getRootView();
        final SlidingPaneLayout mSlidingPaneLayout = rootView.findViewById(R.id.sliding_pane_layout);

        ListView listView = rootView.findViewById(R.id.near_streets_list_view);

        // set the behavior of the closing button
        view.findViewById(R.id.near_me_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSlidingPaneLayout.closePane();
            }
        });
    }

}
