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

    private Context context;

    private OpenGridMapDbHelper dbHelper = null;

    public Submission() {
        Timestamp timestamp = new Timestamp(new Date().getTime());

        this.createdTimestamp = timestamp;
        this.updatedTimestamp = timestamp;

        powerElements = new ArrayList<PowerElement>();
        images = new ArrayList<Image>();
    }

    public Submission(Context context) {
        this();
        this.context = context;

        dbHelper = new OpenGridMapDbHelper(this.context);
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

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Timestamp getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setUpdatedTimestamp(Timestamp updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public ArrayList<PowerElement> getPowerElements() {
        return powerElements;
    }

    public void setPowerElements(ArrayList<PowerElement> powerElements) {
        this.powerElements = powerElements;
    }

    public void addPowerElement(PowerElement powerElement) {
        powerElements.add(powerElement);

        dbHelper.addPowerElementToSubmission(powerElement, this);
    }

    public void addPowerElementById(int id) {
        PowerElement powerElement = dbHelper.getPowerElement(id);
        this.addPowerElement(powerElement);
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

    public void addImage(Image image) {
        images.add(image);

        dbHelper.addImageToSubmission(image, this);

        if (status == STATUS_IMAGE_CAPTURE_PENDING) {
            dbHelper.updateSubmissionStatus(this, STATUS_IMAGE_CAPTURE_IN_PROGRESS);
        }
    }

    public void confirmSubmission() {
        if (status == STATUS_IMAGE_CAPTURE_IN_PROGRESS) {
            dbHelper.updateSubmissionStatus(this, STATUS_SUBMISSION_CONFIRMED);
        }
    }

    public boolean addToUploadQueue(Context context) {
        if (status == STATUS_SUBMISSION_CONFIRMED) {
            new UploadQueueItem(context, this);
            dbHelper.updateSubmissionStatus(this, STATUS_UPLOAD_PENDING);
            return true;
        }
        return false;
    }
}
