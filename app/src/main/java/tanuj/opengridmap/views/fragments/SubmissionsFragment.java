package tanuj.opengridmap.views.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.SubmissionStatusActivity;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.UploadSubmissionService;
import tanuj.opengridmap.views.adapters.SubmissionsViewAdapter;

public class SubmissionsFragment extends Fragment implements SubmissionsViewAdapter.OnItemClickListener {
    private static final String TAG = SubmissionsFragment.class.getSimpleName();

    private RecyclerView recyclerView;

    private StaggeredGridLayoutManager staggeredGridLayoutManager;

    private SubmissionsViewAdapter adapter;

    private OpenGridMapDbHelper dbHelper;

    private List<Submission> submissions;

    private BroadcastReceiver uploadUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            processUploadUpdate(
                    intent.getLongExtra(getString(R.string.key_submission_id), -1),
                    intent.getShortExtra(getString(R.string.key_upload_completion), (short) -1)
            );
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
        submissions = dbHelper.getSubmissions(Submission.STATUS_INVALID);
        recyclerView = (RecyclerView) view.findViewById(R.id.submissions_grid);
        adapter = new SubmissionsViewAdapter(context, submissions);

        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickLietener(this);

        return view;
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

        getActivity().registerReceiver(uploadUpdateBroadcastReceiver,
                new IntentFilter(UploadSubmissionService.UPLOAD_UPDATE_BROADCAST));
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d(TAG, submissions.get(position).getSubmissionStatus(getContext()));

        Intent intent = new Intent(getContext(), SubmissionStatusActivity.class);
        intent.putExtra(getString(R.string.key_submission_id), submissions.get(position).getId());

        startActivity(intent);
    }

    private void processUploadUpdate(long submissionId, short uploadCompletion) {
        submissions = dbHelper.getSubmissions(Submission.STATUS_INVALID);
        adapter.setSubmissions(submissions);
        adapter.notifyDataSetChanged();
    }
}
