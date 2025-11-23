package com.example.smartroom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class AirQualityActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AirQualityAdapter adapter;
    private ProgressBar progressBar;
    private TextView txtError;
    private Button btnRefresh;

    private AirQualityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality);

        recyclerView = findViewById(R.id.recyclerAirQuality);
        progressBar = findViewById(R.id.progressBar);
        txtError = findViewById(R.id.txtError);
        btnRefresh = findViewById(R.id.btnRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AirQualityAdapter();
        recyclerView.setAdapter(adapter);

        // Get ViewModel
        viewModel = new ViewModelProvider(this).get(AirQualityViewModel.class);

        // Observe LiveData
        viewModel.getAirQualityItems().observe(this, items -> {
            updateList(items);
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (loading != null && loading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                txtError.setVisibility(View.VISIBLE);
                txtError.setText(error);
            } else {
                txtError.setVisibility(View.GONE);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.loadAirQuality();
            }
        });

        // Load once when opening (if list is empty)
        if (viewModel.getAirQualityItems().getValue() == null ||
                viewModel.getAirQualityItems().getValue().isEmpty()) {
            viewModel.loadAirQuality();
        }
    }

    private void updateList(List<AirQualityItem> items) {
        adapter.setItems(items);
    }
}