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

import java.io.IOException;
import java.util.ArrayList;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Payload;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.models.UploadQueueItem;

public class UploadService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = UploadService.class.getSimpleName();

    public static final String UPLOAD_UPDATE_BROADCAST = "tanuj.opengridmap.upload.update";

    private static final int MAX_UPLOAD_ATTEMPTS = 3;

    private static final String SERVER_BASE_URL = "http://vmjacobsen39.informatik.tu-muenchen.de";

    private static final String WEB_CLIENT_ID = "498377614550-0q8d0e0fott6qm0rvgovd4o04f8krhdb.apps.googleusercontent.com";

    private static final String TOKEN_URL = SERVER_BASE_URL + "/submissions/create";

    private ArrayList<UploadQueueItem> queueItems;

    private OpenGridMapDbHelper dbHelper;

    private GoogleApiClient googleApiClient;

    Intent intent;

    public UploadService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Upload Service OnCreate");

        getUploadQueue();

        Log.v(TAG, "No. Of Queue Items : " + queueItems.size());

        googleApiClient = buildGoogleApiClient();

        intent = new Intent(UPLOAD_UPDATE_BROADCAST);
    }

    private void getUploadQueue() {
        queueItems = null;
        dbHelper = new OpenGridMapDbHelper(getApplicationContext());
        queueItems = dbHelper.getUploadQueue();
        dbHelper.close();
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
        new HandlePayloadsTask().execute();
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
            return token;
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
            handlePayloads();

//            final Context context = getApplicationContext();
//            HttpClient httpClient = new DefaultHttpClient();
//
//            for (int i = 0;i < queueItems.size(); i++) {
//                HttpPost httpPost = new HttpPost(TOKEN_URL);
//
//                String idToken = getIdToken();
//                UploadQueueItem currentItem = queueItems.get(i);
//                Submission submission = currentItem.getSubmission();
//                ArrayList<Payload> payloads = submission.getUploadPayloads(context, idToken);
//
//                int payloadNo = 1;
//                for (Payload payload : payloads) {
//                    Log.v(TAG, "Starting Upload for Payload " + payloadNo++ + " of Submission " +
//                            payload.getSubmissionId());
//
//                    if (!currentItem.isPayloadUploaded(payload)) {
//                        HttpResponse httpResponse = sendPayload(context, httpClient, httpPost,
//                                payload, MAX_UPLOAD_ATTEMPTS);
//
//                        String response = getResponseStringFromHttpResponse(httpResponse);
////                    Log.d(TAG, response);
//
//                        if (httpResponse.getStatusLine().getStatusCode() ==  200) {
//                            currentItem.updateStatus(context,
//                                    UploadQueueItem.STATUS_UPLOAD_STARTED);
//                            currentItem.updatePayloadsUploaded(context, payload.getImageId());
//                        }
//                    } else {
//                        Log.v(TAG, "Payload Already Uploaded");
//                    }
//
//                    publishProgress(currentItem);
//                }
//
//                if (currentItem.isUploadComplete(context)) {
//                    currentItem.updateStatus(context, UploadQueueItem.STATUS_UPLOAD_COMPLETE);
//                    currentItem.getSubmission().uploadComplete(context);
//                }
//
//            }
//            Log.v(TAG, "Uploads Complete");

            return null;
        }

        @Override
        protected void onProgressUpdate(UploadQueueItem... values) {
            super.onProgressUpdate(values);
            broadcastUpdate(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            getUploadQueue();
            if (queueItems.size() == 0) {
                stopSelf();
            } else {
                processUploadQueue();
            }
        }

        private void handlePayloads() {
            final Context context = getApplicationContext();
            HttpClient httpClient = new DefaultHttpClient();

            for (int i = 0;i < queueItems.size(); i++) {
                HttpPost httpPost = new HttpPost(TOKEN_URL);

                String idToken = getIdToken();
                UploadQueueItem currentItem = queueItems.get(i);
                Submission submission = currentItem.getSubmission();
                ArrayList<Payload> payloads = submission.getUploadPayloads(context, idToken);

                int payloadNo = 1;
                for (Payload payload : payloads) {
                    Log.v(TAG, "Starting Upload for Payload " + payloadNo++ + " of Submission " +
                            payload.getSubmissionId());

                    if (!currentItem.isPayloadUploaded(payload)) {
                        HttpResponse httpResponse = sendPayload(context, httpClient, httpPost,
                                payload, MAX_UPLOAD_ATTEMPTS);

                        String response = getResponseStringFromHttpResponse(httpResponse);
                    Log.d(TAG, response);

                        if (httpResponse.getStatusLine().getStatusCode() ==  200) {
                            currentItem.updateStatus(context,
                                    UploadQueueItem.STATUS_UPLOAD_STARTED);
                            currentItem.updatePayloadsUploaded(context, payload.getImageId());
                        }
                    } else {
                        Log.v(TAG, "Payload Already Uploaded");
                    }

                    publishProgress(currentItem);
                }

                if (currentItem.isUploadComplete(context)) {
                    currentItem.updateStatus(context, UploadQueueItem.STATUS_UPLOAD_COMPLETE);
                    currentItem.getSubmission().uploadComplete(context);
                }

            }
            Log.v(TAG, "Uploads Complete");
        }
    }

    private void broadcastUpdate(UploadQueueItem currentItem) {
        final Context context = getApplicationContext();
        int uploadCompletion = (int) (currentItem.getUploadCompletion(context) * 100);

        Log.v(TAG, "Upload Completion for Submission " + currentItem.getSubmissionId() + " : " +
                uploadCompletion + "%");

        intent.putExtra(getString(R.string.key_submission_id), currentItem.getSubmissionId());
        intent.putExtra(getString(R.string.key_upload_completion), uploadCompletion);

        sendBroadcast(intent);
    }

    private HttpResponse sendPayload(Context context, HttpClient httpClient, HttpPost httpPost,
                                     Payload payload, int ttl) {
        if (ttl > 0) {
            Log.v(TAG, "Attempt " + (MAX_UPLOAD_ATTEMPTS - ttl + 1));

            try {
                StringEntity stringEntity = new StringEntity(payload.getPayloadEntity(), HTTP.UTF_8);

                httpPost.setEntity(stringEntity);
                httpPost.addHeader("Content-Type", "application/json");

                HttpResponse httpResponse = httpClient.execute(httpPost);
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    Log.v(TAG, "Payload Successfully Uploaded");
                } else if (statusCode == 400 && getResponseStringFromHttpResponse(httpResponse) ==
                        context.getString(R.string.error_response_invalid_id_token)) {
                    payload.renewPayloadToken(context, getIdToken());
                    httpResponse = sendPayload(context, httpClient, httpPost, payload, ttl - 1);
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
        try {
            return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}