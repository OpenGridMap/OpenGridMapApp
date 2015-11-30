package tanuj.opengridmap.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tanuj.opengridmap.utils.ConnectivityUtil;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = WifiStateReceiver.class.getSimpleName();

    public WifiStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityUtil.isConnectionPermitted(context)) {
//            Intent serviceIntent = new Intent(context, UploadService.class);
//            context.startService(serviceIntent);
        }
    }
}