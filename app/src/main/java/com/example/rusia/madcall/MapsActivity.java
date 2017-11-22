package com.example.rusia.madcall;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/*import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.ResultSet;

import org.apache.jena.query.*;

import virtuoso.jena.driver.*;*/


public class      MapsActivity
       extends    AppCompatActivity
       implements OnMapReadyCallback,
                  GoogleMap.OnMarkerClickListener,
                  GoogleMap.OnMyLocationButtonClickListener,
                  GoogleMap.OnMyLocationClickListener {

    // Keys for storing activity state
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Constants
    private final static String LOG_TAG = "MADCALL";
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    // Design & Layout
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private LinearLayout mInfoBox;
    private ImageView mInfoImage;
    private TextView mInfoDescription;

    // Google Map API -related
    private final LatLng mDefaultLocation = new LatLng(40.432643D, -3.704951D);
    private boolean mLocationPermissionGranted;
    private GoogleMap mMap;
    private Location mLastKnownLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean mInfoBoxOpen;

    // Jena API -related
    //private final static String VIRTUOSO_SERVER_URL = "jdbc:virtuoso://localhost:8890";

    /**
     * Starts the application.
     * @param savedInstanceState The last saved instance of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            //*CameraPosition mCameraPosition = *//*savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Associate a layout file to the activity.
        setContentView(R.layout.activity_maps);

        // Set the customized Action Bar as the default one.
        Toolbar mToolbar = findViewById(R.id.sidebar_actionbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Obtain the Sidebar Drawer Menu and set the listener for opening and closing.
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        // Obtain the menu button and set its behaviour
        FloatingActionButton mMenuButton = findViewById(R.id.fab_menu);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.START);
            }
        });

        // Obtain the InfoBox and set its toggle variable to false (infobox is closed at startup).
        mInfoBox = findViewById(R.id.infobox_container);
        mInfoImage = findViewById(R.id.infobox_image);
        mInfoDescription = findViewById(R.id.infobox_description);
        mInfoBoxOpen = false;

        if(!isConnected(this))
            buildDialog(this).show();

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
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
    public AlertDialog.Builder buildDialog(Context context) {

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

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        mMap.addMarker(new MarkerOptions().position(new LatLng(40.406164D, -3.740124D)));

        mMap.setOnMarkerClickListener(this);
        // Plot few markers
        //String rdfFilename = "/sampledata/MadCall_Model.rdf";
    }

    /**
     * Prompts the user for permission to use the device location.
     * The result of the permission request is handled by a callback,
     * @see onRequestPermissionsResult
     */
    private void getLocationPermission() {
        if (getPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                }, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Simple wrapper to improve code formatting and readability.
     */
    private int getPermission(String permissionType) {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(), permissionType);
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
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

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    /**
     * This method allows the sidebar navigation menu to toggle when touched.
     * @param item The item of the menu that has been clicked.
     * @return -
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the user clicks on his position.
     */
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    /**
     * This method is called when the user clicks on MyLocation button.
     */
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // (For now) Return false so that we don't consume the event and the default behavior still
        // occurs (i.e.the camera animates to the user's current position).
        return false;
    }

    /**
     * This method is called when the user clicks on a marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        if (mInfoBoxOpen) {
            Toast.makeText(this, "InfoBox was open: closing it.", Toast.LENGTH_SHORT)
                    .show();
            mInfoBoxOpen = false;
            mInfoBox.setVisibility(View.GONE);
            //mDrawerLayout.closeDrawer(Gravity.START);
        } else {
            Toast.makeText(this, "InfoBox was closed: opening it.", Toast.LENGTH_SHORT)
                    .show();
            mInfoBoxOpen = true;
            mInfoBox.setVisibility(View.VISIBLE);
            //mDrawerLayout.openDrawer(Gravity.START);

            //TODO: retrieve information for the appropriate marker and display it
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }
}
