package tanuj.opengridmap.views.fragments;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;
import java.util.List;

import tanuj.opengridmap.BuildConfig;
import tanuj.opengridmap.utils.FileUtils;
import tanuj.opengridmap.R;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.services.LocationService;
import tanuj.opengridmap.utils.LocationUtils;
import tanuj.opengridmap.utils.ServiceUtils;
import tanuj.opengridmap.views.activities.SubmitActivity;
import tanuj.opengridmap.views.adapters.PowerElementsRecyclerViewAdapter;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class MainActivityRecyclerViewFragment extends Fragment implements
        PowerElementsRecyclerViewAdapter.OnItemClickListener,
        ResultCallback<LocationSettingsResult> {
    private static final String TAG = MainActivityRecyclerViewFragment.class.getSimpleName();

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

//            Log.v(TAG, location.toString());
        }
    };

    private RecyclerView recyclerView;
    private StaggeredGridLayoutManager staggeredGridLayoutManager;
    private PowerElementsRecyclerViewAdapter adapter;
    private Toolbar toolbar;

    public MainActivityRecyclerViewFragment() {
        // Required empty public constructor
    }

    public static MainActivityRecyclerViewFragment newInstance() {
        MainActivityRecyclerViewFragment fragment = new MainActivityRecyclerViewFragment();
        return fragment;
    }

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_recycler_view, container, false);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        setUpActionBar();

        staggeredGridLayoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        recyclerView = (RecyclerView) view.findViewById(R.id.grid);
        adapter = new PowerElementsRecyclerViewAdapter(getActivity());

        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(this);

        bindLocationService();

        return view;
    }

    @Override
    public void onItemClick(View view, int position) {
        powerElementId = PowerElementsSeedData.powerElements.get(position).getId();
        state = STATE_CAMERA_LAUNCH_CHECK;

        process();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpActionBar() {
        if (toolbar != null) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                ActionBar actionBar = activity.getActionBar();

                if (actionBar != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        activity.setSupportActionBar(toolbar);
//                        activity.setActionBar(toolbar);
                        activity.getActionBar().setElevation(7);
                    }
                    actionBar.setDisplayHomeAsUpEnabled(false);
                    actionBar.setDisplayShowTitleEnabled(true);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showTutorial();
    }

    private void showTutorial() {
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // half second between each showcase view

        final Activity activity = getActivity();

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(activity, "main_activity_intro");

        sequence.setConfig(config);

        View view = getActivity().findViewById(R.id.intro);

        sequence.addSequenceItem(view, "Select the device you see", "Got it!");
        sequence.addSequenceItem(view, "Now we will launch the camera and you can capture an Image", "Got it!");

        sequence.start();
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
        unbindLocationService();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindLocationService();
        getActivity().registerReceiver(locationUpdateBroadcastReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE_BROADCAST));
    }

    @Override
    public void onDestroy() {
//        unbindLocationService();
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
                        checkLocationSettings();
//                        LocationUtils.checkLocationSettingsOrLaunchSettingsIntent(context);
                        locationResult = locationService.getLocation();
                        state = STATE_SUBMIT;
                        process();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        state = STATE_DEFAULT;
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
        Activity activity = getActivity();
        File file = FileUtils.getTempMediaFile(activity);
        Uri fileUri = FileUtils.getOutputMediaFileUri(activity, file);

        if (file != null && file.exists()) {
            file.delete();
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        List<ResolveInfo> resolvedIntentActivities = activity.getPackageManager().queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
            String packageName = resolvedIntentInfo.activityInfo.packageName;

            activity.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        locationService.handleExternalIntent();
        locationStart = locationService.getLocation();

        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private void submit() {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, SubmitActivity.class);
        String imageSrc = FileUtils.getTempMediaFile(activity).getAbsolutePath();

        Log.d(TAG, "Power Element ID : " + powerElementId);

        intent.putExtra(getString(R.string.key_location_start), locationStart);
        intent.putExtra(getString(R.string.key_location_result), locationResult);
        intent.putExtra(getString(R.string.key_power_element_id), powerElementId);
        intent.putExtra(getString(R.string.key_image_src), imageSrc);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        powerElementId = -1;

        startActivity(intent);
    }

    private Uri getOutputMediaFileUri(){
        Activity activity = getActivity();

        return FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider",
                FileUtils.getTempMediaFile(activity));
    }

//    private File getTempMediaFile(){
//        File storageDir = new File(getActivity().getExternalFilesDir(""), "images");
//
//        if (!storageDir.exists()){
//            if (!storageDir.mkdirs()){
//                return null;
//            }
//        }
//
//        return new File(storageDir.getPath() + File.separator + "TEMP_IMG.jpg");
//    }

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
