package tanuj.opengridmap.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

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
import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.exceptions.MemoryLowException;
import tanuj.opengridmap.models.Payload;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.utils.ConnectivityUtils;

public class UploadService extends GcmTaskService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = UploadService.class.getSimpleName();

    public static final String UPLOAD_TAG = "upload-submission-";

    public static final String UPLOAD_SUBMISSION_KEY = "submission-id";

    public static final short UPLOAD_STATUS_SUCCESSFUL = 100;

    public static final short UPLOAD_STATUS_IN_PROCESS = 50;

    public static final short UPLOAD_STATUS_FAILED = -1;

    public static final String UPLOAD_UPDATE_BROADCAST = "tanuj.opengridmap.upload.update";

    private static final int MAX_UPLOAD_ATTEMPTS = 3;

    private static final String SERVER_BASE_URL = "http://vmjacobsen39.informatik.tu-muenchen.de";

    public static final String WEB_CLIENT_ID = "498377614550-0q8d0e0fott6qm0rvgovd4o04f8krhdb.apps.googleusercontent.com";

    private static final String TOKEN_URL = SERVER_BASE_URL + "/submissions/create";

    private GoogleApiClient googleApiClient;

    private static int noOfFailedAttempts;

    Intent intent;

    public UploadService() {}

    public static void scheduleUpload(long submissionId, GcmNetworkManager gcmNetworkManager,
                                      Context context) {
        Bundle bundle = new Bundle();
        bundle.putLong(UPLOAD_SUBMISSION_KEY, submissionId);

        OneoffTask.Builder taskBuilder = new OneoffTask.Builder()
                .setService(UploadService.class)
                .setExecutionWindow(0, 30)
                .setTag(UploadService.getTag(submissionId))
                .setUpdateCurrent(false)
                .setRequiresCharging(false)
                .setPersisted(true)
                .setExtras(bundle);

        if (ConnectivityUtils.isWifiOnly(context))
            taskBuilder.setRequiredNetwork(Task.NETWORK_STATE_UNMETERED);
        else
            taskBuilder.setRequiredNetwork(Task.NETWORK_STATE_CONNECTED);

        Task task = taskBuilder.build();

        gcmNetworkManager.schedule(task);
    }

    private static String getTag(long submissionId) {
        return UploadService.UPLOAD_TAG + submissionId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Upload Service OnCreate");
        final Context context = getApplicationContext();

        googleApiClient = buildGoogleApiClient();
        intent = new Intent(UPLOAD_UPDATE_BROADCAST);
        noOfFailedAttempts = 0;
    }

    private GoogleApiClient buildGoogleApiClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();


        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso);

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Log.d(TAG, "Upload Service start");
        Context context = getApplicationContext();
        long submissionId = taskParams.getExtras().getLong(UPLOAD_SUBMISSION_KEY, -1);
        Log.d(TAG, "Submission ID " + submissionId);
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        Submission submission = dbHelper.getSubmission(submissionId);
        dbHelper.close();

        if (submission == null || submission.getStatus() >= Submission.STATUS_SUBMITTED_PENDING_REVIEW) {
            Log.d(TAG, "Upload failed, " + (submission == null ? "Submission not found" : "Already uploaded"));
            return GcmNetworkManager.RESULT_FAILURE;
        }

        submission.uploadInProgess(context);
        broadcastUpdate(UPLOAD_STATUS_IN_PROCESS, submission);

        try {
            HttpClient httpClient = new DefaultHttpClient();
            short res = handlePayload(context, httpClient, submission);

            switch (res) {
                case UPLOAD_STATUS_SUCCESSFUL: {
                    submission.uploadComplete(context);
                    Log.d(TAG, "Upload Successful");
                    broadcastUpdate(UPLOAD_STATUS_SUCCESSFUL, submission);
                    return GcmNetworkManager.RESULT_SUCCESS;
                }
                case UPLOAD_STATUS_FAILED: {
                    submission.uploadFailed(context);
                    broadcastUpdate(UPLOAD_STATUS_FAILED, submission);
                    Log.d(TAG, "Upload Failed : Reschedule");
                    return GcmNetworkManager.RESULT_RESCHEDULE;
                }
            }
        } catch (MemoryLowException | NullPointerException e) {
            e.printStackTrace();
            submission.uploadFailed(context);
        }

        return GcmNetworkManager.RESULT_RESCHEDULE;
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
        Context context = getApplicationContext();
        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
        List<Submission> submissions = dbHelper.getPendingSubmissions();
        GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);

        for (Submission submission : submissions) {
            UploadService.scheduleUpload(submission.getId(), gcmNetworkManager, context);
        }

        dbHelper.close();
    }

    @Override
    public void onDestroy() {
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
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        int errorCode = connectionResult.getErrorCode();
        Log.e(TAG, "onConnectionFailed: ErrorCode : " + errorCode);

        if (errorCode == ConnectionResult.API_UNAVAILABLE) {
            Log.v(TAG, "API Unavailable");
        }
    }

    private String getIdToken() {
        String idToken = null;
        ConnectionResult result = googleApiClient.blockingConnect();

        if (result.isSuccess()) {
            GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.silentSignIn(googleApiClient).await();
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();

            if (googleSignInAccount == null)
                return null;

            idToken = googleSignInResult.getSignInAccount().getIdToken();
        }

        googleApiClient.disconnect();

        return idToken;
    }

    private short handlePayload(Context context, HttpClient httpClient, Submission submission)
            throws MemoryLowException, NullPointerException {
        Log.v(TAG, "Starting Upload for Payload of Submission " + submission.getId());

        String idToken = getIdToken();

        if (idToken == null)
            throw new NullPointerException("Idtoken could not be retrieved");

        Payload payload = submission.getUploadPayload(context, idToken, 0);


        HttpResponse httpResponse = sendPayload(context, httpClient, payload, MAX_UPLOAD_ATTEMPTS);

        if (httpResponse == null)
            return UPLOAD_STATUS_FAILED;

        if(httpResponse.getStatusLine().getStatusCode() == 500) {
            String response = getResponseStringFromHttpResponse(httpResponse);
            Log.d(TAG, response);
        } else if (httpResponse.getStatusLine().getStatusCode() ==  200) {
            String response = getResponseStringFromHttpResponse(httpResponse);
            try {
                JSONObject responseJSON = new JSONObject(response);

                if (responseJSON.has(getString(R.string.response_key_status)) &&
                        responseJSON.getString(getString(R.string.response_key_status)).equals(
                                getString(R.string.response_status_ok))) {

                    return UPLOAD_STATUS_SUCCESSFUL;
                } else {
                    Log.d(TAG, "Invalid Response");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return UPLOAD_STATUS_FAILED;
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
                } else if (statusCode == 400 &&
                        getResponseStringFromHttpResponse(httpResponse).
                                equals(context.getString(R.string.response_error_invalid_id_token))) {
                    payload.renewPayloadToken(context, getIdToken());
                    httpResponse = sendPayload(context, httpClient, payload, ttl - 1);
                } else {
                    Log.v(TAG, "Payload Upload Failed, Response Code : " + statusCode);

                    if (++noOfFailedAttempts > 3) {
                        return null;
                    }
                }

                return httpResponse;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private String getResponseStringFromHttpResponse(HttpResponse httpResponse) {
        try {
            return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void broadcastUpdate(short uploadCompletion, Submission submission) {
        Log.v(TAG, "Upload Completion for Submission " + submission.getId() + " : " + uploadCompletion);

        Intent intent = new Intent(UPLOAD_UPDATE_BROADCAST);
        intent.putExtra(getString(R.string.key_submission_id), submission.getId());
        intent.putExtra(getString(R.string.key_upload_completion), uploadCompletion);

        sendBroadcast(intent);

    }
}