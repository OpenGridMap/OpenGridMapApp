package tanuj.opengridmap.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import tanuj.opengridmap.R;

/**
 * Created by Tanuj on 12/10/2015.
 */
public class ConnectivityUtils {
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

        return wifiNetworkInfo.isAvailable() && wifiNetworkInfo.isConnected();
    }

    public static boolean isMobileDataConnected(final Context context) {
        NetworkInfo mobileNetworkInfo = getMobileNetworkInfo(context);

        return mobileNetworkInfo.isAvailable() && mobileNetworkInfo.isConnected();
    }

    public static boolean isInternetConnected(final Context context) {
        return isWifiConnected(context) || isMobileDataConnected(context);
    }

    public static boolean isConnectionPermitted(final Context context) {
        boolean wifiOnly = isWifiOnly(context);

        return (!wifiOnly && isInternetConnected(context)) ||
                (wifiOnly && isWifiConnected(context));
    }

    public static boolean isWifiOnly(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return preferences.getBoolean(context.getString(
                R.string.pref_key_sync_wifi_only), false);
    }
}
