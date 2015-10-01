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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import tanuj.opengridmap.data.OpenGridMapDbHelper;
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
        processUploadQueue();

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

//    private class GetIdTokenTask extends AsyncTask<Void, Void, String> {
//
//        @Override
//        protected String doInBackground(Void... params) {
//            return getIdToken(params[0]);
//        }
//
//        @Override
//        protected void onPostExecute(String idToken) {
//            super.onPostExecute(idToken);
//
//            new SendPayloadsTask().execute(idToken);
//        }
//    }

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
                    ArrayList<String> payloads = submission.getUploadPayloads(context, idToken);

                    for (String payload : payloads) {
                        int tryCount = 0;
                        try {
                            HttpResponse httpResponse = sendPayload(httpClient, httpPost, payload);

                            if (httpResponse.getStatusLine().getStatusCode() ==  200) {
                                currentItem.updateStatus(context, UploadQueueItem.STATUS_UPLOAD_STARTED);
                                currentItem.updatePayloadsUploaded(context, i);
                            } else {
                                tryCount++;
                                httpResponse = sendPayload(httpClient, httpPost, payload);
                            }
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    currentItem.updateStatus(context, UploadQueueItem.STATUS_UPLOAD_FINISHED);
                }
            }
        };

        return new Thread(runnable);
    }

    private HttpResponse sendPayload(HttpClient httpClient, HttpPost httpPost, String payload) throws IOException {
        httpPost.addHeader("Content-Type", "application/json");

        StringEntity stringEntity = null;
        stringEntity = new StringEntity(payload, HTTP.UTF_8);
        httpPost.setEntity(stringEntity);

        return httpClient.execute(httpPost);
    }
}
