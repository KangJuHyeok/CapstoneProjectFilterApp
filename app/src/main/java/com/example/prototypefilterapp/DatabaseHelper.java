package com.example.prototypefilterapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ImagePathsDB";
    private static final String TABLE_IMAGE_PATHS = "image_paths";

    private static final String KEY_ID = "id";
    private static final String KEY_PATH = "path";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE_PATHS);
        onCreate(db);
    }

    // 이미지 경로 추가
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

    // 이미지 경로 삭제
    public void deleteImagePath(String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_IMAGE_PATHS, KEY_PATH + " = ?", new String[]{path});
        db.close();
    }
}
