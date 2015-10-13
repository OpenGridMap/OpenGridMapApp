package tanuj.opengridmap.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Tanuj on 12/10/2015.
 */
public class ConnectivityUtil {
    public static NetworkInfo getWifiNetworkInfo(final Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    private static NetworkInfo getMobileNetworkInfo(final Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isWifiConnected(final Context context) {
        NetworkInfo wifiNetworkInfo = getWifiNetworkInfo(context);

        if (wifiNetworkInfo.isAvailable() && wifiNetworkInfo.isConnected()) {
            return true;
        }

        return false;
    }

    public static boolean isMobileDataConnected(final Context context) {
        NetworkInfo mobileNetworkInfo = getMobileNetworkInfo(context);

        if (mobileNetworkInfo.isAvailable() && mobileNetworkInfo.isConnected()) {
            return true;
        }

        return true;
    }
}
