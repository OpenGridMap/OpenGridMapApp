package tanuj.opengridmap.services;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Payload;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.models.UploadQueueItem;

public class UploadService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = UploadService.class.getSimpleName();

    private static final String SERVER_BASE_URL = "http://vmjacobsen39.informatik.tu-muenchen.de";

    private static final String WEB_CLIENT_ID = "498377614550-0q8d0e0fott6qm0rvgovd4o04f8krhdb.apps.googleusercontent.com";

    private static final String TOKEN_URL = SERVER_BASE_URL + "/submissions/create";

    private ArrayList<UploadQueueItem> queueItems;

    private OpenGridMapDbHelper dbHelper;

    private GoogleApiClient googleApiClient;

    public UploadService() {}

    @Override
    public void onCreate() {
        super.onCreate();

        dbHelper = new OpenGridMapDbHelper(getApplicationContext());
        queueItems = dbHelper.getQueue();
        dbHelper.close();

        googleApiClient = buildGoogleApiClient();
        Log.d(TAG, "Upload Service OnCreate");
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
//        processUploadQueue();

        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void processUploadQueue() {
        handlePayloads().start();
//        new SendPayloadsTask().execute();
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
        }
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google Play Services");
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

    private Thread handlePayloads() {
        final Context context = getApplicationContext();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();

                for (int i = 0;i < queueItems.size(); i++) {
                    HttpPost httpPost = new HttpPost(TOKEN_URL);

                    String idToken = getIdToken();
                    UploadQueueItem currentItem = queueItems.get(i);
                    Submission submission = currentItem.getSubmission();
                    ArrayList<Payload> payloads = submission.getUploadPayloads(context, idToken);

                    for (Payload payload : payloads) {
                        try {
                            HttpResponse httpResponse = sendPayload(httpClient, httpPost, payload);

                            String response = EntityUtils.toString(httpResponse.getEntity(),
                                    "UTF-8");

                            Log.d(TAG, response);

                            if (httpResponse.getStatusLine().getStatusCode() ==  200) {
                                currentItem.updateStatus(context,
                                        UploadQueueItem.STATUS_UPLOAD_STARTED);
                                currentItem.updatePayloadsUploaded(context, payload.getImageId());
                                Log.v(TAG, "Payload Successfully Uploaded");
                            } else {
//                                tryCount++;
                                Log.v(TAG, "Payload Upload Failed, Response Code : " +
                                        httpResponse.getStatusLine().getStatusCode());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (currentItem.isUploadComplete(context)) {
                        currentItem.updateStatus(context, UploadQueueItem.STATUS_UPLOAD_COMPLETE);
                        currentItem.getSubmission().uploadComplete(context);
                    }

                }
            }
        };

        return new Thread(runnable);
    }

    private HttpResponse sendPayload(HttpClient httpClient, HttpPost httpPost, Payload payload)
            throws IOException {
        Log.d(TAG, "Starting Upload for Image " + payload.getImageId() + " of Submission " +
                payload.getSubmissionId());

        StringEntity stringEntity = new StringEntity(payload.getPayloadEntity(), HTTP.UTF_8);

        httpPost.setEntity(stringEntity);
        httpPost.addHeader("Content-Type", "application/json");

        return httpClient.execute(httpPost);
    }
}
