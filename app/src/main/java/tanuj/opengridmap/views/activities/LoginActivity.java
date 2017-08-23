package tanuj.opengridmap.views.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.Plus;

import tanuj.opengridmap.R;
import tanuj.opengridmap.services.UploadService;

public class LoginActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<GoogleSignInResult> {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private static final int RC_SIGN_IN = 0;

    private static final String KEY_SAVED_PROGRESS = "sign_in_progress";

    private int signInProgress;

    private boolean signedIn = false;

    private PendingIntent signInIntent;

    private int signInError;

    private GoogleApiClient googleApiClient;

    private SignInButton signInButton;

    private LinearLayout signInSection;

    private LinearLayout spinnerSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (preferences.contains(getString(R.string.pref_key_signed_in)) &&
                preferences.getBoolean(getString(R.string.pref_key_signed_in), false)) {
            signedIn = true;
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        signInButton = findViewById(R.id.plus_sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            signInProgress = savedInstanceState.getInt(KEY_SAVED_PROGRESS, STATE_DEFAULT);
        }

        googleApiClient = buildGoogleApiClient();

        signInSection = findViewById(R.id.sign_in_section);
        spinnerSection = findViewById(R.id.spinner_section);

        OptionalPendingResult<GoogleSignInResult> googleSignInResult = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        googleSignInResult.setResultCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    private GoogleApiClient buildGoogleApiClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(UploadService.WEB_CLIENT_ID)
                .requestEmail()
                .build();

        return new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        int errorCode = connectionResult.getErrorCode();
        Log.e(TAG, "onConnectionFailed: ErrorCode : " + errorCode);

        if (errorCode == ConnectionResult.API_UNAVAILABLE) {
            Log.v(TAG, "API Unavailable");
        } else if (signInProgress != STATE_IN_PROGRESS) {
            signInIntent = connectionResult.getResolution();
            signInError = errorCode;

            if (signInProgress == STATE_SIGN_IN) {
                resolveSignInError();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.plus_sign_in_button: {
                signInProgress = STATE_IN_PROGRESS;
                signIn();
                break;
            }
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN: {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
                break;
            }
        }
    }

    @Override
    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
        handleSignInResult(googleSignInResult);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        if (result.isSuccess()) {
            showSpinnerSection();
            Log.i(TAG, "Connected to Google API Services");
            signInProgress = STATE_DEFAULT;
            editor.putBoolean(getString(R.string.pref_key_signed_in), true);

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            showSignInSection();
            signInProgress = STATE_DEFAULT;
            editor.putBoolean(getString(R.string.pref_key_signed_in), false);
        }
        editor.apply();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    private void showSignInSection() {
        signInSection.setVisibility(View.VISIBLE);
        spinnerSection.setVisibility(View.GONE);
    }

    private void showSpinnerSection() {
        signInSection.setVisibility(View.GONE);
        spinnerSection.setVisibility(View.VISIBLE);
    }

    private void resolveSignInError() {
        if (signInIntent != null) {
            try {
                signInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(signInIntent.getIntentSender(), RC_SIGN_IN, null, 0, 0,
                        0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Could not send Sign In Intent");
                signInProgress = STATE_SIGN_IN;
                googleApiClient.connect();
            }
        } else {
            createErrorDialog().show();
        }
    }

    private Dialog createErrorDialog() {
        if (GooglePlayServicesUtil.isUserRecoverableError(signInError)) {
            return GooglePlayServicesUtil.getErrorDialog(
                    signInError,
                    this,
                    RC_SIGN_IN,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            Log.e(TAG, "Google Play Services resolution Cancelled");
                            signInProgress = STATE_DEFAULT;
                        }
                    });
        } else {
            return new AlertDialog.Builder(this)
                    .setMessage(R.string.play_services_error)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.e(TAG, "Google Play Services Error could not be resolved " +
                                    signInError);
                            signInProgress = STATE_DEFAULT;
                        }
                    }).create();
        }
    }
}
