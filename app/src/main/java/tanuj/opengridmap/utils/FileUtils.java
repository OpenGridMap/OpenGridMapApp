package tanuj.opengridmap.utils;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import java.io.File;

import tanuj.opengridmap.BuildConfig;

/**
 * Created by Tanuj on 1/1/2017.
 */

public class FileUtils {
    public static Uri getOutputMediaFileUri(Activity activity, File file){
        return FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider",
                file);
    }

    public static File getStorageDir(Activity activity) {
        return new File(activity.getExternalFilesDir("ogm"), "images");
    }

    public static File getTempMediaFile(Activity activity){
        File storageDir = getStorageDir(activity);

        if (!storageDir.exists()){
            if (!storageDir.mkdirs()){
                return null;
            }
        }

//        return new File(storageDir.getPath() + File.separator + "TEMP_IMG.jpg");
        return new File(storageDir.getPath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".jpg");
    }
}
