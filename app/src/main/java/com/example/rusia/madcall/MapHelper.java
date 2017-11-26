package com.example.rusia.madcall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.os.Handler;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Random;

import static com.example.rusia.madcall.MapHelper.CameraConstant.DEFAULT_BOUNDS;
import static com.example.rusia.madcall.MapHelper.CameraConstant.DEFAULT_ZOOM;
import static com.example.rusia.madcall.MapHelper.CameraConstant.MAX_LAT;
import static com.example.rusia.madcall.MapHelper.CameraConstant.MAX_LNG;
import static com.example.rusia.madcall.MapHelper.CameraConstant.MIN_LAT;
import static com.example.rusia.madcall.MapHelper.CameraConstant.MIN_LNG;

/**
 * Created by rusia on 24/11/2017.
 * Useful methods to handle MapActivity tasks.
 */

class MapHelper {

    static final String LOG_TAG = "MADCALL";
    static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    interface CameraConstant {
        int    DEFAULT_ZOOM = 17;
        double MAX_LAT = 40.512562D;
        double MIN_LAT = 40.323582D;
        double MAX_LNG = -3.518181D;
        double MIN_LNG = -3.822365D;
        LatLng DEFAULT_LOCATION = new LatLng(40.432643D, -3.704951D);
        LatLngBounds DEFAULT_BOUNDS = new LatLngBounds(new LatLng(MIN_LAT, MIN_LNG),
                new LatLng(MAX_LAT, MAX_LNG));
    }

    private static final int[]  ICONS = {R.drawable.ic_event_black_24dp,
            R.drawable.ic_location_city_black_24dp,
            R.drawable.ic_person_black_24dp};

    // Google Map API related
    private static Location           mLastKnownLocation;

    private MapsActivity helpedActivity;
    private Context ctx;
    private boolean isCameraMapCentered, isCameraMapStraight;

    private FloatingActionButton mMyLocationButton, mMyOrientationButton;

    MapHelper(MapsActivity helpedActivity) {
        this.helpedActivity = helpedActivity;
        this.ctx = helpedActivity.getApplicationContext();
    }

    public FloatingActionButton getmMyOrientationButton() {
        return mMyOrientationButton;
    }

    /**
     * Sets up the layout of the map.
     */
    void setupMapLayout(final GoogleMap mMap) {

        // Constrain the map to Madrid area
        mMap.setLatLngBoundsForCameraTarget(DEFAULT_BOUNDS);

        // Hide the default MyLocation button and other tools provided by Google Maps API
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Initialize a variable to save if the camera is centered on current location or not
        this.isCameraMapCentered = false;
        this.isCameraMapStraight = true;

        // Obtain the location and orientation buttons
        mMyLocationButton = helpedActivity.findViewById(R.id.fab_location);
        mMyOrientationButton = helpedActivity.findViewById(R.id.fab_orientation);

        // Define the listener for our customized MyLocation button
        mMyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                centerCameraOnDeviceLocation(mMap);
            }
        });
        mMyOrientationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetCameraOrientation(mMap);
            }
        });
    }

    /**
     * Gets the last known location of the device.
     */
    void getDeviceLastKnownLocation(final GoogleMap mMap) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        // Construct a FusedLocationProviderClient: it allows to get the best location using
        //  a combination of information coming from Wifi/Mobile Data & GPS.
        FusedLocationProviderClient mFusedLocationProviderClient
                = LocationServices.getFusedLocationProviderClient(ctx);

        try {
            if (helpedActivity.ismLocationPermissionGranted()) {
                Log.wtf(LOG_TAG, "permission was granted");
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(helpedActivity, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        mLastKnownLocation = (task.isSuccessful()) ? task.getResult() : null;
                        Log.wtf(LOG_TAG, "last known location: " + mLastKnownLocation);
                        centerCameraOnDeviceLocation(mMap);
                    }
                });
            } else {
                Log.wtf(LOG_TAG, "here is the bitch");
                mLastKnownLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Positions the camera on the last known location (if available) or on a default location
     */
    private void centerCameraOnDeviceLocation(GoogleMap mMap) {

        Log.wtf(LOG_TAG, "last known location at centering camera time: " + mLastKnownLocation);
        if (mLastKnownLocation == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    MapHelper.CameraConstant.DEFAULT_LOCATION, DEFAULT_ZOOM));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }

        setCameraMapCentered(true);
    }

    private void resetCameraOrientation(GoogleMap mMap) {

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                        .target(mMap.getCameraPosition().target)
                        .bearing(0)
                        .zoom(DEFAULT_ZOOM)
                        .build()));

        mMyOrientationButton.setRotation(-45f);

        setCameraMapStraight(true);
    }

    /**
     * Places n random markers (with random icons) on the map.
     */
    void placeRandomMarkers(GoogleMap mMap, int n) {
        for (int i = 0; i < n; i++) {
            Double lat = MIN_LAT + (MAX_LAT - MIN_LAT)
                    * (double) Math.round((new Random().nextDouble()) * 1000000d) / 1000000d;
            Double lng = MIN_LNG + (MAX_LNG - MIN_LNG)
                    * (double) Math.round((new Random().nextDouble()) * 1000000d) / 1000000d;
            int ic = new Random().nextInt(ICONS.length);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .icon(bitmapDescriptorFromVector(ICONS[ic])));
        }
    }

    /**
     * This method transforms a Vector (icon) into a Bitmap. The previous implementation was
     * making the emulator crash and was removed.
     * Source: https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */
    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(ctx, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        vectorDrawable.draw(new Canvas(bitmap));
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    boolean isCameraMapCentered() {
        return isCameraMapCentered;
    }

    void setCameraMapCentered(boolean cameraMapCentered) {

        isCameraMapCentered = cameraMapCentered;

        // Update also the appearance of MyLocation button
        if (isCameraMapCentered) {
            mMyLocationButton.setClickable(false);
            mMyLocationButton.setBackgroundTintList(
                    helpedActivity.getResources().getColorStateList(R.color.colorPrimaryFaded));

            // Make the button disappear after 1 sec if the camera is still straight
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCameraMapCentered)
                        mMyLocationButton.setVisibility(View.GONE);
                }
            }, 1000);
        } else {
            mMyLocationButton.setVisibility(View.VISIBLE);
            mMyLocationButton.setClickable(true);
            mMyLocationButton.setBackgroundTintList(
                    helpedActivity.getResources().getColorStateList(R.color.colorPrimary));
        }
    }

    boolean isCameraMapStraight() {
        return isCameraMapStraight;
    }

    void setCameraMapStraight(boolean cameraMapStraight) {
        isCameraMapStraight = cameraMapStraight;

        // Update also the appearance of MyLocation button
        if (isCameraMapStraight) {
            mMyOrientationButton.setClickable(false);
            mMyOrientationButton.setBackgroundTintList(
                    helpedActivity.getResources().getColorStateList(R.color.colorPrimaryFaded));

            // Make the button disappear after 1 sec if the camera is still straight
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCameraMapStraight)
                        mMyOrientationButton.setVisibility(View.GONE);
                }
            },1000);
        } else {
            mMyOrientationButton.setVisibility(View.VISIBLE);
            mMyOrientationButton.setClickable(true);
            mMyOrientationButton.setBackgroundTintList(
                    helpedActivity.getResources().getColorStateList(R.color.colorPrimary));
        }
    }
}