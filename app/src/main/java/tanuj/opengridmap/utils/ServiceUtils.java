package tanuj.opengridmap.utils;

import android.app.ActivityManager;

import tanuj.opengridmap.services.LocationService;

/**
 * Created by Tanuj on 20/11/2015.
 */
public class ServiceUtils {
    public static boolean isMyServiceRunning(ActivityManager activityManager) {
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}