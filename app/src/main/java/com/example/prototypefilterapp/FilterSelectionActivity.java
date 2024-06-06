package com.example.prototypefilterapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FilterSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFilters;
    private ImageButton goToAlbumButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_selection);

        recyclerViewFilters = findViewById(R.id.recyclerViewFilters);
        goToAlbumButton = findViewById(R.id.button_go_to_album);

        recyclerViewFilters.setLayoutManager(new LinearLayoutManager(this));

        FilterAdapter filterAdapter = new FilterAdapter(getFilterList());
        recyclerViewFilters.setAdapter(filterAdapter);

        goToAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FilterSelectionActivity.this, PhotoActivity.class);
                startActivity(intent);
            }
        });
    }

    private List<FilterItem> getFilterList() {
        List<FilterItem> filterList = new ArrayList<>();
        filterList.add(new FilterItem("따뜻한 아날로그", "따스하고 어두운 아날로그 느낌의 필터","#unpack @blend ol grainFilm5.jpg 95 @adjust saturation 0.80 @adjust brightness -0.1 @adjust contrast 0.80 @adjust whitebalance 0.40 1"));
        filterList.add(new FilterItem("옛날 디카", "날카롭고 선명한 옛날 디지털 카메라 느낌의 필터","@adjust sharpen 5 @adjust contrast 1.2 @adjust saturation 1.4"));
        filterList.add(new FilterItem("석양의 아날로그", "석양빛이 매우 강한 아날로그 느낌의 필터","#unpack @blend ol grainFilm.jpg 70 @adjust lut late_sunset.png @adjust saturation 1.1 @adjust brightness 0.2 @adjust contrast 1.1 @adjust whitebalance 0.1 1"));
        filterList.add(new FilterItem("필름카메라 빛샘", "필름에 빛이 들어간 느낌의 필터","#unpack @blend ol grainFilm6.jpg 90 @adjust saturation 1.1  @adjust contrast 0.9"));
        filterList.add(new FilterItem("한 줄기 빛샘", "빛샘 효과가 한 줄기만 들어간 필터","#unpack @blend ol grainFilm4.jpg 70 @adjust saturation 1.0 @adjust brightness 0.2 @adjust contrast 1.20"));
        filterList.add(new FilterItem("은은한 무지개 빛", "은은한 무지개 빛이 비치는 필터","#unpack @blend ol hehe.jpg 50 @adjust saturation 0.90 @adjust brightness 0.1 @adjust contrast 0.90 @adjust whitebalance 0.30 1"));
        filterList.add(new FilterItem("어두운 안개", "짙은 회색 빛의 안개가 낀 느낌의 필터","@adjust lut foggy_night.png @adjust brightness 0.3 @adjust contrast 0.80 @adjust whitebalance 0.20 1"));
        filterList.add(new FilterItem("상쾌한 빛", "기존의 노란빛이 좀 더 하얗게 변함","@adjust lut wildbird.png @adjust saturation 1.2 @adjust brightness 0.2"));
        filterList.add(new FilterItem("밝고 선명", "기존의 빛들이 많이 밝아지고 선명해짐","@adjust lut filmstock.png @adjust sharpen 2 @adjust contrast 1.2"));
        filterList.add(new FilterItem("상쾌한 빛", "기존의 노란빛이 좀더 하얗게 변함","@adjust lut wildbird.png @adjust saturation 1.2 @adjust brightness 0.2"));
        return filterList;
    }

    private void showFilterOptionsDialog(final String filterType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("필터 선택");
        builder.setItems(new CharSequence[]{"기존 사진에 적용", "실시간 촬영"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                switch (which) {
                    case 0:
                        intent = new Intent(FilterSelectionActivity.this, PhotoActivity.class);
                        intent.putExtra("FILTER_TYPE", filterType);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(FilterSelectionActivity.this, CameraActivity.class);
                        intent.putExtra("FILTER_TYPE", filterType);
                        startActivity(intent);
                        break;
                }
            }
        });
        builder.show();
    }

    private static class FilterItem {
        String filterName;
        String filterDescription;
        String filterType;

        FilterItem(String filterName, String filterDescription,String filterType) {
            this.filterName = filterName;
            this.filterDescription = filterDescription;
            this.filterType = filterType;
        }
    }

    private class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {
        private List<FilterItem> filterList;

        FilterAdapter(List<FilterItem> filterList) {
            this.filterList = filterList;
        }

        @NonNull
        @Override
        public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter, parent, false);
            return new FilterViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
            final FilterItem filterItem = filterList.get(position);
            holder.filterNameTextView.setText(filterItem.filterName);
            holder.filterDescriptionTextView.setText(filterItem.filterDescription);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFilterOptionsDialog(filterItem.filterType);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filterList.size();
        }

        class FilterViewHolder extends RecyclerView.ViewHolder {
            TextView filterNameTextView;
            TextView filterDescriptionTextView;

            FilterViewHolder(@NonNull View itemView) {
                super(itemView);
                filterNameTextView = itemView.findViewById(R.id.textViewFilterName);
                filterDescriptionTextView = itemView.findViewById(R.id.textViewFilterDescription);
            }
        }
    }
}
