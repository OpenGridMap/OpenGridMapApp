package tanuj.opengridmap.models;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;

import tanuj.opengridmap.data.OpenGridMapDbHelper;

/**
 * Created by Tanuj on 14/8/2015.
 */
public class UploadQueueItem extends Submission{
    private static final String TAG = UploadQueueItem.class.getSimpleName();

    public static final int STATUS_UPLOAD_CANCELLED = -2;
    public static final int STATUS_UPLOAD_FAILED = -1;
    public static final int STATUS_QUEUED = 0;
    public static final int STATUS_UPLOAD_IN_PROGRESS = 1;
    public static final int STATUS_UPLOAD_COMPLETE = 2;

    private long id;

    private int status = STATUS_QUEUED;

    private Timestamp createdAtTimestamp;

    private Timestamp updatedAtTimestamp;

    private JSONObject payloadsUploaded;

    public UploadQueueItem(long id, Submission submission, int status, String payloadsUploaded,
                           Timestamp createdAtTimestamp, Timestamp updatedAtTimestamp) {
        super(submission);
        this.id = id;
        this.status = status;

        try {
            this.payloadsUploaded = new JSONObject(payloadsUploaded);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.createdAtTimestamp = createdAtTimestamp;
        this.updatedAtTimestamp = updatedAtTimestamp;
    }

    public JSONObject getPayloadsUploaded() {
        return payloadsUploaded;
    }

    public String getPayloadsUploadedString() {
        return payloadsUploaded.toString();
    }

    public UploadQueueItem(Context context, Submission submission) {
        super(submission);
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        Timestamp timestamp = new Timestamp(new Date().getTime());

        this.createdAtTimestamp = timestamp;
        this.updatedAtTimestamp = timestamp;
        this.payloadsUploaded = new JSONObject();


        for (Image image: submission.getImages()) {
            try {
                Log.d(TAG, Long.toString(image.getId()));
                payloadsUploaded.put(Long.toString(image.getId()), false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        this.id = dbHelper.addQueueItem(this);
        dbHelper.close();

        Log.d(TAG, payloadsUploaded.toString());
    }

    public long getId() {
        return id;
    }

    public Submission getSubmission() {
        return super.getSubmission();
    }

    public long getSubmissionPayloadsId() {
        return super.getId();
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

    public int getNoOfPayloads() {
        return getNoOfImages();
    }

    public void setPayloadsUploaded(JSONObject payloadsUploaded) {
        this.payloadsUploaded = payloadsUploaded;
    }

    public void updatePayloadsUploaded(final Context context, Payload payload) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        dbHelper.updateQueueItemPayloadUploads(this, payload.getImageId());
        dbHelper.close();
    }

    public int getNoOfPayloadsUploaded(final Context context) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        JSONObject payloadsUploaded = dbHelper.getQueueItemPayloadsUploaded(this);
        int noOfPayloadUploaded = 0;

        Iterator<String> keys = payloadsUploaded.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            try {
                if (payloadsUploaded.getBoolean(key)) {
                    noOfPayloadUploaded++;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        dbHelper.close();

        return noOfPayloadUploaded;
    }

    public float getUploadCompletion(final Context context) {
        int noOfPayloads = getNoOfPayloadsUploaded(context);

        return (float) noOfPayloads / (float) getNoOfImages();
    }

    public boolean isPayloadUploaded(Payload payload) {
        boolean status = false;
        Log.d(TAG, payloadsUploaded.toString());
        try {
            status = payloadsUploaded.getBoolean(Long.toString(payload.getImageId()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return status;
    }

    public boolean isUploadComplete(final Context context) {
        return getNoOfPayloadsUploaded(context) == getNoOfImages();
    }

    public void delete(Context context) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        dbHelper.deleteQueueItem(id);
        dbHelper.close();
    }
}
