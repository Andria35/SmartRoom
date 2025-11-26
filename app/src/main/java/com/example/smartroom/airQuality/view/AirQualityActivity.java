package com.example.smartroom.airQuality.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.smartroom.helpers.AccessibilityPrefs;
import com.example.smartroom.airQuality.model.AirQualityItem;
import com.example.smartroom.airQuality.viewModel.AirQualityViewModel;
import com.example.smartroom.R;

import java.util.List;

public class AirQualityActivity extends AppCompatActivity {

    private ViewGroup root;
    private TextView txtTitle;
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

        // ---- Find views ----
        root = findViewById(R.id.airQualityRoot);
        txtTitle = findViewById(R.id.txtTitle);
        recyclerView = findViewById(R.id.recyclerAirQuality);
        progressBar = findViewById(R.id.progressBar);
        txtError = findViewById(R.id.txtError);
        btnRefresh = findViewById(R.id.btnRefresh);

        // ---- RecyclerView ----
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AirQualityAdapter();
        recyclerView.setAdapter(adapter);

        // ---- ViewModel ----
        viewModel = new ViewModelProvider(this).get(AirQualityViewModel.class);

        // List of items
        viewModel.getAirQualityItems().observe(this, this::updateList);

        // Loading
        viewModel.getIsLoading().observe(this, loading -> {
            if (loading != null && loading) {
                progressBar.setVisibility(ProgressBar.VISIBLE);
            } else {
                progressBar.setVisibility(ProgressBar.GONE);
            }
        });

        // Error
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                txtError.setVisibility(TextView.VISIBLE);
                txtError.setText(error);
            } else {
                txtError.setVisibility(TextView.GONE);
            }
        });

        // Refresh button
        btnRefresh.setOnClickListener(v -> viewModel.loadAirQuality());

        // Load once when opening (if list is empty)
        if (viewModel.getAirQualityItems().getValue() == null ||
                viewModel.getAirQualityItems().getValue().isEmpty()) {
            viewModel.loadAirQuality();
        }

        // Initial accessibility styling (also sets adapter font sizes)
        applyAccessibilityMode();
        boolean acc = AccessibilityPrefs.isAccessibilityEnabled(this);
        adapter.setAccessibilityEnabled(acc);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply when coming back from Settings
        applyAccessibilityMode();
        boolean acc = AccessibilityPrefs.isAccessibilityEnabled(this);
        adapter.setAccessibilityEnabled(acc);
    }

    private void applyAccessibilityMode() {
        boolean enabled = AccessibilityPrefs.isAccessibilityEnabled(this);

        // Background
        if (root != null) {
            if (enabled) {
                root.setBackgroundColor(Color.WHITE);       // high contrast
            } else {
                root.setBackgroundColor(0xFFF6F4FB);        // default lilac-ish
            }
        }

        // Title font size: 28sp when accessibility is ON
        float titleSize = enabled ? 32f : 22f;
        txtTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize);
        txtTitle.setTextColor(Color.BLACK);

        // Error text
        float errorSize = enabled ? 28f : 14f;
        txtError.setTextSize(TypedValue.COMPLEX_UNIT_SP, errorSize);
        txtError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

        // Button font
        float buttonSize = enabled ? 26f : 16f;
        btnRefresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonSize);
    }

    private void updateList(List<AirQualityItem> items) {
        adapter.setItems(items);
    }
}