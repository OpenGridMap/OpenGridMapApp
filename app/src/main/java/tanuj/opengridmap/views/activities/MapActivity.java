package tanuj.opengridmap.views.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import tanuj.opengridmap.R;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
    }
}
