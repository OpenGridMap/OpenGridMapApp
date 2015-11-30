package tanuj.opengridmap;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class SubmitActivity extends AppCompatActivity {
    private static final String TAG = SubmitActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
    }

    @Override
    protected void onUserLeaveHint() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SubmitActivityFragment fragment = (SubmitActivityFragment) fragmentManager.findFragmentById(
                R.id.fragment);
        fragment.onUserLeaveHint();
    }
}
