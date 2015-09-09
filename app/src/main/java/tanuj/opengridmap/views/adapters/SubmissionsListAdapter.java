package tanuj.opengridmap.views.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;

public class SubmissionsListAdapter extends BaseAdapter {
    private Context context = null;
    private List<Submission> submissions;
    private LayoutInflater layoutInflater = null;

    public SubmissionsListAdapter(Context context, List<Submission> submissions) {
        this.context = context;
        this.submissions = submissions;
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return submissions.size();
    }

    @Override
    public Submission getItem(int position) {
        return submissions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (submissions.get(position)).getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            layoutInflater = ((Activity) context).getLayoutInflater();
            view = layoutInflater.inflate(R.layout.submissions_list_item, parent, false);
        }

        ImageView submissionsImage = (ImageView) view.findViewById(R.id.submission_image);
        TextView powerElementTagsTextView = (TextView) view.findViewById(
                R.id.submission_power_elements);
        TextView submissionNoImagesTextView = (TextView) view.findViewById(R.id.no_of_images);

//        TextView submissionCoordsTextView = (TextView) view.findViewById(R.id.submission_coordinates);

        TextView latitudeTextView = (TextView) view.findViewById(R.id.latitude);
        TextView longitudeTextView = (TextView) view.findViewById(R.id.longitude);
        TextView accuracyTextView = (TextView) view.findViewById(R.id.accuracy);

        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);

        Submission submission = submissions.get(position);


        Image image = submission.getImage(0);
        Bitmap bitmap = image.getThumbnailBitmap(context, Image.TYPE_LIST);
        Location location = image.getLocation();
        String[] coordinates = image.getLocationInDegrees(context);
        String powerElementNamesString = submission.getPowerElementTagsString();

        if(bitmap != null){
            submissionsImage.setImageBitmap(bitmap);
        } else {
            submissionsImage.setBackgroundResource(R.drawable.camera_shutter);
        }


        powerElementTagsTextView.setText(powerElementNamesString);
        submissionNoImagesTextView.setText(String.valueOf(submission.getNoOfImages()));
        latitudeTextView.setText(coordinates[0]);
        longitudeTextView.setText(coordinates[1]);
        accuracyTextView.setText(String.format("%.2f", location.getAccuracy()));
        progressBar.setProgress(new Random().nextInt(95));

        return view;
    }
}
