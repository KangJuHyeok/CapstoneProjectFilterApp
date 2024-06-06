package com.example.prototypefilterapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.widget.Button;
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
import java.util.Date;

public class CameraActivity extends AppCompatActivity {
    
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private TextureView textureView;
    private ImageButton captureButton;
    private ImageButton switchCameraButton;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private ImageView filteredImageView; // 추가: 필터링된 이미지를 표시할 ImageView
    // 현재 카메라 모드를 추적하기 위한 변수 추가
    private boolean isFrontCamera = false;
    private DatabaseHelper databaseHelper;
    private int frameCount = 0;
    private static final int FRAME_SKIP = 1; //현재는 프레임 하나당 바로 적용
    private String filterType;

    {
        // 로딩 콜백을 설정합니다.
        CGENativeLibrary.setLoadImageCallback(new LoadImageCallback() {
            @Override
            public Bitmap loadImage(String imgName, Object arg) {
                // 이미지를 로드하는 로직을 구현합니다.
                // 지금은 필요없으므로 null을 반환합니다.
                int resID = getResources().getIdentifier(imgName, "drawable", getPackageName());
                if (resID != 0) {
                    return BitmapFactory.decodeResource(getResources(), resID);
                }
                return null;
            }

            @Override
            public void loadImageOK(Bitmap bmp, Object arg) {
                // 이미지를 성공적으로 로드했을 때 호출됩니다.
                // 여기서 이미지를 사용한 후 자원을 해제할 수 있습니다.
                if (bmp != null) {
                    bmp.recycle();
                }
            }
        }, null); // 두 번째 인자는 필요하지 않으면 null을 전달할 수 있습니다.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 상태 표시줄을 숨깁니다.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback() {

            //Notice: the 'name' passed in is just what you write in the rule, e.g: 1.jpg
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

                //The bitmap is which you returned at 'loadImage'.
                //You can call recycle when this function is called, or just keep it for further usage.
                bmp.recycle();
            }
        };

        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);
        setContentView(R.layout.activity_camera);
        setContentView(R.layout.activity_camera);
        databaseHelper = new DatabaseHelper(this);
        //인텐트에서 필터 종류를 가져옵니다.
        filterType = getIntent().getStringExtra("FILTER_TYPE");
        textureView = findViewById(R.id.texture_view);
        captureButton = findViewById(R.id.capture_button);
        filteredImageView = findViewById(R.id.filtered_image_view); // 추가
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        captureButton.setOnClickListener(view -> takePicture());
        switchCameraButton = findViewById(R.id.switch_camera_button); // 추가: 카메라 전환 버튼
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
    }
    private void switchCamera() {
        // 현재 카메라 디바이스를 닫습니다.
        closeCamera();

        // 카메라 디바이스를 변경합니다.
        if (isFrontCamera) {
            isFrontCamera = false;
        } else {
            isFrontCamera = true;
        }
        // 카메라를 엽니다.
        openCamera();
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            frameCount++;
            if (frameCount % FRAME_SKIP == 0) {
                Bitmap bitmap = textureView.getBitmap(textureView.getWidth(), textureView.getHeight());
                Bitmap filteredBitmap = abcFilteringFunction(bitmap);
                filteredImageView.setImageBitmap(filteredBitmap);
                filteredImageView.setVisibility(View.VISIBLE);
            }
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String[] cameraIdList = manager.getCameraIdList();
            String cameraId = null;
            // 전면 카메라를 사용하는 경우
            if (isFrontCamera) {
                for (String id : cameraIdList) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                    // 전면 카메라를 찾습니다.
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraId = id;
                        break;
                    }
                }
            } else { // 후면 카메라를 사용하는 경우
                for (String id : cameraIdList) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                    // 후면 카메라를 찾습니다.
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id;
                        break;
                    }
                }
            }

            if (cameraId == null) {
                // 선택된 카메라가 없는 경우 오류 메시지를 출력하고 메서드를 종료합니다.
                Log.e("CameraActivity", "Failed to find camera");
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] jpegSizes = null;
            if (map != null) {
                jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
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
                        filteredImageView.setImageBitmap(filteredBitmap);
                        filteredImageView.setVisibility(View.VISIBLE);
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }, null);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // 이미 SurfaceTexture가 사용 중인 경우이므로 그냥 리턴합니다.
            return;
        }
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
            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
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

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    //사진을 찍을 때
    private void takePicture() {
        // 현재 화면의 filteredImageView에서 Bitmap을 가져옵니다.
        Bitmap bitmap = ((BitmapDrawable) filteredImageView.getDrawable()).getBitmap();

        playShutterSound();
        // 이미지를 저장할 파일 이름을 생성합니다.
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // 외부 저장소의 앨범 디렉토리에 이미지를 저장할 폴더를 생성합니다.
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FilterApp");


        // 만약 폴더가 존재하지 않는다면 생성합니다.
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
                // 파일로 이미지를 저장합니다.
                OutputStream os = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
                // 저장된 이미지를 미디어 스캔하여 갤러리에 보이도록 합니다.
                MediaScannerConnection.scanFile(this,
                        new String[]{imageFile.getAbsolutePath()},
                        new String[]{"image/jpeg"},
                        null);
                // 데이터베이스에 이미지 경로를 추가합니다.
                String imagePath = imageFile.getAbsolutePath();
                databaseHelper.addImagePath(imagePath); // 이 부분에서 데이터베이스에 imagePath를 추가하는 메서드를 호출해야 합니다.
                // 이미지 저장이 완료되었음을 사용자에게 알립니다.
                Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playShutterSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shutter_sound);
        mediaPlayer.start();
    }

    private Bitmap abcFilteringFunction(Bitmap bitmap) {
        // Implement your filtering function here
        Bitmap newBitmap= CGENativeLibrary.filterImage_MultipleEffects(bitmap, filterType, 1.0f);
        return newBitmap; // Placeholder return statement, replace it with your actual filtering logic
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
