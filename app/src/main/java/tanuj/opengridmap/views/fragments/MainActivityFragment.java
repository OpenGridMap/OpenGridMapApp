package tanuj.opengridmap.views.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;

import tanuj.opengridmap.R;
import tanuj.opengridmap.views.activities.SubmitActivity;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.services.LocationService;
import tanuj.opengridmap.utils.LocationUtils;
import tanuj.opengridmap.utils.ServiceUtils;
import tanuj.opengridmap.views.adapters.PowerElementsGridAdapter;

public class MainActivityFragment extends Fragment implements
        ResultCallback<LocationSettingsResult> {
    public static final String TAG = MainActivityFragment.class.getSimpleName();

    private static final short STATE_DEFAULT = 0;

    private static final short STATE_CAMERA_LAUNCH_CHECK = 1;

    private static final short STATE_LAUNCH_CAMERA = 2;

    private static final short STATE_SUBMIT = 3;

    private static final short REQUEST_CHECK_SETTINGS = 101;

    public static final short REQUEST_CAMERA = 100;

    private short state = STATE_DEFAULT;

    private static LocationService locationService;

    private boolean locationServiceBindingStatus = false;

    private Location locationStart;

    private Location locationResult;

    private long powerElementId = -1;

    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "LocationService Connected");
            LocationService.LocalBinder localBinder = (LocationService.LocalBinder) service;
            locationService = localBinder.getServiceInstance();
            locationServiceBindingStatus = true;

            checkLocationSettings();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "LocationService Disconnected");
            locationServiceBindingStatus = false;
            locationService = null;
        }
    };

    private BroadcastReceiver locationUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra("location");

            Log.v(TAG, location.toString());
        }
    };

    public MainActivityFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();

//        LocationUtils.checkLocationSettingsOrLaunchSettingsIntent(context);

        if (!ServiceUtils.isMyServiceRunning((ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE))) {
            Log.d(TAG, "Starting Location Service");
            Intent intent = new Intent(context, LocationService.class);
            context.startService(intent);
        } else {
            Log.d(TAG, "Location Service already active");
        }

        bindLocationService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final Context context = getActivity();

        GridView gridView = (GridView) rootView.findViewById(R.id.main_gridview);
        gridView.setAdapter(new PowerElementsGridAdapter(context,
                PowerElementsSeedData.powerElements));
        gridView.setOnItemClickListener(onItemClickListener);

        if (savedInstanceState != null && savedInstanceState.containsKey(getString(
                R.string.key_power_element_id))) {
            powerElementId = savedInstanceState.getLong(getString(R.string.key_power_element_id));
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
//        bindLocationService();
    }

    @Override
    public void onStop() {
//        unbindLocationService();
        super.onStop();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(locationUpdateBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(locationUpdateBroadcastReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE_BROADCAST));
    }

    @Override
    public void onDestroy() {
        unbindLocationService();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(getString(R.string.key_power_element_id), powerElementId);
        super.onSaveInstanceState(outState);
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            powerElementId = PowerElementsSeedData.powerElements.get(position).getId();
            state = STATE_CAMERA_LAUNCH_CHECK;

            process();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Context context = getActivity();

        switch (requestCode) {
            case REQUEST_CAMERA: {
                locationService.handleExternalIntentResult();

                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        LocationUtils.checkLocationSettingsOrLaunchSettingsIntent(context);
                        locationResult = locationService.getLocation();
                        state = STATE_SUBMIT;
                        process();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        state = STATE_DEFAULT;
                        Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default: {
                        state = STATE_DEFAULT;
                        Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                break;
            }
            case REQUEST_CHECK_SETTINGS: {
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        if (locationService != null)
                            locationService.startLocationUpdates();

                        if (state == STATE_CAMERA_LAUNCH_CHECK)
                            state = STATE_LAUNCH_CAMERA;

                        process();

                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        checkLocationSettings();
                        break;
                    }
                }
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResult(LocationSettingsResult result) {
        final Status status = result.getStatus();

        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS: {
                process();
                break;
            }
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                try {
                    status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {}
                break;
            }
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                break;
            }
        }
    }

    private void process() {
        switch (state) {
            case STATE_DEFAULT: {
                break;
            }
            case STATE_CAMERA_LAUNCH_CHECK: {
                if (LocationUtils.isLocationEnabled(getActivity())) {
                    state = STATE_LAUNCH_CAMERA;
                    process();
                } else {
                    checkLocationSettings();
                }
                break;
            }
            case STATE_LAUNCH_CAMERA: {
                launchCamera();
                break;
            }
            case STATE_SUBMIT: {
                submit();
                break;
            }
        }
    }

    private void checkLocationSettings() {
        if (!LocationUtils.isLocationEnabled(getActivity())) {
            if (locationService == null) {
                Log.d(TAG, "Location Service Null");
                return;
            }

            Log.d(TAG, "Checking Location Settings");
            locationService.getLocationSettingsPendingResult().setResultCallback(this);
        }
    }

    private void launchCamera() {
//        LocationUtils.checkLocationSettingsOrLaunchSettingsIntent(getActivity());
        Uri fileUri = getOutputMediaFileUri();
        Log.d(TAG, fileUri.toString());

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        locationService.handleExternalIntent();
        locationStart = locationService.getLocation();

        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private void submit() {
        Intent intent = new Intent(getActivity(), SubmitActivity.class);

        Log.d(TAG, "Power Element ID : " + powerElementId);

        intent.putExtra(getString(R.string.key_location_start), locationStart);
        intent.putExtra(getString(R.string.key_location_result), locationResult);
        intent.putExtra(getString(R.string.key_power_element_id), powerElementId);
        intent.putExtra(getString(R.string.key_image_src), getOutputMediaFile().getAbsolutePath());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        powerElementId = -1;

        startActivity(intent);
    }

    private Uri getOutputMediaFileUri(){
        return Uri.fromFile(getOutputMediaFile());
    }

    private File getOutputMediaFile(){
        File storageDir = new File(getActivity().getExternalFilesDir(""), "images");

        if (!storageDir.exists()){
            if (!storageDir.mkdirs()){
                return null;
            }
        }

        return new File(storageDir.getPath() + File.separator + "TEMP_IMG.jpg");
    }

    protected void bindLocationService() {
        if (!locationServiceBindingStatus) {
            Log.v(TAG, "Binding Location Service");
            Context context = getActivity();
            Intent locationServiceIntent = new Intent(context, LocationService.class);
            context.bindService(locationServiceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindLocationService() {
        if (locationServiceBindingStatus) {
            getActivity().unbindService(locationServiceConnection);
            locationServiceBindingStatus = false;
        }
    }

    public void onUserLeaveHint() {
        if (locationService != null)
            locationService.resolveServiceShutdown();
    }
}