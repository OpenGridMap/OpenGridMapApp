package tanuj.opengridmap.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;


public class SubmissionDetailsFragment extends Fragment {
    private static final String KEY_SUBMISSION_ID = "submission_id";

    private ImageView imagePreview;

    private TextView powerElementTypeTextView;

    private TextView locationTextView;

    private TextView accuracyTextView;

    private TextView powerTagsTextView;

    private TextView dateTextView;

    private TextView statusTextView;

    private Submission submission;

    public SubmissionDetailsFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SubmissionDetailsFragment newInstance(long submissionId) {
        SubmissionDetailsFragment fragment = new SubmissionDetailsFragment();

        Bundle args = new Bundle();

        args.putLong(KEY_SUBMISSION_ID, submissionId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submission_details, container, false);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getContext());
        submission = dbHelper.getSubmission(getArguments().getLong(KEY_SUBMISSION_ID));
        dbHelper.close();

        imagePreview = (ImageView) view.findViewById(R.id.image_preview);
        powerElementTypeTextView = (TextView) view.findViewById(R.id.type);
        locationTextView = (TextView) view.findViewById(R.id.location);
        accuracyTextView = (TextView) view.findViewById(R.id.accuracy);
        dateTextView = (TextView) view.findViewById(R.id.date);
        statusTextView = (TextView) view.findViewById(R.id.status);
        powerTagsTextView = (TextView) view.findViewById(R.id.power_tags);

        populateSubmissionDetails();

        return view;
    }

    private void populateSubmissionDetails() {
        if (submission != null) {
            Context context = getContext();
            Picasso.with(context).load(new File(submission.getImageSrc())).into(imagePreview);

            powerElementTypeTextView.setText(getString(R.string.type_format,
                    submission.getPowerElements().get(0).getName()));
            locationTextView.setText(getString(R.string.location_format,
                    submission.getLocationString(context)));
            accuracyTextView.setText(getString(R.string.accuracy_format,
                    submission.getImage().getLocation().getAccuracy()));
            dateTextView.setText(getString(R.string.date_format, submission.getCreatedTimestamp()));
            statusTextView.setText(getString(R.string.status_format, submission.getSubmissionStatus(context)));
            powerTagsTextView.setText(getString(R.string.power_tags_format,
                    submission.getPowerElements().get(0).getOsmTags()));
        }
    }
}