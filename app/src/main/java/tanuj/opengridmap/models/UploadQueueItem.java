package tanuj.opengridmap.models;

import android.content.Context;

import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import tanuj.opengridmap.data.OpenGridMapDbHelper;

/**
 * Created by Tanuj on 14/8/2015.
 */
public class UploadQueueItem {
    private static final int STATUS_QUEUED = 0;
    private static final int STATUS_UPLOAD_STARTED = 1;
    private static final int STATUS_UPLOAD_FINISHED = 2;
    private static final int STATUS_UPLOAD_FAILED = 3;
    private static final int STATUS_UPLOAD_CANCELLED = 3;

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
    }

    public UploadQueueItem(Context context, long id) {
        this.id = id;

        Timestamp timestamp = new Timestamp(new Date().getTime());
        this.createdAtTimestamp = timestamp;

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
    }

    public long getSubmissionId() {
        return this.submission.getId();
    }

    public Submission getSubmission() {
        return submission;
    }

    public int getStatus() {
        return this.status;
    }

    public Timestamp getUpdatedAtTimestamp() {
        return updatedAtTimestamp;
    }

    public Timestamp getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }
}
