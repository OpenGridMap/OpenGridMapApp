package tanuj.opengridmap.views.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import tanuj.opengridmap.BuildConfig;
import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.services.ThumbnailGenerationService;
import tanuj.opengridmap.utils.CameraUtils;
import tanuj.opengridmap.views.activities.TagSelectionActivity;
import tanuj.opengridmap.views.custom_views.AutoFitTextureView;

@SuppressLint("NewApi")
public class CameraActivityFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = CameraActivityFragment.class.getSimpleName();

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int LOCATION_STATUS_OUTDATED = -2;

    private static final int LOCATION_STATUS_ACCURACY_UNACCEPTABLE = -1;

    private static final int LOCATION_STATUS_UNAVAILABLE = 0;

    private static final int LOCATION_STATUS_OK = 1;

    private static boolean cameraBusy = false;

    private static final int STATE_PREVIEW = 0;

    private static final int STATE_WAITING_LOCK = 1;

    private static final int STATE_WAITING_PRECAPTURE = 2;

    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    private static final int STATE_PICTURE_TAKEN = 4;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            };

    private String mCameraId;

    private AutoFitTextureView mTextureView;

    private CameraCaptureSession mCaptureSession;

    private CameraDevice mCameraDevice;

    private Size mPreviewSize;

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraOpenClosedLock.release();
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenClosedLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenClosedLock.release();
            mCameraDevice.close();
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private ImageReader mImageReader;

    private File mFile;

    private String mFileName;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    final Context context = getActivity();

                    if (currentLocation != null) {
                        startTime = System.currentTimeMillis();
                        mFileName = Long.toString(startTime) + ".jpg";

                        mFile = new File(context.getExternalFilesDir(
                                tanuj.opengridmap.models.Image.IMAGE_STORE_PATH), mFileName);

                        mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), mFile));

                        if (submission == null) {
                            submission = new Submission(context);
                            noSavedImages = 0;

                            long powerElementId = ((Activity) context).getIntent().getExtras()
                                    .getLong(getString(R.string.key_power_element_id), -1);

                            Log.d(TAG, "Power Element ID : " + powerElementId);

                            if (powerElementId > -1) {
                                submission.addPowerElementById(context, powerElementId);
                            }
                        }

                        image = new tanuj.opengridmap.models.Image(mFile.getPath(),
                                currentLocation);
                        submission.addImage(context, image);
                        Log.d(TAG, "Image Saved : " + mFile.getPath());
                    }
                }
            };

    private void displayImageCaptureAnimation() {
        final Activity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AnimationDrawable drawable = new AnimationDrawable();
                final Handler handler = new Handler();

                drawable.addFrame(new ColorDrawable(activity.getResources().getColor(
                        R.color.shutter_color)), 80);
                drawable.addFrame(new ColorDrawable(activity.getResources().getColor(
                        R.color.transparent)), 1);
                drawable.setOneShot(true);
                cameraTextureOverlay.setBackgroundDrawable(drawable);
                drawable.start();
            }
        });
    }

    private static int noSavedImages = 0;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CaptureRequest mPreviewRequest;

    private int mState = STATE_PREVIEW;

    private Semaphore mCameraOpenClosedLock = new Semaphore(1);

    private static TextView latitudeTextView;
    private static TextView longitudeTextView;
    private static TextView accuracyTextView;
    private static TextView bearingTextView;

    private ImageView cameraPreviewImageView = null;

    private static boolean previewAvailable = false;

    private static ImageButton cameraShutterButton = null;

    private ImageButton confirmButton = null;

    private static CardView cameraDialogBoxCardView = null;

    private static ProgressBar cameraDialogBoxProgressBar = null;

    private static ImageView cameraDialogBoxImageView = null;

    private static TextView cameraDialogBoxTextView = null;

    private LinearLayout cameraTextureOverlay = null;

    private Button zoomPlusButton;

    private Button zoomMinusButton;

    private SeekBar zoomSeekBar;

    private double zoom = 1;

    private long startTime;
    private long finishTime;

    private Submission submission = null;

    private static Location currentLocation = null;

    private tanuj.opengridmap.models.Image image = null;

    private static tanuj.opengridmap.models.Image lastSavedImage = null;

