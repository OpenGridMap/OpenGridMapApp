package tanuj.opengridmap;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.views.adapters.ImageAdapter;
import tanuj.opengridmap.views.adapters.PowerElementTagsAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class AltSubmissionActivityFragment extends Fragment {
    private static final String TAG = AltSubmissionActivityFragment.class.getSimpleName();

    private Submission submission = null;

    private TextView latitudeTextView;

    private TextView longitudeTextView;

    private TextView accuracyTextView;

    private TextView noImagesTextView;

    private ListView powerElementsListView;

    private GridView imageGridView;

    public AltSubmissionActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alt_submission, container, false);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());

        long submissionId = getActivity().getIntent().getLongExtra(String.valueOf(
                R.string.key_submission_id), -1);

        if (submissionId > -1) {
            submission = dbHelper.getSubmission(submissionId);
        }

        submission = dbHelper.getSubmission((int) getActivity().getIntent()
                .getLongExtra(String.valueOf(R.string.key_submission_id), -1));

        if (null != submission) {
            final Context context = getActivity();

            latitudeTextView = (TextView) view.findViewById(R.id.latitude);
            longitudeTextView = (TextView) view.findViewById(R.id.best_submission_longitude);
            accuracyTextView = (TextView) view.findViewById(R.id.submission_accuracy);
            noImagesTextView = (TextView) view.findViewById(R.id.submission_no_of_images);

            powerElementsListView = (ListView) view.findViewById(
                    R.id.submission_power_elements_list);
            imageGridView = (GridView) view.findViewById(R.id.submission_images_grid);

            powerElementsListView.setAdapter(new PowerElementTagsAdapter(context,
                    submission.getPowerElements()));
            imageGridView.setAdapter(new ImageAdapter(context, submission.getImages()));

            Image image = submission.getImage(0);

            latitudeTextView.setText("Latitude : " + image.getLocation().getLatitude());
            longitudeTextView.setText("Longitude : " + image.getLocation().getLongitude());
            accuracyTextView.setText("Accuracy : " + image.getLocation().getAccuracy() + " %");
            noImagesTextView.setText("No. of Images : " + submission.getNoOfImages());
        }

        return view;
    }
}
