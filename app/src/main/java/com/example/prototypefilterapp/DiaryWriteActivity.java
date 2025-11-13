package com.example.prototypefilterapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.app.AlertDialog;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.ImageButton;
import java.util.Date;
import java.text.SimpleDateFormat;
import androidx.annotation.Nullable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import org.wysaid.nativePort.CGENativeLibrary;
import android.content.res.AssetManager;
import java.io.InputStream;
import java.io.IOException;
import org.wysaid.common.Common;


public class DiaryWriteActivity extends AppCompatActivity {

    private EditText diaryEditText;
    private Button analyzeButton;

    private static final String BASE_URL = "https://filtering-service-140244301345.us-central1.run.app/";
    private ImageView photoPreview;
    private ImageButton attachPhotoButton;
    private Uri selectedImageUri;
    private String imagePath;
    private DatabaseHelper dbHelper;
    private ProgressBar progressBarRecommendation;
    private final int MAX_RETRIES = 2;
    private int retryCount = 0;
    private static final int PICK_IMAGE_REQUEST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_write);

        CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback() {
            @Override
            public Bitmap loadImage(String name, Object arg) {
                // Log.i(Common.LOG_TAG, "Loading file from DiaryWrite: " + name); // 디버그용
                AssetManager am = getAssets();
                InputStream is;
                try {
                    is = am.open(name);
                } catch (IOException e) {
                    Log.e(Common.LOG_TAG, "Can not open asset file: " + name);
                    return null;
                }
                return BitmapFactory.decodeStream(is);
            }
            @Override
            public void loadImageOK(Bitmap bmp, Object arg) {
                if (bmp != null) bmp.recycle();
            }
        };
        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);


        diaryEditText = findViewById(R.id.diary_edit_text);
        analyzeButton = findViewById(R.id.analyze_button);
        photoPreview = findViewById(R.id.photo_preview);
        attachPhotoButton = findViewById(R.id.attach_photo_button);
        dbHelper = new DatabaseHelper(this);

        progressBarRecommendation = findViewById(R.id.progress_bar_recommendation);
        progressBarRecommendation.setVisibility(View.GONE);

        updateAnalyzeButtonText();

        attachPhotoButton.setOnClickListener(v -> openAlbum());

        analyzeButton.setOnClickListener(v -> {
            String diaryContent = diaryEditText.getText().toString();
            if (diaryContent.isEmpty()) {
                Toast.makeText(this, "일기를 작성해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                progressBarRecommendation.setVisibility(View.VISIBLE);
                analyzeButton.setEnabled(false);
                Toast.makeText(this, "AI 필터 추천을 시작합니다...", Toast.LENGTH_SHORT).show();
                analyzeDiaryRecommendation(diaryContent);
            }
        });
    }

    private void openAlbum() {
        Intent intent = new Intent(DiaryWriteActivity.this, PhotoActivity.class);
        intent.putExtra("IS_SELECTION_MODE", true);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            String selectedPath = data.getStringExtra("selectedImagePath");

            if (selectedPath != null && !selectedPath.isEmpty()) {
                imagePath = selectedPath;

                File file = new File(imagePath);
                selectedImageUri = Uri.fromFile(file);

                photoPreview.setImageURI(selectedImageUri);
                photoPreview.setVisibility(View.VISIBLE);
                updateAnalyzeButtonText();
                Toast.makeText(this, "사진이 첨부되었습니다.", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "이미지 경로를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateAnalyzeButtonText() {
        if (imagePath != null && !imagePath.isEmpty()) {
            analyzeButton.setText("분석 및 필터 적용");
        } else {
            analyzeButton.setText("추천 필터 찾고 저장하기");
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    return cursor.getString(column_index);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private void analyzeDiaryRecommendation(String text) {
        if (progressBarRecommendation.getVisibility() != View.VISIBLE) {
            progressBarRecommendation.setVisibility(View.VISIBLE);
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitApiService apiService = retrofit.create(RetrofitApiService.class);

        JsonArray filterArray = new JsonArray();
        for (FilterData.FilterItem item : FilterData.getInstance().getFilterList()) {
            filterArray.add(item.toJsonObject());
        }

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("text", text);
        jsonBody.add("filter_list", filterArray);

        Call<JsonObject> call = apiService.analyzeRecommendation(jsonBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    retryCount = 0; // 성공 시 재시도 카운트 리셋
                    progressBarRecommendation.setVisibility(View.GONE); // ProgressBar 숨김
                    analyzeButton.setEnabled(true);
                    JsonObject recommendedFilter = response.body();

                    String name = recommendedFilter.get("recommendedFilterName").getAsString().trim();


                    FilterData.FilterItem originalFilter = FilterData.getInstance().getFilterItemByName(name);

                    if (originalFilter != null) {
                        showRecommendationDialog(originalFilter);
                    } else {
                        Toast.makeText(DiaryWriteActivity.this,
                                "추천된 필터 이름을 목록에서 찾을 수 없습니다: " + name,
                                Toast.LENGTH_LONG).show();
                    }

                } else {
                    handleRetry(text, "서버 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                handleRetry(text, "네트워크 또는 서버 오류: " + t.getMessage());
            }
        });
    }

    private void handleRetry(String text, String errorReason) {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            Toast.makeText(DiaryWriteActivity.this,
                    errorReason + " (재시도 중... " + retryCount + "/" + MAX_RETRIES + ")",
                    Toast.LENGTH_SHORT).show();

            // 3초 딜레이 후 재시도
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                analyzeDiaryRecommendation(text); // 재귀 호출
            }, 3000);

        } else {
            // 💡 [최종 실패]
            retryCount = 0;
            progressBarRecommendation.setVisibility(View.GONE); // ProgressBar 숨김
            analyzeButton.setEnabled(true);

            Toast.makeText(DiaryWriteActivity.this,
                    errorReason + " (최대 재시도 횟수 초과)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void saveDiaryEntry(String filterName, @Nullable String imagePathToSave) {
        String title = diaryEditText.getText().toString().substring(0, Math.min(diaryEditText.getText().length(), 20)) + "...";
        String content = diaryEditText.getText().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        dbHelper.addStory(title, content, date, imagePathToSave, filterName);
        Toast.makeText(this, "일기와 추천 필터가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void confirmFilterAndSaveDialog(final FilterData.FilterItem filter) {
        new AlertDialog.Builder(this)
                .setTitle("✨ AI 필터 추천 결과 ✨")
                .setMessage("일기 분석 결과, '" + filter.filterName + "' 필터가 오늘 당신의 감성과 가장 잘 어울립니다.\n\n" +
                        "설명: " + filter.filterDescription + "\n\n 필터를 적용한 사진으로 일기를 저장하시겠습니까?")
                .setNegativeButton("예(필터 적용 사진 저장)", (dialog, which) -> {
                    applyAndSaveFilteredImage(filter);
                    finish();
                })
                .setPositiveButton("아니오(원본 사진 저장)", (dialog, which) -> {
                    saveDiaryEntry(filter.filterName, imagePath);
                    finish();
                })
                .show();
    }

    private void showRecommendationDialog(final FilterData.FilterItem filter) {
        if (imagePath != null && !imagePath.isEmpty()) {
            confirmFilterAndSaveDialog(filter);
            return;
        }

        String positiveButtonText = "글 저장하기";

        new AlertDialog.Builder(this)
                .setTitle("✨ AI 필터 추천 결과 ✨")
                .setMessage("일기 분석 결과, '" + filter.filterName + "' 필터가 오늘 당신의 감성과 가장 잘 어울립니다.\n\n" +
                        "설명: " + filter.filterDescription)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    saveDiaryEntry(filter.filterName, null);
                    finish();
                })
                .show();
    }

    private void applyAndSaveFilteredImage(FilterData.FilterItem filter) {
        if (imagePath == null || imagePath.isEmpty()) {
            Toast.makeText(this, "사진 파일 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            final int MAX_SAVE_SIZE = 2048; // 최대 2048x2048로 로드하여 화질 유지

            // 1. 이미지 로드 및 회전 처리 (MAX_SAVE_SIZE를 기준으로 로드)
            Bitmap srcImage = decodeSampledBitmapFromFile(imagePath, MAX_SAVE_SIZE, MAX_SAVE_SIZE);
            srcImage = rotateImageIfRequired(srcImage, imagePath);

            // 2. 로컬 필터 적용 (CGENativeLibrary 사용)
            Bitmap filteredBitmap = CGENativeLibrary.filterImage_MultipleEffects(srcImage, filter.filterType, 1.0f);


            // 3. 필터 적용된 이미지를 새로운 파일로 저장
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + filter.filterName;

            // Context.MODE_PRIVATE를 사용하여 앱 내부 저장소에 저장
            File storageDir = getDir("story_images", Context.MODE_PRIVATE);

            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Toast.makeText(this, "폴더 생성 실패.", Toast.LENGTH_SHORT).show();
                return;
            }

            File imageFile = new File(storageDir, imageFileName);

            try (OutputStream os = new FileOutputStream(imageFile)) {
                filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            }

            // 4. DB에 최종 저장 (필터 적용된 이미지 경로 사용)
            String newImagePath = imageFile.getAbsolutePath();
            saveDiaryEntry(filter.filterName, newImagePath); // DB 저장

            // 5. 메모리 정리
            if (srcImage != null) srcImage.recycle();
            if (filteredBitmap != null) filteredBitmap.recycle();

            Toast.makeText(this, "필터 적용 및 저장 완료!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("FilterSave", "Error during filter application and save: " + e.getMessage());
            Toast.makeText(this, "필터 적용 및 저장 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqWidth && (halfWidth / inSampleSize) >= reqHeight) {
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