package tanuj.opengridmap.views.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;

import tanuj.opengridmap.R;
import tanuj.opengridmap.views.activities.MainActivity;

/**
 * Created by Tanuj on 10/6/2016.
 */
public class IntroActivity extends AppIntro2 {
    private int i = 1;
    private static final String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.GET_ACCOUNTS
    };

    @Override
    public void init(@Nullable Bundle savedInstanceState) {
        addSlide(AppIntroFragment.newInstance(getString(R.string.opengridmap),
                getString(R.string.string_intro),
                R.drawable.open_grid_map_logo_rounded, R.color.primary_text));

        addSlide(AppIntroFragment.newInstance(getString(R.string.permissions),
                getString(R.string.msg_request_permissions),
                android.R.drawable.ic_lock_lock,
                R.color.primary_text));

        addSlide(AppIntroFragment.newInstance("Done!", "You can now start contributing to Open Grid Map. Let's Start",
                R.drawable.ic_done_white_24px,
                R.color.primary_text));

        askForPermissions(permissions, 2);

//        showSkipButton(false);
    }

    @Override
    public void onNextPressed() {
//        pager.setCurrentItem(pager.getCurrentItem() - 1);

//        if (pager.getCurrentItem() == 1) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(permissionsArray.get(0).getPermission(), 1);
//                permissionsArray.remove(2);
//            } else {
//                pager.setCurrentItem(pager.getCurrentItem() + 1);
//                onNextPressed();
//            }
//        }

    }

    @Override
    public void onDonePressed() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSlideChanged() {
        if (pager.getCurrentItem() == 2) {
            boolean hasPermissions = hasPermissions(getApplicationContext());

            if (!hasPermissions)
            {
                pager.setCurrentItem(pager.getCurrentItem() - 1);
                Toast.makeText(IntroActivity.this, R.string.msg_permissions_required,
                        Toast.LENGTH_SHORT).show();
                askForPermissions(permissions, 2);
                onNextPressed();
            }
        }
    }

    public static boolean hasPermissions(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null &&
                permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
