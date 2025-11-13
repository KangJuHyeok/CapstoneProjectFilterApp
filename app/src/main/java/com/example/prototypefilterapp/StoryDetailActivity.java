package com.example.prototypefilterapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class StoryDetailActivity extends AppCompatActivity {

    private long storyId;
    private String imagePath;
    private String filterName;
    private String filterType;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        storyId = intent.getLongExtra("STORY_ID", -1);
        String title = intent.getStringExtra("TITLE");
        String content = intent.getStringExtra("CONTENT");
        String date = intent.getStringExtra("DATE");
        imagePath = intent.getStringExtra("IMAGE_PATH");
        filterName = intent.getStringExtra("FILTER_NAME");

        FilterData.FilterItem item = FilterData.getInstance().getFilterItemByName(filterName);
        if (item != null) {
            filterType = item.filterType;
        }

        TextView tvDate = findViewById(R.id.tvDetailDate);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvContent = findViewById(R.id.tvDetailContent);
        TextView tvFilterName = findViewById(R.id.tvDetailFilterName);
        ImageView ivPhoto = findViewById(R.id.ivDetailPhoto);
        ImageButton deleteButton = findViewById(R.id.btnDeleteStory);
        Button applyFilterButton = findViewById(R.id.btnApplyFilter);

        tvDate.setText(date);
        tvTitle.setText(title);
        tvContent.setText(content);
        tvFilterName.setText("AI 추천 필터: " + filterName);


        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(this).load(imagePath).into(ivPhoto);
            ivPhoto.setVisibility(View.VISIBLE);
            final String finalImagePath = imagePath;
            final String finalFilterType = filterType; // 필터 타입 (로컬 필터 적용용)

            ivPhoto.setOnClickListener(v -> {
                Intent fullScreenIntent = new Intent(StoryDetailActivity.this, FullScreenImageActivity.class);
                fullScreenIntent.putExtra("imagePath", finalImagePath);
                fullScreenIntent.putExtra("StoryDetailCase", true);
                startActivity(fullScreenIntent);
            });
        } else {
            ivPhoto.setVisibility(View.GONE);
        }
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        applyFilterButton.setOnClickListener(v -> applyRecommendedFilter());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("스토리 삭제")
                .setMessage("이 스토리를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
                .setNegativeButton("예", (dialog, which) -> {
                    if (dbHelper.deleteStory(storyId)) {
                        Toast.makeText(this, "스토리가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        finish(); // 현재 상세 화면을 닫고 이전 피드 화면으로 돌아갑니다.
                    } else {
                        Toast.makeText(this, "스토리 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("아니오", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void applyRecommendedFilter() {
        if (filterType == null || filterType.isEmpty()) {
            Toast.makeText(this, "추천 필터 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "추천 필터가 선택되었습니다. 적용할 사진을 앨범에서 선택해주세요.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("FILTER_TYPE", filterType);
        startActivity(intent);

    }
}