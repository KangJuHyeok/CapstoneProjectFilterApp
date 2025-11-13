package com.example.prototypefilterapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.wysaid.common.Common;
import org.wysaid.nativePort.CGENativeLibrary.LoadImageCallback;
import org.wysaid.nativePort.CGENativeLibrary;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;
    private ImageButton captureButton;
    private ImageButton switchCameraButton;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private ImageView filteredImageView;

    private boolean isFrontCamera = false;
    private DatabaseHelper databaseHelper;
    private int frameCount = 0;
    private static final int FRAME_SKIP = 3;
    private String filterType;
    private Size previewSize;
    private Size imageSize;

    {
        CGENativeLibrary.setLoadImageCallback(new LoadImageCallback() {
            @Override
            public Bitmap loadImage(String imgName, Object arg) {
                int resID = getResources().getIdentifier(imgName, "drawable", getPackageName());
                if (resID != 0) {
                    return BitmapFactory.decodeResource(getResources(), resID);
                }
                return null;
            }

            @Override
            public void loadImageOK(Bitmap bmp, Object arg) {
                if (bmp != null) {
                    bmp.recycle();
                }
            }
        }, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback() {
            @Override
            public Bitmap loadImage(String name, Object arg) {
                Log.i(Common.LOG_TAG, "Loading file: " + name);
                AssetManager am = getAssets();
                InputStream is;
                try {
                    is = am.open(name);
                } catch (IOException e) {
                    Log.e(Common.LOG_TAG, "Can not open file " + name);
                    return null;
                }
                return BitmapFactory.decodeStream(is);
            }
            @Override
            public void loadImageOK(Bitmap bmp, Object arg) {
                Log.i(Common.LOG_TAG, "Loading bitmap over, you can choose to recycle or cache");
                if (bmp != null) bmp.recycle();
            }
        };

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);

        setContentView(R.layout.activity_camera);

        databaseHelper = new DatabaseHelper(this);
        filterType = getIntent().getStringExtra("FILTER_TYPE");
        textureView = findViewById(R.id.texture_view);
        captureButton = findViewById(R.id.capture_button);
        filteredImageView = findViewById(R.id.filtered_image_view);
        switchCameraButton = findViewById(R.id.switch_camera_button);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        captureButton.setOnClickListener(view -> takePicture());
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
    }

    private void switchCamera() {
        closeCamera();
        isFrontCamera = !isFrontCamera;
        openCamera();
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            if (cameraDevice != null && previewSize != null) {
                configureTransform(width, height);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            frameCount++;
            if (frameCount % FRAME_SKIP == 0) {
                // 비동기 스레드에서 작업 실행
                new Thread(() -> {
                    Bitmap bitmap = textureView.getBitmap(textureView.getWidth(), textureView.getHeight());
                    Bitmap filteredBitmap = abcFilteringFunction(bitmap);

                    runOnUiThread(() -> {
                        filteredImageView.setImageBitmap(filteredBitmap);
                        filteredImageView.setVisibility(View.VISIBLE);
                        if (bitmap != null) bitmap.recycle();
                    });
                }).start();
            }
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = chooseCameraId(manager);
            if (cameraId == null) {
                Log.e("CameraActivity", "Failed to find camera");
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            imageSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

            int displayWidth = textureView.getWidth();
            int displayHeight = textureView.getHeight();

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    displayWidth,
                    displayHeight,
                    imageSize);

            imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Bitmap filteredBitmap = abcFilteringFunction(bitmap);

                        runOnUiThread(() -> {
                            filteredImageView.setImageBitmap(filteredBitmap);
                            filteredImageView.setVisibility(View.VISIBLE);
                        });
                        if (bitmap != null) bitmap.recycle();
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }, null);

            // 권한 확인 및 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private String chooseCameraId(CameraManager manager) throws CameraAccessException {
        String[] cameraIdList = manager.getCameraIdList();
        int facing = isFrontCamera ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;

        for (String id : cameraIdList) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                return id;
            }
        }
        return null;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            if (previewSize != null) {
                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            } else {
                texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            }

            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            configureTransform(textureView.getWidth(), textureView.getHeight());

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null) {
                        return;
                    }
                    CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (previewSize == null) {
            return;
        }

        Matrix matrix = new Matrix();
        float viewRatio = (float) viewWidth / viewHeight;
        float previewRatio = (float) previewSize.getWidth() / previewSize.getHeight();
        float scale;

        if (viewRatio > previewRatio) {
            scale = (float) viewHeight / previewSize.getHeight();
        } else {
            scale = (float) viewWidth / previewSize.getWidth();
        }

        matrix.setScale(scale, scale, viewWidth / 2, viewHeight / 2);

        textureView.setTransform(matrix);
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, Size aspectRatio) {
        List<Size> bigEnough = new java.util.ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return Collections.max(Arrays.asList(choices), new CompareSizesByArea());
        }
    }

    static class CompareSizesByArea implements java.util.Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    private void takePicture() {
        Bitmap bitmap = ((BitmapDrawable) filteredImageView.getDrawable()).getBitmap();

        playShutterSound();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FilterApp");

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("CameraActivity", "Failed to create directory");
                return;
            }
        }

        // 저장할 파일 경로를 FilterApp 폴더 내에 지정합니다.
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                    imageFileName,  /* 파일 이름 */
                    ".jpg",         /* 파일 확장자 */
                    storageDir      /* 저장 디렉토리 */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (imageFile != null) {
            try {
                OutputStream os = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
                // 저장된 이미지를 미디어 스캔하여 갤러리에 보이도록
                MediaScannerConnection.scanFile(this,
                        new String[]{imageFile.getAbsolutePath()},
                        new String[]{"image/jpeg"},
                        null);
                // 데이터베이스에 이미지 경로를 추가
                String imagePath = imageFile.getAbsolutePath();
                databaseHelper.addImagePath(imagePath);
                Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playShutterSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shutter_sound);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        } else {
            Log.e("CameraActivity", "Shutter sound resource not found (R.raw.shutter_sound)");
        }
    }

    private Bitmap abcFilteringFunction(Bitmap bitmap) {
        Bitmap newBitmap= CGENativeLibrary.filterImage_MultipleEffects(bitmap, filterType, 1.0f);
        return newBitmap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You need to grant camera permission to use this app", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                openCamera();
            }
        }
    }
}