package com.example.smartroom.subscriber.view;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartroom.helpers.AccessibilityPrefs;
import com.example.smartroom.R;
import com.example.smartroom.subscriber.viewModel.SubscriberViewModel;

public class SubscriberActivity extends AppCompatActivity {

    private SubscriberViewModel viewModel;

    private ViewGroup subscriberRoot;
    private TextView statusText;
    private TextView lightValue;
    private TextView accelValue;
    private TextView soundValue;
    private Button btnConnect;
    private Button btnDisconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriber);

        viewModel = new ViewModelProvider(this).get(SubscriberViewModel.class);

        // ---- Find views ----
        subscriberRoot = findViewById(R.id.subscriberRoot);
        statusText     = findViewById(R.id.statusText);
        lightValue     = findViewById(R.id.lightValue);
        accelValue     = findViewById(R.id.accelValue);
        soundValue     = findViewById(R.id.soundValue);
        btnConnect     = findViewById(R.id.btnConnect);
        btnDisconnect  = findViewById(R.id.btnDisconnect);

        statusText.setAccessibilityLiveRegion(
                View.ACCESSIBILITY_LIVE_REGION_POLITE);

        // ---- ViewModel bindings ----

        // статус
        viewModel.getStatusText().observe(this, statusText::setText);

        // новые данные сенсора
        viewModel.getParsedData().observe(this, data -> {
            if (data != null) {
                lightValue.setText("Light: " + data.light);
                accelValue.setText(
                        "Accelerometer:\n  ax=" + data.ax +
                                "\n  ay=" + data.ay +
                                "\n  az=" + data.az
                );
                soundValue.setText("Sound: " + data.sound);
            }
        });

        btnConnect.setOnClickListener(v -> viewModel.connectAndSubscribe());
        btnDisconnect.setOnClickListener(v -> viewModel.disconnect());

        // Initial accessibility styling
        applyAccessibilityMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply if user changed accessibility in Settings
        applyAccessibilityMode();
    }

    private void applyAccessibilityMode() {
        boolean enabled = AccessibilityPrefs.isAccessibilityEnabled(this);

        // Background
        if (subscriberRoot != null) {
            // You can differentiate here if you want, but both are white now
            subscriberRoot.setBackgroundColor(Color.WHITE);
        }

        if (enabled) {
            // Accessibility ON → everything 28sp
            float big = 28f;

            statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            statusText.setTextColor(Color.BLACK);

            lightValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            accelValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            soundValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);

            btnConnect.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            btnDisconnect.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
        } else {
            // Accessibility OFF → match your XML defaults
            statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            statusText.setTextColor(Color.BLACK);

            lightValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            accelValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            soundValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);

            btnConnect.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            btnDisconnect.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        }
    }
}