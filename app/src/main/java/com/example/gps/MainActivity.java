package com.example.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings.Secure;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Variables
    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;
    GeoJsonLayer layer;
    String currentLGA;
    boolean choroplethOn;
    boolean policeOn;

    private final Double currentLat = -37.9145;
    private final Double currentLong = 145.1350;
    private String userID;

    private String trackLink;

    private final static int REQUEST_CODE = 100;
    public final String APIKEY = BuildConfig.APIKEY;

    //Location changes to define LocationListener
    int LOCATION_REFRESH_TIME = 10000; // 10 seconds to update
    int LOCATION_REFRESH_DISTANCE = 50; // 50 metres to update
    LocationManager mLocationManager;

    // The geographical location where the device is currently located.
    private Location lastKnownLocation;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-37.8136, 144.9631);
    private static final int DEFAULT_ZOOM = 12;
    private boolean locationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    // Colors for Choropleth map
    public int lowestColor = Color.argb(125, 201, 111, 47);
    public int lowColor = Color.argb(125, 231, 176, 86);
    public int midColor = Color.argb(125, 255, 242, 143);
    public int highColor = Color.argb(125, 159, 197, 73);
    public int highestColor = Color.argb(125, 8, 152, 20);
    public int clearColor = Color.argb(0, 0, 0, 0);

    // Polygon Styles
    GeoJsonPolygonStyle highestStyle = new GeoJsonPolygonStyle();
    GeoJsonPolygonStyle highStyle = new GeoJsonPolygonStyle();
    GeoJsonPolygonStyle midStyle = new GeoJsonPolygonStyle();
    GeoJsonPolygonStyle lowStyle = new GeoJsonPolygonStyle();
    GeoJsonPolygonStyle lowestStyle = new GeoJsonPolygonStyle();
    GeoJsonPolygonStyle clearStyle = new GeoJsonPolygonStyle();

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // URL for police dataset
    public final String serverURL = "http://13.236.36.223/";
    public final String policeURL = serverURL + "police_lat_long";
    public final String ratingsURL = serverURL + "lga_ratings";
    public JSONArray lgaRatings;
    public JSONArray policeLatLng;
    List<Marker> mMarkers = new ArrayList<>();

    TextView lgaInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userID = Settings.Secure.getString(this.getContentResolver(),
        Settings.Secure.ANDROID_ID);

        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        FloatingActionButton fabDash = findViewById(R.id.fab2);
        ImageButton zoomInBtn = findViewById(R.id.zoomInBtn);
        ImageButton zoomOutBtn = findViewById(R.id.zoomOutBtn);
        lgaInfo = findViewById(R.id.currentLGA);

        highestStyle.setFillColor(highestColor);
        highestStyle.setStrokeWidth(3F);
        highestStyle.setStrokeColor(Color.BLACK);

        highStyle.setFillColor(highColor);
        highStyle.setStrokeWidth(3F);
        highStyle.setStrokeColor(Color.BLACK);

        midStyle.setFillColor(midColor);
        midStyle.setStrokeWidth(3F);
        midStyle.setStrokeColor(Color.BLACK);

        lowStyle.setFillColor(lowColor);
        lowStyle.setStrokeWidth(3F);
        lowStyle.setStrokeColor(Color.BLACK);

        lowestStyle.setFillColor(lowestColor);
        lowestStyle.setStrokeWidth(3F);
        lowestStyle.setStrokeColor(Color.BLACK);

        clearStyle.setFillColor(clearColor);
        clearStyle.setStrokeWidth(3F);
        clearStyle.setStrokeColor(Color.BLACK);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getLocation();
            }
        });

        // GoogleMap Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);

        // Autocomplete search
        Places.initialize(getApplicationContext(), APIKEY);
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setTypeFilter(TypeFilter.ADDRESS);
        autocompleteFragment.setLocationBias(RectangularBounds.newInstance(
                new LatLng(-38.388695, 144.015627),
                new LatLng(-37.366701, 145.783542)));
        autocompleteFragment.setCountries("AU");

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                changeLocation(latLng);
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, "No address matches", Toast.LENGTH_SHORT).show();
            }
        });

        // Zoom Buttons
        zoomInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });
        zoomOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        // Sending current LGA to webview
        fabDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Dashboard.class);
                intent.putExtra("lga_name", currentLGA);
                intent.putExtra("url", serverURL);
                startActivity(intent);
            }
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle("Melbourne Safety");
        setSupportActionBar(toolbar);

        // Coordinate tracking
        final int delay = 2000; //run every 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                sendLatLong();
            }
        }, delay);
    }

    private void sendLatLong() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = serverURL + "get_location?userID=" + userID + "&lat=" + currentLat +
                "&long=" + currentLong;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        trackLink = response;
                    }
                }, null);

        queue.add(stringRequest);
    }


    public void changeLocation(LatLng latLng){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(18)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_styles));

        // Adding the Choropleth Map layer
        try {
            layer = new GeoJsonLayer(mMap, R.raw.lga, this);

            // Getting JSON object from server
            getRatings(new VolleyCallBack() {
                @Override
                public void onSuccess() {
                    colorMap(layer);
                }
            });
            GeoJsonPolygonStyle polygonStyle = layer.getDefaultPolygonStyle();
            polygonStyle.setFillColor(clearColor);
            polygonStyle.setStrokeWidth(3F);
            polygonStyle.setStrokeColor(Color.BLACK);
            layer.addLayerToMap();
            choroplethOn = true;

            layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    currentLGA = feature.getProperty("NAME");
                    String info = "LGA: " + currentLGA;
                    lgaInfo.setText(info);

                }
            });
            //Click event for pressing a LGA polygon
