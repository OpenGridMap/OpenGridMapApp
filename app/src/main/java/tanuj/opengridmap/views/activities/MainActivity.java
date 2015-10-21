package tanuj.opengridmap.views.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import tanuj.opengridmap.R;
import tanuj.opengridmap.SettingsActivity;

public class MainActivity extends AppCompatActivity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult> {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final long INTERVAL = 10;
    private static final long FASTEST_INTERVAL = 5;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest.Builder locationSettingsRequestBuilder;

    public static int noOfLocationUpdates = 0;

//    private MainActivityFragment fragment;

//    private MockLocationProvider mockLocationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getApplicationContext();

        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!locationEnabled) {
            Toast.makeText(context, "Please Enable High Accuracy Mode in Location Settings", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        buildLocationSettingsRequest();

//        FragmentManager fragmentManager = getSupportFragmentManager();
//        fragment = (MainActivityFragment) fragmentManager.findFragmentById(R.id.fragment);

        if (isGooglePlayServicesAvailable()) {
            Log.d(TAG, "Google Play Services Available");
        } else {
            Log.d(TAG, "Google Play Services Not Available");
        }

        createLocationRequest();
        buildGoogleApiClient();

//        Intent intent = new Intent(context, UploadService.class);
//        startService(intent);
//        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(LocationSettingsResult locationSettingsResult) {
//                final Status status = locationSettingsResult.getStatus();
//                final LocationSettingsStates locationSettingsStates= locationSettingsResult.getLocationSettingsStates();
//                switch (status.getStatusCode()) {
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        Log.d(TAG, "LOCATION SUCCESS");
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        Log.d(TAG, "LOCATION PERMISSION REQUIRED");
//                        try {
//                            // Show the dialog by calling startResolutionForResult(),
//                            // and check the result in onActivityResult().
//                            status.startResolutionForResult(
//                                    MainActivity.this,
//                                    1000);
//                        } catch (IntentSender.SendIntentException e) {
//                            // Ignore the error.
//                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        Log.d(TAG, "LOCATION UNAVAILABLE");
//                        break;
//                }
//            }
//        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_submissions: {
                Intent intent = new Intent(getApplicationContext(), SubmissionsActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_about: {
                Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectToGoogleApiClient();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectFromGoogleApiClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            startLocationUpdates();
        }
        noOfLocationUpdates = 0;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Google API Services Connected");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google API Services Connected");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location Updated, location : " + location.toString());
        if (noOfLocationUpdates++ >= 5) {
            disconnectFromGoogleApiClient();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed : " + connectionResult.toString());
    }

    public boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 9).show();
            return false;
        }
    }

    private void buildLocationSettingsRequest() {
        locationSettingsRequestBuilder = new LocationSettingsRequest.Builder();
        locationSettingsRequestBuilder.addLocationRequest(locationRequest);
        locationSettingsRequestBuilder.setAlwaysShow(true);
    }

    private void connectToGoogleApiClient() {
        Log.d(TAG, "Connecting to Google API Client.......");
        if (null != googleApiClient) {
            googleApiClient.connect();
            Log.d(TAG, "Connected to Google API Client");
        }
    }

    private void disconnectFromGoogleApiClient() {
        Log.d(TAG, "Disconnecting from Google API Client.......");
        if (null != googleApiClient && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            Log.d(TAG, "Disconnected from Google API Client");
        }
    }

    private void startLocationUpdates() {
//        checkLocationSettings();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequestBuilder.build());
        Log.d(TAG, "Awaiting Location Settings Check");
        result.setResultCallback(this);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest,
                this);
        Log.d(TAG, "Starting Location Updates");
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        Log.d(TAG, "Stopping Location Updates");
    }

    public void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        Log.d(TAG, "Location Settings Check Complete");
        Log.d("STATUS", status.toString());

        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS: {
                Toast.makeText(getApplication(), "LOCATION SUCCESS", Toast.LENGTH_SHORT).show();
                break;
            }
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                Toast.makeText(getApplication(), "LOCATION REQUIRED", Toast.LENGTH_SHORT).show();
                try {
//                status.startResolutionForResult(getApplicationContext(), REQUEST_CHECK_SETTINGS);
                    status.startResolutionForResult((Activity) getApplicationContext(), 1000);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
            }
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                Toast.makeText(getApplication(), "LOCATION UNAVAILABLE", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

//    protected void checkLocationSettings() {
//        Log.d(TAG, "Checking Location Settings");
//        LocationServices.SettingsApi.checkLocationSettings(
//                googleApiClient,
//                locationSettingsRequestBuilder.build()
//        ).setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(LocationSettingsResult locationSettingsResult) {
//                final Status status = locationSettingsResult.getStatus();
//                switch (status.getStatusCode()) {
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        Log.i(TAG, "All location settings are satisfied.");
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
//                                "upgrade location settings ");
//
//                        try {
//                            status.startResolutionForResult(MainActivity.this, 1000);
//                        } catch (IntentSender.SendIntentException e) {
//                            Log.i(TAG, "PendingIntent unable to execute request.");
//                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
//                                "not created.");
//                        break;
//                }
//            }
//        });
//    }


}