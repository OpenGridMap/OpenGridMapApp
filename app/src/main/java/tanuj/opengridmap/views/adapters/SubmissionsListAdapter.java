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
import android.widget.TextView;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.utils.ImageUtils;
import tanuj.opengridmap.views.custom_views.CircularProgressBar;

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
        TextView latitudeTextView = (TextView) view.findViewById(R.id.latitude);
        TextView longitudeTextView = (TextView) view.findViewById(R.id.longitude);
        TextView accuracyTextView = (TextView) view.findViewById(R.id.accuracy);
//        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);
        CircularProgressBar progressBar = (CircularProgressBar) view.findViewById(
                R.id.upload_progress_bar);
        ImageView submissionStatusImage = (ImageView) view.findViewById(
                R.id.submission_status_image);

        Submission submission = submissions.get(position);

        Image image = submission.getImage(0);
        Bitmap bitmap = image.getThumbnailBitmap(context, Image.TYPE_LIST);
        Location location = image.getLocation();
        String[] coordinates = image.getLocationInDegrees(context);
        String powerElementNamesString = submission.getPowerElementTagsString();

        if(bitmap != null){
            submissionsImage.setImageBitmap(bitmap);
        } else {
            submissionsImage.setBackgroundResource(R.drawable.photo212);
        }

        powerElementTagsTextView.setText(powerElementNamesString);
        powerElementTagsTextView.setTextColor(getPowerElementsTextColor(submission));
        submissionNoImagesTextView.setText(String.valueOf(submission.getNoOfImages()));
        latitudeTextView.setText(coordinates[0]);
        longitudeTextView.setText(coordinates[1]);
        accuracyTextView.setText(String.format("%.2f", location.getAccuracy()));

        int submissionStatus = submission.getStatus();

        if (submissionStatus <= Submission.STATUS_SUBMISSION_CONFIRMED) {
            powerElementTagsTextView.setTextColor(getPowerElementsTextColor(submission));

            progressBar.setVisibility(View.GONE);
            submissionStatusImage.setVisibility(View.VISIBLE);

            ImageUtils.setImageViewDrawable(context, submissionStatusImage,
                    R.drawable.verification24);
        } else if (submissionStatus == Submission.STATUS_UPLOAD_IN_PROGRESS) {
            int submissionUploadProgress = submission.getUploadStatus();

            powerElementTagsTextView.setTextColor(getPowerElementsTextColor(submission));

            submissionStatusImage.setVisibility(View.GONE);

            if (progressBar.getVisibility() == View.GONE) {
                progressBar.setVisibility(View.VISIBLE);
            }

            progressBar.setProgress(submissionUploadProgress);

            if (submissionUploadProgress == 100) {
                powerElementTagsTextView.setTextColor(getPowerElementsTextColor(submission));

                progressBar.setVisibility(View.GONE);

                ImageUtils.setImageViewDrawable(context, submissionStatusImage,
                        R.drawable.double126);
                submissionStatusImage.setVisibility(View.VISIBLE);
            }
        } else if (submissionStatus >= Submission.STATUS_SUBMITTED_PENDING_REVIEW) {
            powerElementTagsTextView.setTextColor(getPowerElementsTextColor(submission));

            progressBar.setVisibility(View.GONE);

            ImageUtils.setImageViewDrawable(context, submissionStatusImage,
                    R.drawable.double126);

            submissionStatusImage.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private int getPowerElementsTextColor(Submission submission) {
        int powerElementsColor = 0;

        switch (submission.getStatus()) {
            case Submission.STATUS_SUBMISSION_CONFIRMED: {
                powerElementsColor = context.getResources().getColor(R.color.amber_600);
                break;
            }
            case Submission.STATUS_UPLOAD_PENDING: {
                powerElementsColor = context.getResources().getColor(R.color.lime_700);
                break;
            }
            case Submission.STATUS_UPLOAD_IN_PROGRESS: {
                powerElementsColor = context.getResources().getColor(R.color.teal_400);
                break;
            }
            case Submission.STATUS_SUBMITTED_PENDING_REVIEW: {
                powerElementsColor = context.getResources().getColor(R.color.teal_800);
                break;
            }
            case Submission.STATUS_SUBMITTED_APPROVED: {
                powerElementsColor = context.getResources().getColor(R.color.teal_900);
                break;
            }
            case Submission.STATUS_SUBMITTED_REJECTED: {
                powerElementsColor = context.getResources().getColor(R.color.amber_900);
                break;
            }
        }
        return powerElementsColor;
    }

    public void notifyDataSetChanged(List<Submission> submissions) {
        this.submissions = submissions;
        this.notifyDataSetChanged();
    }
}
