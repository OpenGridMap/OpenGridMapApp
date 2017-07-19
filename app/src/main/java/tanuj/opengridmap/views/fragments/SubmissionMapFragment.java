package tanuj.opengridmap.views.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;


public class SubmissionMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String KEY_SUBMISSION_ID = "submission_id";

    private MapView mapView;

    private GoogleMap map;

    private Submission submission;

    public SubmissionMapFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SubmissionMapFragment newInstance(long submissionId) {
        SubmissionMapFragment fragment = new SubmissionMapFragment();

        Bundle args = new Bundle();

        args.putLong(KEY_SUBMISSION_ID, submissionId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submission_map, container, false);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getContext());
        submission = dbHelper.getSubmission(getArguments().getLong(KEY_SUBMISSION_ID));
        dbHelper.close();

        mapView = (MapView) view.findViewById(R.id.map);

        mapView.onCreate(savedInstanceState);

        if (mapView != null)
            mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        populateMap();
        map.getUiSettings().setScrollGesturesEnabled(false);
    }

    private void populateMap() {
        Location location = submission.getImage(0).getLocation();

        if (location != null) {
            LatLng point;
            point = new LatLng(location.getLatitude(), location.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
            map.addMarker(new MarkerOptions().position(point).title(
                    submission.getPowerElementTagsString())).showInfoWindow();
        }
    }
}