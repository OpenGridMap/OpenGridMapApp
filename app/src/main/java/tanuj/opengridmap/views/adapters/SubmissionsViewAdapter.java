package tanuj.opengridmap.views.adapters;


import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;

public class SubmissionsViewAdapter extends RecyclerView.Adapter<SubmissionsViewAdapter.ViewHolder>{
    private Context context;

    private List<Submission> submissions;

    private OnItemClickListener itemClickListener;

    public SubmissionsViewAdapter(Context context, List<Submission> submissions) {
        this.context = context;
        this.submissions = submissions;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.submissions_grid_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Submission submission = this.submissions.get(position);
        holder.populateHolder(submission);
    }

    @Override
    public int getItemCount() {
        return this.submissions.size();
    }

    public List<Submission> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<Submission> submissions) {
        this.submissions = submissions;
    }

    public void setOnItemClickLietener(final OnItemClickListener itemClickLietener) {
        this.itemClickListener = itemClickLietener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private LinearLayout submissionHolder;
        private ImageView submissionImage;
        private ProgressBar uploadProgress;
        private ImageView uploadStatusIcon;

        ViewHolder(View itemView) {
            super(itemView);

            submissionHolder = (LinearLayout) itemView.findViewById(R.id.mainHolder);
            submissionImage = (ImageView) itemView.findViewById(R.id.submissionImage);
            uploadStatusIcon = (ImageView) itemView.findViewById(R.id.submissionStatusIcon);
            uploadProgress = (ProgressBar) itemView.findViewById(R.id.uploadProgress);

            submissionHolder.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null)
                itemClickListener.onItemClick(itemView, getAdapterPosition());
        }

        public void populateHolder(final Submission submission) {
            setUploadStatusIndicator(submission);
            setSubmissionImage(submission);

////            TODO: Dummy
//            switch ((int) (submission.getId() % 3)) {
//                case 0: {
//                    hideProgressBar();
//                    setUploadStatusIcon(R.drawable.ic_cloud_off_white_24dp);
//                    showUploadStatusIcon();
//                    break;
//                }
//                case 1: {
//                    hideUploadStatusIcon();
//                    showProgressBar();
//                    break;
//                }
//                case 2: {
//                    hideProgressBar();
//                    setUploadStatusIcon(R.drawable.ic_cloud_done_white_24dp);
//                    showUploadStatusIcon();
//                    break;
//                }
//
//            }
        }

        private void setSubmissionImage(final Submission submission) {
            Image image = submission.getImage(0);
            image.generateThumbnails(context);
            Picasso.with(context).load(image.getThumbnailFile(context, Image.TYPE_GRID)).into(submissionImage);
        }

        private void setUploadStatusIndicator(final Submission submission) {
            switch (submission.getStatus()) {
                case Submission.STATUS_UPLOAD_PENDING: {
                    hideProgressBar();
                    setUploadStatusIcon(R.drawable.ic_cloud_off_white_24dp);
                    showUploadStatusIcon();
                    break;
                }
                case Submission.STATUS_UPLOAD_IN_PROGRESS: {
                    hideUploadStatusIcon();
                    showProgressBar();
                    break;
                }
                case Submission.STATUS_SUBMITTED_PENDING_REVIEW: {
                    hideProgressBar();
                    setUploadStatusIcon(R.drawable.ic_cloud_done_white_24dp);
                    showUploadStatusIcon();
                    break;
                }
            }
        }

        private void showUploadStatusIcon() {
            if (uploadStatusIcon.getVisibility() == View.GONE)
                uploadStatusIcon.setVisibility(View.VISIBLE);
        }

        private void hideUploadStatusIcon() {
            if (uploadStatusIcon.getVisibility() == View.VISIBLE)
                uploadStatusIcon.setVisibility(View.GONE);
        }

        private void showProgressBar() {
            if (uploadProgress.getVisibility() == View.GONE)
                uploadProgress.setVisibility(View.VISIBLE);
        }

        private void hideProgressBar() {
            if (uploadProgress.getVisibility() == View.VISIBLE)
                uploadProgress.setVisibility(View.GONE);
        }

        private void setUploadStatusIcon(int icon) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                uploadStatusIcon.setImageDrawable(context.getDrawable(icon));
            } else {
                uploadStatusIcon.setImageDrawable(context.getResources().getDrawable(icon));
            }
        }
    }
}
