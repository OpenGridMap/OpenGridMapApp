package tanuj.opengridmap.providers;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

/**
 * Created by Tanuj on 30/6/2015.
 */
public class MockLocationProvider {
    private String name;
    private Context context;

    public MockLocationProvider(String name, Context context) {
        this.name = name;
        this.context = context;

        LocationManager locationManager = (LocationManager) context.getSystemService(
                Context.LOCATION_SERVICE);

        locationManager.addTestProvider(name, false, false, false, false, true, true, true, 0, 5);

        locationManager.setTestProviderEnabled(name, true);
    }

    public void pushLocation(double latitude, double longitude) {
        LocationManager locationManager = (LocationManager) context.getSystemService(
                context.LOCATION_SERVICE);

        Location mockLocation = new Location(name);
        mockLocation.setLatitude(latitude);
        mockLocation.setLongitude(longitude);
        mockLocation.setAltitude(0);
        mockLocation.setAccuracy(12);
        mockLocation.setElapsedRealtimeNanos(1312);
        mockLocation.setTime(System.currentTimeMillis());

        locationManager.setTestProviderLocation(name, mockLocation);
    }

    public void shutdowm() {
        LocationManager locationManager = (LocationManager) context.getSystemService(
                context.LOCATION_SERVICE);

        locationManager.removeTestProvider(name);
    }
}
