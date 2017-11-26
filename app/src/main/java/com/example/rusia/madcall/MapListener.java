package com.example.rusia.madcall;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;


/**
 * Created by rusia on 26/11/2017.
 * -
 */

public class       MapListener
       implements  GoogleMap.OnMarkerClickListener,
                   GoogleMap.OnCameraMoveStartedListener,
                   GoogleMap.OnCameraMoveListener,
                   GoogleMap.OnCameraIdleListener {

    private MapsActivity activity;
    private GoogleMap mMap;
    private MapHelper mapHelper;
    private boolean wasMapMovedByUser;

    MapListener(MapsActivity activity, GoogleMap mMap) {
        this.activity = activity;
        this.mMap = mMap;
        this.mapHelper = activity.getmMapHelper();
        this.wasMapMovedByUser = false;
    }

    @Override
    public void onCameraMoveStarted(int reason) {

        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            this.wasMapMovedByUser = true;
            if (mapHelper.isCameraMapCentered()) {
                // map is no more centered - highlight myLocationButton
                mapHelper.setCameraMapCentered(false);
            }
        }
    }



    /**
     * This method is called when the user clicks on a marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        Context ctx = activity.getApplicationContext();
        BottomSheetLayout mBottomSheet = activity.findViewById(R.id.bottomsheet);

        // Slide in the infobox (bottomsheet) and inflate the corresponding layout.
        mBottomSheet.showWithSheetView(LayoutInflater.from(ctx).inflate(
                R.layout.infobox, mBottomSheet, false));

        Toast.makeText(ctx,
                "(" + marker.getPosition().latitude + ", " + marker.getPosition().longitude + ")",
                Toast.LENGTH_SHORT).show();

        //TODO: retrieve information for the appropriate marker and display it in the bottom sheet

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public void onCameraMove() {
        if (mMap.getCameraPosition().bearing != 0
                && this.wasMapMovedByUser) {
            if (mapHelper.isCameraMapStraight()) {
                mapHelper.setCameraMapStraight(false);
                return;
            }
            mapHelper.getmMyOrientationButton().setRotation(-45f -mMap.getCameraPosition().bearing);
        }
    }

    @Override
    public void onCameraIdle() {

        Log.wtf(MapHelper.LOG_TAG, "Bearing: " + mMap.getCameraPosition().bearing);
        if (mMap.getCameraPosition().bearing != 0
                 && this.wasMapMovedByUser) {
            mapHelper.getmMyOrientationButton().setRotation(-45f -mMap.getCameraPosition().bearing);
            this.wasMapMovedByUser = false;
        }

    }
}
