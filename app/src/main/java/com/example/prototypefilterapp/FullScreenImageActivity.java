package com.example.prototypefilterapp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.wysaid.common.Common;
import org.wysaid.nativePort.CGENativeLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 이미지를 업로드하고 처리할 수 있는 곳
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

        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.fullscreen_image_view);
        // 인텐트에서 이미지 경로를 가져옵니다.
        String imagePath = getIntent().getStringExtra("imagePath");
        //인텐트에서 필터 종류를 가져옵니다.
        String filterType = getIntent().getStringExtra("FILTER_TYPE");
        // Get the display dimensions
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        // Decode the Bitmap from the file path with the appropriate sample size
        Bitmap srcImage = decodeSampledBitmapFromFile(imagePath, displayWidth, displayHeight);
        // 이미지 회전 처리
        srcImage = rotateImageIfRequired(srcImage, imagePath);
        // 필터 적용
        Bitmap dstImage = CGENativeLibrary.filterImage_MultipleEffects(srcImage, filterType, 1.0f);
        // 이미지 뷰에 비트맵 설정
        imageView.setImageBitmap(dstImage);
        // 이미지 비율에 맞게 설정
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // 저장 버튼을 찾아옵니다.
        ImageButton saveButton = findViewById(R.id.save_button);
        // 저장 버튼 클릭 시 이벤트 처리
        saveButton.setOnClickListener(view -> saveImageToExternalStorage(dstImage));
    }


    // 이미지 크기를 디바이스의 디스플레이 크기에 맞게 조정하는 방법
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    // 외부 저장소에 이미지를 저장하는 메서드
    private void saveImageToExternalStorage(Bitmap imageBitmap) {
        // 외부 저장소의 앨범 디렉토리에 이미지를 저장할 파일을 생성합니다.
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
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
                // 저장된 이미지를 미디어 스캔하여 갤러리에 보이도록 합니다.
                MediaScannerConnection.scanFile(this,
                        new String[]{imageFile.getAbsolutePath()},
                        new String[]{"image/jpeg"},
                        null);
                // 이미지 저장이 완료되었음을 사용자에게 알립니다.
                Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String imagePath) {
        ExifInterface ei;
        try {
            ei = new ExifInterface(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }
    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }
}
