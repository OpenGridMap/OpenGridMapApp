package tanuj.opengridmap.views.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.File;

import tanuj.opengridmap.services.LocationService;
import tanuj.opengridmap.R;
import tanuj.opengridmap.SubmitActivity;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.utils.ServiceUtils;
import tanuj.opengridmap.views.adapters.PowerElementsGridAdapter;

public class MainActivityFragment extends Fragment {
    public static final String TAG = MainActivityFragment.class.getSimpleName();

    private LocationService locationService;

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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "LocationService Disconnected");
            locationServiceBindingStatus = false;
        }
    };

    private BroadcastReceiver locationUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra("location");

            Log.d(TAG, location.toString());
        }
    };

    public MainActivityFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
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
        bindLocationService();
    }

    @Override
    public void onStop() {
        unbindLocationService();
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(getString(R.string.key_power_element_id), powerElementId);
        super.onSaveInstanceState(outState);
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            powerElementId = PowerElementsSeedData.powerElements.get(position).getId();
            launchCamera();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Context context = getActivity();
        locationService.handleExternalIntentResult();

        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                locationResult = locationService.getLocation();
                submit();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        Uri fileUri = getOutputMediaFileUri();
        Log.d(TAG, fileUri.toString());

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        locationService.handleExternalIntent();
        locationStart = locationService.getLocation();

        startActivityForResult(cameraIntent, 100);
    }

    private void submit() {
        Intent intent = new Intent(getActivity(), SubmitActivity.class);

        Log.d(TAG, "Power Element ID : " + powerElementId);

        intent.putExtra(getString(R.string.key_location_start), locationStart);
        intent.putExtra(getString(R.string.key_location_result), locationResult);
        intent.putExtra(getString(R.string.key_power_element_id), powerElementId);
        intent.putExtra(getString(R.string.key_image_src), getOutputMediaFile().getAbsolutePath());

        powerElementId = -1;

        startActivity(intent);
    }

    private Uri getOutputMediaFileUri(){
        return Uri.fromFile(getOutputMediaFile());
    }

    private File getOutputMediaFile(){
        File storageDir = new File(getActivity().getExternalFilesDir(""), "images");

        Log.d(TAG, storageDir.getAbsolutePath());

        if (!storageDir.exists()){
            if (!storageDir.mkdirs()){
                return null;
            }
        }

        return new File(storageDir.getPath() + File.separator + "TEMP_IMG.jpg");
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
}