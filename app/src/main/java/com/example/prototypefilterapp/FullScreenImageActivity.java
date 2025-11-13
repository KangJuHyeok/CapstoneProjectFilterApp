package com.example.prototypefilterapp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import org.wysaid.common.Common;
import org.wysaid.nativePort.CGENativeLibrary;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FullScreenImageActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private Bitmap finalBitmapToSave; // 💡 저장할 최종 비트맵 (AI 결과 또는 로컬 필터 결과)
    private ImageView imageView; // XML ID를 사용

    // CGENativeLibrary.LoadImageCallback mLoadImageCallback 은 onCreate 밖으로 이동하지 않으므로 그대로 둡니다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 💡 기존 CGENativeLibrary 콜백 설정 유지
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
        // ----------------------------------------------------

        setContentView(R.layout.activity_full_screen_image);

        databaseHelper = new DatabaseHelper(this); // dbHelper 초기화 유지

        // UI 컴포넌트 초기화
        imageView = findViewById(R.id.fullscreen_image_view);
        ImageButton saveButton = findViewById(R.id.save_button);

        // 인텐트 데이터 추출
        String imagePath = getIntent().getStringExtra("imagePath"); // 로컬 필터용
        String filterType = getIntent().getStringExtra("FILTER_TYPE"); // 로컬
        Boolean StoryDetailCase = getIntent().getBooleanExtra("StoryDetailCase",false); // 로컬 필터용

        // ⬇️ ⬇️ ⬇️ 핵심: GlobalData 싱글톤에서 Base64 데이터 로드 ⬇️ ⬇️ ⬇️
        String base64Image = GlobalData.getInstance().getFilteredImageBase64Data();
