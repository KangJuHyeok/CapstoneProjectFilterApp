package com.example.prototypefilterapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.os.Handler;
import android.os.Looper;


public class PromptImageActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 1;

    private static final String BASE_URL = "https://filtering-service-140244301345.us-central1.run.app/";
    private ImageView imageViewAttachedPhoto;
    private Button buttonAttachChangePhoto;
    private EditText editTextAiPrompt;
    private Button buttonRequestAi;
    private ProgressBar progressBar;
    private ImageView filteredResultView;
    private Uri attachedImageUri = null;
    private Button buttonSaveResult;
    private final int MAX_RETRIES = 2; // gemini api 문제가 생겼을 때 최대 재시도 횟수 설정 (총 3회 시도)
    private int retryCount = 0; // 현재 재시도 횟수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt_image);

        imageViewAttachedPhoto = findViewById(R.id.imageViewAttachedPhoto);
        buttonAttachChangePhoto = findViewById(R.id.buttonAttachChangePhoto);
        editTextAiPrompt = findViewById(R.id.editTextAiPrompt);
        buttonRequestAi = findViewById(R.id.buttonRequestAi);
        progressBar = findViewById(R.id.progress_bar);
        filteredResultView = findViewById(R.id.filtered_result_view);
        buttonSaveResult = findViewById(R.id.buttonSaveResult);

        buttonAttachChangePhoto.setOnClickListener(v -> openGallery());
        buttonRequestAi.setOnClickListener(v -> handleAiRequest());
        buttonSaveResult.setOnClickListener(v -> saveFilteredImage());

        updateImageAttachmentUI();
    }

    private void openGallery() {
        Intent intent = new Intent(PromptImageActivity.this, PhotoActivity.class);
        intent.putExtra("IS_SELECTION_MODE", true);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }
    private void updateImageAttachmentUI() {
        if (attachedImageUri != null) {
            imageViewAttachedPhoto.setImageURI(attachedImageUri);
            imageViewAttachedPhoto.setBackgroundResource(0);
            buttonAttachChangePhoto.setText("사진 수정");
            filteredResultView.setVisibility(View.GONE);
        } else {
            imageViewAttachedPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            buttonAttachChangePhoto.setText("사진 첨부");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            String selectedPath = data.getStringExtra("selectedImagePath");
            if (selectedPath != null && !selectedPath.isEmpty()) {
                attachedImageUri = Uri.fromFile(new File(selectedPath));
                updateImageAttachmentUI();
                Toast.makeText(this, "사진이 첨부되었습니다. 글을 작성하거나 바로 AI에게 요청하세요.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "이미지 경로를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] getCompressedImageBytesFromUri(Uri uri) {
        if (uri == null) return null;

        final int MAX_SAFE_SIZE = 4096; // 💡 기준 해상도: 4096px

        try {
            // 1. 원본 이미지의 크기 정보만 읽기
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            InputStream tempInputStream = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(tempInputStream, null, options);
            tempInputStream.close();

            int width = options.outWidth;
            int height = options.outHeight;
            float ratio = 1.0f;

            // 2. 축소 필요성 확인 및 비율 계산
            if (width > MAX_SAFE_SIZE || height > MAX_SAFE_SIZE) {
                // 해상도가 4096px를 초과하는 경우에만 비율과 샘플링 인자를 계산
                ratio = Math.max((float) width / MAX_SAFE_SIZE, (float) height / MAX_SAFE_SIZE);
                options.inSampleSize = (int) Math.pow(2, (int) Math.ceil(Math.log(ratio) / Math.log(2)));
            } else {
                //  원본 그대로 로드: 축소 불필요
                options.inSampleSize = 1;
            }

            // 3. 비트맵 로드
            options.inJustDecodeBounds = false;
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap finalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (finalBitmap == null) return null;

            // 4. 최종 비율 유지 리사이즈 (샘플링 후 남은 오차 수정)
            Bitmap processedBitmap;
            if (ratio > 1.0f) {
                //  리사이즈가 필요한 경우에만 createScaledBitmap 실행
                int finalWidth = (int) (finalBitmap.getWidth() / ratio);
                int finalHeight = (int) (finalBitmap.getHeight() / ratio);

                processedBitmap = Bitmap.createScaledBitmap(
                        finalBitmap, finalWidth, finalHeight, true);

                finalBitmap.recycle();
            } else {
                // 리사이즈 불필요 시, 원본(샘플링 안된) 비트맵을 그대로 사용
                processedBitmap = finalBitmap;
            }


            // 5. 바이트 배열로 압축
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            //  원본 이미지라도 네트워크 효율을 위해 80% 압축을 적용
            //    (무압축 전송은 OOM/TimeOut 위험이 너무 큼)
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            processedBitmap.recycle();

            return byteArrayOutputStream.toByteArray();

        } catch (Exception e) {
            Log.e("ImageRead", "Failed to process image bytes: " + e.getMessage());
            Toast.makeText(this, "이미지 처리 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    private void displayFilteredImage(String base64Image) {

        if (base64Image != null && !base64Image.isEmpty()) {
            Toast.makeText(this, "AI 필터가 적용된 이미지를 받았습니다! (전체 화면으로 표시)", Toast.LENGTH_LONG).show();
            // Intent 대신 GlobalData 싱글톤에 저장
            GlobalData.getInstance().setFilteredImageBase64Data(base64Image);
            Intent intent = new Intent(PromptImageActivity.this, FullScreenImageActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "AI가 필터링된 이미지 데이터를 반환하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }
    private void saveFilteredImage() {
        Toast.makeText(this, "이미지 저장 기능은 전체 화면에서 제공됩니다.", Toast.LENGTH_SHORT).show();
    }

    private void handleAiRequest() {
        String promptText = editTextAiPrompt.getText().toString().trim();
        boolean hasPhoto = attachedImageUri != null;
        boolean hasText = !TextUtils.isEmpty(promptText);

        if (!hasPhoto) {
            Toast.makeText(this, "⚠️ AI 필터 추천을 위해 사진 첨부는 필수입니다.", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. UI 상태 변경 (로딩 시작)
        progressBar.setVisibility(View.VISIBLE);
        buttonRequestAi.setEnabled(false);
        // 2. Base64 인코딩 작업을 백그라운드 스레드에서 시작
        new Thread(() -> {
            byte[] imageBytes = getCompressedImageBytesFromUri(attachedImageUri);

            String finalPrompt = hasText ? promptText : "이 사진과 어울리는 감성 또는 분위기의 필터를 추천해줘.";
            byte[] finalImageBytes = imageBytes;

            // 3. 메인 스레드로 돌아가 API 요청 실행
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalImageBytes != null) {
                    // 바이트 배열 준비 성공 시 API 요청 실행
                    Toast.makeText(PromptImageActivity.this, "✔️ 서버로 요청 전송 시작...", Toast.LENGTH_SHORT).show();
                    sendApiRequest(finalPrompt, finalImageBytes);
                } else {
                    progressBar.setVisibility(View.GONE);
                    buttonRequestAi.setEnabled(true);
                    Toast.makeText(PromptImageActivity.this, "이미지 파일 읽기/인코딩에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void sendApiRequest(String prompt, byte[] imageBytes) {

        // Retrofit 설정
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(200, TimeUnit.SECONDS)
                .readTimeout(420, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitApiService apiService = retrofit.create(RetrofitApiService.class);

        // 요청 Multipart Form Data 구성
        // 이미지 파일 part 구성
        RequestBody imageRequestBody = RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), imageBytes);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "photo.jpg", imageRequestBody);
        // 프롬프트 텍스트 part 구성
        RequestBody promptRequestBody = RequestBody.create(okhttp3.MediaType.parse("text/plain"), prompt);
        // 비동기 요청 (Multipart API 호출)
        Call<JsonObject> call = apiService.generateFilterImage(imagePart, promptRequestBody);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {


                if (response.isSuccessful() && response.body() != null) {
                    retryCount = 0;
                    progressBar.setVisibility(View.GONE);
                    buttonRequestAi.setEnabled(true);

                    // 서버가 Base64 이미지 데이터를 직접 반환한다고 가정
                    String filteredImageBase64 = response.body().get("filtered_image_data").getAsString();

                    if (filteredImageBase64 != null && !filteredImageBase64.isEmpty()) {
                        // 2. 성공: Base64 문자열을 displayFilteredImage에 전달하여 표시
                        displayFilteredImage(filteredImageBase64); // <-- Base64 문자열 전달
                    } else {
                        // 3. 성공했으나 데이터가 없는 경우
                        Toast.makeText(PromptImageActivity.this, "AI가 필터링된 이미지 데이터를 반환하지 못했습니다.", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    // 💡 [재시도 로직 - 서버 오류 5xx, 4xx 등]
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        Toast.makeText(PromptImageActivity.this,
                                "서버 오류 감지! (재시도 중... " + retryCount + "/" + MAX_RETRIES + ")",
                                Toast.LENGTH_SHORT).show();

                        // 3초 딜레이 후 재시도
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // 재귀 호출 시, 원본 데이터(prompt, imageBytes)를 다시 전달해야 함
                            sendApiRequest(prompt, imageBytes);
                        }, 3000);
                    } else {
                        // 💡 [최종 실패] 최대 재시도 횟수 초과
                        retryCount = 0;
                        progressBar.setVisibility(View.GONE);
                        buttonRequestAi.setEnabled(true);

                        String errorMsg = "서버 오류: " + response.code() + " (최대 재시도 횟수 초과)";
                        Toast.makeText(PromptImageActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    Toast.makeText(PromptImageActivity.this,
                            "네트워크 오류 감지! (재시도 중... " + retryCount + "/" + MAX_RETRIES + ")",
                            Toast.LENGTH_SHORT).show();

                    // 3초 딜레이 후 재시도
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        sendApiRequest(prompt, imageBytes);
                    }, 3000);
                } else {
                    retryCount = 0;
                    progressBar.setVisibility(View.GONE);
                    buttonRequestAi.setEnabled(true);

                    Toast.makeText(PromptImageActivity.this, "네트워크 타임아웃 또는 연결 오류: " + t.getMessage() + " (최대 재시도 횟수 초과)", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}