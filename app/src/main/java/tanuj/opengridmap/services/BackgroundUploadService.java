//package tanuj.opengridmap.services;
//
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.AsyncTask;
//import android.os.IBinder;
//import android.util.Log;
//
//import java.util.List;
//
//import tanuj.opengridmap.R;
//import tanuj.opengridmap.data.OpenGridMapDbHelper;
//import tanuj.opengridmap.models.Submission;
//
//public class BackgroundUploadService extends Service {
//    private static final String TAG = BackgroundUploadService.class.getSimpleName();
//
//    private boolean uploadLock;
//
//    private boolean uploadFailed = false;
//
//    private BroadcastReceiver uploadBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            short uploadCompletion = intent.getShortExtra(getString(R.string.key_upload_completion), (short) -1);
//            if (uploadCompletion <= UploadSubmissionService.UPLOAD_STATUS_FAIL) {
//                uploadFailed = true;
//            }
//            uploadLock = false;
//        }
//    };
//
//    public BackgroundUploadService() {
//    }
//
//    @Override
//    public void onCreate() {
//        getApplicationContext().registerReceiver(uploadBroadcastReceiver,
//                new IntentFilter(UploadSubmissionService.UPLOAD_UPDATE_BROADCAST));
//    }
//
//    @Override
//    public void onStart(Intent intent, int startId) {
//        new UploadPendingSubmissionsTask().execute();
//    }
//
//    @Override
//    public void onDestroy() {
//        getApplicationContext().unregisterReceiver(uploadBroadcastReceiver);
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    private class UploadPendingSubmissionsTask extends AsyncTask<Void, Void, Void> {
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            try {
//                processUploadQueue();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }
//
//    private void processUploadQueue() throws InterruptedException {
//        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getApplicationContext());
//
//        List<Submission> submissions = dbHelper.getSubmissions(Submission.STATUS_SUBMISSION_CONFIRMED);
//
//        for (Submission submission: submissions) {
//            long submissionId = submission.getId();
//            Log.d(TAG, "Starting upload for submission " + submissionId);
//            UploadSubmissionService.startUpload(getApplicationContext(), submissionId);
//        }
//
//
////        submissionId = dbHelper.getFirstSubmissionId();
////
////        while (submissionId != -1) {
////            Log.d(TAG, "Starting upload for submission " + submissionId);
////            UploadSubmissionService.startUpload(getApplicationContext(), submissionId);
////            uploadLock = true;
////            while (uploadLock) {
////                Log.d(TAG, "Waiting for upload to finish");
////                Thread.sleep(200);
////            }
////            if (uploadFailed) {
////                Log.d(TAG, "Upload failed...stopping service");
////                stopSelf();
////                break;
////            }
////        }
//    }
//}
