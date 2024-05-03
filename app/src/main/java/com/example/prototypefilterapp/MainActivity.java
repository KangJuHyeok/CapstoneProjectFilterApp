package com.example.prototypefilterapp;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.wysaid.common.Common;
import org.wysaid.myUtils.ImageUtil;
import org.wysaid.nativePort.CGENativeLibrary;

//사진 반드시 6장 선택
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_IMAGES = 123;
    private Bitmap[] srcImages = new Bitmap[6]; // 선택한 사진들을 저장할 배열
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), srcImages.length); // 최대 6장까지만 선택
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    try {
                        srcImages[i] = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 이미지를 업로드하고 처리할 수 있는 곳
                CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback() {

                    //Notice: the 'name' passed in is just what you write in the rule, e.g: 1.jpg
                    @Override
                    public Bitmap loadImage(String name, Object arg) {

                        Log.i(Common.LOG_TAG, "Loading file: " + name);
                        AssetManager am = getAssets();
                        InputStream is;
                        try {
                            is = am.open(name);
                        } catch (IOException e) {
                            Log.e(Common.LOG_TAG, "Can not open file " + name);
                            return null;
                        }

                        return BitmapFactory.decodeStream(is);
                    }

                    @Override
                    public void loadImageOK(Bitmap bmp, Object arg) {
                        Log.i(Common.LOG_TAG, "Loading bitmap over, you can choose to recycle or cache");

                        //The bitmap is which you returned at 'loadImage'.
                        //You can call recycle when this function is called, or just keep it for further usage.
                        bmp.recycle();
                    }
                };

                CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);

                // Load the source image from resources (or any other source)
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3; // 이미지를 원래 크기의 1/3 해상도로 축소합니다.

                // Load the source images from resources
                //Bitmap[] srcImages = new Bitmap[6];
