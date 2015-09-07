package tanuj.opengridmap.models;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tanuj.opengridmap.data.OpenGridMapDbHelper;

/**
 * Created by Tanuj on 09-06-2015.
 */
public class Submission {
    private static final String TAG = Submission.class.getSimpleName();

    private static final int STATUS_INVALID = -3;
    private static final int STATUS_IMAGE_CAPTURE_PENDING = -2;
    private static final int STATUS_IMAGE_CAPTURE_IN_PROGRESS = -1;
    private static final int STATUS_SUBMISSION_CONFIRMED = 0;
    private static final int STATUS_UPLOAD_PENDING = 1;
    private static final int STATUS_SUBMITTED_PENDING_REVIEW = 2;
    private static final int STATUS_SUBMITTED_APPROVED = 3;
    private static final int STATUS_SUBMITTED_REJECTED = 4;

    private long id;

    private int status = STATUS_IMAGE_CAPTURE_PENDING;

    private List<Image> images;

    private ArrayList<PowerElement> powerElements;

    private Timestamp createdTimestamp;

    private Timestamp updatedTimestamp;

//    private Context context;

//    private OpenGridMapDbHelper dbHelper = null;

    public Submission() {
        Timestamp timestamp = new Timestamp(new Date().getTime());

        this.createdTimestamp = timestamp;
        this.updatedTimestamp = timestamp;

        powerElements = new ArrayList<PowerElement>();
        images = new ArrayList<Image>();
    }

    public Submission(Context context) {
        this();

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        this.id = dbHelper.addSubmission(this);
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Timestamp getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(Timestamp updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public ArrayList<PowerElement> getPowerElements() {
        return powerElements;
    }

    public void setPowerElements(ArrayList<PowerElement> powerElements) {
        this.powerElements = powerElements;
    }

    public void addPowerElement(Context context, PowerElement powerElement) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        powerElements.add(powerElement);

        dbHelper.addPowerElementToSubmission(powerElement, this);
    }

    public void addPowerElementById(Context context, long id) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        PowerElement powerElement = dbHelper.getPowerElement(id);
        this.addPowerElement(context, powerElement);
        dbHelper.addPowerElementToSubmission(powerElement, this);
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public Image getImage(int index) {
        return images.get(index);
    }

//    public List<Image> getImages() {
//        return dbHelper.getImagesBySubmissionId(this.id);
//    }

    public void addImage(Context context, Image image) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        images.add(image);
        dbHelper.addImageToSubmission(image, this);
//        image.generateThumbnails(context);

        if (status == STATUS_IMAGE_CAPTURE_PENDING) {
            dbHelper.updateSubmissionStatus(this, STATUS_IMAGE_CAPTURE_IN_PROGRESS);
        }
    }

    public void confirmSubmission(Context context) {
        if (status == STATUS_IMAGE_CAPTURE_IN_PROGRESS) {
            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

//            for (Image image : images) {
//                image.generateThumbnails(context);
//            }

            dbHelper.updateSubmissionStatus(this, STATUS_SUBMISSION_CONFIRMED);
        }
    }

    public boolean addToUploadQueue(Context context) {
        if (status == STATUS_SUBMISSION_CONFIRMED) {
            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

            dbHelper.updateSubmissionStatus(this, STATUS_UPLOAD_PENDING);
            new UploadQueueItem(context, this);
            return true;
        }
        return false;
    }
}
