package tanuj.opengridmap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;

import java.util.ArrayList;

import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.UploadQueueItem;

public class UploadService extends Service {
    private static final String TAG = UploadService.class.getSimpleName();

    private ArrayList<UploadQueueItem> queueItems;

    private OpenGridMapDbHelper dbHelper;

    public UploadService() {}

    @Override
    public void onCreate() {
        super.onCreate();

        dbHelper = new OpenGridMapDbHelper(getApplicationContext());

        queueItems = dbHelper.getQueue();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Context context = getApplicationContext();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                for (UploadQueueItem queueItem: queueItems) {
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
