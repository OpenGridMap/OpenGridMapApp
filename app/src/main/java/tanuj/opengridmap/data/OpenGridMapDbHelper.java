package tanuj.opengridmap.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tanuj.opengridmap.data.OpenGridMapContract.ImageEntry;
import tanuj.opengridmap.data.OpenGridMapContract.PowerElementEntry;
import tanuj.opengridmap.data.OpenGridMapContract.PowerElementSubmissionEntry;
import tanuj.opengridmap.data.OpenGridMapContract.SubmissionEntry;
import tanuj.opengridmap.data.OpenGridMapContract.UploadQueueEntry;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.PowerElement;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.models.UploadQueueItem;
import tanuj.opengridmap.utils.TimestampUtils;

/**
 * Created by Tanuj on 09-06-2015.
 */
public class OpenGridMapDbHelper extends SQLiteOpenHelper {
    private static final String TAG = OpenGridMapDbHelper.class.getSimpleName();

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "OpenGridMap.db";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String CONSTRAINT_PRIMARY_KEY = " PRIMARY KEY";
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_REAL = " REAL";
    private static final String CONSTRAINT_UNIQUE = " UNIQUE";
    private static final String CONSTRAINT_NOT_NULL = " NOT NULL";
    private static final String CONSTRAINT_AUTOINCREMENT = " AUTOINCREMENT";
    private static final String CONSTRAINT_FOREIGN_KEY = " FOREIGN KEY";
    private static final String CONSTRAINT_FOREIGN_KEY_REFERENCES = " REFERENCES ";
    private static final String CONSTRAINT_FOREIGN_KEY_ON_DELETE_CASCADE = " ON DELETE CASCADE";
    private static final String CONSTRAINT_FOREIGN_KEY_ON_DELETE_SET_NULL = " ON DELETE SET NULL";
    private static final String DESC = " desc";

    private static final String[] submissionColumns = {
            SubmissionEntry._ID,
            SubmissionEntry.COLUMN_STATUS,
            SubmissionEntry.COLUMN_SUBMISSION_ID,
            SubmissionEntry.COLUMN_CREATED_TIMESTAMP,
            SubmissionEntry.COLUMN_UPDATED_TIMESTAMP};

    private static final String[] imageColumns = {
            ImageEntry._ID,
            ImageEntry.COLUMN_SUBMISSION_ID,
            ImageEntry.COLUMN_SRC,
            ImageEntry.COLUMN_PROVIDER,
            ImageEntry.COLUMN_LATITUDE,
            ImageEntry.COLUMN_LONGITUDE,
            ImageEntry.COLUMN_BEARING,
            ImageEntry.COLUMN_SPEED,
            ImageEntry.COLUMN_ALTITUDE,
            ImageEntry.COLUMN_ACCURACY,
            ImageEntry.COLUMN_CREATED_TIMESTAMP,
            ImageEntry.COLUMN_UPDATED_TIMESTAMP};

    private static final String[] powerElementColumns = {
            PowerElementEntry._ID,
            PowerElementEntry.COLUMN_POWER_ELEMENT_NAME};

    private static final String[] uploadQueueColumns = {
            UploadQueueEntry._ID,
            UploadQueueEntry.COLUMN_SUBMISSION_ID,
            UploadQueueEntry.COLUMN_STATUS,
            UploadQueueEntry.COLUMN_PAYLOADS_UPLOADED,
            UploadQueueEntry.COLUMN_CREATED_TIMESTAMP,
            UploadQueueEntry.COLUMN_UPDATED_TIMESTAMP};

    public OpenGridMapDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_POWER_ELEMENTS_TABLE = CREATE_TABLE +
                PowerElementEntry.TABLE_NAME + " (" +
                PowerElementEntry._ID + TYPE_INTEGER + CONSTRAINT_PRIMARY_KEY + "," +
                PowerElementEntry.COLUMN_POWER_ELEMENT_NAME + TYPE_TEXT + CONSTRAINT_UNIQUE +
                CONSTRAINT_NOT_NULL + ", " + PowerElementEntry.COLUMN_DESCRIPTION + TYPE_TEXT + ");";

