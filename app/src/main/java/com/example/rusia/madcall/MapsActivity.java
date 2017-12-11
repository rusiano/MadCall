package com.example.rusia.madcall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.rusia.madcall.design.CustomSlidingPaneLayout;
import com.example.rusia.madcall.fragment.NearMeFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class      MapsActivity
       extends    AppCompatActivity
       implements OnMapReadyCallback {

    private boolean mLocationPermissionGranted;

    private SupportMapFragment mMapFragment;
    private static GoogleMap mMap;


    /**
     * Starts the application.
     * @param savedInstanceState The last saved instance of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    @SuppressLint("ClickableViewAccessibility")
    private void setupLayoutAndDesign() {
        // Associate a layout file to the activity.
        setContentView(R.layout.activity_maps);

        // Obtain the SupportFragmentManager.
        FragmentManager mFragmentManager = getSupportFragmentManager();

        // Obtain the Sliding Pane Layout.
        CustomSlidingPaneLayout mSlidingPaneLayout = findViewById(R.id.sliding_pane_layout);

        // Obtain the map fragment
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Since the master pane it is initially empty,
        // set one default layout and close it
        mFragmentManager.beginTransaction()
                .add(R.id.master_pane, new NearMeFragment()).commit();
        mSlidingPaneLayout.closePane();

        // Obtain the option buttons
        FloatingActionButton[] optionsButtons = new FloatingActionButton[]{
                findViewById(R.id.fab_near_me),
                findViewById(R.id.fab_search),
                findViewById(R.id.fab_settings)};

        // Initialize the option buttons listener.
        ButtonListener optionButtonsListener =
                new ButtonListener(this, mFragmentManager);

        // Set the listener to the menu button and all the option buttons.
        findViewById(R.id.fab_menu).setOnClickListener(optionButtonsListener);
        for (FloatingActionButton button : optionsButtons) {
            button.setOnClickListener(optionButtonsListener);
            button.setOnLongClickListener(optionButtonsListener);
            button.setOnTouchListener(optionButtonsListener);
        }
    }

    public static GoogleMap getmMap() {
        return mMap;
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

    //
    // THESE METHODS MUST BE IN THIS CLASS!
    //

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Activate MyLocation service
        activateMyLocation();

        /*// If after the UI update the MyLocation layer is still not enabled, re-try asking for permission
        if (!mMap.isMyLocationEnabled())
            getLocationPermission();*/

        // Instantiate a MapHelper object
        MapHelper mMapHelper = new MapHelper(this);

        // Setup the (customized) layout of the map.
        mMapHelper.setupMapLayout(mMap);

        // Center the camera on the last known position of the device.
        mMapHelper.getDeviceLastKnownLocation(mMap, true);

        // Instantiate and set a listener for gestures on map
        mMap.setOnCameraMoveStartedListener(mMapHelper);
        mMap.setOnMarkerClickListener(mMapHelper);
        mMap.setOnCameraMoveListener(mMapHelper);
        mMap.setOnCameraIdleListener(mMapHelper);

    }

    /**
     * Checks for permission to access the device location.
     * ONLY In case this permission is not locally available, it prompts the user for permission.
     * The result of this permission request is handled by a callback,
     * @see onRequestPermissionsResult
     */
    void getLocationPermission() {

        int locationPermissionCode = ContextCompat.checkSelfPermission(
                this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (locationPermissionCode == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            Log.wtf(MapHelper.LOG_TAG, "permission was already granted");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MapHelper.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }

    /**
     * Handles the result of the request for location permissions.
     * PLEASE NOTICE this method is called only if the permission is not available locally.
     * In our case, this method is generally avoided.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case MapHelper.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }

        activateMyLocation();
    }

    private void activateMyLocation() {
        if (mMap == null)
            return;

        // Allow for the blue dot of user current location only if location permissions are granted.
        try {
            mMap.setMyLocationEnabled(mLocationPermissionGranted);
            Log.wtf(MapHelper.LOG_TAG, "My Location Enabled: " + mLocationPermissionGranted);
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public boolean ismLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }
}