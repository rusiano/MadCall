package com.example.rusia.madcall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
    private static final String ESDBPEDIA_SPARQL_ENDPOINT_URL = "http://es.dbpedia.org/sparql";
    private static final String DEFAULT_GRAPH_IRI = "http://localhost:8890/Callejero_6";

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
                                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                            "SELECT DISTINCT ?name ?label ?lat ?lng " +
                            "WHERE { " +
                                "?street <http://dbpedia.org/ontology/name> ?name. " +
                                "?street rdfs:label ?label. " +
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
                                String label = binding.getLiteral("label").getString();
                                double lat = Double.parseDouble(
                                        binding.getLiteral("lat").getString());
                                double lng = Double.parseDouble(
                                        binding.getLiteral("lng").getString());

                                Marker newMarker = MapsActivity.getmMap().addMarker(
                                        new MarkerOptions().position(new LatLng(lat, lng)));

                                newMarker.setTag(new MarkerData(name, label));
                                Log.wtf("MADCALL", "New Marker: " + name + " (" + label + ")");
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

        final Context ctx = activity.getApplicationContext();

        // Slide in the infobox (bottomsheet) and inflate the corresponding layout.
        final BottomSheetLayout mBottomSheet = activity.findViewById(R.id.bottomsheet);
        mBottomSheet.showWithSheetView(LayoutInflater.from(ctx).inflate(
                R.layout.infobox, mBottomSheet, false));

        // Retrieve infobox picture, title and description boxes to fill in later
        final ImageView infoboxPicture = mBottomSheet.findViewById(R.id.infobox_picture);
        final TextView infoboxTitle = mBottomSheet.findViewById(R.id.infobox_title);
        final TextView infoboxSubtitle = mBottomSheet.findViewById(R.id.infobox_subtitle);
        final TextView infoboxDescription = mBottomSheet.findViewById(R.id.infobox_description);

        // Retrieve the clicked marker
        final MarkerData markerData = (MarkerData) marker.getTag();
        if (markerData == null)
            return false;

        // Change the inflated layout so to display all the data associated with the marker
        final String streetName = markerData.getName();
        final String streetLabel = markerData.getLabel();
        infoboxTitle.setText(streetName);
        infoboxSubtitle.setText(streetLabel);

        // Define the thread in which to query the rdf to retrieve additional useful information
        // about the selected street
        Thread queriesTread = new Thread(new Runnable() {

            @Override
            public void run() {

                //
                // First query:
                // retrieve the dbpedia uri of the resource referenced by the selected street
                //
                //Log.wtf("MADCALL", "Queried Street name: " + streetName);
                String queryString =
                        "SELECT DISTINCT ?dbres " +
                                "WHERE {" +
                                "?street <http://dbpedia.org/ontology/name> '" + streetName + "'. " +
                                "?street <http://dbpedia.org/ontology/namedAfter> ?dbres." +
                                "}";

                Log.wtf("MADCALL", "Street name: " + streetName);
                Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
                QueryExecution awsSparqlService = QueryExecutionFactory.sparqlService(
                        AWS_SPARQL_ENDPOINT_URL, query, DEFAULT_GRAPH_IRI);

                ResultSet results = awsSparqlService.execSelect();
                String resUri = "";
                while (results.hasNext()) {
                    QuerySolution binding = results.nextSolution();
                    resUri = binding.get("dbres").toString();
                    Log.wtf("MADCALL", "Dbpedia Res. URI: " + resUri);
                    break;
                }

                // If no querable URI is available, return
                if (resUri.equals("") || resUri.contains("disambiguation")) {
                    Log.wtf("MADCALL", "[Thread " + Thread.currentThread().getId() +
                            "] No querable URI found.");
                    infoboxDescription.setText(R.string.no_description_found);
                    return;
                }

                //
                // Second query:
                // use the retrieved uri to query dbpedia rdf and get additional information
                //

                String queryString2 =
                        "PREFIX dbo: <http://dbpedia.org/ontology/> " +
                                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                                "SELECT DISTINCT ?abstract ?abstract2 ?thumbnail ?thumbnail2 ?label " +
                                "WHERE { " +
                                "OPTIONAL { <" + resUri + "> dbo:abstract ?abstract. } " +
                                "OPTIONAL { <" + resUri + "> dbo:thumbnail ?thumbnail. } " +
                                "OPTIONAL { <" + resUri + "> dbo:wikiPageRedirects ?redirect. " +
                                "           ?redirect rdfs:label ?label. " +
                                "           ?redirect dbo:abstract ?abstract2. " +
                                "           OPTIONAL { ?redirect dbo:thumbnail ?thumbnail2. } } " +
                                "}";

                Query query2 = QueryFactory.create(queryString2, Syntax.syntaxARQ);
                QueryExecution dbpediaSparqlService = QueryExecutionFactory.sparqlService(
                        ESDBPEDIA_SPARQL_ENDPOINT_URL, query2);

                final ResultSet results2 = dbpediaSparqlService.execSelect();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        QuerySolution binding2 = null;
                        while (results2.hasNext()) {
                            binding2 = results2.nextSolution();
                            break;
                        }

                        if (binding2 == null)
                            return;

                        // Display the abstract
                        Literal resAbstract = binding2.getLiteral("abstract");
                        Literal resAbstract2 = binding2.getLiteral("abstract2");
                        Literal resName2 = binding2.getLiteral("label");
                        String resAbstractString;
                        // If no abstract is available, return
                        if (resAbstract == null && resAbstract2 == null) {
                            Log.wtf("MADCALL", "No abstract found");
                            infoboxDescription.setText(R.string.no_description_found);
                            return;
                        }

                        if (resAbstract != null) {
                            resAbstractString = resAbstract.toString();
                            if (!resAbstractString.equals(""))
                                infoboxDescription.setText(resAbstractString);

                        } else {
                            infoboxTitle.setText(resName2.toString());
                            resAbstractString = resAbstract2.getString();
                            if (!resAbstractString.equals(""))
                                infoboxDescription.setText(resAbstractString);

                        }

                        // Display the image if an abstract has been found
                        final RDFNode resPictureResult = binding2.get("thumbnail");
                        final RDFNode resPictureResult2 = binding2.get("thumbnail2");

                        if (resPictureResult == null && resPictureResult2 == null)
                            return;

                        final String resPictureString = (resPictureResult != null) ?
                                resPictureResult.toString() :
                                resPictureResult2.toString();

                        if (resPictureString.equals(""))
                            return;

                        Log.wtf("MADCALL", "Picture URL: " + resPictureString);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    URL resPictureUrl = new URL(resPictureString);
                                    HttpURLConnection httpconn = (HttpURLConnection)
                                            resPictureUrl.openConnection();
                                    httpconn.setInstanceFollowRedirects(false);
                                    resPictureUrl = new URL(
                                            httpconn.getHeaderField("Location"));
                                    URLConnection connection = resPictureUrl.openConnection();

                                    final Bitmap resBm = BitmapFactory.decodeStream(
                                            connection.getInputStream());

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            infoboxPicture.setImageBitmap(resBm);
                                            infoboxPicture.setVisibility(View.VISIBLE);
                                        }
                                    });

                                } catch (MalformedURLException mue) {
                                    mue.printStackTrace();
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        }).start();
                    }
                });
            }

        });

        // IMPORTANT:
        // set this exception handler to avoid the app crashing on device when invalid uri is hit
        queriesTread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.wtf("MADCAL", "Thread was successfully interrupted.");
            }
        });
        // Start the thread
        queriesTread.start();

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }



}