package com.example.prototypefilterapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.wysaid.nativePort.CGENativeLibrary;


public class CustomFilterActivity extends AppCompatActivity {

    private ImageView previewImageView;
    private SeekBar seekbarBrightness, seekbarContrast, seekbarSaturation;
    private Button applyButton;
    private Bitmap originalBitmap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_filter);

        previewImageView = findViewById(R.id.preview_image_view);
        seekbarBrightness = findViewById(R.id.seekbar_brightness);
        seekbarContrast = findViewById(R.id.seekbar_contrast);
        seekbarSaturation = findViewById(R.id.seekbar_saturation);
        applyButton = findViewById(R.id.button_apply_custom_filter);

        CGENativeLibrary.setLoadImageCallback(new CGENativeLibrary.LoadImageCallback() {
            @Override
            public Bitmap loadImage(String name, Object arg) {
                // 이 부분은 assets 폴더에서 LUT 파일을 로드하기 위해 필요-> 나중에 필터 만들 때 lut파일을 본인이 가져와서 필터 만들 수 있게 구현 가능
                return null;
            }
            @Override
            public void loadImageOK(Bitmap bmp, Object arg) {
                if (bmp != null) {
                    bmp.recycle();
                }
            }
        }, null);

        String imagePath = getIntent().getStringExtra("imagePath");

        if (imagePath != null) {
            originalBitmap = BitmapFactory.decodeFile(imagePath);

            if (originalBitmap != null) {
                previewImageView.setImageBitmap(originalBitmap);
            } else {
                Toast.makeText(this, "선택한 이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "이미지 경로가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    applyFilter();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekbarBrightness.setOnSeekBarChangeListener(seekBarChangeListener);
        seekbarContrast.setOnSeekBarChangeListener(seekBarChangeListener);
        seekbarSaturation.setOnSeekBarChangeListener(seekBarChangeListener);

        applyButton.setOnClickListener(v -> {
            if (originalBitmap == null) {
                Toast.makeText(CustomFilterActivity.this, "이미지가 로드되지 않았습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            String customFilterType = buildFilterString();

            Intent intent = new Intent(CustomFilterActivity.this, FullScreenImageActivity.class);

            intent.putExtra("imagePath", imagePath);
            intent.putExtra("FILTER_TYPE", customFilterType);

            startActivity(intent);
        });
    }

    private void applyFilter() {
        if (originalBitmap == null) {
            return;
        }

        // UI 스레드를 차단하지 않도록 별도의 스레드에서 필터링을 수행
        new Thread(new Runnable() {
            @Override
            public void run() {
                String filterString = buildFilterString();

                // 필터링 작업은 백그라운드 스레드에서 실행
                Bitmap filteredBitmap = CGENativeLibrary.filterImage_MultipleEffects(originalBitmap, filterString, 1.0f);

                // 필터링이 완료되면 UI 업데이트는 메인 스레드에서 처리
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        previewImageView.setImageBitmap(filteredBitmap);
                    }
                });
            }
        }).start();
    }

    private String buildFilterString() {
        float brightness = (seekbarBrightness.getProgress() - 50) / 50.0f; // -1.0 ~ 1.0
        float contrast = seekbarContrast.getProgress() / 50.0f; // 0.0 ~ 2.0
        float saturation = seekbarSaturation.getProgress() / 50.0f; // 0.0 ~ 2.0

        return String.format(
                "@adjust brightness %.2f @adjust contrast %.2f @adjust saturation %.2f",
                brightness, contrast, saturation
        );
    }
}