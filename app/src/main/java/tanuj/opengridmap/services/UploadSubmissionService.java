package tanuj.opengridmap.services;

import android.accounts.Account;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import tanuj.opengridmap.data.PGISRestClient;
import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.exceptions.MemoryLowException;
import tanuj.opengridmap.models.Submission;

public class UploadSubmissionService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = UploadSubmissionService.class.getSimpleName();

    public static final String UPLOAD_UPDATE_BROADCAST = "tanuj.opengridmap.upload.update";

    public static final short LOW_MEMORY = -4;

    public static final short NO_INTERNET_CONNECTIVITY = -3;

    public static final short SUBMISSION_NOT_FOUND = -2;

    public static final short UPLOAD_STATUS_FAIL = -1;

    public static final short UPLOAD_STATUS_SUCCESS = 100;

    private static final short MAX_UPLOAD_ATTEMPTS = 3;

    private static final String WEB_CLIENT_ID = "498377614550-0q8d0e0fott6qm0rvgovd4o04f8krhdb.apps.googleusercontent.com";

    private static final String ACTION_UPLOAD = "tanuj.opengridmap.services.action.upload";

    private static final String EXTRA_SUBMISSION_ID = "tanuj.opengridmap.services.extra.upload_queue_item";

    private int getPayloadAttempts = 0;

    private GoogleApiClient googleApiClient;

    private long submissionId;

    public UploadSubmissionService() {
        super("UploadSubmissionService");
    }

    public static void startUpload(Context context, long submissionId) {
        Intent intent = new Intent(context, UploadSubmissionService.class);
        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(EXTRA_SUBMISSION_ID, submissionId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                submissionId = intent.getLongExtra(EXTRA_SUBMISSION_ID, -1);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Upload Service OnCreate");
        googleApiClient = buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            Log.d(TAG, "Disconnected from Google Play Services");
        }

        Log.v(TAG, "Stopping " + TAG);
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Connected to Google Play Services");
        new GetIdTokenTask().execute();
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

    private void handleUpload(String jsonPayload) {
        try {
            handlePayload(jsonPayload);
        } catch (OutOfMemoryError e) {
            if (getPayloadAttempts++ < MAX_UPLOAD_ATTEMPTS) {
                System.gc();
                handleUpload(jsonPayload);
            } else {
                handleFailure();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getJsonPayload(String idToken) throws MemoryLowException {
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getApplicationContext());
        Submission submission = dbHelper.getSubmission(submissionId);
        dbHelper.close();

        if (submission == null)
            broadcastUpdate(SUBMISSION_NOT_FOUND);

        return submission.getUploadPayload(getApplicationContext(), idToken, 0)
                        .getPayloadEntity();
    }

    public void handlePayload(final String json) throws IOException {
        PGISRestClient.postSubmission(this, json, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d(TAG, "Starting Upload");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                super.onSuccess(statusCode, headers, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                Log.d(TAG, response.toString());

                try {
                    if (response.has(getString(R.string.response_key_status)) &&
                            response.getString(getString(R.string.response_key_status))
                                    .equals(getString(R.string.response_status_ok))) {
                        handleSuccess();
                    } else if (response.has(getString(R.string.response_key_status)) &&
                            response.getString(getString(R.string.response_key_status))
                                    .equals(getString(R.string.response_status_error))) {
                            handleFailure();
                    } else {
                        handleFailure();
                    }
                } catch (JSONException e) {
                    handleFailure();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                handleFailure(throwable, responseString);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                handleFailure(throwable, errorResponse);
            }
        });
    }

    private void handleFailure() {
        broadcastUpdate(UPLOAD_STATUS_FAIL);

//        Context context = getApplicationContext();
//        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
//        dbHelper.getSubmission(submissionId).deleteSubmission(context);
//        dbHelper.close();
    }

    private void handleFailure(Throwable throwable, String response) {
        if (throwable instanceof IOException)
            broadcastUpdate(NO_INTERNET_CONNECTIVITY);

        Log.d(TAG, "hf1");
        Log.d(TAG, response);

//        handleFailure();
    }

    private void handleFailure(Throwable throwable, JSONObject response) {
        try {
            if (response.has(getString(R.string.response_key_status)) &&
                    response.getString(getString(R.string.response_key_status))
                            .equals(getString(R.string.response_status_error))) {
                broadcastUpdate(UPLOAD_STATUS_FAIL);
            } else {
                if (throwable instanceof IOException)
                    broadcastUpdate(NO_INTERNET_CONNECTIVITY);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }



        Log.d(TAG, "hf2");
        Log.d(TAG, response.toString());
//        handleFailure();
    }

    private void handleSuccess() {
        Context context = getApplicationContext();
        broadcastUpdate(UPLOAD_STATUS_SUCCESS);
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        dbHelper.getSubmission(submissionId).deleteSubmission(context);
        dbHelper.close();
        Log.d(TAG, "Submission " + submissionId + " successfully uploaded");
    }

    private void broadcastUpdate(short uploadCompletion) {
        Log.v(TAG, "Upload Completion for Submission " + submissionId + " : " + uploadCompletion);

        Intent intent = new Intent(UPLOAD_UPDATE_BROADCAST);
        intent.putExtra(getString(R.string.key_submission_id), submissionId);
        intent.putExtra(getString(R.string.key_upload_completion), uploadCompletion);

        sendBroadcast(intent);

    }

    private class GetIdTokenTask extends AsyncTask<Void, Void, Void> {
        private String idToken;

        @Override
        protected Void doInBackground(Void... params) {
            idToken = getIdToken();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (idToken == null) {
                Log.e(TAG, "Error Retrieving idToken");
            }
            System.gc();
//            handleUpload(idToken);
            new GetPayloadTask().execute(idToken);
        }

        private String getIdToken() {
            String accountName = Plus.AccountApi.getAccountName(googleApiClient);
            Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            String scopes = "audience:server:client_id:" + WEB_CLIENT_ID;
            String token = null;
            final Context context = getApplicationContext();

            try {
                token = GoogleAuthUtil.getToken(context, account, scopes);
            } catch (UserRecoverableAuthException e) {
                e.printStackTrace();
                Log.e(TAG, "Error Retrieving ID Token : " + e);
            } catch (GoogleAuthException e) {
                e.printStackTrace();
                Log.e(TAG, "Error Retrieving ID Token : " + e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error Retrieving ID Token : " + e);
            }

            return token;
        }
    }

    private class GetPayloadTask extends AsyncTask<String, Void, Void> {
        private String jsonPayload;
        private Exception e;

        @Override
        protected Void doInBackground(String... params) {
            try {
                jsonPayload = getJsonPayload(params[0]);
            } catch (MemoryLowException e) {
                e.printStackTrace();
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (e == null) {
                handleUpload(jsonPayload);
            } else {
                broadcastUpdate(LOW_MEMORY);
            }
        }
    }
}