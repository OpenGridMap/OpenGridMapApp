package tanuj.opengridmap.views.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import tanuj.opengridmap.R;
import tanuj.opengridmap.SubmissionsActivity;
import tanuj.opengridmap.views.fragments.MainActivityFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_submissions: {
                startActivity(SubmissionsActivity.class);
                break;
            }
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
        MainActivityFragment mainActivityFragment = getMainActivityFragment();
        if (mainActivityFragment != null) {
            mainActivityFragment.onUserLeaveHint();
        }
    }

    private MainActivityFragment getMainActivityFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        return (MainActivityFragment) fragmentManager.findFragmentById(R.id.fragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MainActivityFragment fragment = getMainActivityFragment();

        if (fragment != null)
            fragment.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }
}