//    private List<tanuj.opengridmap.models.Image> images = new ArrayList<>();

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult result) {
                    switch (mState){
                        case STATE_PREVIEW: {
                            break;
                        }
                        case STATE_WAITING_LOCK: {
                            Log.d(TAG, "Process : STATE_WAITING_LOCK");
                            int afState = result.get(CaptureResult.CONTROL_AF_STATE);

                            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                                if (aeState == null ||
                                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                    aeState = STATE_WAITING_NON_PRECAPTURE;
                                } else {
                                    runPrecaptureSequence();
                                }
                            }
                            break;
                        }
                        case STATE_WAITING_PRECAPTURE: {
                            Log.d(TAG, "Process : STATE_WAITING_PRECAPTURE");
                            Integer aestate = result.get(CaptureResult.CONTROL_AE_STATE);

                            if (aestate == null ||
                                    aestate == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                    aestate == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                                mState = STATE_WAITING_NON_PRECAPTURE;
                            }
                            break;
                        }
                        case STATE_WAITING_NON_PRECAPTURE: {
                            Log.d(TAG, "Process : STATE_WAITING_NON_PRECAPTURE");
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                            if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                                captureStillImage();
                                mState = STATE_PICTURE_TAKEN;
                            }
                            break;
                        }
                    }

                    if (previewAvailable) {
                        updatePreview(image.getThumbnailBitmap(getActivity(),
                                tanuj.opengridmap.models.Image.TYPE_LIST));
                        lastSavedImage = image;
                        previewAvailable = false;
                    }
                }

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest
                        request, CaptureResult partialResult) {
                    process(partialResult);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    process(result);
                }
            };

    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
