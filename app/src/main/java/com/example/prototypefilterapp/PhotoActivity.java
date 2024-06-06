package com.example.prototypefilterapp;

import android.Manifest;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

public class PhotoActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 2;
    private GridView gridView;
    private DatabaseHelper dbHelper;
    static ArrayList<String> imagePaths = new ArrayList<>();; // 사진 경로 리스트

    // 멀티 선택 모드와 선택된 아이템의 리스트를 관리할 변수
    boolean isMultiSelect = false;
    private static final int REQUEST_CODE_READ_MEDIA_IMAGES = 102;
    ArrayList<Integer> selectedItems = new ArrayList<>();
    // 사진 어댑터 설정
    ImageAdapter imageAdapter;
    private LinearLayout selectionButtonsLayout;

    // 데이터베이스로부터 이미지 경로를 가져와서 imagePaths 리스트를 업데이트하는 메서드
    private void loadImagePathsFromDatabase() {
        // 데이터베이스에서 이미지 경로를 가져오는 코드 작성
        ArrayList<String> databasePaths = new ArrayList<>(dbHelper.getAllImagePaths());
        // 실제로 존재하는 이미지 경로만을 남기기 위해 확인
        ArrayList<String> validPaths = new ArrayList<>();
        for (String path : databasePaths) {
            File file = new File(path);
            if (file.exists()) {
                validPaths.add(path);
            } else {
                // 이미지가 존재하지 않는다면 데이터베이스에서도 해당 경로를 삭제
                dbHelper.deleteImagePath(path);
            }
        }
        // 최신화된 이미지 경로 리스트로 업데이트
        imagePaths.clear();
        imagePaths.addAll(validPaths);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        // onCreate() 메서드에서 초기화
        selectionButtonsLayout = findViewById(R.id.selection_buttons_layout);

        String filterType = getIntent().getStringExtra("FILTER_TYPE");
        gridView = findViewById(R.id.gridView);
        // DatabaseHelper 인스턴스 생성
        dbHelper = new DatabaseHelper(this);
        loadImagePathsFromDatabase();
        // 사진 경로 리스트를 가져오는 메서드 호출 즉, MediaStore에서 이미지의 경로를 가져와서 사진 경로 리스트에 저장
        //loadImagePaths();
        // 사진 어댑터 설정
        imageAdapter = new ImageAdapter(this, imagePaths);
        //ImageAdapter imageAdapter = new ImageAdapter(this, imagePaths);
        gridView.setAdapter(imageAdapter);
        // 데이터 변경 후 어댑터 갱신
        imageAdapter.notifyDataSetChanged();
        // 사진 클릭 이벤트 처리
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (isMultiSelect) {
                // 멀티 선택 모드에서는 아이템 클릭 시 선택 상태를 토글
                toggleItemSelected(position, view);
            }
            else{
                String imagePath = imagePaths.get(position);
                Log.d("abcd",""+imagePath);
                Toast.makeText(PhotoActivity.this, "클릭한 사진: " + imagePath, Toast.LENGTH_SHORT).show();
                // FullScreenImageActivity로 이동하여 이미지를 전체 화면으로 보여줍니다.
                Intent fullScreenIntent = new Intent(PhotoActivity.this, FullScreenImageActivity.class);
                fullScreenIntent.putExtra("imagePath", imagePath);
                fullScreenIntent.putExtra("FILTER_TYPE", filterType);
                startActivity(fullScreenIntent);
            }
        });
        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            // 멀티 선택 모드 활성화
            isMultiSelect = true;
            // 현재 길게 눌린 아이템을 선택 상태로 변경
            toggleItemSelected(position, view);
            return true;
        });
        // 사진 추가 버튼 설정
        ImageButton addPhotoButton = findViewById(R.id.button_add_photo);
        addPhotoButton.setOnClickListener(v -> {
            requestReadMediaImagesPermission();
        });
        // "삭제" 버튼 클릭 이벤트 처리
        ImageButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 선택된 항목들을 삭제하는 메서드 호출
                deleteSelectedPhotos();
                // 선택 모드 종료
                isMultiSelect = false;
                // UI 업데이트를 위한 메서드 호출
                updateUIAfterAction();
            }
        });

