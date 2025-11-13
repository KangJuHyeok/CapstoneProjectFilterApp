package com.example.prototypefilterapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
public class    MainActivity extends AppCompatActivity {
    public static int screenWidth ;
    public static int screenHeight;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        setContentView(R.layout.activity_main);
        Button filterSelectionButton = findViewById(R.id.button_filter_selection);
        filterSelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FilterSelectionActivity.class);
                startActivity(intent);
            }
        });
        Button storyButton = findViewById(R.id.button_story);
        storyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StoryFeedActivity.class);
                startActivity(intent);
            }
        });

        Button promptButton = findViewById(R.id.button_filter_store);
        promptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PromptImageActivity.class);
                startActivity(intent);
            }
        });
    }
}
