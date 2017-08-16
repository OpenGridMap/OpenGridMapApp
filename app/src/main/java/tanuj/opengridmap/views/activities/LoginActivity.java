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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import tanuj.opengridmap.R;

public class LoginActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!signedIn) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SAVED_PROGRESS, signInProgress);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onClick(View view) {
        if (!googleApiClient.isConnecting()) {
            switch (view.getId()) {
                case R.id.plus_sign_in_button: {
                    signInProgress = STATE_SIGN_IN;
                    googleApiClient.connect();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN: {
                if (resultCode == RESULT_OK) {
                    signInProgress = STATE_SIGN_IN;
                } else {
                    signInProgress = STATE_DEFAULT;
                }

                if (!googleApiClient.isConnecting()) {
                    googleApiClient.connect();
                }
            }
        }
    }

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN);

        return builder.build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        showSpinnerSection();

        Log.i(TAG, "Connected to Google API Services");
        signInProgress = STATE_DEFAULT;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(getString(R.string.pref_key_signed_in), true);
        editor.apply();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showSignInSection();
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

        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    private void showSignInSection() {
        signInSection.setVisibility(View.VISIBLE);
        spinnerSection.setVisibility(View.GONE);
    }

    private void showSpinnerSection() {
        signInSection.setVisibility(View.GONE);
        spinnerSection.setVisibility(View.VISIBLE);
    }

//    @Override
//    public CheckResult onCheckServerAuthorization(String idToken, Set<Scope> set) {
//        Log.i(TAG, "Checking Server's Authorizations");
//
//        HttpClient httpClient = new DefaultHttpClient();
//        HttpGet httpGet = new HttpGet(SELECT_SCOPES_URL);
//        HashSet<Scope> serverScopeSet = new HashSet<Scope>();
//
//        try {
//            HttpResponse httpResponse = httpClient.execute(httpGet);
//            int responseCode = httpResponse.getStatusLine().getStatusCode();
//            String responseBody = EntityUtils.toString(httpResponse.getEntity());
//
//            if (responseCode == 200) {
//                String[] scopeStrings = responseBody.split(" ");
//                for (String scope : scopeStrings) {
//                    Log.i(TAG, "Server Scope: " + scope);
//                    serverScopeSet.add(new Scope(scope));
//                }
//            } else {
//                Log.e(TAG, "Error in getting server scopes: " + responseCode);
//            }
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return CheckResult.newAuthRequiredResult(serverScopeSet);
//    }
//
//    @Override
//    public boolean onUploadServerAuthCode(String idToken, String serverAuthCode) {
//        HttpClient httpClient = new DefaultHttpClient();
//        HttpPost httpPost = new HttpPost(EXCHANGE_TOKEN_URL);
//
//        try {
//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
//            nameValuePairs.add(new BasicNameValuePair("serverAuthCode", serverAuthCode));
//            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//
//            HttpResponse response = httpClient.execute(httpPost);
//            int statusCode = response.getStatusLine().getStatusCode();
//            final String responseBody = EntityUtils.toString(response.getEntity());
//            Log.i(TAG, "Code: " + statusCode);
//            Log.i(TAG, "Resp: " + responseBody);
//
//            return (statusCode == 200);
//        } catch (ClientProtocolException e) {
//            Log.e(TAG, "Error in auth code exchange.", e);
//            return false;
//        } catch (IOException e) {
//            Log.e(TAG, "Error in auth code exchange.", e);
//            return false;
//        }
//    }

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
//                e.printStackTrace();
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
