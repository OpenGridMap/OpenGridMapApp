package tanuj.opengridmap.utils;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Created by Tanuj on 9/12/2015.
 */
public class LocationUtils {

    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static void checkLocationSettingsOrLaunchSettingsIntent(Context context) {
        boolean locationEnabled = isLocationEnabled(context);

        if (!locationEnabled) {
            Toast.makeText(context, "Please Enable High Accuracy Mode in Location Settings",
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);
        }
    }
}