//            super.handleMessage(msg);
        }
    };

    private static Activity activity;

    private void showText(String text) {
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    public static CameraActivityFragment newInstance() {
        CameraActivityFragment fragment = new CameraActivityFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public CameraActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        final Context context = getActivity();

        if (BuildConfig.DEBUG) {
            latitudeTextView = (TextView) view.findViewById(R.id.latitude);
            longitudeTextView = (TextView) view.findViewById(R.id.longitude);
            accuracyTextView = (TextView) view.findViewById(R.id.accuracy);
            bearingTextView = (TextView) view.findViewById(R.id.bearing);
        }

        confirmButton = (ImageButton) view.findViewById(R.id.camera_confirm_button);
        cameraShutterButton = (ImageButton) view.findViewById(R.id.camera_shutter_button);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.camera_texture);

        cameraPreviewImageView = (ImageView) view.findViewById(R.id.camera_preview);
        cameraDialogBoxCardView = (CardView) view.findViewById(R.id.camera_dialog_box_card);
        cameraDialogBoxProgressBar = (ProgressBar) view.findViewById(R.id.card_progress_spinner);
        cameraDialogBoxImageView = (ImageView) view.findViewById(R.id.card_image);
        cameraDialogBoxTextView = (TextView) view.findViewById(R.id.card_text);

        cameraTextureOverlay = (LinearLayout) view.findViewById(R.id.camera_output_texture_overlay);

        zoomSeekBar = (SeekBar) view.findViewById(R.id.zoom_seek_bar);

        view.findViewById(R.id.touch_layer).setOnTouchListener(onTouchListener);

        zoomSeekBar.setOnSeekBarChangeListener(this);

        if (savedInstanceState != null && savedInstanceState.containsKey(getString(R.string.key_submission_id))) {
            long submissionId = savedInstanceState.getLong(getString(R.string.key_submission_id));

            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

            submission = dbHelper.getSubmission(submissionId);
            noSavedImages = submission.getNoOfImages();
            lastSavedImage = submission.getImage(noSavedImages - 1);

            dbHelper.close();
        }

        if (noSavedImages > 0) {
            cameraPreviewImageView.setImageBitmap(lastSavedImage.getThumbnailBitmap(getActivity(),
                    tanuj.opengridmap.models.Image.TYPE_LIST));
        }

        activity = getActivity();

        return view;
    }

    public static double calculateDistance(float x1, float y1, float x2, float y2) {
        float x = x1 - x2;
        float y = y1 - y2;
        return Math.sqrt(x * x + y * y);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        cameraShutterButton.setOnClickListener(this);
        confirmButton.setOnClickListener(this);
//        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();

        closeCamera();
        stopBackgroundProcess();
    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public static void clearActivityFragment() {
        noSavedImages = 0;
        latitudeTextView = null;
        longitudeTextView = null;
        accuracyTextView = null;
        bearingTextView = null;
        previewAvailable = false;
        cameraShutterButton = null;
        cameraDialogBoxCardView = null;
        cameraDialogBoxProgressBar = null;
        cameraDialogBoxImageView = null;
        cameraDialogBoxTextView = null;
        currentLocation = null;
        lastSavedImage = null;
        activity = null;

        Log.d(TAG,"CameraActivityFragmentCleared");
    }

    private void setUpCameraOptions(int width, int height) {
        Activity activity = getActivity();

        CameraManager cameraManager = (CameraManager)
                activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = cameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CameraUtils.CompareSizesByArea());

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 2);

                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,
                        mBackgroundHandler);

                mPreviewSize = CameraUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width,
                        height, largest);

                int orientation = getResources().getConfiguration().orientation;

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mCameraId = cameraId;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            new ErrorDialog().show(getFragmentManager(), "dialog");
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        setUpCameraOptions(width, height);
        configureTransform(width, height);

        Activity activity = getActivity();
        CameraManager cameraManager = (CameraManager) activity.getSystemService(
                Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenClosedLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lck camera open");
            }
            cameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenClosedLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }

            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenClosedLock.release();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundProcess() {
        mBackgroundThread.quitSafely();

        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Surface surface =  new Surface(texture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (mCameraDevice == null) {
                                return;
                            }
                            Log.d(TAG, "Configured");

                            mCaptureSession = session;

                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            showText("Failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();

        if (mTextureView  == null|| mPreviewSize  == null|| activity == null) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth()
            );

            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }

        mTextureView.setTransform(matrix);
    }

    private void takePicture() {
        if (checkLocationStatus()) {
            cameraBusy = true;
            disableCamera();
            lockFocus();
            runPrecaptureSequence();
        }
    }

    private void lockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);

            mState = STATE_WAITING_LOCK;

            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
