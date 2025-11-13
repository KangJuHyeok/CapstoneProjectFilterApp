package com.example.prototypefilterapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FilterSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFilters;
    private ImageButton goToAlbumButton;
    private ImageButton createCustomFilterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_selection);

        recyclerViewFilters = findViewById(R.id.recyclerViewFilters);
        goToAlbumButton = findViewById(R.id.button_go_to_album);
        createCustomFilterButton = findViewById(R.id.button_create_custom_filter);

        recyclerViewFilters.setLayoutManager(new LinearLayoutManager(this));

        FilterAdapter filterAdapter = new FilterAdapter(FilterData.getInstance().getFilterList());
        recyclerViewFilters.setAdapter(filterAdapter);

        goToAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FilterSelectionActivity.this, PhotoActivity.class);
                intent.putExtra("IS_GALLERY_MODE", true);
                startActivity(intent);
            }
        });

        createCustomFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FilterSelectionActivity.this, PhotoActivity.class);
                intent.putExtra("IS_CUSTOM_FILTER_MODE", true);
                startActivity(intent);
            }
        });
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
        String filterTags;

        FilterItem(String filterName, String filterDescription,String filterType) {
            this.filterName = filterName;
            this.filterDescription = filterDescription;
            this.filterType = filterType;
            this.filterTags = filterTags;
        }
    }
    private class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {
        private List<FilterData.FilterItem> filterList;

        FilterAdapter(List<FilterData.FilterItem> filterList) {
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
            final FilterData.FilterItem filterItem = filterList.get(position);

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
