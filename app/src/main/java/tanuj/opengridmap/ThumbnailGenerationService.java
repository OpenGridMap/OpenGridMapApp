package tanuj.opengridmap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.models.UploadQueueItem;

public class ThumbnailGenerationService extends Service {

    private static final String TAG = ThumbnailGenerationService.class.getSimpleName();

    private OpenGridMapDbHelper dbHelper;

    public ThumbnailGenerationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new OpenGridMapDbHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        final Context context = getApplicationContext();

        long time = System.currentTimeMillis();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                long submissionId = intent.getLongExtra(getString(R.string.key_submission_id), -1);
                Submission submission = null;

                if (submissionId > -1) {
                    submission = dbHelper.getSubmission(submissionId);
                }

                if (null != submission) {
                    List<Image> images = dbHelper.getImagesBySubmissionId(submission.getId());

                    for (Image image : images) {
                        image.generateThumbnails(context);
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        Log.d(TAG, "Runtime : " + (time - System.currentTimeMillis()));

        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy");
        super.onDestroy();
    }
}