//            displayImageCaptureAnimation();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillImage() {
        if (!checkLocationStatus())
            return;

        try {
            final Activity activity = getActivity();

            if (activity  == null|| mCameraDevice == null) {
                return;
            }

            Rect previewRect = CameraUtils.getZoomRect(zoom, mPreviewSize.getWidth(),
                    mPreviewSize.getHeight());
            mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, previewRect);

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback captureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session,
                                                       CaptureRequest request,
                                                       TotalCaptureResult result) {
                            displayImageCaptureAnimation();
                            unlockFocus();
                            cameraBusy = false;
                            enableCamera();
//                            super.onCaptureCompleted(session, request, result);
                        }
                    };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);

            finishTime = System.currentTimeMillis();

            Log.d(TAG, "Time taken for pic capture : " + (finishTime - startTime));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);

            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_shutter_button: {
                takePicture();
                break;
            }
            case R.id.camera_confirm_button: {
                confirmSubmission();
                break;
            }
        }
    }

    private void setZoom(double z) {
        if (mCameraDevice == null) return;

        float maxZoom = CameraUtils.getMaxZoom(getActivity(), mCameraDevice.getId());
        float minZoom = 1;

        if (z > maxZoom) {
            z = maxZoom;
        }

        if (z < minZoom) {
            z = minZoom;
        }

        if (z >= minZoom && z <= maxZoom) {
            try {
                Rect previewRect = CameraUtils.getZoomRect(z, mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, previewRect);
                mPreviewRequest = mPreviewRequestBuilder.build();
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } finally {
                zoom = z;
            }
        }
    }

    public void updatePreview(final Bitmap bitmap) {
        final Activity activity = getActivity();
        if (cameraPreviewImageView == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraPreviewImageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setZoom(CameraUtils.getZoomLevelFromSeekBarProgress(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        private double initialDistance = -1;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getPointerCount() == 2) {
                if (initialDistance == -1) {
                    initialDistance = calculateDistance(event.getX(0), event.getY(0),
                            event.getX(1), event.getY(1));
                } else {
                    double distance = calculateDistance(event.getX(0), event.getY(0),
                            event.getX(1), event.getY(1));

                    double z = (distance / initialDistance) * zoom * 0.05;

                    z = distance > initialDistance ? zoom + z : zoom - z;

                    float maxZoom = CameraUtils.getMaxZoom(getActivity(), mCameraDevice.getId());
                    float minZoom = 1;

                    if (z > maxZoom) {
                        z = maxZoom;
                    }

                    if (z < minZoom) {
                        z = minZoom;
                    }

                    setZoom(z);
                    zoomSeekBar.setProgress(CameraUtils.getSeekBarProgressFromZoomValue(z));
                }
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                initialDistance = -1;
                Log.d(TAG, "Touch Up");
            }
            return true;
        }
    };

    private static class ImageSaver implements Runnable {
        private final Image mImage;
        private final File mFile;

        private ImageSaver(Image mImage, File mFile) {
            this.mImage = mImage;
            this.mFile = mFile;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream outputStream = null;

            try {
                outputStream = new FileOutputStream(mFile);
                outputStream.write(bytes);
                if (bytes == null) {
                    throw new IOException();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        noSavedImages++;
                        previewAvailable = true;
                        Log.d(TAG, String.valueOf(noSavedImages));
                    }
                }
            }
        }
    }

    public static class ErrorDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            return new AlertDialog.Builder(activity)
                    .setMessage("The device does not support Camera2 API")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    }).create();
        }
    }

    private boolean checkLocationStatus() {
        if (currentLocation == null) {
            showText("Location not Available");
            return false;
        } else if (currentLocation.getAccuracy() > 200.0) {
            showText("Location not Accurate");
            return false;
        }
        return true;
    }

    private void confirmSubmission() {
        if (submission  == null|| submission.isEmpty()) {
            showText("No Pics Taken");
        } else {
            disableCamera();
            showSavingImagesDialog(getActivity());

            Log.d(TAG, "No of Submitted Images : " + submission.getImages().size());
            Log.d(TAG, "No of Saved Images : " + noSavedImages);

            if (noSavedImages != submission.getNoOfImages()) {
                Log.d(TAG, "No of Pending Images" + (submission.getImages().size() - noSavedImages));
                ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        confirmSubmission();
                    }
                };

                worker.schedule(runnable, 200, TimeUnit.MILLISECONDS);
                return;
            }

            final Context context = getActivity();
            submission.confirmSubmission(context);

            Intent intent = new Intent(getActivity(), TagSelectionActivity.class);
            intent.putExtra(getString(R.string.key_submission_id), submission.getId());

            Intent serviceIntent = new Intent(context, ThumbnailGenerationService.class);
            serviceIntent.putExtra(getString(R.string.key_submission_id), submission.getId());
            context.startService(serviceIntent);

            clearActivityFragment();

            startActivity(intent);
            ((Activity) context).finish();
        }
    }

    public static void setLocation(Location location, Context context) {
        currentLocation = location;
        updateUi(location);
        processCameraState(context);
    }

    public static void updateUi(Location location) {
        if (latitudeTextView == null) {
            return;
        }
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        float bearing = location.getBearing();

        latitudeTextView.setText("Latitude : " + String.valueOf(latitude));
        longitudeTextView.setText("Longitude : " + String.valueOf(longitude));
        accuracyTextView.setText("Accuracy : " + String.valueOf(accuracy) + "m");
        bearingTextView.setText("Bearing : " + String.valueOf(bearing));
    }

    private static int getCameraStatus() {
        if (currentLocation == null) {
            return LOCATION_STATUS_UNAVAILABLE;
        } else if (System.currentTimeMillis() - currentLocation.getTime() > 5000){
            return LOCATION_STATUS_OUTDATED;
        } else if (currentLocation.getAccuracy() > 200.0) {
            return LOCATION_STATUS_ACCURACY_UNACCEPTABLE;
        }

        return LOCATION_STATUS_OK;
    }

    public static void processCameraState(final Context context) {
        final int status = getCameraStatus();

        switch (status) {
            case LOCATION_STATUS_UNAVAILABLE: {
                Log.d(TAG, "LOCATION_STATUS_UNAVAILABLE");
                disableCamera();
                break;
            }
            case LOCATION_STATUS_OUTDATED: {
                Log.d(TAG, "LOCATION_STATUS_OUTDATED");
                disableCamera();
                showLocationAcquisitionDialog(context);
                break;
            }
            case LOCATION_STATUS_ACCURACY_UNACCEPTABLE: {
                Log.d(TAG, "LOCATION_STATUS_ACCURACY_UNACCEPTABLE");
                disableCamera();
                showLocationAcquisitionDialog(context);
                break;
            }
            case LOCATION_STATUS_OK: {
                hideLocationAcquisitionDialog();
                enableCamera();
                break;
            }
        }
    }

    public static void disableCamera() {
        if (cameraShutterButton == null) {
            return;
        }
        if (cameraShutterButton.isClickable()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraShutterButton.setClickable(false);
                    cameraShutterButton.setEnabled(false);
                }
            });

