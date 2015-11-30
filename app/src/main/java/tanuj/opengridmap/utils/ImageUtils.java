package tanuj.opengridmap.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by Tanuj on 20/10/2015.
 */
public class ImageUtils {
    public static void setImageViewDrawable(Context context, ImageView imageView, int drawableId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setImageDrawable(context.getResources().getDrawable(
                    drawableId, context.getTheme()));
        } else {
            imageView.setImageDrawable(context.getResources().getDrawable(
                    drawableId));
        }
    }

    public static Bitmap getOptimizedImageBitmap(String src) {
        File file = new File(src);
        Bitmap bitmap = null;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        options.inDensity = 1;

        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        return bitmap;
    }
}
