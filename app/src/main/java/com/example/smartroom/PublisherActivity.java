package com.example.smartroom;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PublisherActivity extends AppCompatActivity implements SensorEventListener {

    // UI references
    private TextView txtConnectionStatus;
    private TextView txtLightValue;
    private TextView txtAccelValue;
    private Button btnStartPublishing;
    private Button btnStopPublishing;

    // Sensor stuff
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor accelerometer;

    private boolean isSensing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publisher);  // uses your XML

        // 1. Connect XML views to Java
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus);
        txtLightValue       = findViewById(R.id.txtLightValue);
        txtAccelValue       = findViewById(R.id.txtAccelValue);
        btnStartPublishing  = findViewById(R.id.btnStartPublishing);
        btnStopPublishing   = findViewById(R.id.btnStopPublishing);

        // 2. Get SensorManager and sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            lightSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // 3. Show which sensors we have
        if (lightSensor == null && accelerometer == null) {
            txtConnectionStatus.setText("Sensors not available");
        } else if (lightSensor == null) {
            txtConnectionStatus.setText("No light sensor, accelerometer OK");
        } else if (accelerometer == null) {
            txtConnectionStatus.setText("Light OK, no accelerometer");
        } else {
            txtConnectionStatus.setText("Sensors ready (no MQTT yet)");
        }

        // 4. Set click listeners
        btnStartPublishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSensing();
            }
        });

        btnStopPublishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSensing();
            }
        });
    }

    private void startSensing() {
        if (sensorManager == null) return;

        // Register listeners only if sensor exists
        if (lightSensor != null) {
            sensorManager.registerListener(
                    this,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }

        isSensing = true;
        txtConnectionStatus.setText("Sensing (updating values on screen)");
    }

    private void stopSensing() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        isSensing = false;
        txtConnectionStatus.setText("Sensing stopped");
        // optional: clear values
        // txtLightValue.setText("—");
        // txtAccelValue.setText("—");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isSensing) return;

        int sensorType = event.sensor.getType();

        if (sensorType == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            txtLightValue.setText("Light: " + lux + " lx");
        } else if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            txtAccelValue.setText(
                    "Accel:\n" +
                            "x = " + x + "\n" +
                            "y = " + y + "\n" +
                            "z = " + z
            );
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // You can ignore this for now
    }

    @Override
    protected void onPause() {
        super.onPause();
        // To avoid wasting battery if user leaves screen
        stopSensing();
    }
}