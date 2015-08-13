package tanuj.opengridmap.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapContract.*;
import tanuj.opengridmap.models.PowerElement;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.models.Image;

/**
 * Created by Tanuj on 09-06-2015.
 */
public class OpenGridMapDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "OpenGridMap.db";

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

    public OpenGridMapDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_POWER_ELEMENTS_TABLE = CREATE_TABLE +
                PowerElementEntry.TABLE_NAME + " (" +
                PowerElementEntry._ID + TYPE_INTEGER + CONSTRAINT_PRIMARY_KEY + "," +
                PowerElementEntry.COLUMN_POWER_ELEMENT_NAME + TYPE_TEXT + CONSTRAINT_UNIQUE + CONSTRAINT_NOT_NULL + ", " +
                PowerElementEntry.COLUMN_IMAGE + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                PowerElementEntry.COLUMN_DESCRIPTION + TYPE_TEXT + ");";

        final String CREATE_SUBMISSIONS_TABLE = CREATE_TABLE +
                SubmissionEntry.TABLE_NAME + " (" +
                SubmissionEntry._ID + TYPE_INTEGER + CONSTRAINT_PRIMARY_KEY + CONSTRAINT_AUTOINCREMENT + "," +
                SubmissionEntry.COLUMN_STATUS + TYPE_INTEGER + CONSTRAINT_NOT_NULL + ", " +
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
                UploadQueueEntry.COLUMN_SUBMISSION_ID + TYPE_INTEGER + ", " +
                UploadQueueEntry.COLUMN_STATUS + TYPE_TEXT  + CONSTRAINT_NOT_NULL + ", " +
                UploadQueueEntry.COLUMN_CREATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                UploadQueueEntry.COLUMN_UPDATED_TIMESTAMP + TYPE_TEXT + CONSTRAINT_NOT_NULL + ", " +
                UploadQueueEntry.COLUMN_DELETED_TIMESTAMP + TYPE_TEXT + ", " +
                CONSTRAINT_FOREIGN_KEY + " (" + UploadQueueEntry.COLUMN_SUBMISSION_ID  + ")" + CONSTRAINT_FOREIGN_KEY_REFERENCES +
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
        addPowerElement(new PowerElement("Transformer", 0, R.drawable.transformer), db);
        addPowerElement(new PowerElement("Substation", 1, R.drawable.substation), db);
        addPowerElement(new PowerElement("Generator", 2, R.drawable.power_station), db);
        addPowerElement(new PowerElement("PV or Wind Farm", 3, R.drawable.pv_wind), db);
        addPowerElement(new PowerElement("Other", 4, R.drawable.lightening_logo), db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE + ImageEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + PowerElementSubmissionEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + SubmissionEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + PowerElementEntry.TABLE_NAME);
        db.execSQL(DROP_TABLE + UploadQueueEntry.TABLE_NAME);

        onCreate(db);
    }

    public long addSubmission(Submission submission) {
        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();
        values.put(SubmissionEntry.COLUMN_STATUS, submission.getStatus());
        values.put(SubmissionEntry.COLUMN_CREATED_TIMESTAMP, timestamp.toString());
        values.put(SubmissionEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        long res = db.insert(SubmissionEntry.TABLE_NAME, null, values);
        db.close();

        return res;
    }

    public Submission getSubmission(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + SubmissionEntry.TABLE_NAME + " where " +
                SubmissionEntry._ID + "=" + id, null);

        Submission submission = null;

        if (cursor.moveToFirst()) {
            submission = new Submission();
            submission.setId(cursor.getInt(0));
            submission.setStatus(cursor.getInt(1));
            submission.setCreatedTimestamp(getTimestampFromString(cursor.getString(2)));
            submission.setUpdatedTimestamp(getTimestampFromString(cursor.getString(3)));
        }

        cursor.close();
        db.close();

        submission.setImages(this.getImagesBySubmissionId(submission.getId()));
        submission.setPowerElements(this.getPowerElementsBySubmissionId(submission));

        return submission;
    }

    public List<Submission> getSubmissions() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + SubmissionEntry.TABLE_NAME, null);

        List<Submission> submissions = new ArrayList<Submission>();

        Submission submission = null;

        if (cursor.moveToFirst()) {
            do {
                submission = new Submission();

                submission.setId(cursor.getInt(0));
                submission.setStatus(cursor.getInt(1));
                submission.setCreatedTimestamp(getTimestampFromString(cursor.getString(2)));
                submission.setUpdatedTimestamp(getTimestampFromString(cursor.getString(3)));

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

    public void addImageToSubmission(Image image, Submission submission) {
        SQLiteDatabase db = this.getWritableDatabase();
        Timestamp timestamp = new Timestamp(new Date().getTime());

        ContentValues values = new ContentValues();
        values.put(ImageEntry.COLUMN_SUBMISSION_ID, submission.getId());
        values.put(ImageEntry.COLUMN_SRC, image.getSrc());
        values.put(ImageEntry.COLUMN_LATITUDE, image.getLocation().getLatitude());
        values.put(ImageEntry.COLUMN_LONGITUDE, image.getLocation().getLongitude());
        values.put(ImageEntry.COLUMN_BEARING, image.getLocation().getLongitude());
        values.put(ImageEntry.COLUMN_SPEED, image.getLocation().getSpeed());
        values.put(ImageEntry.COLUMN_ALTITUDE, image.getLocation().getAltitude());
        values.put(ImageEntry.COLUMN_ACCURACY, image.getLocation().getAccuracy());
        values.put(ImageEntry.COLUMN_PROVIDER, image.getLocation().getProvider());
        values.put(ImageEntry.COLUMN_CREATED_TIMESTAMP, timestamp.toString());
        values.put(ImageEntry.COLUMN_UPDATED_TIMESTAMP, timestamp.toString());

        long a = db.insert(ImageEntry.TABLE_NAME, null, values);
        Log.d("DB", a + "");
        db.close();
    }

    public List<Image> getImagesBySubmissionId(long submissionId) {
        List<Image> images = new ArrayList<Image>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + ImageEntry.TABLE_NAME + " where " +
                ImageEntry.COLUMN_SUBMISSION_ID + "=" + submissionId, null);

        Image image = null;
        Location location = null;

        if (cursor.moveToFirst()) {
            do {
                image = new Image();
                image.setId(cursor.getInt(0));
                image.setSubmissionId(cursor.getInt(1));
                image.setSrc(cursor.getString(2));

                location = new Location(cursor.getString(10));
                location.setLatitude(cursor.getDouble(3));
                location.setLongitude(cursor.getDouble(4));
                location.setBearing(cursor.getFloat(5));
                location.setSpeed(cursor.getFloat(6));
                location.setAltitude(cursor.getDouble(7));
                location.setAccuracy(cursor.getFloat(8));
//                location.setProvider(cursor.getString(10));

                image.setLocation(location);

                image.setCreatedTimestamp(getTimestampFromString(cursor.getString(11)));
                image.setUpdatedTimestamp(getTimestampFromString(cursor.getString(12)));

                images.add(image);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return images;
    }

    private void addPowerElement(PowerElement powerElement, SQLiteDatabase db) {
//        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PowerElementEntry.COLUMN_POWER_ELEMENT_NAME, powerElement.getName());
        values.put(PowerElementEntry.COLUMN_IMAGE, powerElement.getImageId());
        values.put(PowerElementEntry.COLUMN_DESCRIPTION, powerElement.getDescription());

        db.insert(PowerElementEntry.TABLE_NAME, null, values);
//        db.close();
    }

    public void addPowerElementToSubmission(PowerElement powerElement, Submission submission) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PowerElementSubmissionEntry.COLUMN_POWER_ELEMENT_ID, powerElement.getId());
        values.put(PowerElementSubmissionEntry.COLUMN_SUBMISSION_ID, submission.getId());

        db.insert(PowerElementSubmissionEntry.TABLE_NAME, null, values);
        db.close();
    }

    public PowerElement getPowerElement(int id) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + PowerElementEntry.TABLE_NAME + " where " +
                PowerElementEntry._ID + "=" + id, null);

        PowerElement powerElement = null;

        if (cursor.moveToFirst()) {
                powerElement = new PowerElement();
                powerElement.setId(cursor.getInt(0));
                powerElement.setName(cursor.getString(1));
                powerElement.setImageId(cursor.getInt(2));
                powerElement.setDescription(cursor.getString(3));
        }

        Log.d("FB", powerElement.getName());

        cursor.close();
        db.close();

        return powerElement;
    }

    public List<PowerElement> getPowerElements() {
        List<PowerElement> powerElements = new ArrayList<PowerElement>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from  " + PowerElementEntry.TABLE_NAME, null);

        PowerElement powerElement = null;

        if (cursor.moveToFirst()) {
            do {
                powerElement = new PowerElement();
                powerElement.setId(cursor.getInt(0));
                powerElement.setName(cursor.getString(1));
                powerElement.setImageId(cursor.getInt(2));
                powerElement.setDescription(cursor.getString(3));

                powerElements.add(powerElement);
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

        PowerElement powerElement = null;

        if (cursor.moveToFirst()) {
            do {
                powerElement = new PowerElement();
                powerElement.setId(cursor.getInt(0));
                powerElement.setName(cursor.getString(1));
                powerElement.setImageId(cursor.getInt(2));
                powerElement.setDescription(cursor.getString(3));

                powerElements.add(powerElement);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return powerElements;
    }

    private Timestamp getTimestampFromString(String time){
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");;
        Date date;
        Timestamp timestamp = null;
        try {
            date = formatter.parse(time);
            timestamp = new Timestamp(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
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

        PowerElement powerElement = null;

        if (cursor.moveToFirst()) {
            do {
                powerElement = new PowerElement();
                powerElement.setId(cursor.getInt(0));
                powerElement.setName(cursor.getString(1));
                powerElement.setImageId(cursor.getInt(2));
                powerElement.setDescription(cursor.getString(3));

                powerElements.add(powerElement);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return powerElements;
    }
}

