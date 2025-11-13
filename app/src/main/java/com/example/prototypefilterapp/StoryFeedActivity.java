package com.example.prototypefilterapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.widget.Button;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StoryFeedActivity extends AppCompatActivity implements StoryAdapter.StoryDeleteListener {

    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;
    private StoryAdapter adapter;
    private FloatingActionButton fabFilterDate;
    private Calendar startDateFilter;
    private Calendar endDateFilter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_feed);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewStories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabAdd = findViewById(R.id.fabAddStory);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(StoryFeedActivity.this, DiaryWriteActivity.class);
            startActivity(intent);
        });

        fabFilterDate = findViewById(R.id.fabFilterDate);

        fabFilterDate.setOnClickListener(v -> showDateRangePickerDialog());

        adapter = new StoryAdapter(this, null, this);
        recyclerView.setAdapter(adapter);

        loadAllStories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 날짜 필터링 상태를 유지한 채 목록 갱신
        if (startDateFilter != null && endDateFilter != null) {
            applyDateFilter(dateFormat.format(startDateFilter.getTime()), dateFormat.format(endDateFilter.getTime()));
        } else {
            loadAllStories();
        }
    }

    // --- 스토리 로드 및 필터링 로직 ---

    private void loadStories(Cursor cursor) {
        if (adapter != null) {
            adapter.swapCursor(cursor);
        }
        if (cursor.getCount() == 0 && (startDateFilter != null || endDateFilter != null)) {
            Toast.makeText(this, "선택한 기간에 작성된 글이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAllStories() {
        startDateFilter = null;
        endDateFilter = null;
        Cursor cursor = dbHelper.getAllStoriesCursor();
        loadStories(cursor);
        Toast.makeText(this, "전체 기간의 글을 표시합니다.", Toast.LENGTH_SHORT).show();
    }

    private void applyDateFilter(String startDate, String endDate) {
        // 날짜 형식은 YYYY-MM-DD HH:mm:ss 형태가 데이터베이스와 일치하도록 조정
        Cursor cursor = dbHelper.getStoriesByDateRange(startDate + " 00:00:00", endDate + " 23:59:59");
        loadStories(cursor);
    }

    @Override
    public void onDeleteStory(long id) {
        if (dbHelper.deleteStory(id)) {
            Toast.makeText(this, "스토리가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            if (startDateFilter != null && endDateFilter != null) {
                applyDateFilter(dateFormat.format(startDateFilter.getTime()), dateFormat.format(endDateFilter.getTime()));
            } else {
                loadAllStories();
            }
        } else {
            Toast.makeText(this, "스토리 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDateRangePickerDialog() {
        // 날짜 선택 다이얼로그를 보여주는 로직 구현
        new AlertDialog.Builder(this)
                .setTitle("기간 검색")
                .setItems(new CharSequence[]{"기간 설정", "기간 초기화 (전체 보기)"}, (dialog, which) -> {
                    if (which == 0) {
                        showStartDatePicker();
                    } else {
                        // 기간 초기화 버튼
                        loadAllStories();
                    }
                }).show();
    }

    private void showStartDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    startDateFilter = Calendar.getInstance();
                    startDateFilter.set(y, m, d);
                    showEndDatePickerDialog(); // 시작 날짜 선택 후 종료 날짜 선택 다이얼로그 호출
                }, year, month, day);
        datePickerDialog.setTitle("시작 날짜 선택");
        datePickerDialog.show();
    }

    private void showEndDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    endDateFilter = Calendar.getInstance();
                    endDateFilter.set(y, m, d);

                    // 시작 날짜가 종료 날짜보다 늦은지 확인
                    if (startDateFilter.after(endDateFilter)) {
                        Toast.makeText(this, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.", Toast.LENGTH_LONG).show();
                    } else {
                        // 필터 적용
                        String start = dateFormat.format(startDateFilter.getTime());
                        String end = dateFormat.format(endDateFilter.getTime());
                        applyDateFilter(start, end);

                        Toast.makeText(this, start + " 부터 " + end + " 까지의 글을 표시합니다.", Toast.LENGTH_LONG).show();
                    }
                }, year, month, day);

        datePickerDialog.setTitle("종료 날짜 선택");
        // 사용자가 시작 날짜보다 이전 날짜를 선택하지 못하도록 최소 날짜 설정
        if (startDateFilter != null) {
            datePickerDialog.getDatePicker().setMinDate(startDateFilter.getTimeInMillis());
        }
        datePickerDialog.show();
    }
}