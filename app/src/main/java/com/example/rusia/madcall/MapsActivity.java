package com.example.rusia.madcall;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.rusia.madcall.design.CustomSlidingPaneLayout;
//import com.example.rusia.madcall.fragment.NearMeFragment;
import com.example.rusia.madcall.fragment.AdvancedSearchFragment;
import com.example.rusia.madcall.fragment.NearMeFragment;
import com.example.rusia.madcall.fragment.SettingsFragment;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class      MapsActivity
       extends    AppCompatActivity
       implements OnMapReadyCallback,
                  GoogleMap.OnMarkerClickListener,
                  GoogleMap.OnCameraMoveStartedListener,
                  GoogleMap.OnCameraMoveListener,
                  GoogleMap.OnCameraIdleListener {

    // Keys for storing activity state
    private static final String     KEY_LOCATION = "location";

    // Design & Layout
    private CustomSlidingPaneLayout mSlidingPaneLayout;
    private BottomSheetLayout       mBottomSheet;
    private FloatingActionButton    mMyLocationButton, mMenuButton;
    private SupportMapFragment      mMapFragment;
    //private NearMeFragment      mMasterPaneFragment;

    // Others
    private static boolean          mLocationPermissionGranted;
    private GoogleMap               mMap;


    /**
     * Starts the application.
     * @param savedInstanceState The last saved instance of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case there is a previously saved instance of the app, retrieve information from it.
        if (savedInstanceState != null) {
            MapsUtils.setmLastKnownLocation(
                    (Location) savedInstanceState.getParcelable(KEY_LOCATION));
        }

        // Set up all the variables related to the layout.
        setupLayoutAndDesign();

        // In case the user has no internet connection, show an Alert Dialog.
        if(!isConnected(this))
            buildConnectionAlertDialog(this).show();

        // Get notified when the map is ready to be used.
        mMapFragment.getMapAsync(this);
    }

    /**
     * Put here all initializations/declarations that regard layout and design elements.
     */
    private void setupLayoutAndDesign() {
        // Associate a layout file to the activity.
        setContentView(R.layout.activity_maps);

        // Obtain the Sliding Pane Layout object
        mSlidingPaneLayout = findViewById(R.id.sliding_pane_layout);

        // Since the master pane it is initially empty and open,
        // set one default layout and close it
        getSupportFragmentManager().beginTransaction()
                .add(R.id.master_pane, new NearMeFragment()).commit();
        mSlidingPaneLayout.closePane();

        // Obtain the customized MyLocation button and the other buttons
        mMyLocationButton = findViewById(R.id.fab_location);

        // Obtain the menu button and define its behavior
        final boolean[] isMenuOpen = {false};   // save button state
        mMenuButton = findViewById(R.id.fab_menu);
        mMenuButton.setOnClickListener(new View.OnClickListener() {

            // When the menu button is clicked show/hide all navigation buttons
            @Override
            public void onClick(View view) {
                if(isMenuOpen[0]) {
                    isMenuOpen[0] = false;
                    mMenuButton.setRotation(0);
                    findViewById(R.id.left_icons).setVisibility(View.GONE);
                } else {
                    isMenuOpen[0] = true;
                    mMenuButton.setRotation(90);
                    findViewById(R.id.left_icons).setVisibility(View.VISIBLE);
                }
            }
        });


        // Obtain the NearMe button and define its behavior
        final boolean[] isNearMeButtonPressed = {false};    // save button state
        FloatingActionButton mNearMeButton = findViewById(R.id.fab_near_me);
        mNearMeButton.setOnClickListener(new View.OnClickListener() {

            // When NearMe button is clicked open the master pane with NearMe layout
            @Override
            public void onClick(View view) {
                mSlidingPaneLayout.openPane();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.master_pane, new NearMeFragment()).commit();
                //TODO: change master pane layout accordingly
            }
        });
        mNearMeButton.setOnLongClickListener(new View.OnLongClickListener() {

            // When NearMe button is longclicked save state to catch later the button release
            @Override
            public boolean onLongClick(View view) {
                // Show the description of the button and save the state
                findViewById(R.id.fab_near_me_description).setVisibility(View.VISIBLE);
                isNearMeButtonPressed[0] = true;

                // Return true to consume the event and not trigger the simple click, too
                return true;
            }
        });
        mNearMeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // If search button is pressed and the user releases it, hide the button description
                if (isNearMeButtonPressed[0] && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    findViewById(R.id.fab_near_me_description).setVisibility(View.GONE);
                    isNearMeButtonPressed[0] = false;
                }
                return false;
            }
        });

        // Obtain the AdvancedSearch button and define its behavior
        final boolean[] isSearchButtonPressed = {false};    // save button state
        FloatingActionButton mSearchButton = findViewById(R.id.fab_search);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSlidingPaneLayout.openPane();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.master_pane, new AdvancedSearchFragment()).commit();
                //TODO: change master pane layout accordingly
            }
        });
        mSearchButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Show the description of the button and save the state
                findViewById(R.id.fab_search_description).setVisibility(View.VISIBLE);
                isSearchButtonPressed[0] = true;

                // Return true to consume the event and not trigger the simple click, too
                return true;
            }
        });
        mSearchButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // If search button is pressed and the user releases it, hide the button description
                if (isSearchButtonPressed[0] && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    findViewById(R.id.fab_search_description).setVisibility(View.GONE);
                    isSearchButtonPressed[0] = false;
                }
                return false;
            }
        });

        // Obtain the Settings button and define its behavior
        final boolean[] isSettingsButtonPressed = {false};    // save button state
        FloatingActionButton mSettingsButton = findViewById(R.id.fab_settings);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSlidingPaneLayout.openPane();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.master_pane, new SettingsFragment()).commit();
                //TODO: change master pane layout accordingly
            }
        });
        mSettingsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Show the description of the button and save the state
                findViewById(R.id.fab_settings_description).setVisibility(View.VISIBLE);
                isSettingsButtonPressed[0] = true;

                // Return true to consume the event and not trigger the simple click, too
                return true;
            }
        });
        mSettingsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // If search button is pressed and the user releases it, hide the button description
                if (isSettingsButtonPressed[0] && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    findViewById(R.id.fab_settings_description).setVisibility(View.GONE);
                    isSettingsButtonPressed[0] = false;
                }
                return false;
            }
        });

        // Obtain the InfoBox.
        mBottomSheet = findViewById(R.id.bottomsheet);

        // Obtain the SupportMapFragment (= map)
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
    }

    /*private void openMasterPaneLayout(int layout) {
        // Create new fragment and transaction
        mMasterPaneFragment = new NearMeFragment();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.master_pane, mMasterPaneFragment);

        *//*
         * Before you call commit(), however, you might want to call addToBackStack(), in order to
         * add the transaction to a back stack of fragment transactions. This back stack is managed
         * by the activity and allows the user to return to the previous fragment state,
         * by pressing the Back button.
         *//*
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }*/

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_LOCATION, MapsUtils.getmLastKnownLocation());
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Checks if either mobile data or wifi are turnt on.
     */
    public boolean isConnected(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = (cm != null) ?
                               cm.getActiveNetworkInfo() :
                               null;
        if (netInfo == null)
            return false;

        if (!netInfo.isConnectedOrConnecting())
            return false;

        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        return (mobile != null && mobile.isConnectedOrConnecting())
                || (wifi != null && wifi.isConnectedOrConnecting());
    }

    /**
     * Shows a simple Alert Dialog Box to inform the user about lack of connection.
     */
    public AlertDialog.Builder buildConnectionAlertDialog(Context context) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("No Internet Connection!");
        builder.setMessage("You must have either Mobile Data or Wifi on to have access to online " +
                "content. You can do it from Settings > Wireless & networks. ");

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!isConnected(MapsActivity.this))
                    Toast.makeText(MapsActivity.this,"No internet connection. " +
                            "Only offline content available.", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            }
        });

        return builder;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related controls on the map.
        MapsUtils.updateLocationUI(this, mMap, mMyLocationButton);

        // If after the UI update the MyLocation layer is still not enabled, re-try asking for permission
        if (!mMap.isMyLocationEnabled())
            getLocationPermission();

        // Get the current location of the device and set the position of the map.
        MapsUtils.getDeviceLocation(this, mMap);

        // TODO: display markers around device location

        // calle sepulveda
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(40.404482D, -3.739823D))
                .icon(MapsUtils.bitmapDescriptorFromVector(this,
                        R.drawable.ic_location_city_black_24dp)));

        MapsUtils.placeRandomMarkers(this, mMap, 200);

        mMap.setOnMarkerClickListener(this);

    }

    /**
     * Prompts the user for permission to use the device location.
     * The result of the permission request is handled by a callback,
     * @see onRequestPermissionsResult
     */
    void getLocationPermission() {
        if (getPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, MapsUtils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Simple wrapper to improve code formatting and readability.
     */
    private int getPermission(String permissionType) {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(), permissionType);
    }


    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case MapsUtils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }

        MapsUtils.updateLocationUI(this, mMap, mMyLocationButton);
    }

    /**
     * This method is called when the user clicks on a marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Slide in the infobox (bottomsheet) and inflate the corresponding layout.
        mBottomSheet.showWithSheetView(LayoutInflater.from(this).inflate(
                R.layout.infobox, mBottomSheet, false));

        Toast.makeText(this,
                "(" + marker.getPosition().latitude + ", " + marker.getPosition().longitude + ")",
                Toast.LENGTH_SHORT).show();

        //TODO: retrieve information for the appropriate marker and display it in the bottom sheet

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    //TODO: implement
    @Override
    public void onCameraIdle() {

    }

    //TODO: implement
    @Override
    public void onCameraMove() {

    }

    //TODO: implement
    @Override
    public void onCameraMoveStarted(int i) {

    }

    static boolean ismLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

}
