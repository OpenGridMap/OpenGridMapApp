package tanuj.opengridmap.models;

import android.content.Context;
import android.location.Location;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
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
import tanuj.opengridmap.exceptions.MemoryLowException;
import tanuj.opengridmap.utils.HashUtils;

/**
 * Created by Tanuj on 09-06-2015.
 */
public class Submission {
    private static final String TAG = Submission.class.getSimpleName();

    public static final int STATUS_INVALID = -3;
    public static final int STATUS_IMAGE_CAPTURE_PENDING = -2;
    public static final int STATUS_IMAGE_CAPTURE_IN_PROGRESS = -1;
    public static final int STATUS_SUBMISSION_CONFIRMED = 0;
    public static final int STATUS_UPLOAD_PENDING = 1;
    public static final int STATUS_UPLOAD_IN_PROGRESS = 2;
    public static final int STATUS_SUBMITTED_PENDING_REVIEW = 3;
    public static final int STATUS_SUBMITTED_APPROVED = 4;
    public static final int STATUS_SUBMITTED_REJECTED = 5;

    private long id;

    private int status = STATUS_IMAGE_CAPTURE_PENDING;

    private long submissionPayloadsId;

    private List<Image> images;

    private ArrayList<PowerElement> powerElements;

    private Timestamp createdTimestamp;

    private Timestamp updatedTimestamp;

    private int uploadStatus = 0;

    public Submission() {
        Timestamp timestamp = new Timestamp(new Date().getTime());

        this.createdTimestamp = timestamp;
        this.updatedTimestamp = timestamp;

        powerElements = new ArrayList<PowerElement>();
        images = new ArrayList<Image>();
    }

    public Submission(Submission submission) {
        this.id = submission.getId();
        this.status = submission.getStatus();
        this.submissionPayloadsId = submission.getSubmissionPayloadsId();
        this.images = submission.getImages();
        this.powerElements = submission.getPowerElements();
        this.createdTimestamp = submission.getCreatedTimestamp();
        this.updatedTimestamp = submission.getUpdatedTimestamp();
    }

    public Submission(Context context) {
        this();

        this.submissionPayloadsId = generateSubmissionId(context);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        this.id = dbHelper.addSubmission(this);
        dbHelper.close();
    }

    public Submission(long id, int status, long submissionId, Timestamp createdTimestamp,
                      Timestamp updatedTimestamp) {
        this.id = id;
        this.status = status;
        this.submissionPayloadsId = submissionId;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
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

    public int getNoOfImages() {
        return images.size();
    }

    public int getUploadStatus() {
        return uploadStatus;
    }

    public long getSubmissionPayloadsId() {
        return submissionPayloadsId;
    }

    public void setUploadStatus(int uploadStatus) {
        this.uploadStatus = uploadStatus;
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
        dbHelper.close();
    }

    public void addPowerElementById(Context context, long id) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        PowerElement powerElement = dbHelper.getPowerElement(id);
        dbHelper.close();

        this.addPowerElement(context, powerElement);
//        dbHelper.addPowerElementToSubmission(powerElement, this);
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

    private void updateStatus(Context context, int newStatus) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        status = newStatus;
        dbHelper.updateSubmissionStatus(this, status);

        dbHelper.close();
    }

    public void addImage(Context context, Image image) {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        long imageId = dbHelper.addImageToSubmission(image, this);
        image.setId(imageId);
        images.add(image);
        dbHelper.close();

        if (status == STATUS_IMAGE_CAPTURE_PENDING) {
            updateStatus(context, STATUS_IMAGE_CAPTURE_IN_PROGRESS);
        }
    }

    public void confirmSubmission(Context context) {
        if (status == STATUS_IMAGE_CAPTURE_IN_PROGRESS) {
            updateStatus(context, STATUS_SUBMISSION_CONFIRMED);
            this.addToUploadQueue(context);
        }
    }

    public boolean addToUploadQueue(Context context) {
        if (status == STATUS_SUBMISSION_CONFIRMED) {
            new UploadQueueItem(context, this);
            updateStatus(context, STATUS_UPLOAD_PENDING);
            return true;
        }
        return false;
    }

    public Payload getUploadPayload(Context context, String idToken, int imageIndex) throws MemoryLowException {
        updateStatus(context, STATUS_UPLOAD_IN_PROGRESS);
        return getPayloadByImage(context, idToken, getImage(imageIndex));
    }

    public ArrayList<Payload> getUploadPayloads(Context context, String idToken, int j) throws MemoryLowException {
        ArrayList<Payload> payloads = new ArrayList<Payload>();
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

        if (status == STATUS_UPLOAD_PENDING) {
            updateStatus(context, STATUS_UPLOAD_IN_PROGRESS);
        }

        for (int i = 0; i < getNoOfImages(); i++)

        for (Image image : images) {
            payloads.add(getPayloadByImage(context, idToken, image));
        }

        dbHelper.close();

        return payloads;
    }

    private Payload getPayloadByImage(Context context, String idToken, Image image) throws MemoryLowException {
        JSONObject json = getPayloadJsonObject(context, idToken, image);
        return new Payload(this.getId(), image.getId(), json);
    }

