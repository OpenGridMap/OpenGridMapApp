package tanuj.opengridmap.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

/**
 * Created by Tanuj on 09-06-2015.
 */
public class Image {
    private static final String TAG = Image.class.getSimpleName();

    public static final String IMAGE_STORE_BASE_PATH = "imageStore/";

    public static final String IMAGE_STORE_PATH = IMAGE_STORE_BASE_PATH + "images/";

    public static final String IMAGE_LIST_THUMBNAILS_PATH = IMAGE_STORE_BASE_PATH + "thumbs/";

    public static final String IMAGE_GRID_THUMBNAILS_PATH = IMAGE_STORE_BASE_PATH + "grid_thumbs/";

    public static final String TYPE_LIST = "list";

    private static final int LIST_THUMB_WIDTH = 100;

    private static final int LIST_THUMB_HEIGHT = 100;

    public static final String TYPE_GRID = "grid";

    private static final int GRID_THUMB_WIDTH = 200;

    private static final int GRID_THUMB_HEIGHT = 200;

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private long submissionId;

    private String externalDirectoryPath;

    private String src;

    private Location location;

    private Timestamp createdTimestamp;

    private Timestamp updatedTimestamp;

    public Image() {
        Timestamp timestamp = new Timestamp(new Date().getTime());

        this.createdTimestamp = timestamp;
        this.updatedTimestamp = timestamp;
    }

    public Image(String src, Location location) {
        this();

        this.location = location;
        this.src = src;
    }

    public Image(Timestamp createdTimestamp, Timestamp updatedTimestamp) {
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(long submissionId) {
        this.submissionId = submissionId;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

//    public void setCreatedTimestamp(Timestamp createdTimestamp) {
//        this.createdTimestamp = createdTimestamp;
//    }

    public Timestamp getUpdatedTimestamp() {
        return updatedTimestamp;
    }

//    public void setUpdatedTimestamp(Timestamp updatedTimestamp) {
//        this.updatedTimestamp = updatedTimestamp;
//    }

    public Bitmap getImageBitmap() {
        File file = new File(src);
        Bitmap bitmap = null;
        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return bitmap;
    }

    public Bitmap getThumbnailBitmap(String type, Context context) {
        Bitmap bitmap = null;
        File file = null;
        int width = 0, height = 0;

        switch (type) {
            case TYPE_LIST: {
                width = LIST_THUMB_WIDTH;
                height= LIST_THUMB_HEIGHT;
                break;
            }
            case TYPE_GRID: {
                width = GRID_THUMB_WIDTH;
                height= GRID_THUMB_HEIGHT;
                break;
            }
            default: {
                return bitmap;
            }
        }

//        if (type.equals(TYPE_LIST)) {
//            width = LIST_THUMB_WIDTH;
//            height= LIST_THUMB_HEIGHT;
//        } else if (type.equals(TYPE_GRID)) {
//            width = GRID_THUMB_WIDTH;
//            height= GRID_THUMB_HEIGHT;
//        } else {
//            return bitmap;
//        }

        file = getThumbnailFile(type, context);

        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Log.d(TAG, "Using Cached Thumbnail : " + file.getPath());
        } else {
            File originalImageFile = new File(src);
            bitmap = getResizedBitmap(originalImageFile, width, height);

            saveToFile(bitmap, file);

            Log.d(TAG, "Generating thumbnail : " + file.getPath());
        }

        return bitmap;
    }

//    public Bitmap getGridThumbnailBitmap() {
//        File file = getThumbnailFile("grid");
//
//        Bitmap bitmap = null;
//        if (file.exists()) {
//            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//            Log.d(TAG, "Thumbnail loaded from thumb : " + file.getPath());
//        } else {
//            File originalImageFile = new File(src);
//            bitmap = getResizedBitmap(originalImageFile, GRID_THUMB_WIDTH, GRID_THUMB_HEIGHT);
//
//            saveToFile(bitmap, file);
//        }
//
//        return bitmap;
//    }

    private Bitmap getResizedBitmap(File file, int width, int height) {
        return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getAbsolutePath()),
                width, height);
    }

    private File getThumbnailFile(String type, Context context) {
        File thumbnailFile = null;
        File file = new File(src);
        String fileName = file.getName();
        String subPath = null;

        switch (type) {
            case TYPE_LIST: {
                subPath = "/" + IMAGE_LIST_THUMBNAILS_PATH;
                break;
            }
            case TYPE_GRID: {
                subPath = "/" + IMAGE_GRID_THUMBNAILS_PATH;
                break;
            }
        }

//        if (type.equals("list")) {
//            subPath = "/" + IMAGE_LIST_THUMBNAILS_PATH;
//        } else if (type.equals("grid")){
//            subPath = "/" + IMAGE_GRID_THUMBNAILS_PATH;
//        }

        thumbnailFile = new File(context.getExternalFilesDir(subPath), fileName);

//        thumbnailFile = new File(file.getParentFile().getParentFile().getParentFile()
//                .getAbsolutePath() + subPath, fileName);

        return thumbnailFile;
    }

    private void saveToFile(Bitmap bitmap, File file) {
        Log.d(TAG, "Saving thumbnail : " + file.getPath());
        try {
            OutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.d(TAG, "Thumbnail Generated : " + file.getPath());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