        final String CREATE_SUBMISSIONS_TABLE = CREATE_TABLE +
                SubmissionEntry.TABLE_NAME + " (" +
                SubmissionEntry._ID + TYPE_INTEGER + CONSTRAINT_PRIMARY_KEY + CONSTRAINT_AUTOINCREMENT + "," +
                SubmissionEntry.COLUMN_STATUS + TYPE_INTEGER + CONSTRAINT_NOT_NULL + ", " +
                SubmissionEntry.COLUMN_SUBMISSION_ID + TYPE_TEXT + "," +
                SubmissionEntry.COLUMN_CREATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                SubmissionEntry.COLUMN_UPDATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                SubmissionEntry.COLUMN_DELETED_TIMESTAMP + TYPE_TEXT + ");";

        final String CREATE_POWER_ELEMENT_SUBMISSION_TABLE = CREATE_TABLE +
                PowerElementSubmissionEntry.TABLE_NAME + " (" +
                PowerElementSubmissionEntry.COLUMN_POWER_ELEMENT_ID + TYPE_INTEGER + CONSTRAINT_NOT_NULL + ", " +
                PowerElementSubmissionEntry.COLUMN_SUBMISSION_ID + TYPE_INTEGER + CONSTRAINT_NOT_NULL + ", " +
                CONSTRAINT_FOREIGN_KEY + " (" + PowerElementSubmissionEntry.COLUMN_POWER_ELEMENT_ID + ")" + CONSTRAINT_FOREIGN_KEY_REFERENCES +
                PowerElementEntry.TABLE_NAME + "(" + PowerElementEntry._ID + ")" + CONSTRAINT_FOREIGN_KEY_ON_DELETE_CASCADE + " ," +
                CONSTRAINT_FOREIGN_KEY + " (" + PowerElementSubmissionEntry.COLUMN_SUBMISSION_ID + ")" + CONSTRAINT_FOREIGN_KEY_REFERENCES +
                SubmissionEntry.TABLE_NAME + "(" + SubmissionEntry._ID + ")" + CONSTRAINT_FOREIGN_KEY_ON_DELETE_CASCADE + ");";

        final String CREATE_IMAGE_TABLE = CREATE_TABLE +
                ImageEntry.TABLE_NAME + " (" +
                ImageEntry._ID + TYPE_INTEGER + CONSTRAINT_PRIMARY_KEY + CONSTRAINT_AUTOINCREMENT + "," +
                ImageEntry.COLUMN_SUBMISSION_ID + TYPE_INTEGER + ", " +
                ImageEntry.COLUMN_SRC + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_LATITUDE + TYPE_REAL + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_LONGITUDE + TYPE_REAL + CONSTRAINT_NOT_NULL + "," +
                ImageEntry.COLUMN_BEARING + TYPE_REAL + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_SPEED + TYPE_REAL + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_ALTITUDE + TYPE_REAL + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_ACCURACY + TYPE_REAL + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_NO_OF_SATELLITES + TYPE_INTEGER + ", " +
                ImageEntry.COLUMN_PROVIDER + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_CREATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_UPDATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                ImageEntry.COLUMN_DELETED_TIMESTAMP + TYPE_TEXT + ", " +
                CONSTRAINT_FOREIGN_KEY + " (" + ImageEntry.COLUMN_SUBMISSION_ID + ")" + CONSTRAINT_FOREIGN_KEY_REFERENCES +
                SubmissionEntry.TABLE_NAME + " (" + SubmissionEntry._ID + ") " +
                CONSTRAINT_FOREIGN_KEY_ON_DELETE_SET_NULL + ");";

