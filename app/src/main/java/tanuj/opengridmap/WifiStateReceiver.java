package tanuj.opengridmap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = WifiStateReceiver.class.getSimpleName();

    public WifiStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isWifiConnected(context)) {
            Intent serviceIntent = new Intent(context, UploadService.class);
            context.startService(serviceIntent);
//            TODO Upload Service
        }
    }

    private boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (networkInfo.isConnected()) {
            return true;
        }

        return false;
    }
}