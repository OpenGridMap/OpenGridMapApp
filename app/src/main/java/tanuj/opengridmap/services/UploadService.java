package tanuj.opengridmap.services;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Payload;
import tanuj.opengridmap.models.UploadQueueItem;
import tanuj.opengridmap.utils.ConnectivityUtil;

public class UploadService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = UploadService.class.getSimpleName();

    public static final String UPLOAD_UPDATE_BROADCAST = "tanuj.opengridmap.upload.update";

    private static final int MAX_UPLOAD_ATTEMPTS = 3;

    private static final String SERVER_BASE_URL = "http://vmjacobsen39.informatik.tu-muenchen.de";

    private static final String WEB_CLIENT_ID = "498377614550-0q8d0e0fott6qm0rvgovd4o04f8krhdb.apps.googleusercontent.com";

    private static final String TOKEN_URL = SERVER_BASE_URL + "/submissions/create";

    private OpenGridMapDbHelper dbHelper;

    private GoogleApiClient googleApiClient;

    Intent intent;

    public UploadService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Upload Service OnCreate");
        final Context context = getApplicationContext();

        googleApiClient = buildGoogleApiClient();
        dbHelper = new OpenGridMapDbHelper(context);
        intent = new Intent(UPLOAD_UPDATE_BROADCAST);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void processUploadQueue() {
        if (ConnectivityUtil.isConnectionPermitted(getApplicationContext())) {
            new HandlePayloadsTask().execute();
        } else {
            Log.d(TAG, "Permitted Connectivity not available");
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        dbHelper.close();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            Log.d(TAG, "Disconnected from Google Play Services");
        }
        Log.v(TAG, "Stopping UploadService");
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Connected to Google Play Services");
        processUploadQueue();
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

    private class HandlePayloadsTask extends AsyncTask<Void, UploadQueueItem, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            processQueue();
            return null;
        }

        @Override
        protected void onProgressUpdate(UploadQueueItem... values) {
            super.onProgressUpdate(values);
            UploadQueueItem queueItem = values != null ? values[0] : null;
            broadcastUpdate(queueItem);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
        }

        private void processQueue() {
            final Context context = getApplicationContext();
            HttpClient httpClient = new DefaultHttpClient();

            UploadQueueItem queueItem;

            while ((queueItem = dbHelper.getPendingQueueItem()) != null) {
                int noOfPayloads = queueItem.getNoOfPayloads();

                for (int j = 0; j < noOfPayloads; j++) {
                    if (ConnectivityUtil.isConnectionPermitted(context)) {
                        handlePayload(context, httpClient, queueItem, j);
                        publishProgress(queueItem);
                    } else return;
                }

                if (queueItem.isUploadComplete(context)) {
                    queueItem.updateStatus(context, UploadQueueItem.STATUS_UPLOAD_COMPLETE);
                    queueItem.getSubmission().uploadComplete(context);
                }
            }
            publishProgress(null);
        }

        private void handlePayload(Context context, HttpClient httpClient,
                                   UploadQueueItem queueItem, int j) {
            Log.v(TAG, "Starting Upload for Payload " + (j + 1) + " of Submission " +
                    queueItem.getSubmissionPayloadsId());

            String idToken = getIdToken();
            Payload payload = queueItem.getUploadPayload(context, idToken, j);

            if (!queueItem.isPayloadUploaded(payload)) {
                HttpResponse httpResponse = sendPayload(context, httpClient, payload,
                        MAX_UPLOAD_ATTEMPTS);

                String response = getResponseStringFromHttpResponse(httpResponse);
//                Log.d(TAG, response);

                if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() ==  200) {
                    try {
                        JSONObject responseJSON = new JSONObject(response);

                        if (responseJSON.has(getString(R.string.response_key_status)) &&
                                Objects.equals(
                                        responseJSON.getString(getString(R.string.response_key_status)),
                                        getString(R.string.response_status_ok))) {
                            queueItem.updateStatus(context,
                                    UploadQueueItem.STATUS_UPLOAD_IN_PROGRESS);
                            queueItem.updatePayloadsUploaded(context, payload);
                        } else {
                            Log.d(TAG, "Invalid Response");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.v(TAG, "Payload Already Uploaded");
            }
        }

        private HttpResponse sendPayload(Context context, HttpClient httpClient, Payload payload,
                                         int ttl) {
            if (ttl > 0) {
                Log.v(TAG, "Attempt " + (MAX_UPLOAD_ATTEMPTS - ttl + 1));

                HttpPost httpPost = new HttpPost(TOKEN_URL);

                try {
                    StringEntity stringEntity = new StringEntity(payload.getPayloadEntity(),
                            HTTP.UTF_8);

                    httpPost.setEntity(stringEntity);
                    httpPost.addHeader("Content-Type", "application/json");

                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        Log.v(TAG, "Payload Successfully Uploaded");
                    } else if (statusCode == 400 && getResponseStringFromHttpResponse(httpResponse)
                            == context.getString(R.string.response_error_invalid_id_token)) {
                        payload.renewPayloadToken(context, getIdToken());
                        httpResponse = sendPayload(context, httpClient, payload, ttl - 1);
                    } else {
                        Log.v(TAG, "Payload Upload Failed, Response Code : " + statusCode);
                    }

                    return httpResponse;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private String getResponseStringFromHttpResponse(HttpResponse httpResponse) {
            if (httpResponse == null) {
                return null;
            }

            try {
                return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private void broadcastUpdate(UploadQueueItem currentItem) {
        final Context context = getApplicationContext();
        if (currentItem != null) {
            int uploadCompletion = (int) (currentItem.getUploadCompletion(context) * 100);

            Log.v(TAG, "Upload Completion for Submission " + currentItem.getSubmissionPayloadsId()
                    + " : " + uploadCompletion + "%");

            intent.putExtra(getString(R.string.key_submission_id),
                    currentItem.getSubmissionPayloadsId());
            intent.putExtra(getString(R.string.key_upload_completion), uploadCompletion);
        }

        sendBroadcast(intent);
    }
}