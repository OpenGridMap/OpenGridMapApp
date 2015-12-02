package tanuj.opengridmap.views.activities;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import tanuj.opengridmap.R;
import tanuj.opengridmap.views.fragments.MainActivityFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getApplicationContext();

        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!locationEnabled) {
            Toast.makeText(context, "Please Enable High Accuracy Mode in Location Settings", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_map: {
                startActivity(MapActivity.class);
                break;
            }
            case R.id.action_settings: {
                startActivity(SettingsActivity.class);
                break;
            }
            case R.id.action_about: {
                startActivity(AboutActivity.class);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void startActivity(Class activityClass) {
        Intent intent = new Intent(getApplicationContext(), activityClass);
        startActivity(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        MainActivityFragment fragment = (MainActivityFragment) fragmentManager.findFragmentById(
                R.id.fragment);
        fragment.onUserLeaveHint();
    }
}