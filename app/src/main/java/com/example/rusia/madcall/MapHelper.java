package com.example.rusia.madcall;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Resource;

import static com.example.rusia.madcall.MapHelper.CameraConstant.DEFAULT_BOUNDS;
import static com.example.rusia.madcall.MapHelper.CameraConstant.DEFAULT_ZOOM;

/**
 * Created by rusia on 24/11/2017.
 * Useful methods to handle MapActivity tasks.
 */

class MapHelper
        implements  GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraIdleListener{

    static final String LOG_TAG = "MADCALL";
    private static final String AWS_SPARQL_ENDPOINT_URL
            = "http://ec2-54-208-226-156.compute-1.amazonaws.com/sparql";
    private static final String DEFAULT_GRAPH_IRI = "http://localhost:8890/Callejero_4";

    static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final float DFLT_COMPASS_BTN_ROT = -45f;


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

    // Google Map API related
    private static Location           mLastKnownLocation;

    private MapsActivity activity;
    private Context ctx;
    private boolean isCameraMapCentered, isCameraMapStraight, wasMapMovedByUser;

    private FloatingActionButton mMyLocationButton, mMyOrientationButton;

    private double currentMinLat;
    private double currentMaxLat;
    private double currentMinLng;
    private double currentMaxLng;

    private Resource subj;
    public String algo;
    public String abs;

    MapHelper(MapsActivity activity) {
        this.activity = activity;
        this.ctx = activity.getApplicationContext();
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
        mMyLocationButton = activity.findViewById(R.id.fab_location);
        mMyOrientationButton = activity.findViewById(R.id.fab_orientation);

        // Define the listener for our customized MyLocation button
        mMyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLastKnownLocation(mMap, true);
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
    void getDeviceLastKnownLocation(final GoogleMap mMap, final boolean centerCamera) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        // Construct a FusedLocationProviderClient: it allows to get the best location using
        //  a combination of information coming from Wifi/Mobile Data & GPS.
        FusedLocationProviderClient mFusedLocationProviderClient
                = LocationServices.getFusedLocationProviderClient(ctx);

        try {
            if (activity.ismLocationPermissionGranted()) {
                Log.wtf(LOG_TAG, "permission was granted");
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(activity, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        mLastKnownLocation = (task.isSuccessful()) ? task.getResult() : null;
                        Log.wtf(LOG_TAG, "last known location: " + mLastKnownLocation);
                        if (centerCamera)
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

    private boolean isCameraMapCentered() {
        return isCameraMapCentered;
    }

    private void setCameraMapCentered(boolean cameraMapCentered) {

        isCameraMapCentered = cameraMapCentered;

        // Update also the appearance of MyLocation button
        if (isCameraMapCentered) {
            mMyLocationButton.setClickable(false);
            mMyLocationButton.setBackgroundTintList(
                    activity.getResources().getColorStateList(R.color.colorPrimaryFaded));

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
                    activity.getResources().getColorStateList(R.color.colorPrimary));
        }
    }

    private boolean isCameraMapStraight() {
        return isCameraMapStraight;
    }

    private void setCameraMapStraight(boolean cameraMapStraight) {
        isCameraMapStraight = cameraMapStraight;

        // Update also the appearance of MyLocation button
        if (isCameraMapStraight) {
            mMyOrientationButton.setClickable(false);
            mMyOrientationButton.setBackgroundTintList(
                    activity.getResources().getColorStateList(R.color.colorPrimaryFaded));

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
                    activity.getResources().getColorStateList(R.color.colorPrimary));
        }
    }

    ///
    /// LISTENER'S METHODS
    ///

    @Override
    public void onCameraMoveStarted(int reason) {

        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            this.wasMapMovedByUser = true;
            if (isCameraMapCentered()) {
                // map is no more centered - highlight myLocationButton
                setCameraMapCentered(false);
            }
        }
    }

    @Override
    public void onCameraMove() {

        if (MapsActivity.getmMap().getCameraPosition().bearing != 0
                && this.wasMapMovedByUser) {
            if (isCameraMapStraight()) {
                setCameraMapStraight(false);
                return;
            }
            mMyOrientationButton.setRotation(
                    DFLT_COMPASS_BTN_ROT - MapsActivity.getmMap().getCameraPosition().bearing);
        }
    }

    @Override
    public void onCameraIdle() {

        GoogleMap mMap = MapsActivity.getmMap();

        // if camera was previously moved by the user and the map was rotated, adjust the compass
        //  icon orientation and save that the camera stopped moving
        if (mMap.getCameraPosition().bearing != 0 && this.wasMapMovedByUser) {
            mMyOrientationButton.setRotation(
                    DFLT_COMPASS_BTN_ROT - MapsActivity.getmMap().getCameraPosition().bearing);
            this.wasMapMovedByUser = false;
        }

        // if the map is too zoomed out, then do not show any street
        if (mMap.getCameraPosition().zoom < DEFAULT_ZOOM) {
            mMap.clear();
            return;
        }

        // otherwise, get the new bounds of the screen and load the streets that are in the
        //  currently visible region
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        currentMinLat = bounds.southwest.latitude;
        currentMaxLat = bounds.northeast.latitude;

        currentMinLng = bounds.southwest.longitude;
        currentMaxLng = bounds.northeast.longitude;

        // query for streets whose coordinates lay within the current visible region
        new Thread( new Runnable() {

            @Override
            public void run() {
                try  {

                    String queryString =
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                            "SELECT DISTINCT ?name ?lat ?lng " +
                            "WHERE { " +
                                "?street <http://dbpedia.org/ontology/name> ?name. " +
                                "?street <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat. " +
                                "?street <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lng. " +
                                "FILTER (xsd:double(?lat) < " + currentMaxLat + "). " +
                                "FILTER (xsd:double(?lat) > " + currentMinLat + "). " +
                                "FILTER (xsd:double(?lng) > " + currentMinLng + "). " +
                                "FILTER (xsd:double(?lng) < " + currentMaxLng + "). " +
                            "} " +
                            "LIMIT 50" ;

                    Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
                    QueryExecution exec = QueryExecutionFactory.sparqlService(
                            AWS_SPARQL_ENDPOINT_URL, query, DEFAULT_GRAPH_IRI ) ;

                    final ResultSet results = exec.execSelect() ;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            while (results.hasNext()) {
                                QuerySolution binding = results.nextSolution();

                                String name = binding.getLiteral("name").getString();
                                double lat = Double.parseDouble(
                                        binding.getLiteral("lat").getString());
                                double lng = Double.parseDouble(
                                        binding.getLiteral("lng").getString());

                                Marker newMarker = MapsActivity.getmMap().addMarker(
                                        new MarkerOptions().position(new LatLng(lat, lng)));

                                newMarker.setTag(new MarkerData(name, ""));
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     * This method is called when the user clicks on a marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        final MarkerData markerData = (MarkerData) marker.getTag();

        if (markerData == null)
            return false;
        final String nameAdd = markerData.getName();
        Context ctx = activity.getApplicationContext();
        final BottomSheetLayout mBottomSheet = activity.findViewById(R.id.bottomsheet);

        // Slide in the infobox (bottomsheet) and inflate the corresponding layout.
        mBottomSheet.showWithSheetView(LayoutInflater.from(ctx).inflate(
                R.layout.infobox, mBottomSheet, false));

        // Change the inflated layout so to display all the data associated with the marker
        TextView infoboxTitle = mBottomSheet.findViewById(R.id.infobox_title);
        infoboxTitle.setText(nameAdd);

        new Thread(new Runnable() {

            @Override
            public void run() {
                String queryString =
                            " SELECT DISTINCT ?dbres " +
                                    "  WHERE {" +
                                    " ?street <http://dbpedia.org/ontology/name> '" + nameAdd + "' . " +
                                    " ?street <http://dbpedia.org/ontology/namedAfter> ?dbres." +
                                    " }";

                    Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
                    QueryExecution exec = QueryExecutionFactory.sparqlService(
                            AWS_SPARQL_ENDPOINT_URL, query, DEFAULT_GRAPH_IRI);

                final ResultSet results = exec.execSelect();

                            while (results.hasNext()) {
                                QuerySolution binding = results.nextSolution();
                                algo =  binding.get("dbres").toString();

                            }


                    /*String queryString2 =
                            "prefix dbpedia-owl: <http://dbpedia.org/ontology/>" +
                                    "select ?abstract ?thumbnail where {" +
                                    " <" + subj[0] + "> dbpedia-owl:abstract ?abstract ;" +
                                    "                           dbpedia-owl:thumbnail ?thumbnail ." +
                                    "  filter(langMatches(lang(?abstract),\"en\"))" +
                                    "}";
*/                  System.out.println("DBPEDIA RESOURCE--->" + algo);
                String queryString2=
                        "prefix dbpedia-owl: <http://dbpedia.org/ontology/>" +
                                "select ?abstract  where { " +
                                "  <" + algo + "> dbpedia-owl:abstract ?abstract." +
                                "}";

                Query query2 = QueryFactory.create(queryString2, Syntax.syntaxARQ);
                QueryExecution exec2 = QueryExecutionFactory.sparqlService(
                        "http://es.dbpedia.org/sparql", query2);
                final ResultSet results2 = exec2.execSelect();
                abs=" --------------------- SORRY, TRY ANOTHER STREET ---------------------";
                        while (results2.hasNext()) {
                            QuerySolution binding2 = results2.nextSolution();

                            abs = (String) binding2.getLiteral("abstract").getString();
                            System.out.println(abs);
                        }

                markerData.setDescription(abs);
                TextView infoboxDescription = mBottomSheet.findViewById(R.id.infobox_description);
                infoboxDescription.setText(markerData.getDescription());

            }
        }).start();


/*
        TextView infoboxDescription = mBottomSheet.findViewById(R.id.infobox_description);
        infoboxDescription.setText(markerData.getDescription());

        Toast.makeText(ctx,
                "(" + marker.getPosition().latitude + ", " + marker.getPosition().longitude + ")",
                Toast.LENGTH_SHORT).show();
*/
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }
}