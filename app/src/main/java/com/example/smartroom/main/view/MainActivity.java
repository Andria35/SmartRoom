package com.example.smartroom.main.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartroom.helpers.AccessibilityPrefs;
import com.example.smartroom.publisher.view.PublisherActivity;
import com.example.smartroom.R;
import com.example.smartroom.settings.view.SettingsActivity;
import com.example.smartroom.subscriber.view.SubscriberActivity;
import com.example.smartroom.airQuality.view.AirQualityActivity;

public class MainActivity extends AppCompatActivity {

    private ViewGroup mainRoot;
    private TextView txtHeader;
    private TextView txtSubtitle;

    private Button btnAirQuality;
    private Button btnPublish;
    private Button btnSubscribe;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // uses your XML

        // 1. Connect XML views to Java
        mainRoot    = findViewById(R.id.mainRoot);
        txtHeader   = findViewById(R.id.txtHeader);
        txtSubtitle = findViewById(R.id.txtSubtitle);

        btnAirQuality = findViewById(R.id.btnAirQuality);
        btnPublish    = findViewById(R.id.btnPublish);
        btnSubscribe  = findViewById(R.id.btnSubscribe);
        btnSettings   = findViewById(R.id.btnSettings);

        // 2. Set click listeners
        btnAirQuality.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AirQualityActivity.class);
                startActivity(intent);
            }
        });

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PublisherActivity.class);
                startActivity(intent);
            }
        });

        btnSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SubscriberActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // 3. Initial accessibility styling
        applyAccessibilityMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply in case user changed the accessibility toggle in Settings
        applyAccessibilityMode();
    }

    private void applyAccessibilityMode() {
        boolean enabled = AccessibilityPrefs.isAccessibilityEnabled(this);

        // Background
        if (mainRoot != null) {
            if (enabled) {
                mainRoot.setBackgroundColor(Color.WHITE);    // high contrast mode
            } else {
                mainRoot.setBackgroundColor(0xFFF5F5F7);     // original background
            }
        }

        if (enabled) {
            // Accessibility ON → use 28sp for all texts
            float big = 30f;

            txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            txtHeader.setTextColor(Color.BLACK);

            txtSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            txtSubtitle.setTextColor(Color.BLACK);

            btnAirQuality.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            btnPublish.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            btnSubscribe.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
            btnSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, big);
        } else {
            // Accessibility OFF → normal design from your XML
            txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);
            txtHeader.setTextColor(Color.parseColor("#222222"));

            txtSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            txtSubtitle.setTextColor(Color.parseColor("#666666"));

            float normalButtonSize = 18f;
            btnAirQuality.setTextSize(TypedValue.COMPLEX_UNIT_SP, normalButtonSize);
            btnPublish.setTextSize(TypedValue.COMPLEX_UNIT_SP, normalButtonSize);
            btnSubscribe.setTextSize(TypedValue.COMPLEX_UNIT_SP, normalButtonSize);
            btnSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, normalButtonSize);
        }
    }
}