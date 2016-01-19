package tanuj.opengridmap.views.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.LocationService;
import tanuj.opengridmap.services.UploadSubmissionService;
import tanuj.opengridmap.utils.ConnectivityUtils;
import tanuj.opengridmap.utils.LocationUtils;

public class SubmitActivityFragment extends Fragment implements View.OnClickListener, ResultCallback<LocationSettingsResult> {

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

    private ImageView imageView;

    private TextView feedbackTextView;

    private ProgressBar locationQualityIndicator;

    private CircularProgressButton submitButton;

    private CircularProgressButton retryButton;

    private static LocationService locationService;

    private Submission submission;

    private boolean locationServiceBindingStatus = false;

    private long submissionId;

    private boolean uploadComplete = false;

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

            Log.v(TAG, location.toString());
        }
    };

    private BroadcastReceiver uploadUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            processUploadUpdate(intent.getLongExtra(getString(R.string.key_submission_id), -1),
                    intent.getShortExtra(getString(R.string.key_upload_completion), (short) -1));
        }
    };

    public SubmitActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submit, container, false);

        imageView = (ImageView) view.findViewById(R.id.image_preview);
        feedbackTextView = (TextView) view.findViewById(R.id.location_feedback);
        locationQualityIndicator = (ProgressBar) view.findViewById(R.id.location_quality_indicator);
        submitButton = (CircularProgressButton) view.findViewById(R.id.submit_button);
        retryButton = (CircularProgressButton) view.findViewById(R.id.retry_button);

        Intent intent = getActivity().getIntent();

        location = intent.getParcelableExtra(getString(R.string.key_location_start));

        if (location == null) {
            location = intent.getParcelableExtra(getString(R.string.key_location_result));
            checkLocationSettings();
//            LocationUtils.checkLocationSettingsOrLaunchSettingsIntent(getActivity());
        }

        powerElementId = intent.getLongExtra(getString(R.string.key_power_element_id), -1);
        imageSrc = intent.getStringExtra(getString(R.string.key_image_src));

        submitButton.setOnClickListener(this);
        retryButton.setOnClickListener(this);

        submitButton.setIndeterminateProgressMode(true);

        setOptimizedImageBitmap(imageSrc);
        setLocationFeedback();

        return view;
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
        unbindLocationService();
        Context context = getActivity();
        context.unregisterReceiver(locationUpdateBroadcastReceiver);
        context.unregisterReceiver(uploadUpdateBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindLocationService();
        Context context = getActivity();
        context.registerReceiver(locationUpdateBroadcastReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE_BROADCAST));
        context.registerReceiver(uploadUpdateBroadcastReceiver,
                new IntentFilter(UploadSubmissionService.UPLOAD_UPDATE_BROADCAST));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(getString(R.string.key_power_element_id), powerElementId);
        outState.putLong(getString(R.string.key_submission_id), submissionId);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submit_button: {
                if (!uploadComplete) {
                    if (ConnectivityUtils.isInternetConnected(getActivity()))
                        submit();
                    else {
                        feedbackTextView.setText(R.string.no_internet);
                        submitButton.setProgress(-1);
                        retryButton.setVisibility(View.GONE);

                        Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                    }
                }
                else finish();
                break;
            }
            case R.id.retry_button: {
                if (!uploadComplete) {
                    location = null;
                    launchCamera();
                }
                else finish();
                break;
            }
            case R.id.fragment_submit_layout: {
                if (uploadComplete) finish();
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Context context = getActivity();
        locationService.handleExternalIntentResult();

        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                if (location != null)
                    location = locationService.getLocation();

                setOptimizedImageBitmap(imageSrc);
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
        getActivity().finish();
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
        String msg = "";
        if (locationStatus > LOCATION_STATUS_NOT_AVAILABLE) {
            msg += getString(R.string.accuracy) + getString(R.string.colon) + String.format("%.2f",
                    location.getAccuracy()) + getString(R.string.meter) +
                    getString(R.string.newline);
        }

        switch (locationStatus) {
            case LOCATION_STATUS_NOT_AVAILABLE: {
                msg += getString(R.string.location_not_available);
                break;
            }
            case LOCATION_STATUS_NOT_ACCEPTABLE: {
                msg += getString(R.string.location_accuracy_not_acceptable);
                break;
            }
            case LOCATION_STATUS_OK: {
                msg += getString(R.string.location_accuracy_not_ideal);
                break;
            }
            case LOCATION_STATUS_GOOD: {
                msg += getString(R.string.location_accuracy_good);
                break;
            }
            case LOCATION_STATUS_EXCELLENT: {
                msg += getString(R.string.location_accuracy_excellent);
                break;
            }
        }
        return msg;
    }

    private void setLocationFeedback() {
        String locationFeedback = getLocationFeedbackMsg(location);
        int locationStatus = getLocationStatus(location);

        feedbackTextView.setText(locationFeedback);

        if (locationStatus > LOCATION_STATUS_NOT_ACCEPTABLE) {
            submitButton.setEnabled(true);
        } else {
            submitButton.setEnabled(false);
        }

        if (locationStatus < LOCATION_STATUS_EXCELLENT) {
            retryButton.setEnabled(true);
            retryButton.setVisibility(View.VISIBLE);
        } else {
            retryButton.setEnabled(false);
            retryButton.setVisibility(View.GONE);
        }
        setLocationQualityIndicator();
    }

    private void processUploadUpdate(long submissionId, short uploadCompletion) {
        if (this.submissionId != submissionId) return;

        switch (uploadCompletion) {
            case UploadSubmissionService.UPLOAD_STATUS_SUCCESS: {
                submitButton.setClickable(true);
                retryButton.setEnabled(false);
                retryButton.setVisibility(View.GONE);
                feedbackTextView.setText(R.string.upload_complete);

//                ((LinearLayout) submitButton.getParent()).setOnClickListener(this);

                uploadComplete = true;
                break;
            }
            case UploadSubmissionService.UPLOAD_STATUS_FAIL: {
                feedbackTextView.setText(R.string.upload_failed);
                submitButton.setClickable(true);
                retryButton.setEnabled(false);
                retryButton.setVisibility(View.GONE);
                break;
            }
            case UploadSubmissionService.SUBMISSION_NOT_FOUND: {
                feedbackTextView.setText(R.string.upload_submission_error);
                submitButton.setClickable(false);
                retryButton.setEnabled(true);
                retryButton.setVisibility(View.VISIBLE);
                break;
            }
            case UploadSubmissionService.NO_INTERNET_CONNECTIVITY: {
                feedbackTextView.setText(R.string.no_internet);
                submitButton.setClickable(true);
                submitButton.setEnabled(true);
                break;
            }
            case UploadSubmissionService.LOW_MEMORY: {
                feedbackTextView.setText(R.string.upload_failed);
                submitButton.setClickable(true);
                submitButton.setEnabled(true);
                break;
            }
        }

        if (uploadCompletion < -1)
            uploadCompletion = -1;

        submitButton.setProgress(uploadCompletion);
    }

    public void setOptimizedImageBitmap(String src) {
        File file = new File(src);
        Bitmap bitmap = null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        options.inDensity = 1;

        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        imageView.setImageBitmap(bitmap);
    }

    private void submit() {
        submitButton.setProgress(0);
        submitButton.setProgress(1);
        submitButton.setClickable(false);
        retryButton.setEnabled(false);
        retryButton.setVisibility(View.GONE);

        Context context = getActivity();

        if (submission == null) {
            submission = new Submission(context);
            submission.addPowerElementById(context, powerElementId);
            submission.addImage(context, new Image(getNewFileName(), location));
            submission.confirmSubmission(context);
            submissionId = submission.getId();
        } else if (submissionId > 0) {
            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
            submission = dbHelper.getSubmission(submissionId);
            dbHelper.close();
        }

        UploadSubmissionService.startUpload(context, submission.getId());
        feedbackTextView.setText(getString(R.string.upload_in_progress));
    }

    private void launchCamera() {
        if (submission != null)
            submission.getImage(0).delete(getActivity());

        submitButton.setProgress(0);

        Uri fileUri = getOutputMediaFileUri();
        Log.d(TAG, fileUri.toString());

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        locationService.handleExternalIntent();
        location = locationService.getLocation();

        startActivityForResult(cameraIntent, 100);
    }

    private String getNewFileName() {
        File from = new File(imageSrc);
        File storageDir = new File(getActivity().getExternalFilesDir(""), "images");
        File to = new File(storageDir.getPath() + File.separator +
                String.valueOf(System.currentTimeMillis()) + ".jpg");

        String path = null;
        if (from.exists()) {
            path = from.renameTo(to)? to.getPath() : from.getPath();
        }
        return path;
    }

    private Uri getOutputMediaFileUri(){
        return Uri.fromFile(new File(imageSrc));
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
//                process();
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
}