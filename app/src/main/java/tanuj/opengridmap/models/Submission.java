package tanuj.opengridmap.models;

import android.content.Context;
import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import tanuj.opengridmap.R;
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

    public ArrayList<String> getPowerElementNames() {
        ArrayList<String> powerElementNames = new ArrayList<String>();

        for (PowerElement powerElement: powerElements) {
            powerElementNames.add(powerElement.getName());
        }

        return powerElementNames;
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

    public void addImage(Context context, Image image) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        images.add(image);
        dbHelper.addImageToSubmission(image, this);

        if (status == STATUS_IMAGE_CAPTURE_PENDING) {
            dbHelper.updateSubmissionStatus(this, STATUS_IMAGE_CAPTURE_IN_PROGRESS);
        }
    }

    public void confirmSubmission(Context context) {
        if (status == STATUS_IMAGE_CAPTURE_IN_PROGRESS) {
            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
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

    public int getNoOfImages() {
        return images.size();
    }

    public ArrayList<String> getUploadPayloads(Context context) {
        ArrayList<String> payloads = new ArrayList<String>();

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        for (Image image : images) {
            JSONObject json = new JSONObject();
            JSONObject point = new JSONObject();
            JSONObject tags = new JSONObject();

            try {
                tags.put(context.getString(R.string.json_key_type), context.getString(
                        R.string.json_value_point));

                Location location = image.getLocation();

                point.put(context.getString(R.string.json_key_latitude), location.getLatitude());
                point.put(context.getString(R.string.json_key_longitude), location.getLongitude());
                point.put(context.getString(R.string.json_key_tags), tags);

                json.put(context.getString(R.string.json_key_submission_id), id);
                json.put(context.getString(R.string.json_key_image), image.getBase64EncodedImage());
                json.put(context.getString(R.string.json_key_number_of_points), getNoOfImages());
                json.put(context.getString(R.string.json_key_point), point);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            payloads.add(json.toString());
        }

        return payloads;
    }

    public float getBestAccuracy() {
        return getBestImageByLocationAccuracy().getLocation().getAccuracy();
    }

    public float getMeanAccuracy() {
        float mean = 0;

        for (Image image: images) {
            mean += image.getLocation().getAccuracy();
        }

        return mean / getNoOfImages();
    }

    public float getWorstAccuracy() {
        return getWorstImageByLocationAccuracy().getLocation().getAccuracy();
    }

    public Image getBestImageByLocationAccuracy() {
        return Collections.min(images, new CompareImagesByAccuracy());
    }

    public Image getWorstImageByLocationAccuracy() {
        return Collections.max(images, new CompareImagesByAccuracy());
    }

    public ArrayList<Float> getConsecutivePointDistances() {
        ArrayList<Float> distances = new ArrayList<Float>();

        Location previousLocation = getImage(0).getLocation();

        for (int i = 1; i < this.getNoOfImages(); i++) {
            float[] res = new float[0];
            Location currentLocation = getImage(i).getLocation();
            Location.distanceBetween(previousLocation.getLatitude(), previousLocation.getLongitude(),
                    currentLocation.getLatitude(), currentLocation.getLongitude(), res);

            distances.add(res[0]);
            previousLocation = currentLocation;
        }

        return distances;
    }

    public Float getMeanDistanceBetweenImages() {
        float meanDistance = 0;

        ArrayList<Float> distances = getConsecutivePointDistances();

        for (Float distance: distances) {
            meanDistance += distance;
        }

        return meanDistance / (getNoOfImages() - 1);
    }

    static class CompareImagesByAccuracy implements Comparator<Image> {
        @Override
        public int compare(Image lhs, Image rhs) {
            return Long.signum((long) lhs.getLocation().getAccuracy() -
                    (long) rhs.getLocation().getAccuracy());
        }
    }
}