// ⬆️ ⬆️ ⬆️ GlobalData 싱글톤에서 Base64 데이터 로드 ⬆️ ⬆️ ⬆️

        // ⬇️ ⬇️ ⬇️ 핵심: 로직 분기 및 저장 버튼 가시성 제어 ⬇️ ⬇️ ⬇️
        boolean isFilterApplied = false; // 필터가 적용되었는지 추적

        // ⬇️ ⬇️ ⬇️ 핵심: 인텐트 종류에 따라 로직 분기 ⬇️ ⬇️ ⬇️
        if (base64Image != null && !base64Image.isEmpty()) {
            // A. AI 서버 결과 (Base64) 로드 - 새로운 로직
            loadAiFilteredImage(base64Image);
            GlobalData.getInstance().clearData(); // 💡 데이터 사용 후 메모리 정리
            isFilterApplied = true;
        } else if (imagePath != null && filterType != null) {
            // B. 로컬 필터링 결과 로드 - 기존 로직 유지
            loadLocallyFilteredImage(imagePath, filterType);
            isFilterApplied = true;

        } else if (imagePath != null && StoryDetailCase) {
            // C. 순수 원본 확대 모드 (imagePath만 넘어왔을 때 - 앨범 보기 모드)
            loadOriginalImage(imagePath);
            isFilterApplied = true;

        } else if (imagePath != null) {
            // C. 순수 원본 확대 모드 (imagePath만 넘어왔을 때 - 앨범 보기 모드)
            loadOriginalImage(imagePath);
            isFilterApplied = false;

        } else {
            // D. 모든 데이터가 부족할 때
            Toast.makeText(this, "이미지 정보가 부족합니다.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ⬇️ ⬇️ ⬇️ 저장 버튼 가시성 최종 제어 ⬇️ ⬇️ ⬇️
        if (isFilterApplied) {
            saveButton.setVisibility(View.VISIBLE); // 필터링된 이미지만 저장 가능
        } else {
            saveButton.setVisibility(View.GONE); // 원본 이미지는 저장 버튼 숨김
        }
        // ⬆️ ⬆️ ⬆️ 저장 버튼 가시성 최종 제어 ⬆️ ⬆️ ⬆️


        // 저장 버튼 클릭 리스너 (공통)
        saveButton.setOnClickListener(view -> {
            if (finalBitmapToSave != null) {
                saveImageToExternalStorage(finalBitmapToSave);
            } else {
                Toast.makeText(this, "저장할 이미지가 준비되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ----------------------------------------------------
    // [A] AI 서버 결과 로드 로직 (Base64 수신 및 표시)
    // ----------------------------------------------------
    private void loadAiFilteredImage(String base64Image) {
        try {
            // 1. Base64를 Bitmap으로 변환 (저장용)
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            finalBitmapToSave = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            // 2. Glide를 사용하여 Base64 URI 로드 및 전체 화면에 표시
            String imageUri = "data:image/jpeg;base64," + base64Image;
            Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                    .into(imageView);

            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } catch (Exception e) {
            Log.e("AILoad", "AI 이미지 로드 오류: " + e.getMessage());
            Toast.makeText(this, "AI 이미지 로드 오류 발생.", Toast.LENGTH_LONG).show();
        }
    }

    // ----------------------------------------------------
    // [B] 기존 로컬 필터링 결과 로드 로직 (기존 로직 대체)
    // ----------------------------------------------------
    private void loadLocallyFilteredImage(String imagePath, String filterType) {
        try {
            // Get the display dimensions
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = displayMetrics.widthPixels;
            int displayHeight = displayMetrics.heightPixels;

            // Decode the Bitmap from the file path with the appropriate sample size
            Bitmap srcImage = decodeSampledBitmapFromFile(imagePath, displayWidth, displayHeight);
            // 이미지 회전 처리
            srcImage = rotateImageIfRequired(srcImage, imagePath);

            // 필터 적용 (CGENativeLibrary 사용)
            Bitmap dstImage = CGENativeLibrary.filterImage_MultipleEffects(srcImage, filterType, 1.0f);

            // 최종 비트맵 설정 및 표시
            finalBitmapToSave = dstImage;
            imageView.setImageBitmap(dstImage);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // 원본 비트맵 메모리 해제
            if (srcImage != null) srcImage.recycle();

        } catch (Exception e) {
            Log.e("LocalFilter", "Local filtering error: " + e.getMessage());
            Toast.makeText(this, "로컬 필터링 중 오류 발생.", Toast.LENGTH_LONG).show();
        }
    }

    // ----------------------------------------------------
    // [C] 순수 원본 이미지 로드 로직 (NEW)
    // ----------------------------------------------------
    private void loadOriginalImage(String imagePath) {
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = displayMetrics.widthPixels;
            int displayHeight = displayMetrics.heightPixels;

            Bitmap srcImage = decodeSampledBitmapFromFile(imagePath, displayWidth, displayHeight);
            srcImage = rotateImageIfRequired(srcImage, imagePath);

            finalBitmapToSave = srcImage;
            imageView.setImageBitmap(srcImage);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        } catch (Exception e) {
            Log.e("OriginalLoad", "Original image loading error: " + e.getMessage());
            Toast.makeText(this, "원본 이미지 로드 중 오류 발생.", Toast.LENGTH_LONG).show();
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private void saveImageToExternalStorage(Bitmap imageBitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String albumName = "FilterApp";

        // ⬇️ ⬇️ ⬇️ 핵심 수정: DIRECTORY_DCIM -> DIRECTORY_PICTURES로 변경 ⬇️ ⬇️ ⬇️
        // 카메라 앨범과 분리하기 위해 Pictures 디렉토리를 사용합니다.
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
        // ⬆️ ⬆️ ⬆️ DIRECTORY_PICTURES로 변경 완료 ⬆️ ⬆️ ⬆️

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("SaveImage", "Failed to create directory: " + storageDir.getAbsolutePath());
                return;
            }
        }

        File imageFile = null;
        try {
            // ... (나머지 File.createTempFile 로직 유지) ...
            imageFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            Log.e("SaveImage", "Error creating temp file: " + e.getMessage());
            return;
        }

        if (imageFile != null) {
            try {
                OutputStream os = new FileOutputStream(imageFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();

                // 미디어 스캔
                MediaScannerConnection.scanFile(this,
                        new String[]{imageFile.getAbsolutePath()},
                        new String[]{"image/jpeg"},
                        null);

                // DB 로직 유지
                String imagePath = imageFile.getAbsolutePath();
                if (databaseHelper != null) {
                    databaseHelper.addImagePath(imagePath);
                }

                Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e("SaveImage", "Error saving file: " + e.getMessage());
            }
        }
    }
    // 이미지 회전 로직 (기존 코드 그대로 유지)
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