package com.example.smartroom;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartroom.R;

public class MainActivity extends AppCompatActivity {

    private Button btnAirQuality;
    private Button btnPublish;
    private Button btnSubscribe;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // uses your XML

        // 1. Connect XML views to Java
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
                Toast.makeText(MainActivity.this,
                        "Subscribe clicked", Toast.LENGTH_SHORT).show();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                        "Settings clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
}