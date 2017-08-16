package tanuj.opengridmap.views.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;


public class SubmissionDetailsFragment extends Fragment implements View.OnClickListener, DialogInterface.OnClickListener {
    private static final String KEY_SUBMISSION_ID = "submission_id";

    private ImageView imagePreview;

    private FloatingActionButton deleteButton;

    private TextView powerElementTypeTextView;

    private TextView locationTextView;

    private TextView accuracyTextView;

    private TextView powerTagsTextView;

    private TextView dateTextView;

    private TextView statusTextView;

    private AlertDialog alertDialog;

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

        imagePreview = view.findViewById(R.id.image_preview);
        deleteButton = view.findViewById(R.id.delete_button);
        powerElementTypeTextView = view.findViewById(R.id.power_element_type);
        locationTextView = view.findViewById(R.id.location);
        accuracyTextView = view.findViewById(R.id.accuracy);
        dateTextView = view.findViewById(R.id.date);
        statusTextView = view.findViewById(R.id.status);
        powerTagsTextView = view.findViewById(R.id.power_tags);

        populateSubmissionDetails();

        deleteButton.setOnClickListener(this);

        buildAlertDialog();

        return view;
    }

    private void buildAlertDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext(), R.style.Theme_AppCompat_Light_Dialog_Alert);
        alertBuilder.setMessage("Are you sure you want to delete this submission?");
        alertBuilder.setPositiveButton("Yes", this);
        alertBuilder.setNegativeButton("No", this);
        alertDialog = alertBuilder.create();
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delete_button: {
                alertDialog.show();
                break;
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        Log.d("Hello", String.valueOf(i));
        switch (i) {
            case DialogInterface.BUTTON_POSITIVE: {
                final Activity activity = getActivity();
                if (submission != null)
                    submission.deleteSubmission(activity);
                activity.finish();
                break;
            }
            case DialogInterface.BUTTON_NEGATIVE: {
                dialogInterface.dismiss();
                break;
            }
        }
    }
}