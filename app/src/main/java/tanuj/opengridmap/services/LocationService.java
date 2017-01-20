package tanuj.opengridmap.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.plus.Plus;

import java.util.Timer;
import java.util.TimerTask;

import tanuj.opengridmap.utils.LocationUtils;

public class LocationService extends Service implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = LocationService.class.getSimpleName();

    public static final String LOCATION_UPDATE_BROADCAST = "tanuj.opengridmap.broadcast.location";

    private static final long INTERVAL = 10;

    private static final long FASTEST_INTERVAL = 5;

    private GoogleApiClient googleApiClient;

    private LocationRequest locationRequest;

    private LocationSettingsRequest.Builder locationSettingsRequestBuilder;

    private Location location;

    private final IBinder iBinder = new LocalBinder();

    private int bindingsCount = 0;

    private Intent intent;

    private boolean receivingLocationUpdates = false;

    private boolean resolvingShutDown = false;

    private int externalIntentNo = 0;

    public LocationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

//        if (!LocationUtils.isLocationEnabled(getApplicationContext())) {
//            stopSelf();
//        }


        if (isGooglePlayServicesAvailable()) {
            Log.d(TAG, "Google Play Services Available");
        } else {
            Log.d(TAG, "Google Play Services Not Available");
        }

        createLocationRequest();
        buildGoogleApiClient();

        buildLocationSettingsRequest();

        intent = new Intent(LOCATION_UPDATE_BROADCAST);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCmd");

        connectToGoogleApiClient();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bind");
        processBinding();
        connectToGoogleApiClient();
        return iBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Rebind");
        processBinding();
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind");
        processUnbinding();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");

        if (googleApiClient.isConnected()) {
            stopLocationUpdates();
        }

        disconnectFromGoogleApiClient();
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
//        Log.v(TAG, "Location Updated, location : " + location.toString());
        this.location = location;
        sendLocationBroadcast(location);
        resolveServiceShutdown();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed : " + connectionResult.toString());
    }

    public boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        return status == ConnectionResult.SUCCESS;
    }

    private void buildLocationSettingsRequest() {
        locationSettingsRequestBuilder = new LocationSettingsRequest.Builder();
        locationSettingsRequestBuilder.addLocationRequest(locationRequest);
        locationSettingsRequestBuilder.setAlwaysShow(true);
    }

    public PendingResult<LocationSettingsResult> getLocationSettingsPendingResult() {
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.
                checkLocationSettings(googleApiClient, locationSettingsRequestBuilder.build());
        Log.d(TAG, "Awaiting Location Settings Check");

        return result;
    }

    private void connectToGoogleApiClient() {
        Log.d(TAG, "Connecting to Google API Client.......");
        if (null != googleApiClient && !googleApiClient.isConnected() &&
                !googleApiClient.isConnecting()) {
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

    public void startLocationUpdates() {
        if (googleApiClient != null && googleApiClient.isConnected() &&
                LocationUtils.isLocationEnabled(this) && !receivingLocationUpdates) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest,
                    this);
            Log.d(TAG, "Starting Location Updates");
            receivingLocationUpdates = true;
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            Log.d(TAG, "Stopping Location Updates");
            receivingLocationUpdates = false;
        }
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
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public Location getLocation() {
        return location;
    }

    public class LocalBinder extends Binder {
        public LocationService getServiceInstance() {
//            if (locationService == null) {
//                locationService = LocationService.this;
//            }
//
//            return locationService;
            return LocationService.this;
        }
    }

    public void sendLocationBroadcast(Location location) {
        intent.putExtra("location", location);
        sendBroadcast(intent);
    }

    private void processBinding() {
        bindingsCount++;
        Log.d(TAG, "Binding Count : " + bindingsCount);
    }

    private void processUnbinding() {
        bindingsCount--;
        resolveServiceShutdown();
    }

    public void handleExternalIntent() {
        Log.d(TAG, "ExternalIntent");
        externalIntentNo++;
        processBinding();
        final int currentExternalIntentNo = externalIntentNo;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!resolvingShutDown && externalIntentNo == currentExternalIntentNo) {
                    shutdownService();
                }
            }
        }, 40000);
    }

    public void handleExternalIntentResult() {
        Log.d(TAG, "ExternalIntentResult");
        processUnbinding();
    }

    public void resolveServiceShutdown() {
        Log.d(TAG, "Binding Count : " + bindingsCount);
        if (bindingsCount == 0 && !resolvingShutDown) {
            Log.d(TAG, "Initiating Service Shutdown Check");
            resolvingShutDown = true;

                new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            if (bindingsCount == 0) {
                                shutdownService();
                            } else {
                                Log.d(TAG, "Service Shutdown Cancelled");
                            }
                            resolvingShutDown = false;
                        }
                    }, 10000);
        }
    }

    private void shutdownService() {
//        stopLocationUpdates();
        Log.d(TAG, "Shutting down Service");
        stopSelf();
        Log.d(TAG, "Service Shutdown Successfully");
    }
}