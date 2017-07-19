package tanuj.opengridmap.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tanuj.opengridmap.services.BackgroundUploadService;
import tanuj.opengridmap.utils.ConnectivityUtils;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = WifiStateReceiver.class.getSimpleName();

    public WifiStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityUtils.isConnectionPermitted(context)) {
//            Intent serviceIntent = new Intent(context, BackgroundUploadService.class);
//            context.startService(serviceIntent);
        }
    }
}