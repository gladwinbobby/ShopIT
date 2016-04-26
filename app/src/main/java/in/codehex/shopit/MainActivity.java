package in.codehex.shopit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.codehex.shopit.util.AppController;
import in.codehex.shopit.util.Config;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    final int UPDATE_INTERVAL = 10000;
    final int FASTEST_INTERVAL = 5000;
    Toolbar toolbar;
    EditText editTag, editDistance;
    FloatingActionButton fab;
    Intent intent;
    Location location;
    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    String searchTag;
    double lat, lng;
    SharedPreferences sharedPreferences, favorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences(Config.pref, MODE_PRIVATE);
        favorite = getSharedPreferences(Config.favorite, MODE_PRIVATE);

        editTag = (EditText) findViewById(R.id.search_tag);
        editDistance = (EditText) findViewById(R.id.distance);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTag = editTag.getText().toString();
                float distance = Float.parseFloat(editDistance.getText().toString());

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("searchTag", searchTag);
                editor.putString("lat", String.valueOf(lat));
                editor.putString("lng", String.valueOf(lng));
                editor.putFloat("distance", distance);
                editor.apply();

                intent = new Intent(getApplicationContext(), ShopActivity.class);
                startActivity(intent);
            }
        });

        createLocationRequest();

        if (checkPlayServices())
            buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null)
            if (!googleApiClient.isConnected())
                googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient != null)
            if (googleApiClient.isConnected())
                googleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient != null)
            if (googleApiClient.isConnected())
                stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (googleApiClient != null)
            if (googleApiClient.isConnected())
                startLocationUpdates();
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location loc) {
        location = loc;
        lat = location.getLatitude();
        lng = location.getLongitude();
        processShops();
    }

    private void processShops() {
        Map<String, ?> keys = favorite.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String productName = entry.getValue().toString();
            getShopDetails(productName);
        }
    }

    private void getShopDetails(final String productName) {
        // volley string request to server with POST parameters
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                // parsing json response data
                try {
                    JSONArray array = new JSONArray(response);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);

                        String shopName = object.getString("shop_name");
                        double latitude = object.getDouble("latitude");
                        double longitude = object.getDouble("longitude");

                        processDistance(latitude, longitude, shopName, productName);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),
                        "Network error! Check your internet connection!",
                        Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to the register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "favorite");
                params.put("product_name", productName);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq);
    }

    private void processDistance(double latitude, double longitude, final String shopName
            , final String productName) {
        Location source = new Location("source");
        source.setLatitude(lat);
        source.setLongitude(lng);

        Location destination = new Location("destination");
        destination.setLatitude(latitude);
        destination.setLongitude(longitude);

        float distance = source.distanceTo(destination);

        if (distance <= 1000) {
            showNotification(shopName, productName);
        }
    }

    private void showNotification(String shopName, String productName) {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, UserFavoriteActivity.class), 0);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        long[] v = {500, 1000};

        Notification notification = new NotificationCompat.Builder(this)
                .setTicker(productName + " is in nearby shop!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(shopName)
                .setContentText(productName + " is available in " + shopName)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setSound(uri)
                .setVibrate(v)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private void startLocationUpdates() {
        // marshmallow runtime location permission
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(googleApiClient, locationRequest, this);
        } else if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_ACCESS_FINE_LOCATION);
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_user_favorite) {
            intent = new Intent(getApplicationContext(), UserFavoriteActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
