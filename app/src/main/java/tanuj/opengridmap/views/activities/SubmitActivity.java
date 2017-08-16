package tanuj.opengridmap.views.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import tanuj.opengridmap.R;
import tanuj.opengridmap.views.fragments.SubmitActivityFragment;

public class SubmitActivity extends AppCompatActivity {
    private static final String TAG = SubmitActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }

    @Override
    protected void onUserLeaveHint() {
        SubmitActivityFragment fragment = getSubmitActivityFragment();

        if (fragment != null)
            fragment.onUserLeaveHint();
    }

    private SubmitActivityFragment getSubmitActivityFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return (SubmitActivityFragment) fragmentManager.findFragmentById(
                R.id.fragment);
    }

    @Override
    public void finish() {
        processImageFile();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        processImageFile();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        processImageFile();
        super.onBackPressed();
    }

    private void processImageFile() {
        SubmitActivityFragment fragment = getSubmitActivityFragment();

        if (fragment != null)
            fragment.onFinish();
    }
}
