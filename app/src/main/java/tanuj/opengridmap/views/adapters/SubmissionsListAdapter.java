package tanuj.opengridmap.views.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

//        View currentView;
        layoutInflater = ((Activity) context).getLayoutInflater();

        if (view == null) {
            view = layoutInflater.inflate(R.layout.submissions_list_item, parent, false);
        }

        ImageView submissionsImage = (ImageView) view.findViewById(R.id.submission_image);
        TextView submissionTypeTextView = (TextView) view.findViewById(R.id.submission_type);
        TextView submissionNoImagesTextView = (TextView) view.findViewById(R.id.no_of_images);
        TextView submissionCoordsTextView = (TextView) view.findViewById(R.id.submission_coordinates);
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.submission_upload_progress_bar);

        Submission submission = submissions.get(position);

        Bitmap bitmap = submission.getImage(0).getThumbnailBitmap(context, Image.TYPE_LIST);

        if(bitmap != null){
            submissionsImage.setImageBitmap(bitmap);
        } else {
            submissionsImage.setBackgroundResource(R.drawable.camera_shutter);
        }

        submissionTypeTextView.setText((submission.getPowerElements().get(0).getName()));
        submissionNoImagesTextView.setText(
                "No of Images : " +
                submission.getImages().size());
        submissionCoordsTextView.setText(
                "Lat : " +
                        Double.toString(submission.getImage(0).getLocation().getLatitude()) +
                        "\nLon : " +
                        Double.toString(submission.getImage(0).getLocation().getLatitude()) +
                        "\nAccuracy : " +
                        Float.toString(submission.getImage(0).getLocation().getAccuracy())
        );
        progressBar.setProgress(new Random().nextInt(95));

        return view;
    }
}
