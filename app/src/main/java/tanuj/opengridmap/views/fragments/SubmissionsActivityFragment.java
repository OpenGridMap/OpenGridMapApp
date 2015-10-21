package tanuj.opengridmap.views.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.UploadService;
import tanuj.opengridmap.views.activities.SubmissionActivity;
import tanuj.opengridmap.views.adapters.SubmissionsListAdapter;


public class SubmissionsActivityFragment extends Fragment {
    private static final String TAG = SubmissionsActivityFragment.class.getSimpleName();

    private List<Submission> submissions = null;
    private SubmissionsListAdapter submissionsListAdapter;

    public SubmissionsActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submissions, container, false);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());
        submissions = dbHelper.getSubmissions(Submission.STATUS_SUBMISSION_CONFIRMED);
        dbHelper.close();

        ListView listView = (ListView) view.findViewById(R.id.listview_contributions);

        submissionsListAdapter = new SubmissionsListAdapter(getActivity(), submissions);
        listView.setAdapter(submissionsListAdapter);
        listView.setOnItemClickListener(onItemClickListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(uploadUpdatesBroadcastReceiver, new IntentFilter(
                UploadService.UPLOAD_UPDATE_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(uploadUpdatesBroadcastReceiver);
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Context context = getActivity();
            Intent intent = new Intent(context, SubmissionActivity.class);
            intent.putExtra(getString(R.string.key_submission_id), submissions.get(position)
                    .getId());
            startActivity(intent);
        }
    };

    private BroadcastReceiver uploadUpdatesBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "OnReceive Broadcast");
            updateUI(intent);
        }
    };

    private void updateUI(Intent intent) {
        Log.v(TAG, "UpdateUI");
        final Long submissionId = intent.getLongExtra(getString(R.string.key_submission_id), -1);
        final int uploadCompletion = intent.getIntExtra(getString(R.string.key_upload_completion),
                -1);

        Log.d(TAG, "Submission ID : " + submissionId + "    Upload Completion : " +
                uploadCompletion);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());
        submissions = dbHelper.getSubmissions(Submission.STATUS_SUBMISSION_CONFIRMED);
        dbHelper.close();

        if (submissionId != -1 && uploadCompletion != -1) {
            for (Submission submission : submissions) {
                if (submission.getId() == submissionId) {
                    submission.setUploadStatus(uploadCompletion);
                }
            }
        }

        submissionsListAdapter.notifyDataSetChanged(submissions);
    }
}
