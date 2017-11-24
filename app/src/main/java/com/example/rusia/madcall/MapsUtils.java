package com.example.rusia.madcall;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Random;

/**
 * Created by rusia on 24/11/2017.
 */

class MapsUtils {

    private final static String LOG_TAG = "MADCALL";

    static final int            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int    DEFAULT_ZOOM = 17;
    private static final double MAX_LAT = 40.512562D;
    private static final double MIN_LAT = 40.323582D;
    private static final double MAX_LNG = -3.518181D;
    private static final double MIN_LNG = -3.822365D;
    private static final int[]  ICONS = {R.drawable.ic_event_black_24dp,
                                         R.drawable.ic_location_city_black_24dp,
                                         R.drawable.ic_person_black_24dp};

    // Google Map API related
    private static Location           mLastKnownLocation;
    private static final LatLng       mDefaultLocation = new LatLng(40.432643D, -3.704951D);
    private static final LatLng       mSouthWestBound = new LatLng(MIN_LAT, MIN_LNG);
    private static final LatLng       mNorthEastBound = new LatLng(MAX_LAT, MAX_LNG);
    private static final LatLngBounds mDefaultBounds = new LatLngBounds(mSouthWestBound,
                                                                        mNorthEastBound);

    /**
     * Places n random markers (with random icons) on the map.
     */
    static void placeRandomMarkers(Context ctx, GoogleMap mMap, int n) {
        for (int i = 0; i < n; i++) {
            Double lat = MIN_LAT + (MAX_LAT - MIN_LAT)
                    * (double) Math.round((new Random().nextDouble()) * 1000000d) / 1000000d;
            Double lng = MIN_LNG + (MAX_LNG - MIN_LNG)
                    * (double) Math.round((new Random().nextDouble()) * 1000000d) / 1000000d;
            int ic = new Random().nextInt(ICONS.length);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .icon(BitmapDescriptorFactory.fromBitmap(
                            BitmapFactory.decodeResource(
                                    ctx.getResources(), ICONS[ic]))));
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    static void updateLocationUI(final Activity app,
                                 final GoogleMap mMap,
                                 FloatingActionButton mMyLocationButton) {
        if (mMap == null)
            return;

        // Constrain the map to Madrid area
        mMap.setLatLngBoundsForCameraTarget(mDefaultBounds);

        // Hide the default MyLocation button provided by Google Maps API
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Define the listener for our customized MyLocation button
        mMyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation(app, mMap);
            }
        });

        // Allow for the blue dot of user current location only if location permissions are granted.
        try {
            if (MapsActivity.ismLocationPermissionGranted()) {
                mMap.setMyLocationEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mLastKnownLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    static void getDeviceLocation(Activity app, final GoogleMap mMap) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */


        // Construct a FusedLocationProviderClient: it allows to get the best location using
        //  a combination of information coming from Wifi/Mobile Data & GPS.
        FusedLocationProviderClient mFusedLocationProviderClient
                = LocationServices.getFusedLocationProviderClient(app.getApplicationContext());

        try {
            if (MapsActivity.ismLocationPermissionGranted()) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(app, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(LOG_TAG, "Current location is null. Using defaults.");
                            Log.e(LOG_TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    static Location getmLastKnownLocation() {
        return MapsUtils.mLastKnownLocation;
    }

    static void setmLastKnownLocation(Location mLastKnownLocation) {
        MapsUtils.mLastKnownLocation = mLastKnownLocation;
    }
}
