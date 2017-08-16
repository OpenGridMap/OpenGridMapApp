package tanuj.opengridmap.views.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.UploadService;
import tanuj.opengridmap.views.activities.SubmissionStatusActivity;
import tanuj.opengridmap.views.adapters.SubmissionsViewAdapter;

public class SubmissionsFragment extends Fragment implements SubmissionsViewAdapter.OnItemClickListener {
    private static final String TAG = SubmissionsFragment.class.getSimpleName();

    private static final int SUBMISSION_DETAILS = -1;

    private RecyclerView recyclerView;

    private StaggeredGridLayoutManager staggeredGridLayoutManager;

    private SubmissionsViewAdapter adapter;

    private OpenGridMapDbHelper dbHelper;

    private List<Submission> submissions;

    private BroadcastReceiver uploadUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            processSubmissionsUpdate();
        }
    };

    public SubmissionsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submissions, container, false);

        Context context = getActivity();
        dbHelper = new OpenGridMapDbHelper(context);

        staggeredGridLayoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        submissions = getSubmissions();
        recyclerView = view.findViewById(R.id.submissions_grid);
        adapter = new SubmissionsViewAdapter(context, submissions);

        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickLietener(this);

        return view;
    }

    private List<Submission> getSubmissions() {
        return dbHelper.getSubmissions(Submission.STATUS_INVALID);
    }

    @Override
    public void onDestroyView() {
        dbHelper.close();

        super.onDestroyView();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(uploadUpdateBroadcastReceiver);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        processSubmissionsUpdate();

        getActivity().registerReceiver(uploadUpdateBroadcastReceiver,
                new IntentFilter(UploadService.UPLOAD_UPDATE_BROADCAST));
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d(TAG, submissions.get(position).getSubmissionStatus(getContext()));

        Intent intent = new Intent(getContext(), SubmissionStatusActivity.class);
        intent.putExtra(getString(R.string.key_submission_id), submissions.get(position).getId());

        startActivity(intent);
    }

    private void processSubmissionsUpdate() {
        submissions = dbHelper.getSubmissions(Submission.STATUS_INVALID);
        adapter.setSubmissions(submissions);
        adapter.notifyDataSetChanged();
    }
}
