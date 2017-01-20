package tanuj.opengridmap.utils;

import android.app.Activity;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;

import tanuj.opengridmap.BuildConfig;

/**
 * Created by Tanuj on 1/1/2017.
 */

public class FileUtils {
    public static Uri getOutputMediaFileUri(Activity activity, File file){
//        return Uri.fromFile(getOutputMediaFile());
        return FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider",
                file);
    }

    public static File getOutputMediaFile(Activity activity){
        File storageDir = new File(activity.getExternalFilesDir(""), "images");

        if (!storageDir.exists()){
            if (!storageDir.mkdirs()){
                return null;
            }
        }

        return new File(storageDir.getPath() + File.separator + "TEMP_IMG.jpg");
    }
}