//            cameraShutterButton.setClickable(false);
//            cameraShutterButton.setEnabled(false);
            Log.d(TAG, "Disabled Camera Shutter Button");
        }
    }

    public static void enableCamera() {
        if (cameraShutterButton  == null|| cameraBusy) {
            return;
        }
        if (!cameraShutterButton.isClickable()) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraShutterButton.setClickable(true);
                    cameraShutterButton.setEnabled(true);
                }
            });

//            cameraShutterButton.setClickable(true);
//            cameraShutterButton.setEnabled(true);
            Log.d(TAG, "Enabled Camera Shutter Button");
        }
    }

    private static void showLocationAcquisitionDialog(final Context context) {
        if (null != cameraDialogBoxImageView) {
            cameraDialogBoxTextView.setText(context.getString(R.string.location_message_acquiring));
            cameraDialogBoxImageView.setVisibility(View.GONE);
            cameraDialogBoxCardView.setVisibility(View.VISIBLE);
            cameraDialogBoxProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private static void hideLocationAcquisitionDialog() {
        if (null != cameraDialogBoxImageView) {
            cameraDialogBoxImageView.setVisibility(View.GONE);
            cameraDialogBoxCardView.setVisibility(View.GONE);
            cameraDialogBoxProgressBar.setVisibility(View.GONE);
        }
    }

    private static void showSavingImagesDialog(final Context context) {
        if (null != cameraDialogBoxImageView) {
            if (noSavedImages == 1) {
                cameraDialogBoxTextView.setText(context.getString(R.string.camera_message_saving_image));
            } else {
                cameraDialogBoxTextView.setText(context.getString(R.string.camera_message_saving_images));
            }
            cameraDialogBoxImageView.setVisibility(View.GONE);
            cameraDialogBoxCardView.setVisibility(View.VISIBLE);
            cameraDialogBoxProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (null != submission) {
            outState.putLong(getString(R.string.key_submission_id), submission.getId());
        }
        super.onSaveInstanceState(outState);
    }
}