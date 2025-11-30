package com.example.smartroom.settings.view;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartroom.helpers.AccessibilityPrefs;
import com.example.smartroom.R;

public class SettingsActivity extends AppCompatActivity {

    private ViewGroup settingsRoot;
    private TextView txtSettingsTitle;
    private TextView txtAccessibilityDescription;
    private TextView txtAccessibilityLabel;
    private Switch switchAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Find views
        settingsRoot                = findViewById(R.id.settingsRoot);
        txtSettingsTitle            = findViewById(R.id.txtSettingsTitle);
        txtAccessibilityDescription = findViewById(R.id.txtAccessibilityDescription);
        txtAccessibilityLabel       = findViewById(R.id.txtAccessibilityLabel);
        switchAccessibility         = findViewById(R.id.switchAccessibility);

        // Load current preference
        boolean enabled = AccessibilityPrefs.isAccessibilityEnabled(this);
        switchAccessibility.setChecked(enabled);

        // Apply initial style based on current state
        applyAccessibilityMode(enabled);

        // Toggle listener
        switchAccessibility.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // Save preference
                        AccessibilityPrefs.setAccessibilityEnabled(SettingsActivity.this, isChecked);
                        // Update Settings screen itself
                        applyAccessibilityMode(isChecked);
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // In case something changed from outside (just to be safe)
        boolean enabled = AccessibilityPrefs.isAccessibilityEnabled(this);
        switchAccessibility.setChecked(enabled);
        applyAccessibilityMode(enabled);
    }

    /**
     * Here we make Settings screen big by default,
     * and even bigger when accessibility is ON.
     */
    private void applyAccessibilityMode(boolean enabled) {
        // Background (keep white for readability)
        if (settingsRoot != null) {
            settingsRoot.setBackgroundColor(Color.WHITE);
        }

        if (enabled) {
            // Accessibility ON → extra big
            txtSettingsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
            txtAccessibilityDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
            txtAccessibilityLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        } else {
            // Accessibility OFF → still bigger than normal app screens, but less huge
            txtSettingsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
            txtAccessibilityDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            txtAccessibilityLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        }

        txtSettingsTitle.setTextColor(Color.BLACK);
        txtAccessibilityDescription.setTextColor(0xFF555555);
        txtAccessibilityLabel.setTextColor(Color.BLACK);
    }
}