package com.example.prototypefilterapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> imagePaths;

    public ImageAdapter(Context context, ArrayList<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public Object getItem(int position) {
        return imagePaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams((int)(0.3*MainActivity.screenWidth),(int)(0.3*MainActivity.screenWidth))); // 이미지 크기 조절
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(12, 12, 12, 12);
        } else {
            imageView = (ImageView) convertView;
        }

        Glide.with(context)
                .load(imagePaths.get(position))
                .into(imageView);

        return imageView;
    }

}