        final String CREATE_UPLOAD_QUEUE_TABLE = CREATE_TABLE +
                UploadQueueEntry.TABLE_NAME + " (" +
                ImageEntry._ID + TYPE_INTEGER + CONSTRAINT_PRIMARY_KEY + CONSTRAINT_AUTOINCREMENT + "," +
                UploadQueueEntry.COLUMN_SUBMISSION_ID + TYPE_INTEGER + ", " +
                UploadQueueEntry.COLUMN_STATUS + TYPE_TEXT  + CONSTRAINT_NOT_NULL + ", " +
                UploadQueueEntry.COLUMN_PAYLOADS_UPLOADED + TYPE_INTEGER + ", " +
                UploadQueueEntry.COLUMN_CREATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                UploadQueueEntry.COLUMN_UPDATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                CONSTRAINT_FOREIGN_KEY + " (" + UploadQueueEntry.COLUMN_SUBMISSION_ID  + ") " + CONSTRAINT_FOREIGN_KEY_REFERENCES +
                SubmissionEntry.TABLE_NAME + " (" + SubmissionEntry._ID + ") " +
                CONSTRAINT_FOREIGN_KEY_ON_DELETE_CASCADE + ");";

        db.execSQL(CREATE_POWER_ELEMENTS_TABLE);
        db.execSQL(CREATE_SUBMISSIONS_TABLE);
        db.execSQL(CREATE_POWER_ELEMENT_SUBMISSION_TABLE);
        db.execSQL(CREATE_IMAGE_TABLE);
        db.execSQL(CREATE_UPLOAD_QUEUE_TABLE);