//            layer.setOnFeatureClickListener((GeoJsonLayer.GeoJsonOnFeatureClickListener) feature ->
//                    currentLGA = feature.getProperty("NAME"));
////                    feature.setPolygonStyle(lowestStyle);
        }
        catch (IOException e){
            Log.e(TAG, "cannot be read");
            e.printStackTrace();
        }
        catch (JSONException e){
            Log.e(TAG, "cannot be converted to JSON object");
            e.printStackTrace();
        }

        policeOn = false;
        // Get police coordinates and add markers
        getPolice(new VolleyCallBack() {
            @Override
            public void onSuccess() {
                setPoliceStations();
            }
        });

        // Move the center of the camera to Melbourne CBD
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            // Set Location Button
            case R.id.action_favorite:
                setPoliceStations();
                return true;

            case R.id.toggleChoro:
                toggleChoropleth();
                return true;

            case R.id.copy_link:
                copyLinkToClipboard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void copyLinkToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Link", trackLink);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, clipboard.getPrimaryClip().toString(), Toast.LENGTH_LONG).show();
    }

    // Toggles the choropleth map on/off
    public void toggleChoropleth(){
        if(choroplethOn){
            clearMap();
            choroplethOn = false;
        }
        else {
            colorMap(layer);
            choroplethOn = true;
        }
    }

    // Makes the polygons transparent
    public void clearMap(){
        for (GeoJsonFeature f : layer.getFeatures()) {
            f.setPolygonStyle(clearStyle);
        }
    }


    // Colors each polygon(LGA) with the ratings from the JSON array
    public void colorMap(GeoJsonLayer layer){

        // For each (name: LGA, rating: int) pair:
        for (int p = 0; p < lgaRatings.length(); p++) {
            try {
                JSONObject data = lgaRatings.getJSONObject(p);
                String lga_name = data.getString("name");
                lga_name = lga_name.toLowerCase();
                int lga_score = data.getInt("rating");

                // For each LGA polygon:
                for (GeoJsonFeature f : layer.getFeatures()) {
                    String lga_string = f.getProperty("NAME");
                    lga_string = lga_string.toLowerCase();

                    if (lga_string.equals(lga_name)){
                        if (lga_score == 1)
                            f.setPolygonStyle(lowestStyle);
                        else if (lga_score == 2){
                            f.setPolygonStyle(lowStyle);
                        }
                        else if (lga_score == 3){
                            f.setPolygonStyle(midStyle);
                        }
                        else if (lga_score == 4){
                            f.setPolygonStyle(highStyle);
                        }
                        else if (lga_score == 5){
                            f.setPolygonStyle(highestStyle);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            Toast.makeText(MainActivity.this, Double.toString(location.getLatitude()), Toast.LENGTH_LONG).show();
            Toast.makeText(MainActivity.this, Double.toString(location.getLongitude()), Toast.LENGTH_LONG).show();

        }
    };


    public interface VolleyCallBack {
        void onSuccess();
    }


    // Retrieves LGA ratings (JSON array) from the server
    public void getRatings(final VolleyCallBack callBack){
        // Volley
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, ratingsURL, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    lgaRatings = response.getJSONArray("LGA");
                    callBack.onSuccess();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = null;
                if (error instanceof NetworkError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ServerError) {
                    message = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof AuthFailureError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ParseError) {
                    message = "Parsing error! Please try again after some time!!";
                } else if (error instanceof TimeoutError) {
                    message = "Connection TimeOut! Please check your internet connection.";
                }
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }


    // Retrieves police station coordinates (JSON array) from the server
    public void getPolice(final VolleyCallBack callBack){
        // Volley
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, policeURL, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    policeLatLng = response.getJSONArray("police_stations");
                    callBack.onSuccess();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = null;
                if (error instanceof NetworkError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ServerError) {
                    message = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof AuthFailureError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ParseError) {
                    message = "Parsing error! Please try again after some time!!";
                } else if (error instanceof TimeoutError) {
                    message = "Connection TimeOut! Please check your internet connection.";
                }
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    // Add police marks to the map
    public void setPoliceStations(){
        if (mMarkers.isEmpty()){
            BitmapDescriptor police_marker = bitmapDescriptorFromVector(this, R.drawable.ic_police_marker);
            for (int p = 0; p < lgaRatings.length(); p++) {
                try {
                    JSONObject data = policeLatLng.getJSONObject(p);
                    double latitude = data.getDouble("lat");
                    double longitude = data.getDouble("long");
                    String suburb = data.getString("name");
                    LatLng station = new LatLng(latitude, longitude);

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(station);
                    Marker marker = mMap.addMarker(markerOptions);
                    marker.setTitle("Police station in " + suburb);
                    marker.setIcon(police_marker);
                    mMarkers.add(marker);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            policeOn = true;
        }
        else if(policeOn){
            for (Marker marker: mMarkers) {
                marker.setVisible(false);
            }
            policeOn = false;
        }
        else if(!policeOn){
            for (Marker marker: mMarkers) {
                marker.setVisible(true);
            }
            policeOn = true;
        }
    }

    // Create bitmap from image icon
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}