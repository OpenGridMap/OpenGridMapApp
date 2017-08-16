package tanuj.opengridmap.views.fragments;


import android.app.Activity;
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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.LocationService;
import tanuj.opengridmap.services.UploadService;
import tanuj.opengridmap.utils.ConnectivityUtils;
import tanuj.opengridmap.utils.FileUtils;
import tanuj.opengridmap.utils.LocationUtils;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;



public class SubmitActivityFragment extends Fragment implements View.OnClickListener,
        ResultCallback<LocationSettingsResult>, OnMapReadyCallback, GoogleMap.OnCameraChangeListener, GoogleMap.OnCameraMoveListener {
    private static final String TAG = SubmitActivityFragment.class.getSimpleName();

    private static final int LOCATION_STATUS_NOT_AVAILABLE = -1;
    private static final int LOCATION_STATUS_NOT_ACCEPTABLE = 0;
    private static final int LOCATION_STATUS_OK = 1;
    private static final int LOCATION_STATUS_GOOD = 2;
    private static final int LOCATION_STATUS_EXCELLENT = 3;

    private static final short REQUEST_CHECK_SETTINGS = 101;

    private Location location;

    private long powerElementId;

    private String imageSrc;

    private MapView mapView;

    private GoogleMap map;

    private TextView locationFeedbackTextView;

    private TextView locationAccuracyTextView;

    private TextView locationTextView;

    private FloatingActionButton submitButton;

    private FloatingActionButton retryButton;

    private ProgressBar locationQualityIndicator;

    private static LocationService locationService;

    private GcmNetworkManager gcmNetworkManager;

    private boolean locationServiceBindingStatus = false;

    private boolean uploadConfirmed = false;

    private boolean cameraRunning = false;

    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder localBinder = (LocationService.LocalBinder) service;
            locationService = localBinder.getServiceInstance();
            locationServiceBindingStatus = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationServiceBindingStatus = false;
            locationService = null;
        }
    };

    private BroadcastReceiver locationUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (location == null) {
                location = intent.getParcelableExtra("location");
                setLocationFeedback();
            }
        }
    };


    public SubmitActivityFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submit, container, false);

        mapView = view.findViewById(R.id.map);
        locationFeedbackTextView = view.findViewById(R.id.location_feedback);
        locationAccuracyTextView = view.findViewById(R.id.accuracy);
        locationTextView = view.findViewById(R.id.location);
        TextView powerElementTypeTextView = view.findViewById(R.id.power_element_type);
        submitButton = view.findViewById(R.id.submit_button);
        retryButton = view.findViewById(R.id.retry_button);
        locationQualityIndicator = view.findViewById(R.id.location_quality_indicator);

        Intent intent = getActivity().getIntent();

        location = intent.getParcelableExtra(getString(R.string.key_location_start));

        if (location == null) {
            location = intent.getParcelableExtra(getString(R.string.key_location_result));
            checkLocationSettings();
        }

        setLocationFeedback();

        powerElementId = intent.getLongExtra(getString(R.string.key_power_element_id), -1);
        imageSrc = intent.getStringExtra(getString(R.string.key_image_src));

        submitButton.setOnClickListener(this);
        retryButton.setOnClickListener(this);

        mapView.onCreate(savedInstanceState);

        if (mapView != null)
            mapView.getMapAsync(this);

        if (powerElementId > 0) {
            String powerElementName = PowerElementsSeedData.powerElements.get(
                    (int) (powerElementId - 1)).getName();
            powerElementTypeTextView.setText(getString(R.string.type_format, powerElementName));
        }

        gcmNetworkManager = GcmNetworkManager.getInstance(getContext());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showIntroTutorial();
    }

    @Override
    public void onStart() {
        super.onStart();
        bindLocationService();
    }

    @Override
    public void onStop() {
//        unbindLocationService();
        super.onStop();
    }

    @Override
    public void onPause() {
        unbindLocationService();
        Context context = getActivity();
        context.unregisterReceiver(locationUpdateBroadcastReceiver);
//        context.unregisterReceiver(uploadUpdateBroadcastReceiver);

        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindLocationService();
        Context context = getActivity();
        context.registerReceiver(locationUpdateBroadcastReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE_BROADCAST));
