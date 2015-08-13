package tanuj.opengridmap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import tanuj.opengridmap.views.activities.MainActivity;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = WifiStateReceiver.class.getSimpleName();

    public WifiStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isWifiConnected(context)) {
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