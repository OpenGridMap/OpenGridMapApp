package tanuj.opengridmap;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ServerAuthCodeCallbacks {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final int STATE_DEFUALT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;

    private static final String KEY_SAVED_PROGRESS = "sign_in_progress";

    private static final String WEB_CLIENT_ID = "";

    private static final String SERVER_BASE_URL = "";

    private static final String EXCHANGE_TOKEN_URL = SERVER_BASE_URL + "/";

    private static final String SELECT_SCOPES_URL = SERVER_BASE_URL + "selectscopes";

    private int signInProgress;

    private PendingIntent signInIntent;

    private int signInError;

    private GoogleApiClient googleApiClient;

    private SignInButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        signInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);
        signInButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            signInProgress = savedInstanceState.getInt(KEY_SAVED_PROGRESS, STATE_DEFUALT);
        }

        googleApiClient = buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
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

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN);

//        builder = builder.requestServerAuthCode(WEB_CLIENT_ID, this);

        return builder.build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to Google API Services");

        Plus.PeopleApi.getCurrentPerson(googleApiClient);

        signInProgress = STATE_DEFUALT;
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
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
    public CheckResult onCheckServerAuthorization(String idToken, Set<Scope> set) {
        Log.i(TAG, "Checking Server's Authorizations");

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(SELECT_SCOPES_URL);
        HashSet<Scope> serverScopeSet = new HashSet<Scope>();

        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(httpResponse.getEntity());

            if (responseCode == 200) {
                String[] scopeStrings = responseBody.split(" ");
                for (String scope : scopeStrings) {
                    Log.i(TAG, "Server Scope: " + scope);
                    serverScopeSet.add(new Scope(scope));
                }
            } else {
                Log.e(TAG, "Error in getting server scopes: " + responseCode);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return CheckResult.newAuthRequiredResult(serverScopeSet);
    }

    @Override
    public boolean onUploadServerAuthCode(String idToken, String serverAuthCode) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(EXCHANGE_TOKEN_URL);

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("serverAuthCode", serverAuthCode));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            final String responseBody = EntityUtils.toString(response.getEntity());
            Log.i(TAG, "Code: " + statusCode);
            Log.i(TAG, "Resp: " + responseBody);

            return (statusCode == 200);
        } catch (ClientProtocolException e) {
            Log.e(TAG, "Error in auth code exchange.", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error in auth code exchange.", e);
            return false;
        }
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
                e.printStackTrace();
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
                            signInProgress = STATE_DEFUALT;
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
                            signInProgress = STATE_DEFUALT;
                        }
                    }).create();
        }
    }

    private class GetIdTokenTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String accountName = Plus.AccountApi.getAccountName(googleApiClient);
            String idToken = String.valueOf(params[0]);
            Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            String scopes = "audience:server:client_id:" + WEB_CLIENT_ID;
            final Context context = getApplicationContext();
            String token = null;


            try {
                token = GoogleAuthUtil.getToken(context, account, scopes);

                return token;
            } catch (UserRecoverableAuthException e) {
                e.printStackTrace();
//                Log.e(TAG, "Error Retrieving ID Token : " + e);
                return token;
            } catch (GoogleAuthException e) {
                e.printStackTrace();
//                Log.e(TAG, "Error Retrieving ID Token : " + e);
                return token;
            } catch (IOException e) {
                e.printStackTrace();
//                Log.e(TAG, "Error Retrieving ID Token : " + e);
                return token;
            }
        }

        @Override
        protected void onPostExecute(String idToken) {
            if (null != idToken) {
                Log.i(TAG, "ID token received : " + idToken);
            } else {
                Log.e(TAG, "Error Retrieving ID Token");
            }
        }
    }
}
