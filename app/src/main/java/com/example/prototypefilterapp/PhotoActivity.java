package com.example.prototypefilterapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class PhotoActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 2;
    private GridView gridView;
    private DatabaseHelper dbHelper;
    static ArrayList<String> imagePaths = new ArrayList<>();;
    boolean isMultiSelect = false;
    private boolean isCustomFilterMode = false;
    private boolean isGalleryMode = false;
    private boolean isSelectionMode = false;
    private String currentFilterType;
    private static final int REQUEST_CODE_READ_MEDIA_IMAGES = 102;
    ArrayList<Integer> selectedItems = new ArrayList<>();
    ImageAdapter imageAdapter;
    private LinearLayout selectionButtonsLayout;

    private void loadImagePathsFromDatabase() {
        ArrayList<String> databasePaths = new ArrayList<>(dbHelper.getAllImagePaths());
        ArrayList<String> validPaths = new ArrayList<>();
        for (String path : databasePaths) {
            File file = new File(path);
            if (file.exists()) {
                validPaths.add(path);
            } else {
                dbHelper.deleteImagePath(path);
            }
        }
        imagePaths.clear();
        imagePaths.addAll(validPaths);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        isCustomFilterMode = getIntent().getBooleanExtra("IS_CUSTOM_FILTER_MODE", false);
        isGalleryMode = getIntent().getBooleanExtra("IS_GALLERY_MODE", false);
        currentFilterType = getIntent().getStringExtra("FILTER_TYPE");
        isSelectionMode = getIntent().getBooleanExtra("IS_SELECTION_MODE", false);

        selectionButtonsLayout = findViewById(R.id.selection_buttons_layout);
        gridView = findViewById(R.id.gridView);
        dbHelper = new DatabaseHelper(this);
        loadImagePathsFromDatabase();
        imageAdapter = new ImageAdapter(this, imagePaths);
        gridView.setAdapter(imageAdapter);
        imageAdapter.notifyDataSetChanged();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (isMultiSelect) {
                toggleItemSelected(position, view);
            } else {
                String imagePath = imagePaths.get(position);
                if (isSelectionMode) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selectedImagePath", imagePath);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
                if (isCustomFilterMode) {
                    Intent customFilterIntent = new Intent(PhotoActivity.this, CustomFilterActivity.class);
                    customFilterIntent.putExtra("imagePath", imagePath);
                    startActivity(customFilterIntent);
                    finish();
                } else if (isGalleryMode) {
                    Intent fullScreenIntent = new Intent(PhotoActivity.this, FullScreenImageActivity.class);
                    fullScreenIntent.putExtra("imagePath", imagePath);
                    startActivity(fullScreenIntent);
                } else if (currentFilterType != null && !currentFilterType.isEmpty()) {
                    showFilterConfirmationDialog(imagePath, currentFilterType);
                }
            }
        });
        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            isMultiSelect = true;
            toggleItemSelected(position, view);
            return true;
        });

        ImageButton addPhotoButton = findViewById(R.id.button_add_photo);
        addPhotoButton.setOnClickListener(v -> {
            requestReadMediaImagesPermission();
        });

        ImageButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedPhotos();
                isMultiSelect = false;
                updateUIAfterAction();
            }
        });

        ImageButton cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deselectAllItems();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImagePathsFromDatabase();
        if (imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
        }
    }

    private void showFilterConfirmationDialog(final String imagePath, final String filterType) {
        FilterData.FilterItem filterItem = FilterData.getInstance().getFilterItemByType(filterType);
        final String filterName = (filterItem != null) ? filterItem.filterName : filterType;
        new AlertDialog.Builder(this)
                .setTitle("필터 적용 확인")
                .setMessage("선택한 필터를 이 사진에 적용하시겠습니까? (필터 : " + filterName + ")")
                .setNegativeButton("적용하기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent fullScreenIntent = new Intent(PhotoActivity.this, FullScreenImageActivity.class);
                        fullScreenIntent.putExtra("imagePath", imagePath);
                        fullScreenIntent.putExtra("FILTER_TYPE", filterType);
                        startActivity(fullScreenIntent);
                    }
                })
                .setPositiveButton("취소", (dialog, which) -> dialog.dismiss()) // 💡 NegativeButton 자리에 위치
                .show();
    }

    private void requestReadMediaImagesPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_CODE_READ_MEDIA_IMAGES);
        } else {
            openAlbum();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum();
            } else {
                Toast.makeText(this, "앨범에 접근할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void deleteFileFromStorage(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                Log.d("Delete", "파일 삭제 성공: " + filePath);
            } else {
                Log.d("Delete", "파일 삭제 실패: " + filePath);
            }
        } else {
            Log.d("Delete", "파일이 존재하지 않습니다: " + filePath);
        }
    }

    private void deleteSelectedPhotos() {
        Collections.sort(selectedItems, Collections.reverseOrder());
        for (int position : selectedItems) {
            String filePath = imagePaths.get(position);
            deleteFileFromStorage(filePath);
            dbHelper.deleteImagePath(filePath);
            imagePaths.remove(position);
        }
        imageAdapter.notifyDataSetChanged();
        selectedItems.clear();
    }

    private void updateUIAfterAction() {
        for (int i = 0; i < gridView.getChildCount(); i++) {
            View view = gridView.getChildAt(i);
            if (view != null) {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }
        selectedItems.clear();
        isMultiSelect = false;
        selectionButtonsLayout.setVisibility(View.GONE);
    }

    private void deselectAllItems() {
        updateUIAfterAction();
    }

    private void toggleItemSelected(int position, View view) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(Integer.valueOf(position));
            view.setBackgroundColor(Color.TRANSPARENT);
        } else {
            selectedItems.add(position);
            view.setBackgroundColor(Color.YELLOW);
        }
        if (isMultiSelect) {
            selectionButtonsLayout.setVisibility(View.VISIBLE);
        } else {
            selectionButtonsLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                String imagePath = getRealPathFromURI(selectedImageUri);
                if (imagePath != null && !imagePath.isEmpty()) {
                    if (imagePaths.contains(imagePath)) {
                        Toast.makeText(this, "이미 추가된 이미지입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        imagePaths.add(0,imagePath);
                        ((ImageAdapter) gridView.getAdapter()).notifyDataSetChanged();
                        dbHelper.addImagePath(imagePath);
                    }
                } else {
                    Toast.makeText(this, "이미지 경로를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
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
}