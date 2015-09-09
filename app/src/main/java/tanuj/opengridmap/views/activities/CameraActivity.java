package tanuj.opengridmap.views.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

import tanuj.opengridmap.R;
import tanuj.opengridmap.views.fragments.CameraActivityFragment;


public class CameraActivity extends Activity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final long INTERVAL = 5;

    private static final long FASTEST_INTERVAL = 1;

    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

    private GoogleApiClient googleApiClient;

    private LocationRequest locationRequest;

    private Location currentLocation;

    private String lastLoctionUpdateTime;

    private CameraActivityFragment fragment;

    private long startTime;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraActivityFragment.newInstance())
                    .commit();
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragment = (CameraActivityFragment) fragmentManager.findFragmentById(R.id.fragment);

        isGooglePlayServicesAvailable();

        createLocationRequest();
        buildGoogleApiClient();

        startTime = System.currentTimeMillis();
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
        this.currentLocation = location;
        lastLoctionUpdateTime = DateFormat.getTimeInstance().format(new Date());

        if (null != currentLocation) {
            CameraActivityFragment.setLocation(currentLocation, getApplicationContext());
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
}