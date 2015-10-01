package tanuj.opengridmap.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.views.activities.SubmissionActivity;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.views.adapters.SubmissionsListAdapter;


public class SubmissionsActivityFragment extends Fragment {
    private List<Submission> submissions = null;

    public SubmissionsActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submissions, container, false);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());

        submissions = dbHelper.getSubmissions(Submission.STATUS_SUBMISSION_CONFIRMED);

        dbHelper.close();

        ListView listView = (ListView) view.findViewById(R.id.listview_contributions);

        listView.setAdapter(new SubmissionsListAdapter(getActivity(), submissions));
        listView.setOnItemClickListener(onItemClickListener);

        return view;
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
}
