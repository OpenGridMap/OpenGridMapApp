package tanuj.opengridmap.utils;

import android.content.Context;
import android.os.Build;
import android.widget.ImageView;

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
}