        seedPowerElementsTable(db);
    }

    private void seedPowerElementsTable(SQLiteDatabase db) {
        for (PowerElement powerElement: PowerElementsSeedData.powerElements) {
            seedPowerElement(powerElement, db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE + UploadQueueEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + ImageEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + PowerElementSubmissionEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + SubmissionEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + PowerElementEntry.TABLE_NAME);

        onCreate(db);
    }

    public long addSubmission(Submission submission) {
        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();
        values.put(SubmissionEntry.COLUMN_STATUS, submission.getStatus());
        values.put(SubmissionEntry.COLUMN_SUBMISSION_ID, submission.getSubmissionPayloadsId());
        values.put(SubmissionEntry.COLUMN_CREATED_TIMESTAMP, timestamp.toString());
        values.put(SubmissionEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        long res = db.insert(SubmissionEntry.TABLE_NAME, null, values);
        db.close();

        return res;
    }

    public Submission getSubmission(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(SubmissionEntry.TABLE_NAME, submissionColumns, SubmissionEntry._ID + "= ?",
                new String[]{Long.toString(id)}, null, null, null);

        Submission submission = null;

        if (cursor.moveToFirst()) {
            submission = getSubmissionFromCursor(cursor);
        }

        cursor.close();
        db.close();

        if (null != submission) {
            submission.setImages(this.getImagesBySubmissionId(submission.getId()));
            submission.setPowerElements(this.getPowerElementsBySubmissionId(submission));
        }


        return submission;
    }

    public List<Submission> getSubmissions(int minStatus) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(SubmissionEntry.TABLE_NAME, submissionColumns, SubmissionEntry.COLUMN_STATUS
                + " >= ?", new String[] {Integer.toString(minStatus)}, null, null,
                SubmissionEntry.COLUMN_CREATED_TIMESTAMP + DESC);

        List<Submission> submissions = new ArrayList<Submission>();

        Submission submission;

        if (cursor.moveToFirst()) {
            do {
                submission = getSubmissionFromCursor(cursor);

                submissions.add(submission);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        for (Submission sub : submissions) {
            sub.setImages(this.getImagesBySubmissionId(sub.getId()));
            sub.setPowerElements(this.getPowerElementsBySubmissionId(sub));
        }

        return submissions;
    }

    private Submission getSubmissionFromCursor(Cursor cursor) {
        long id = cursor.getLong(0);
        int status = cursor.getInt(1);
        long submissionId = cursor.getLong(2);
        Timestamp createdAtTimestamp = TimestampUtils.getTimestampFromString(cursor.getString(3));
        Timestamp updatedAtTimestamp = TimestampUtils.getTimestampFromString(cursor.getString(4));

        return new Submission(id, status, submissionId, createdAtTimestamp, updatedAtTimestamp);
    }

    public long addImageToSubmission(Image image, Submission submission) {
        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();

        values.put(ImageEntry.COLUMN_SUBMISSION_ID, submission.getId());
        values.put(ImageEntry.COLUMN_SRC, image.getSrc());

        Location location = image.getLocation();

        values.put(ImageEntry.COLUMN_LATITUDE, location.getLatitude());
        values.put(ImageEntry.COLUMN_LONGITUDE, location.getLongitude());
        values.put(ImageEntry.COLUMN_BEARING, location.getLongitude());
        values.put(ImageEntry.COLUMN_SPEED, location.getSpeed());
        values.put(ImageEntry.COLUMN_ALTITUDE, location.getAltitude());
        values.put(ImageEntry.COLUMN_ACCURACY, location.getAccuracy());
        values.put(ImageEntry.COLUMN_PROVIDER, location.getProvider());
        values.put(ImageEntry.COLUMN_CREATED_TIMESTAMP, timestamp.toString());
        values.put(ImageEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        long res = db.insert(ImageEntry.TABLE_NAME, null, values);
        db.close();

        return res;
    }

    public void updateSubmissionStatus(Submission submission, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();

        values.put(SubmissionEntry.COLUMN_STATUS, status);
        values.put(SubmissionEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        db.update(SubmissionEntry.TABLE_NAME, values, SubmissionEntry._ID + " = ?", new String[]{
                Long.toString(submission.getId())});

        submission.setStatus(status);

        db.close();
    }

    public int getSubmissionStatus(Submission submission) {
        SQLiteDatabase db = this.getReadableDatabase();

        final String[] columns = {SubmissionEntry.COLUMN_STATUS};

        Cursor cursor = db.query(SubmissionEntry.TABLE_NAME, columns, SubmissionEntry._ID + " = ?",
                new String[] {Long.toString(submission.getId())}, null, null, null);

        int status = -3;
        if (cursor.moveToFirst()) {
            status = cursor.getInt(0);
        }
        cursor.close();
        db.close();

        return status;
    }

    public List<Image> getImages() {
        List<Image> images = new ArrayList<Image>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(ImageEntry.TABLE_NAME, imageColumns, null, null, null, null, null,
                null);

        if (cursor.moveToFirst()) {
            do {
                images.add(getImageFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return images;
    }

    public List<Image> getImagesBySubmissionId(long submissionId) {
        List<Image> images = new ArrayList<Image>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(ImageEntry.TABLE_NAME, imageColumns, ImageEntry.COLUMN_SUBMISSION_ID +
                "= ?",  new String[] {Long.toString((submissionId))}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                images.add(getImageFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return images;
    }

    private Image getImageFromCursor(Cursor cursor) {
        int id = cursor.getInt(0);
        long submissionId = cursor.getLong(1);
        String src = cursor.getString(2);
        String provider = cursor.getString(3);
        double latitude = cursor.getDouble(4);
        double longitude = cursor.getDouble(5);
        float bearing = cursor.getFloat(6);
        float speed = cursor.getFloat(7);
        double altitude = cursor.getDouble(8);
        float accuracy = cursor.getFloat(9);
        Timestamp createdTimestamp = TimestampUtils.getTimestampFromString(cursor.getString(10));
        Timestamp updatedTimestamp = TimestampUtils.getTimestampFromString(cursor.getString(11));

        Location location = new Location(provider);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setBearing(bearing);
        location.setSpeed(speed);
        location.setAltitude(altitude);
        location.setAccuracy(accuracy);

        Image image = new Image(createdTimestamp, updatedTimestamp);
        image.setId(id);
        image.setSubmissionId(submissionId);
        image.setSrc(src);
        image.setLocation(location);

        return image;
    }

    private void seedPowerElement(PowerElement powerElement, SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        values.put(PowerElementEntry._ID, powerElement.getId());
        values.put(PowerElementEntry.COLUMN_POWER_ELEMENT_NAME, powerElement.getName());
        values.put(PowerElementEntry.COLUMN_DESCRIPTION, powerElement.getDescription());

        db.insert(PowerElementEntry.TABLE_NAME, null, values);
    }

    public void addPowerElementToSubmission(PowerElement powerElement, Submission submission) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(PowerElementSubmissionEntry.COLUMN_POWER_ELEMENT_ID, powerElement.getId());
        values.put(PowerElementSubmissionEntry.COLUMN_SUBMISSION_ID, submission.getId());

        db.insert(PowerElementSubmissionEntry.TABLE_NAME, null, values);
        db.close();
    }

    public PowerElement getPowerElement(long id) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(PowerElementEntry.TABLE_NAME, powerElementColumns,
                PowerElementEntry._ID + "= ?", new String[]{Long.toString(id)}, null, null, null);

        PowerElement powerElement = null;

        if (cursor.moveToFirst()) {
            powerElement = getPowerElementFromCursor(cursor);
        }

        cursor.close();
        db.close();

        return powerElement;
    }

    public List<PowerElement> getPowerElements() {
        List<PowerElement> powerElements = new ArrayList<PowerElement>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(PowerElementEntry.TABLE_NAME, powerElementColumns, null, null,
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                powerElements.add(getPowerElementFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return powerElements;
    }

    public ArrayList<PowerElement> getPowerElementsBySubmissionId(Submission submission) {
        ArrayList<PowerElement> powerElements = new ArrayList<PowerElement>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + PowerElementEntry.TABLE_NAME + " where " +
                PowerElementEntry._ID + " in (select " +
                PowerElementSubmissionEntry.COLUMN_POWER_ELEMENT_ID + " from " +
                PowerElementSubmissionEntry.TABLE_NAME + " where " +
                PowerElementSubmissionEntry.COLUMN_SUBMISSION_ID + "=" +
                submission.getId() + ")", null);

        if (cursor.moveToFirst()) {
            do {
                powerElements.add(getPowerElementFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return powerElements;
    }

    private PowerElement getPowerElementFromCursor(Cursor cursor) {
        long id = cursor.getLong(0);
        String name = cursor.getString(1);
//        String description = cursor.getString(2);

        return new PowerElement(id, name);
    }

    public ArrayList<PowerElement> getNotTaggedPowerElements(Submission submission) {
        ArrayList<PowerElement> powerElements = new ArrayList<PowerElement>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + PowerElementEntry.TABLE_NAME + " where " +
                PowerElementEntry._ID + " not in (select " +
                PowerElementSubmissionEntry.COLUMN_POWER_ELEMENT_ID + " from " +
                PowerElementSubmissionEntry.TABLE_NAME + " where " +
                PowerElementSubmissionEntry.COLUMN_SUBMISSION_ID + "=" +
                submission.getId() + ")", null);

        if (cursor.moveToFirst()) {
            do {
                powerElements.add(getPowerElementFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return powerElements;
    }

    public long addQueueItem(UploadQueueItem item) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(UploadQueueEntry.COLUMN_SUBMISSION_ID, item.getSubmissionPayloadsId());
        values.put(UploadQueueEntry.COLUMN_STATUS, item.getStatus());
        values.put(UploadQueueEntry.COLUMN_PAYLOADS_UPLOADED, item.getPayloadsUploadedString());
        values.put(UploadQueueEntry.COLUMN_CREATED_TIMESTAMP, item.getCreatedAtTimestamp().
                toString());
        values.put(UploadQueueEntry.COLUMN_UPDATED_TIMESTAMP, item.getUpdatedAtTimestamp().
                toString());

        long res = db.insert(UploadQueueEntry.TABLE_NAME, null, values);
        db.close();

        return res;
    }

    public void updateQueueItemStatus(UploadQueueItem queueItem, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();

        values.put(UploadQueueEntry.COLUMN_STATUS, status);
        values.put(UploadQueueEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        db.update(UploadQueueEntry.TABLE_NAME, values, UploadQueueEntry._ID + " = ?", new String[]{
                Long.toString(queueItem.getId())});

        queueItem.setStatus(status);
        db.close();
    }

    public JSONObject getQueueItemPayloadsUploaded(UploadQueueItem queueItem) {
        JSONObject payloadsUploaded = null;
        SQLiteDatabase db = this.getReadableDatabase();

        final String[] columns = {UploadQueueEntry.COLUMN_PAYLOADS_UPLOADED};

        Cursor cursor = db.query(UploadQueueEntry.TABLE_NAME, columns, UploadQueueEntry._ID + " = ?"
                , new String[] {Long.toString(queueItem.getId())}, null, null, null);

        if (cursor.moveToFirst()) {
            String str = cursor.getString(0);

            try {
                payloadsUploaded = new JSONObject(str);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        cursor.close();
        db.close();

        return payloadsUploaded;
    }

    public void updateQueueItemPayloadUploads(UploadQueueItem queueItem, long n) {
        JSONObject payloadsUploaded = getQueueItemPayloadsUploaded(queueItem);

        try {
            payloadsUploaded.put(Long.toString(n), true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        queueItem.setPayloadsUploaded(payloadsUploaded);

        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();

        values.put(UploadQueueEntry.COLUMN_PAYLOADS_UPLOADED, payloadsUploaded.toString());
        values.put(UploadQueueEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        db.update(UploadQueueEntry.TABLE_NAME, values, UploadQueueEntry._ID + " = ?", new String[]{
                Long.toString(queueItem.getId())});

        db.close();
    }

    public ArrayList<UploadQueueItem> getUploadQueue() {
        ArrayList<UploadQueueItem> uploadQueueItems = new ArrayList<UploadQueueItem>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(UploadQueueEntry.TABLE_NAME, uploadQueueColumns,
                UploadQueueEntry.COLUMN_STATUS + " < ?",
                new String[]{Integer.toString(UploadQueueItem.STATUS_UPLOAD_COMPLETE)}, null, null,
                null);

        if (cursor.moveToFirst()) {
            do {
                uploadQueueItems.add(getUploadQueueItemFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return uploadQueueItems;
    }

    public UploadQueueItem getPendingQueueItem() {
        UploadQueueItem queueItem = null;

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(UploadQueueEntry.TABLE_NAME, uploadQueueColumns,
                UploadQueueEntry.COLUMN_STATUS + " < ?",
                new String[]{Integer.toString(UploadQueueItem.STATUS_UPLOAD_COMPLETE)}, null,
                null, null, Integer.toString(1));

        if (cursor.moveToFirst()) {
            queueItem = getUploadQueueItemFromCursor(cursor);
        }

        cursor.close();
        db.close();

        return queueItem;
    }

    private UploadQueueItem getUploadQueueItemFromCursor(Cursor cursor) {
        long id = cursor.getLong(0);
        long submissionId = cursor.getLong(1);
        int status = cursor.getInt(2);
        String payloadsUploaded = cursor.getString(3);
        Timestamp createdAtTimestamp = TimestampUtils.getTimestampFromString(cursor.getString(4));
        Timestamp updatedAtTimestamp = TimestampUtils.getTimestampFromString(cursor.getString(5));

        Submission submission = getSubmission(submissionId);

        return new UploadQueueItem(id, submission, status, payloadsUploaded, createdAtTimestamp,
                updatedAtTimestamp);
    }

    public boolean deleteQueueItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int res = db.delete(UploadQueueEntry.TABLE_NAME, UploadQueueEntry._ID + " = ?",
                new String[]{Long.toString(id)});
        db.close();

        return res > 0;
    }
}

