package tanuj.opengridmap.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.Toast;

import tanuj.opengridmap.R;

/**
 * Created by Tanuj on 9/12/2015.
 */
public class LocationUtils {
    private static final String TAG = LocationUtils.class.getSimpleName();

    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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

    public static String toLocationStringInDegrees(Location location, final Context context) {
        String[] coords = {null, null};
        StringBuilder sb = new StringBuilder();
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();

        coords[0] = Location.convert(latitude, Location.FORMAT_SECONDS);
        coords[1] = Location.convert(longitude, Location.FORMAT_SECONDS);

        for (int i = 0; i < 2; i++) {
            String[] crds = coords[i].split(":");
            String heading = i == 0 ?
                    latitude >= 0 ? context.getString(R.string.north) : context.getString(
                            R.string.south) :
                    longitude >= 0 ? context.getString(R.string.east) : context.getString(
                            R.string.west);

            String coord = context.getString(R.string.coordinates_format, crds[0], crds[1], crds[2], heading);

            sb.append(coord);
            sb.append("\t");
        }

        return sb.toString();
    }
}