    private JSONObject getPayloadJsonObject(Context context, String idToken, Image image) throws MemoryLowException {
        JSONObject json = new JSONObject();
        JSONObject dataPacket = new JSONObject();
        JSONObject point = new JSONObject();
        JSONObject properties = new JSONObject();
        JSONObject tags = new JSONObject();
        JSONArray powerElementsTags = new JSONArray();

//        Log.d(TAG, "OSM Power elements : " + getPowerElements().get(0).getOsmTags());

        String[] powerElementTags = getPowerElements().get(0).getOsmTags().split(";");

//        for (PowerElement powerElement : this.powerElementsTags) {
//            powerElementsTags.put(powerElement.getName());
//        }

        for (String powerElementTag: powerElementTags) {
            powerElementsTags.put(powerElementTag);
        }

        Location location = image.getLocation();

        try {
            tags.put(context.getString(R.string.json_key_accuracy), location.getAccuracy());
            tags.put(context.getString(R.string.json_key_altitude), location.getAltitude());
            tags.put(context.getString(R.string.json_key_power_element_tags), powerElementsTags);
            tags.put(context.getString(R.string.json_key_timestamp), image.getCreatedTimestamp());
            tags.put(context.getString(R.string.json_key_type), context.getString(
                    R.string.json_value_point));

            properties.put(context.getString(R.string.json_key_tags), tags);

            point.put(context.getString(R.string.json_key_latitude), location.getLatitude());
            point.put(context.getString(R.string.json_key_longitude), location.getLongitude());
            point.put(context.getString(R.string.json_key_properties), properties);

            dataPacket.put(context.getString(R.string.json_key_id_token), idToken);
            dataPacket.put(context.getString(R.string.json_key_image), image.getBase64EncodedImage());
            dataPacket.put(context.getString(R.string.json_key_number_of_points), getNoOfImages());
            dataPacket.put(context.getString(R.string.json_key_point), point);
            dataPacket.put(context.getString(R.string.json_key_submission_id), createdTimestamp.getTime());

            json.put(context.getString(R.string.json_key_data_packet), dataPacket);
            json.put(context.getString(R.string.json_key_hash),
                    HashUtils.getSha256String(dataPacket.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void uploadComplete(Context context) {
//        if (status == STATUS_UPLOAD_IN_PROGRESS) {
//            updateStatus(context, STATUS_SUBMITTED_PENDING_REVIEW);

            deleteSubmission(context);
//        }
    }

    public void deleteSubmission(Context context) {
        for (Image image: getImages()) {
            image.delete(context);
        }

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        dbHelper.deleteSubmission(getId());
        dbHelper.close();
    }

    public void submissionApproved(Context context) {
        if (status == STATUS_SUBMITTED_PENDING_REVIEW) {
            updateStatus(context, STATUS_SUBMITTED_APPROVED);
        }
    }

    public void submissionRejected(Context context) {
        if (status == STATUS_SUBMITTED_PENDING_REVIEW) {
            updateStatus(context, STATUS_SUBMITTED_REJECTED);
        }
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
            float[] res = new float[3];
            Location currentLocation = getImage(i).getLocation();
            Location.distanceBetween(previousLocation.getLatitude(), previousLocation.getLongitude(),
                    currentLocation.getLatitude(), currentLocation.getLongitude(), res);

            distances.add(res[0]);
            previousLocation = currentLocation;
        }

        return distances;
    }

    public Float getMeanDistanceBetweenImages() {
        if (getNoOfImages() == 1) {
            return (float) 0;
        }

        float meanDistance = 0;

        ArrayList<Float> distances = getConsecutivePointDistances();

        for (Float distance: distances) {
            meanDistance += distance;
        }

        return meanDistance / (getNoOfImages() - 1);
    }

    public boolean isEmpty() {
        if (getNoOfImages() == 0) {
            return true;
        }
        return false;
    }

    public Submission getSubmission() {
        return this;
    }

    static class CompareImagesByAccuracy implements Comparator<Image> {
        @Override
        public int compare(Image lhs, Image rhs) {
            return Long.signum((long) lhs.getLocation().getAccuracy() -
                    (long) rhs.getLocation().getAccuracy());
        }
    }

    public String getPowerElementTagsString() {
        String powerElementNamesString = "";
        ArrayList<String> powerElementNames = getPowerElementNames();

        for (String name: powerElementNames) {
            if (powerElementNamesString == "")
                powerElementNamesString += name;
            else
                powerElementNamesString += ", " + name;
        }

        return powerElementNamesString;
    }

    public String getSubmissionStatus(final Context context) {
        String status = null;

        switch (this.status) {
            case STATUS_INVALID: {
                status = context.getString(R.string.submission_status_invalid);
                break;
            }
            case STATUS_IMAGE_CAPTURE_PENDING: {
                status = context.getString(R.string.submission_status_capture_pending);
                break;
            }
            case STATUS_IMAGE_CAPTURE_IN_PROGRESS: {
                status = context.getString(R.string.submission_status_capture_in_progress);
                break;
            }
            case STATUS_SUBMISSION_CONFIRMED: {
                status = context.getString(R.string.submission_status_confirmed);
                break;
            }
            case STATUS_UPLOAD_PENDING: {
                status = context.getString(R.string.submission_status_upload_pending);
                break;
            }
            case STATUS_UPLOAD_IN_PROGRESS: {
                status = context.getString(R.string.submission_status_upload_in_progress);
                break;
            }
            case STATUS_SUBMITTED_PENDING_REVIEW: {
                status = context.getString(R.string.submission_status_submitted_pending_review);
                break;
            }
            case STATUS_SUBMITTED_APPROVED: {
                status = context.getString(R.string.submission_status_submitted_approved);
                break;
            }
            case STATUS_SUBMITTED_REJECTED: {
                status = context.getString(R.string.submission_status_submitted_rejected);
                break;
            }
        }
        return status;
    }

    private long generateSubmissionId(Context context) {
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return HashUtils.getSha256Long(android_id + createdTimestamp.toString());
    }
}
