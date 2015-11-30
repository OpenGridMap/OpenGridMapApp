package tanuj.opengridmap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.File;

import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.LocationService;
import tanuj.opengridmap.services.UploadSubmissionService;

public class SubmitActivityFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = SubmitActivityFragment.class.getSimpleName();

    private static final int LOCATION_STATUS_NOT_AVAILABLE = -1;
    private static final int LOCATION_STATUS_NOT_ACCEPTABLE = 0;
    private static final int LOCATION_STATUS_OK = 1;
    private static final int LOCATION_STATUS_GOOD = 2;
    private static final int LOCATION_STATUS_EXCELLENT = 3;

    private Location location;

    private long powerElementId;

    private String imageSrc;

    private ImageView imageView;

    private TextView locationFeedbackTextView;

    private ProgressBar locationQualityIndicator;

    private CircularProgressButton submitButton;

    private CircularProgressButton retryButton;

    private LocationService locationService;

    private boolean locationServiceBindingStatus = false;

    private long submissionId;

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
        }
    };

    private BroadcastReceiver locationUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (location == null) {
                location = intent.getParcelableExtra("location");
                setLocationFeedback();
            }

            Log.d(TAG, location.toString());
        }
    };

    private BroadcastReceiver uploadUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            processUploadUpdate(intent.getLongExtra(getString(R.string.key_submission_id), -1),
                    intent.getIntExtra(getString(R.string.key_upload_completion), -1));
        }
    };

    public SubmitActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submit, container, false);

        imageView = (ImageView) view.findViewById(R.id.image_preview);
        locationFeedbackTextView = (TextView) view.findViewById(R.id.location_feedback);
        locationQualityIndicator = (ProgressBar) view.findViewById(R.id.location_quality_indicator);
        submitButton = (CircularProgressButton) view.findViewById(R.id.submit_button);
        retryButton = (CircularProgressButton) view.findViewById(R.id.retry_button);

        Intent intent = getActivity().getIntent();

        location = intent.getParcelableExtra(getString(R.string.key_location_start));
        if (location == null) {
            location = intent.getParcelableExtra(getString(R.string.key_location_result));
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
        bindLocationService();
    }

    @Override
    public void onStop() {
        unbindLocationService();
        super.onStop();
    }

    @Override
    public void onPause() {
        Context context = getActivity();

        context.unregisterReceiver(locationUpdateBroadcastReceiver);
        context.unregisterReceiver(uploadUpdateBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getActivity();

        context.registerReceiver(locationUpdateBroadcastReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE_BROADCAST));
        context.registerReceiver(uploadUpdateBroadcastReceiver,
                new IntentFilter(UploadSubmissionService.UPLOAD_UPDATE_BROADCAST));
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

    protected void bindLocationService() {
        final Context context = getActivity();
//        if (!locationServiceBindingStatus) {
            Intent locationServiceIntent = new Intent(context, LocationService.class);
            context.bindService(locationServiceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE);
//        }
    }

    private void unbindLocationService() {
        if (locationServiceBindingStatus) {
            getActivity().unbindService(locationServiceConnection);
        }
    }

    public void onUserLeaveHint() {
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
            msg += "Accuracy  : " + String.format("%.2f", location.getAccuracy()) + "m\n";
        }

        switch (locationStatus) {
            case LOCATION_STATUS_NOT_AVAILABLE: {
                msg += "Location not available";
                break;
            }
            case LOCATION_STATUS_NOT_ACCEPTABLE: {
                msg += "Accuracy too low";
                break;
            }
            case LOCATION_STATUS_OK: {
                msg += "Accuracy not the best...Try again?";
                break;
            }
            case LOCATION_STATUS_GOOD: {
                msg += "Good data...Try again for better accuracy?";
                break;
            }
            case LOCATION_STATUS_EXCELLENT: {
                msg += "Nice Work!!!...Excellent accuracy";
                break;
            }
        }
        return msg;
    }

    private void setLocationFeedback() {
        String locationFeedback = getLocationFeedbackMsg(location);
        int locationStatus = getLocationStatus(location);

        locationFeedbackTextView.setText(locationFeedback);

        if (locationStatus > LOCATION_STATUS_NOT_AVAILABLE) {
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

    private void processUploadUpdate(long submissionid, int uploadCompletion) {
        if (this.submissionId != submissionid) return;

        if (uploadCompletion == 100) {
            submitButton.setEnabled(false);
            retryButton.setEnabled(false);
            retryButton.setVisibility(View.GONE);
        } else if (uploadCompletion == -1) {
            submitButton.setText("Retry Upload");
            submitButton.setEnabled(true);
            retryButton.setEnabled(false);
        }

        submitButton.setProgress(uploadCompletion);
    }

    public void setOptimizedImageBitmap(String src) {
        File file = new File(src);
        Bitmap bitmap = null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        options.inDensity = 1;

        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        imageView.setImageBitmap(bitmap);
    }

    private void submit() {
        submitButton.setProgress(1);
        Context context = getActivity();

        Submission submission = new Submission(context);
        submission.addPowerElementById(context, powerElementId);
        submission.addImage(context, new Image(getNewFileName(), location));
        submission.confirmSubmission(context);

        submissionId = submission.getId();

        UploadSubmissionService.startUpload(context, submission.getId());
    }

    private void launchCamera() {
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
                String.valueOf(System.currentTimeMillis()));

        if (from.exists()) {
            return from.renameTo(to)? to.getPath() : null;
        }
        return null;
    }

    private Uri getOutputMediaFileUri(){
        return Uri.fromFile(new File(imageSrc));
    }

    private void setLocationQualityIndicator() {
        int accuracy = (int) location.getAccuracy();

        accuracy = 110 - accuracy;
        if (accuracy < 0)
            accuracy = 0;

        locationQualityIndicator.setProgress(accuracy);
    }
}
