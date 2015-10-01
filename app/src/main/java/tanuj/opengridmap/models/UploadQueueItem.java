package tanuj.opengridmap.models;

import android.content.Context;

import java.sql.Timestamp;
import java.util.Date;

import tanuj.opengridmap.data.OpenGridMapDbHelper;

/**
 * Created by Tanuj on 14/8/2015.
 */
public class UploadQueueItem {
    private static final String TAG = UploadQueueItem.class.getSimpleName();

    public static final int STATUS_QUEUED = 0;
    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;
    public static final int STATUS_UPLOAD_FAILED = 3;
    public static final int STATUS_UPLOAD_CANCELLED = 3;

    private long id;

    private Submission submission;

    private int status = STATUS_QUEUED;

    private Timestamp createdAtTimestamp;

    private Timestamp updatedAtTimestamp;

    public UploadQueueItem(long id, Submission submission, int status, Timestamp createdAtTimestamp,
                           Timestamp updatedAtTimestamp) {
        this.id = id;
        this.submission = submission;
        this.status = status;
        this.createdAtTimestamp = createdAtTimestamp;
        this.updatedAtTimestamp = updatedAtTimestamp;
    }

    public UploadQueueItem(Context context, Submission submission) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        Timestamp timestamp = new Timestamp(new Date().getTime());

        this.submission = submission;
        this.createdAtTimestamp = timestamp;
        this.updatedAtTimestamp = timestamp;
        this.id = dbHelper.addQueueItem(this);
        dbHelper.close();
    }

//    public UploadQueueItem(Context context, long id) {
//        this.id = id;
//
//        Timestamp timestamp = new Timestamp(new Date().getTime());
//        this.createdAtTimestamp = timestamp;
//
//        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
//    }

    public long getSubmissionId() {
        return this.submission.getId();
    }

    public Submission getSubmission() {
        return submission;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Timestamp getUpdatedAtTimestamp() {
        return updatedAtTimestamp;
    }

    public Timestamp getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }

    public void updateStatus(final Context context, int status) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        if (this.status != status) {
            dbHelper.updateQueueItemStatus(this, status);
        }
        dbHelper.close();
    }

    public void updatePayloadsUploaded(final Context context, int n) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        dbHelper.updateQueueItemPayloadUploads(this, n);
        dbHelper.close();
    }

    public long getId() {
        return id;
    }
}
