package com.example.smartroom;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchAccessibility = findViewById(R.id.switchAccessibility);

        // Load current value from SharedPreferences
        boolean enabled = AccessibilityPrefs.isAccessibilityEnabled(this);
        switchAccessibility.setChecked(enabled);

        // Save when user toggles
        switchAccessibility.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AccessibilityPrefs.setAccessibilityEnabled(SettingsActivity.this, isChecked);
                    }
                }
        );
    }
}