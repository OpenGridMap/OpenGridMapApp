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
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
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
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import tanuj.opengridmap.BuildConfig;
import tanuj.opengridmap.R;
import tanuj.opengridmap.TagSelectionActivity;
import tanuj.opengridmap.ThumbnailGenerationService;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.views.custom_views.AutoFitTextureView;

@SuppressLint("NewApi")
public class CameraActivityFragment extends Fragment implements View.OnClickListener {
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

    private static boolean cameraState = false;

    private static final int STATE_PREVIEW = 0;

    private static final int STATE_WAITING_LOCK = 1;

    private static final int STATE_WAITING_PRECAPTURE = 2;

    private static final int STATE_WATING_NON_PRECAPTURE = 3;

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

                    startTime = System.currentTimeMillis();
                    mFileName = Long.toString(startTime) + ".jpg";

                    mFile = new File(getActivity().getExternalFilesDir(
                            tanuj.opengridmap.models.Image.IMAGE_STORE_PATH), mFileName);

                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), mFile));

                    if (submission == null) {
                        submission = new Submission(context);
                        noSavedImages = 0;

                        long powerElementId = ((Activity) context).getIntent().getExtras()
                                .getInt(getString(R.string.key_power_element_id), -1);

                        if (powerElementId > -1) {
                            submission.addPowerElementById(context, powerElementId);
                        }
                    }

                    image = new tanuj.opengridmap.models.Image(mFile.getPath(),
                            currentLocation);
                    submission.addImage(context, image);
                    images.add(image);
                    Log.d(TAG, "Image Saved : " + mFile.getPath());
                }
            };

    private static int noSavedImages = 0;

    private CaptureRequest.Builder mPreviewRequestBuidler;

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

    private LinearLayout cameraDialogBoxLayout = null;

    private static CardView cameraDialogBoxCardView = null;

    private static ProgressBar cameraDialogBoxProgressBar = null;

    private static ImageView cameraDialogBoxImageView = null;

    private static TextView camreaDialogBoxTextView = null;

    private long startTime;
    private long finishTime;

    private Submission submission = null;

    private static Location currentLocation = null;

    private tanuj.opengridmap.models.Image image = null;

    private static tanuj.opengridmap.models.Image lastSavedImage = null;

    private List<tanuj.opengridmap.models.Image> images = new ArrayList<>();

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
                                    aeState = STATE_WATING_NON_PRECAPTURE;
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
                                mState = STATE_WATING_NON_PRECAPTURE;
                            }
                            break;
                        }
                        case STATE_WATING_NON_PRECAPTURE: {
                            Log.d(TAG, "Process : STATE_WATING_NON_PRECAPTURE");
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
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
//            super.handleMessage(msg);
        }
    };

    private void showText(String text) {
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        Log.d(TAG, "Len Choices : " + choices.length);

        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width &&
                    option.getHeight() >= height) {
                bigEnough.add(option);
                Log.d(TAG, "Height : " + option.getHeight() + " Width : " + option.getWidth());
                Log.d(TAG, "Diff Height : " + (option.getHeight() - height) + " Width : " +
                        (option.getWidth() - width));
            }
        }

        if (bigEnough.size() > 0) {
            Size optimalSize = Collections.max(bigEnough, new CompareSizesByArea());
            Log.d(TAG, "Chosen Size | Height : " + optimalSize.getHeight() + " Width : " +
                    optimalSize.getWidth());
            return optimalSize;
        } else {
            Log.e(TAG, "Could not find any suitable preview area");
            return choices[0];
        }
    };

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

        if (BuildConfig.DEBUG) {
            latitudeTextView = (TextView) view.findViewById(R.id.latitude);
            longitudeTextView = (TextView) view.findViewById(R.id.longitude);
            accuracyTextView = (TextView) view.findViewById(R.id.accuracy);
            bearingTextView = (TextView) view.findViewById(R.id.bearing);
        }

        confirmButton = (ImageButton) view.findViewById(R.id.camera_confirm_button);
        cameraShutterButton = (ImageButton) view.findViewById(R.id.camera_shutter_button);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.camera_texture);

        cameraDialogBoxLayout = (LinearLayout) view.findViewById(R.id.camera_dialog_box);
        cameraPreviewImageView = (ImageView) view.findViewById(R.id.camera_preview);
        cameraDialogBoxCardView = (CardView) view.findViewById(R.id.camera_dialog_box_card);
        cameraDialogBoxProgressBar = (ProgressBar) view.findViewById(R.id.card_progress_spinner);
        cameraDialogBoxImageView = (ImageView) view.findViewById(R.id.card_image);
        camreaDialogBoxTextView = (TextView) view.findViewById(R.id.card_text);

        if (noSavedImages > 0) {
            cameraPreviewImageView.setImageBitmap(lastSavedImage.getThumbnailBitmap(getActivity(),
                    tanuj.opengridmap.models.Image.TYPE_LIST));
        }

        return view;
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
        closeCamera();
        stopBackgroundProcess();
        super.onPause();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
        camreaDialogBoxTextView = null;
        currentLocation = null;
        lastSavedImage = null;
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
                        new CompareSizesByArea());

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 2);

                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,
                        mBackgroundHandler);

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width,
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

            mPreviewRequestBuidler = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuidler.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (null == mCameraDevice) {
                                return;
                            }
                            Log.d(TAG, "Configured");

                            mCaptureSession = session;

                            try {
                                mPreviewRequestBuidler.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuidler.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                mPreviewRequest = mPreviewRequestBuidler.build();
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

        if (null == mTextureView || null == mPreviewSize || null == activity) {
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
        lockFocus();
        runPrecaptureSequence();
    }

    private void lockFocus() {
        try {
            mPreviewRequestBuidler.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);

            mState = STATE_WAITING_LOCK;

            mCaptureSession.setRepeatingRequest(mPreviewRequestBuidler.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuidler.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuidler.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillImage() {
        if (!checkLocationStatus())
            return;

        try {
            final Activity activity = getActivity();

            if (null == activity || null == mCameraDevice) {
                return;
            }

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
                            unlockFocus();
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
            mPreviewRequestBuidler.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuidler.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCaptureSession.capture(mPreviewRequestBuidler.build(), mCaptureCallback,
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
                if (images.isEmpty()) {
                    showText("No Pics Taken");
                } else {
                    confirmSubmission();
                }
                break;
            }
        }
    }

    public void updatePreview(final Bitmap bitmap) {
        final Activity activity = getActivity();
        if (null == cameraPreviewImageView) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraPreviewImageView.setImageBitmap(bitmap);
            }
        });
    }

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
                if (null == bytes) {
                    throw new IOException();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != outputStream) {
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

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
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
        if (null == currentLocation) {
            showText("Location not Available");
            return false;
        } else if (currentLocation.getAccuracy() > 200.0) {
            showText("Location not Accurate");
            return false;
        }
        return true;
    }

    private void confirmSubmission() {
        if (images.isEmpty()) {
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

            startActivity(intent);
        }
    }

    public static void setLocation(Location location, Context context) {
        currentLocation = location;
        updateUi(location);
        processCameraState(context);
    }

    public static void updateUi(Location location) {
        if (null == latitudeTextView) {
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

    private static int getLocationStatus() {
        if (null == currentLocation) {
            return LOCATION_STATUS_UNAVAILABLE;
        } else if (System.currentTimeMillis() - currentLocation.getTime() > 5000){
            return LOCATION_STATUS_OUTDATED;
        } else if (currentLocation.getAccuracy() > 200.0) {
            return LOCATION_STATUS_ACCURACY_UNACCEPTABLE;
        }

        return LOCATION_STATUS_OK;
    }

    public static void processCameraState(final Context context) {
        int status = getLocationStatus();

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
        if (null == cameraShutterButton) {
            return;
        }
        if (cameraShutterButton.isClickable()) {
            cameraShutterButton.setClickable(false);
            cameraState = false;
            Log.d(TAG, "Disabled Camera Shutter Button");
        }
    }

    public static void enableCamera() {
        if (null == cameraShutterButton) {
            return;
        }
        if (!cameraShutterButton.isClickable()) {
            cameraShutterButton.setClickable(false);
            cameraState = true;
            Log.d(TAG, "Enabled Camera Shutter Button");
        }
    }

    private static void showLocationAcquisitionDialog(final Context context) {
        camreaDialogBoxTextView.setText(context.getString(R.string.location_message_acquiring));
        cameraDialogBoxImageView.setVisibility(View.GONE);
        cameraDialogBoxCardView.setVisibility(View.VISIBLE);
        cameraDialogBoxProgressBar.setVisibility(View.VISIBLE);
    }

    private static void hideLocationAcquisitionDialog() {
        cameraDialogBoxImageView.setVisibility(View.GONE);
        cameraDialogBoxCardView.setVisibility(View.GONE);
        cameraDialogBoxProgressBar.setVisibility(View.GONE);
    }

    private static void showSavingImagesDialog(final Context context) {
        if (noSavedImages == 1) {
            camreaDialogBoxTextView.setText(context.getString(R.string.camera_message_saving_image));
        } else {
            camreaDialogBoxTextView.setText(context.getString(R.string.camera_message_saving_images));
        }
        cameraDialogBoxImageView.setVisibility(View.GONE);
        cameraDialogBoxCardView.setVisibility(View.VISIBLE);
        cameraDialogBoxProgressBar.setVisibility(View.VISIBLE);
    }
}