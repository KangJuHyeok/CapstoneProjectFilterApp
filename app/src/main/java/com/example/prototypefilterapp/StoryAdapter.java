package com.example.prototypefilterapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {
    private Context context;
    private Cursor cursor;
    private StoryDeleteListener deleteListener;

    public StoryAdapter(Context context, Cursor cursor,StoryDeleteListener listener) {
        this.context = context;
        this.cursor = cursor;
        this.deleteListener = listener;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        if (newCursor != null) {
            this.notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        final long storyId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        final String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
        final String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        final String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
        final String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
        final String filterName = cursor.getString(cursor.getColumnIndexOrThrow("filter_name"));

        FilterData.FilterItem item = FilterData.getInstance().getFilterItemByName(filterName);

        holder.tvDate.setText(date);
        holder.tvTitle.setText(title);
        holder.tvFilter.setText("추천 필터: " + filterName);

        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(context).load(imagePath).into(holder.ivPhoto);
            holder.ivPhoto.setVisibility(View.VISIBLE);
        } else {
            holder.ivPhoto.setVisibility(View.GONE);
        }

        // 스토리 클릭 이벤트 처리 (StoryDetailActivity로 이동)
        holder.itemView.setOnClickListener(v -> {
            Intent detailIntent = new Intent(context, StoryDetailActivity.class);

            // 상세 화면에 필요한 모든 정보를 전달
            detailIntent.putExtra("STORY_ID", storyId);
            detailIntent.putExtra("TITLE", title);
            detailIntent.putExtra("CONTENT", content);
            detailIntent.putExtra("DATE", date);
            detailIntent.putExtra("IMAGE_PATH", imagePath);
            detailIntent.putExtra("FILTER_NAME", filterName);

            context.startActivity(detailIntent);
        });
    }

    @Override
    public int getItemCount() {
        return (cursor == null) ? 0 : cursor.getCount();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDate, tvTitle, tvFilter;
        public ImageView ivPhoto;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvStoryDate);
            tvTitle = itemView.findViewById(R.id.tvStoryTitle);
            tvFilter = itemView.findViewById(R.id.tvStoryFilter);
            ivPhoto = itemView.findViewById(R.id.ivStoryPhoto);
        }
    }
    public interface StoryDeleteListener {
        void onDeleteStory(long id);
    }
}