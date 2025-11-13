package com.example.prototypefilterapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "ImagePathsDB";
    private static final String TABLE_IMAGE_PATHS = "image_paths";

    private static final String KEY_ID = "id";
    private static final String KEY_PATH = "path";

    private static final String TABLE_STORIES = "stories";
    private static final String STORY_KEY_ID = "id";
    private static final String STORY_KEY_TITLE = "title";
    private static final String STORY_KEY_CONTENT = "content";
    private static final String STORY_KEY_DATE = "date";
    private static final String STORY_KEY_IMAGE_PATH = "image_path";
    private static final String STORY_KEY_FILTER_NAME = "filter_name";
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_IMAGE_PATHS_TABLE = "CREATE TABLE " + TABLE_IMAGE_PATHS +
                "(" +
                KEY_ID + " INTEGER PRIMARY KEY," +
                KEY_PATH + " TEXT" +
                ")";
        db.execSQL(CREATE_IMAGE_PATHS_TABLE);
        // 새로운 스토리 테이블 생성
        String CREATE_STORIES_TABLE = "CREATE TABLE " + TABLE_STORIES +
                "(" +
                STORY_KEY_ID + " INTEGER PRIMARY KEY," +
                STORY_KEY_TITLE + " TEXT," +
                STORY_KEY_CONTENT + " TEXT," +
                STORY_KEY_DATE + " TEXT," +
                STORY_KEY_IMAGE_PATH + " TEXT," +
                STORY_KEY_FILTER_NAME + " TEXT" +
                ")";
        db.execSQL(CREATE_STORIES_TABLE);
    }

    public Cursor getStoriesByDateRange(String startDate, String endDate) {
        // 쿼리: date 컬럼이 startDate와 endDate 사이에 있는 스토리를 선택
        // 날짜는 'yyyy-MM-dd HH:mm:ss' 형식으로 저장되므로 문자열 비교가 가능
        String selectQuery = "SELECT * FROM " + TABLE_STORIES +
                " WHERE " + STORY_KEY_DATE + " BETWEEN ? AND ?" +
                " ORDER BY " + STORY_KEY_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        // startDate는 'YYYY-MM-DD 00:00:00', endDate는 'YYYY-MM-DD 23:59:59' 형태
        return db.rawQuery(selectQuery, new String[]{startDate, endDate});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE_PATHS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STORIES);
        onCreate(db);
    }

    public void addImagePath(String path) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PATH, path);

        db.insert(TABLE_IMAGE_PATHS, null, values);
        db.close();
    }

    // 모든 이미지 경로 역순으로 가져오기
    public List<String> getAllImagePaths() {
        List<String> paths = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_IMAGE_PATHS + " ORDER BY id DESC"; // id 역순으로 정렬

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                paths.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return paths;
    }

    public void deleteImagePath(String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_IMAGE_PATHS, KEY_PATH + " = ?", new String[]{path});
        db.close();
    }

    // 스토리 저장 메서드
    public void addStory(String title, String content, String date, String imagePath, String filterName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STORY_KEY_TITLE, title);
        values.put(STORY_KEY_CONTENT, content);
        values.put(STORY_KEY_DATE, date);
        values.put(STORY_KEY_IMAGE_PATH, imagePath);
        values.put(STORY_KEY_FILTER_NAME, filterName);

        db.insert(TABLE_STORIES, null, values);
        db.close();
    }

    // 스토리 목록 불러오기 메서드 추가 (최신순으로)
    public Cursor getAllStoriesCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        // 최신 ID 순으로 정렬
        return db.query(TABLE_STORIES, null, null, null, null, null, STORY_KEY_ID + " DESC");
    }

    public Cursor getStoriesInDateRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        // SQL의 BETWEEN을 사용하여 두 날짜 사이의 데이터를 조회합니다.
        String selection = STORY_KEY_DATE + " BETWEEN ? AND ?";
        String[] selectionArgs = new String[] {startDate, endDate};

        // 최신 ID 순으로 정렬합니다.
        return db.query(
                TABLE_STORIES,
                null, // 모든 컬럼 선택
                selection,
                selectionArgs,
                null,
                null,
                STORY_KEY_ID + " DESC"
        );
    }

    public boolean deleteStory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_STORIES, STORY_KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected > 0;
    }
}