// "취소" 버튼 클릭 이벤트 처리
        ImageButton cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 선택된 항목들의 선택을 해제하고 UI 업데이트
                deselectAllItems();
            }
        });
    }

    private void requestReadMediaImagesPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우, 사용자에게 권한 요청 다이얼로그를 표시
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_CODE_READ_MEDIA_IMAGES);
        } else {
            // 이미 권한이 있는 경우, 앨범 열기 등의 작업 수행
            openAlbum();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 사용자가 권한을 허용한 경우, 앨범 열기 등의 작업 수행
                openAlbum();
            } else {
                // 사용자가 권한을 거부한 경우, 앨범 접근 불가 안내 등의 처리 수행
                Toast.makeText(this, "앨범에 접근할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAlbum() {
        // 외부 갤러리 앱을 호출하여 사진 선택
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void deleteFileFromStorage(String filePath) {
        File file = new File(filePath);
        if (file.exists()) { // 파일이 실제로 존재하는지 확인
            if (file.delete()) { // 파일 삭제 시도
                Log.d("Delete", "파일 삭제 성공: " + filePath);
            } else {
                Log.d("Delete", "파일 삭제 실패: " + filePath);
            }
        } else {
            Log.d("Delete", "파일이 존재하지 않습니다: " + filePath);
        }
    }

    private void deleteSelectedPhotos() {
        // selectedItems 리스트를 역순으로 정렬
        Collections.sort(selectedItems, Collections.reverseOrder());
        for (int position : selectedItems) {
            String filePath = imagePaths.get(position);
            // 실제 파일 시스템에서 파일 삭제
            deleteFileFromStorage(filePath);
            // 데이터베이스에서 해당 이미지 경로 삭제
            dbHelper.deleteImagePath(filePath);
            // 리스트에서 해당 항목 제거
            imagePaths.remove(position);
        }
        // 알림
        imageAdapter.notifyDataSetChanged(); // 데이터 변경을 어댑터에 알림
        // 선택된 항목 리스트 초기화
        selectedItems.clear();
    }

    // 선택 모드 종료 후 UI 업데이트
    private void updateUIAfterAction() {
        for (int i = 0; i < gridView.getChildCount(); i++) {
            View view = gridView.getChildAt(i);
            if (view != null) {
                view.setBackgroundColor(Color.TRANSPARENT); // 예시 색상
            }
        }
        // 선택된 항목 리스트 초기화
        selectedItems.clear();
        // 선택 모드 해제
        isMultiSelect = false;
        selectionButtonsLayout.setVisibility(View.GONE); // 선택 모드 종료 시 숨김
    }
    // 모든 선택 항목 해제 및 UI 업데이트 메서드
    private void deselectAllItems() {
        updateUIAfterAction();
    }

    // 아이템 선택 상태 토글 및 UI 업데이트를 처리하는 메소드
    private void toggleItemSelected(int position, View view) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(Integer.valueOf(position));
            // 아이템 선택 해제 UI 업데이트
            view.setBackgroundColor(Color.TRANSPARENT); // 예시
        } else {
            selectedItems.add(position);
            // 아이템 선택 UI 업데이트
            view.setBackgroundColor(Color.YELLOW); // 예시
        }
        // 선택된 항목이 있을 때 "사진 삭제" 및 "취소" 버튼이 들어있는 레이아웃의 가시성 변경
        if (isMultiSelect) {
            selectionButtonsLayout.setVisibility(View.VISIBLE); // 선택된 항목이 있을 때 표시
        } else {
            selectionButtonsLayout.setVisibility(View.GONE); // 선택된 항목이 없을 때 숨김
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // 갤러리에서 선택한 이미지의 경로를 가져오기
                String imagePath = getRealPathFromURI(selectedImageUri);
                if (imagePath != null && !imagePath.isEmpty()) {
                    // 이미지가 이미 존재하는지 확인
                    if (imagePaths.contains(imagePath)) {
                        // 이미지가 중복되는 경우 경고 메시지 표시
                        Toast.makeText(this, "이미 추가된 이미지입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        // 이미지 경로를 리스트에 추가하고 어댑터에 알림
                        imagePaths.add(0,imagePath);
                        ((ImageAdapter) gridView.getAdapter()).notifyDataSetChanged();
                        // 데이터베이스에 이미지 경로 추가
                        dbHelper.addImagePath(imagePath);
                    }
                } else {
                    Toast.makeText(this, "이미지 경로를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    // Uri로부터 실제 이미지 파일 경로를 가져오는 메서드
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
}
