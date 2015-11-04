package tanuj.opengridmap.utils;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Tanuj on 30/10/2015.
 */
public class CameraUtils {
    private static final String TAG = CameraUtils.class.getSimpleName();

    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        Log.d(TAG, "Len Choices : " + choices.length);

        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width &&
                    option.getHeight() >= height) {
                bigEnough.add(option);
                Log.d(TAG, "Height : " + option.getHeight() + " Width : " + option.getWidth());
                Log.d(TAG, "Diff Height : " + (option.getHeight() - height) + " Width : " +
                        (option.getWidth() - width));
            }
        }

        if (bigEnough.size() > 0) {
            Size optimalSize = Collections.max(bigEnough, new CompareSizesByArea());
            Log.d(TAG, "Chosen Size | Height : " + optimalSize.getHeight() + " Width : " +
                    optimalSize.getWidth());
            return optimalSize;
        } else {
            Log.e(TAG, "Could not find any suitable preview area");
            return choices[0];
        }
    }

    public static Rect getZoomRect(double zoom, int width, int height) {
        int w, h, b, l, r, t;


        w = zoom > 1 ? (int) (width / zoom) : width;
        h = zoom > 1 ? (int) (height / zoom) : height;

//        Log.d(TAG, "Width : " + width + " Height : " + height);
//        Log.d(TAG, "Zoomed Width : " + w + " Zoomed Height : " + h);
//        Log.d(TAG, "Zoom level : " + zoom);

        l = (width - w) / 2;
        r = l + w;
        t = (height - h) / 2;
        b = t + h;

        return new Rect(l, t, r, b);
    }

    public static float getZoomLevelFromSeekBarProgress(int value) {
        return (float) value / 100 + 1;
    }

    public static int getSeekBarProgressFromZoomValue(double z) {
        return (int) ((z - 1) * 100);
    }

    public static float getMaxZoom(Context context, String cameraDeviceId) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics = null;

        try {
            cameraCharacteristics = manager.getCameraCharacteristics(cameraDeviceId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        float maxZoom = 4;
        try {
            maxZoom = cameraCharacteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return maxZoom;
    }

    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