/*
                srcImages[0] = BitmapFactory.decodeResource(getResources(), R.drawable.source_image, options);
                srcImages[1] = BitmapFactory.decodeResource(getResources(), R.drawable.source_image2, options);
                srcImages[2] = BitmapFactory.decodeResource(getResources(), R.drawable.source_image3, options);
                srcImages[3] = BitmapFactory.decodeResource(getResources(), R.drawable.source_image4, options);
                srcImages[4] = BitmapFactory.decodeResource(getResources(), R.drawable.source_image5, options);
                options.inSampleSize=1;
                srcImages[5] = BitmapFactory.decodeResource(getResources(), R.drawable.source_image6, options);
*/

                //아날로그->1번 사진 : "#unpack @blend ol grainFilm5.jpg 95 @adjust saturation 0.80 @adjust brightness -0.1 @adjust contrast 0.80 @adjust whitebalance 0.40 1";
                //옛날 디지털 카메라 -> 4번 사진 : "@adjust sharpen 5 @adjust contrast 1.2 @adjust saturation 1.4"
                //석양 아날로그 ->6번 사진 : #unpack @blend ol grainFilm.jpg 70 @adjust lut late_sunset.png @adjust saturation 1.1 @adjust brightness 0.2 @adjust contrast 1.1 @adjust whitebalance 0.1 1
                String[] ruleString= {"#unpack @blend ol grainFilm5.jpg 50 @adjust saturation 0.80 @adjust brightness -0.1 @adjust contrast 0.80 @adjust whitebalance 0.40 1",
                        "#unpack @blend ol grainFilm5.jpg 50 @adjust saturation 0.80 @adjust brightness -0.1 @adjust contrast 0.80 @adjust whitebalance 0.40 1",
                        "@adjust sharpen 5 @adjust contrast 1.2 @adjust saturation 1.4",
                        "@adjust sharpen 5 @adjust contrast 1.2 @adjust saturation 1.4",
                        "#unpack @blend ol grainFilm.jpg 70 @adjust lut late_sunset.png @adjust saturation 1.1 @adjust brightness 0.2 @adjust contrast 1.1 @adjust whitebalance 0.1 1",
                        "#unpack @blend ol grainFilm.jpg 70 @adjust lut late_sunset.png @adjust saturation 1.1 @adjust brightness 0.2 @adjust contrast 1.1 @adjust whitebalance 0.1 1"
                };



                // Apply the filter to the source images
                Bitmap[] dstImages = new Bitmap[srcImages.length];
                for (int i = 0; i < srcImages.length; i++) {
                    dstImages[i] = CGENativeLibrary.filterImage_MultipleEffects(srcImages[i], ruleString[i], 1.0f);
                }

                makeText(dstImages);
                // 현재 날짜를 받아오기
        /*Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("'dd MM yy'");
        String date = dateFormat.format(calendar.getTime());*/

                ImageView[] imageView = new ImageView[dstImages.length];
                int[] imageViewIds = {R.id.imageView, R.id.imageView2, R.id.imageView3, R.id.imageView4, R.id.imageView5, R.id.imageView6}; // 이미지 뷰들의 id 배열

                // Display the result images in ImageViews
                for(int i=0;i< imageView.length;i++){
                    imageView[i] = findViewById(imageViewIds[i]);
                    imageView[i].setImageBitmap(dstImages[i]);
                }

        /*
        for (int i = 0; i < dstImages.length; i++) {
            String fileName = "image_" + i + ".png"; // 파일 이름 설정
            saveImageToInternalStorage(dstImages[i], fileName); // 이미지를 내부 저장소에 저장
        }
        */
            } else {
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button uploadImageButton = findViewById(R.id.uploadImageButton);
        uploadImageButton.setOnClickListener(v -> {
            pickImages(); // 이미지 선택 메서드를 호출합니다.
            uploadImageButton.setVisibility(View.GONE); // 버튼을 숨깁니다.
        });
    }
    void makeText(Bitmap[] dstImages){
        String date="'24 05 03";
        Canvas[] cs = new Canvas[dstImages.length];
        Paint[] tPaint = new Paint[dstImages.length];
        float[] textSize = new float[dstImages.length];

        float textSizeRatio = 0.03f; // 이미지 너비의 10%
        Typeface typeface = ResourcesCompat.getFont(this, R.font.digibfont);

        for(int i = 0; i<dstImages.length; i++){
            cs[i] = new Canvas(dstImages[i]);
            tPaint[i] = new Paint();
            textSize[i]= dstImages[i].getWidth() * textSizeRatio;
            tPaint[i].setTextSize(textSize[i]);
            tPaint[i].setColor(Color.parseColor("#FFA500"));
            tPaint[i].setStyle(Paint.Style.FILL);

            // Typeface 설정
            tPaint[i].setTypeface(typeface);
            // 자간 설정
            tPaint[i].setLetterSpacing(0.2f);

            // 번져지는 효과를 위해 그림자 효과 설정
            for (int j = 1; j <= 3; j++) {
                float shadowRadius = 0.1f * j * textSize[i]; // 그림자 반경을 이미지 너비의 비율에 맞게 조정
                float dx = 0.05f * j * textSize[i]; // 그림자 x축 이동 거리를 이미지 너비의 비율에 맞게 조정
                float dy = 0.05f * j * textSize[i]; // 그림자 y축 이동 거리를 이미지 높이의 비율에 맞게 조정
                tPaint[i].setShadowLayer(shadowRadius, dx, dy, Color.parseColor("#FFA500")); // 그림자 효과 설정
                cs[i].drawText(date, dstImages[i].getWidth()*0.95f - tPaint[i].measureText(date), dstImages[i].getHeight()*0.95f, tPaint[i]);
            }
        }
    }


    // 이미지를 내부 저장소에 저장하는 메서드
    private void saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 갤러리에서 여러 사진 선택
    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_PICK_IMAGES);
    }
}