//        context.registerReceiver(uploadUpdateBroadcastReceiver,
//                new IntentFilter(UploadService.UPLOAD_UPDATE_BROADCAST));

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (NullPointerException e) {
                Log.e(TAG, "Error while attempting MapView.onDestroy(), ignoring exception", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(getString(R.string.key_power_element_id), powerElementId);
        outState.putLong(getString(R.string.key_power_element_id), powerElementId);
        outState.putString(getString(R.string.key_image_src), imageSrc);

        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submit_button: {
                submit();
                break;
            }
            case R.id.retry_button: {
                launchCamera();
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Context context = getActivity();
        locationService.handleExternalIntentResult();

        if (requestCode == 100) {
            cameraRunning = false;
            if (resultCode == Activity.RESULT_OK) {
                if (location != null)
                    location = locationService.getLocation();

                setLocationFeedback();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void finish() {
        Log.d(TAG, "finish");
        FragmentActivity activity = getActivity();
        if (activity != null) activity.finish();
    }

    protected void bindLocationService() {
        if (!locationServiceBindingStatus) {
            final Context context = getActivity();
            Intent locationServiceIntent = new Intent(context, LocationService.class);
            context.bindService(locationServiceIntent, locationServiceConnection,
                    Context.BIND_AUTO_CREATE);
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

    public void onFinish() {
        if (uploadConfirmed)
            return;

        File file = new File(imageSrc);
        boolean deleted = file.delete();

        Log.d(TAG, "File " + imageSrc + " deleted : " + deleted);
    }

    private int getLocationStatus(Location location) {
        if (location == null)
            return LOCATION_STATUS_NOT_AVAILABLE;

        int accuracy = (int) location.getAccuracy();

        if (accuracy < 20) {
            return LOCATION_STATUS_EXCELLENT;
        } else if (accuracy < 50) {
            return LOCATION_STATUS_GOOD;
        } else if (accuracy < 100) {
            return LOCATION_STATUS_OK;
        } else {
            return LOCATION_STATUS_NOT_ACCEPTABLE;
        }
    }

    private String getLocationFeedbackMsg(Location location) {
        int locationStatus = getLocationStatus(location);

        switch (locationStatus) {
            case LOCATION_STATUS_EXCELLENT: {
                return getString(R.string.location_accuracy_excellent);
            }
            case LOCATION_STATUS_GOOD: {
                return getString(R.string.location_accuracy_good);
            }
            case LOCATION_STATUS_OK: {
                return getString(R.string.location_accuracy_not_ideal);
            }
            case LOCATION_STATUS_NOT_ACCEPTABLE: {
                return getString(R.string.location_accuracy_not_acceptable);
            }
            default: {
                return getString(R.string.location_not_available);
            }
        }
    }

    private void setLocationFeedback() {
        int locationStatus = getLocationStatus(location);

        locationFeedbackTextView.setText(getLocationFeedbackMsg(location));
        locationAccuracyTextView.setText(getString(R.string.accuracy_format, location.getAccuracy()));
        locationTextView.setText(getString(R.string.location_format, LocationUtils.toLocationStringInDegrees(location, getContext())));

        if (locationStatus > LOCATION_STATUS_NOT_ACCEPTABLE) {
            submitButton.show();
        } else {
            submitButton.hide();
        }

        if (locationStatus < LOCATION_STATUS_EXCELLENT) {
            retryButton.show();
        } else {
            retryButton.hide();
        }
        setLocationQualityIndicator();
    }

    private void submit() {
        if (uploadConfirmed)
            return;

        retryButton.hide();
        submitButton.hide();
        disableMap();

        Context context = getActivity();

        Submission submission = new Submission(context);
        submission.addPowerElementById(context, powerElementId);
        submission.addImage(context, new Image(getNewFileName(), location));
        submission.confirmSubmission(context);

        Log.d(TAG, "Submission ID : " + submission.getId());
        Log.d(TAG, String.valueOf(submission.getImages()));

        UploadService.scheduleUpload(submission.getId(), gcmNetworkManager, context);
        showOnConfirmMessage();
        uploadConfirmed = true;
    }

    private void launchCamera() {
        if (cameraRunning)
            return;

        Uri fileUri = FileUtils.getOutputMediaFileUri(getActivity(), new File(imageSrc));
        Log.d(TAG, fileUri.toString());

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        locationService.handleExternalIntent();
        location = locationService.getLocation();

        startActivityForResult(cameraIntent, 100);
        cameraRunning = true;
    }

    private String getNewFileName() {
        return imageSrc;
    }

    private void setLocationQualityIndicator() {
        int accuracy = getLocationStatus(location) > LOCATION_STATUS_NOT_AVAILABLE ?
                (int) location.getAccuracy() : 0;

        accuracy = 110 - accuracy;
        if (accuracy < 0)
            accuracy = 0;

        locationQualityIndicator.setProgress(accuracy);
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

    @Override
    public void onResult(LocationSettingsResult result) {
        final Status status = result.getStatus();

        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS: {
                break;
            }
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                try {
                    status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
            }
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                break;
            }
        }
    }

    private void showIntroTutorial() {
        final Activity activity = getActivity();
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(300);

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(activity, "submit_activity_intro");

        sequence.setConfig(config);

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(activity)
                        .setTarget(mapView)
                        .setDismissText("Got It")
                        .setContentText("You can see the point on the map.")
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(activity)
                        .setTarget(locationQualityIndicator)
                        .setDismissText("Got It")
                        .setContentText("This bar indicates the location accuracy")
                        .withRectangleShape(true)
                        .setDismissOnTouch(true)
                        .build()
        );

        if (retryButton.getVisibility() == View.VISIBLE) {
            sequence.addSequenceItem(
                    new MaterialShowcaseView.Builder(activity)
                            .setTarget(retryButton)
                            .setDismissText("Got It")
                            .setContentText("Click here to retake picture.")
                            .setDismissOnTouch(true)
                            .build()
            );
        }

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(activity)
                        .setTarget(mapView)
                        .setDismissText("Got It")
                        .setContentText("You can also edit the location by dragging the map.")
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(activity)
                        .setTarget(submitButton)
                        .setDismissText("Got It")
                        .setContentText("Click here to submit the point")
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.start();
    }

    public void showOnConfirmMessage() {
        int statusStringId = ConnectivityUtils.isWifiOnly(getContext()) ?
                R.string.upload_in_progress_wifi_only : R.string.upload_in_progress;

        Snackbar.make(submitButton, statusStringId, Snackbar.LENGTH_INDEFINITE)
                .setAction("Done", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                })
                .show();


        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnCameraChangeListener(this);
        map.setOnCameraMoveListener(this);

        LatLng point;
        if (location != null) {
            point = new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            point = new LatLng(48, 11);
        }

        map.moveCamera(CameraUpdateFactory.newLatLng(point));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.d(TAG, "onCameraChange");
        handleMapPositionChange(cameraPosition);
    }

    @Override
    public void onCameraMove() {
        Log.d(TAG, "onCameraMove");
        handleMapPositionChange();
    }

    private void handleMapPositionChange() {
        handleMapPositionChange(map.getCameraPosition());
    }

    private void handleMapPositionChange(CameraPosition cameraPosition) {
        Location location = new Location("");
        location.setLatitude(cameraPosition.target.latitude);
        location.setLongitude(cameraPosition.target.longitude);
        location.setAccuracy(8);

        setLocationFeedback();

        this.location = location;
    }

    private void disableMap() {
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setScrollGesturesEnabled(false);
        uiSettings.setZoomGesturesEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
    }
}
