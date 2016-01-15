package tanuj.opengridmap.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.views.fragments.MainActivityFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getApplicationContext());
        List<Submission> submissions = dbHelper.getSubmissions(Submission.STATUS_INVALID);

        for (Submission submission: submissions) {
            Log.d(TAG, String.valueOf(submission.getId()));
        }

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
            case R.id.action_map: {
                startActivity(MapActivity.class);
                break;
            }
//            case R.id.action_settings: {
//                startActivity(SettingsActivity.class);
//                break;
//            }
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
        getMainActivityFragment().onUserLeaveHint();
    }

    private MainActivityFragment getMainActivityFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return (MainActivityFragment) fragmentManager.findFragmentById(
                R.id.fragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        getMainActivityFragment().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}