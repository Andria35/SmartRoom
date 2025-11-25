package com.example.smartroom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class SubscriberActivity extends AppCompatActivity {

    private SubscriberViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriber);

        viewModel = new ViewModelProvider(this).get(SubscriberViewModel.class);

        TextView statusText = findViewById(R.id.statusText);
        TextView lightValue = findViewById(R.id.lightValue);
        TextView accelValue = findViewById(R.id.accelValue);
        TextView soundValue = findViewById(R.id.soundValue);

        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);

        // статус
        viewModel.getStatusText().observe(this, statusText::setText);

        // новые данные сенсора
        viewModel.getParsedData().observe(this, data -> {
            if (data != null) {
                lightValue.setText("Light: " + data.light);
                accelValue.setText("Accelerometer:\n  ax=" + data.ax + "\n  ay=" + data.ay + "\n  az=" + data.az);
                soundValue.setText("Sound: " + data.sound);
            }
        });

        btnConnect.setOnClickListener(v -> viewModel.connectAndSubscribe());
        btnDisconnect.setOnClickListener(v -> viewModel.disconnect());
    }
}
