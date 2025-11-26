package com.example.smartroom.publisher.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.graphics.Color;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartroom.helpers.AccessibilityPrefs;
import com.example.smartroom.publisher.viewModel.PublisherViewModel;
import com.example.smartroom.R;

import java.io.File;

public class PublisherActivity extends AppCompatActivity implements SensorEventListener {

    // ---- UI ----
    private TextView txtConnectionStatus;
    private TextView txtLightValue;
    private TextView txtAccelValue;
    private TextView txtSoundValue;
    private Button btnStartPublishing;
    private Button btnStopPublishing;

    // ---- Sensors ----
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor accelerometer;
    private boolean isSensing = false;

    // ---- Sound (microphone) ----
    private static final int REQ_RECORD_AUDIO = 1001;
    private MediaRecorder mediaRecorder;
    private Handler soundHandler = new Handler(Looper.getMainLooper());
    private boolean isSoundSensing = false;

    // ---- ViewModel (MQTT + sensor values + publishing state) ----
    private PublisherViewModel viewModel;

    private static final String TAG = "SmartRoomMQTT";

    @Override
    protected void onResume() {
        super.onResume();

        // Re-read the preference every time the screen becomes visible
        boolean accessibilityOn = AccessibilityPrefs.isAccessibilityEnabled(this);
        ViewGroup root = findViewById(R.id.publisherRoot);
        applyAccessibilityMode(accessibilityOn, root);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publisher);

        // ---- Find views ----
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus);
        txtLightValue       = findViewById(R.id.txtLightValue);
        txtAccelValue       = findViewById(R.id.txtAccelValue);
        txtSoundValue       = findViewById(R.id.txtSoundValue);
        btnStartPublishing  = findViewById(R.id.btnStartPublishing);
        btnStopPublishing   = findViewById(R.id.btnStopPublishing);

        // ---- ViewModel ----
        viewModel = new ViewModelProvider(this).get(PublisherViewModel.class);

        // Connection status label
        viewModel.getStatusText().observe(this, text -> {
            if (text != null) txtConnectionStatus.setText(text);
        });

        // Sensor values → update UI (and they re-apply after rotation)
        viewModel.getLightLiveData().observe(this, lux -> {
            if (lux != null) txtLightValue.setText("Light: " + lux + " lx");
        });

        viewModel.getAccelXLiveData().observe(this, x -> updateAccelLabel());
        viewModel.getAccelYLiveData().observe(this, y -> updateAccelLabel());
        viewModel.getAccelZLiveData().observe(this, z -> updateAccelLabel());

        viewModel.getSoundLiveData().observe(this, sound -> {
            if (sound != null) {
                txtSoundValue.setText("Sound level: " + sound);
            }
        });

        // Publishing state: on rotation, this observer will fire
        viewModel.getIsPublishing().observe(this, publishing -> {
            if (publishing == null) return;

            if (publishing) {
                // Make sure MQTT is connected
                viewModel.connectToBroker();

                // Resume sensors if not active
                if (!isSensing) {
                    startSensing();
                }

                // Resume mic if permission is granted and not active
                if (!isSoundSensing &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED) {
                    startSoundSensing();
                }
            } else {
                // User stopped publishing → stop sensors/mic
                if (isSensing) stopSensing();
                if (isSoundSensing) stopSoundSensing();
            }
        });

        // ---- Sensors ----
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (lightSensor == null && accelerometer == null) {
            txtConnectionStatus.setText("No sensors available");
        }

        // Start button
        btnStartPublishing.setOnClickListener(v -> ensureAudioPermissionAndStart());

        // Stop button
        btnStopPublishing.setOnClickListener(v -> {
            viewModel.stopPublishing();
            stopSensing();
            stopSoundSensing();
            viewModel.disconnectFromBroker();
        });
    }

    private void updateAccelLabel() {
        Float ax = viewModel.getAccelXLiveData().getValue();
        Float ay = viewModel.getAccelYLiveData().getValue();
        Float az = viewModel.getAccelZLiveData().getValue();
        if (ax == null || ay == null || az == null) return;

        txtAccelValue.setText(
                "Accel:\n" +
                        "x = " + ax + "\n" +
                        "y = " + ay + "\n" +
                        "z = " + az
        );
    }

    // --------- PERMISSION + ENTRY POINT ---------

    private void ensureAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_RECORD_AUDIO
            );
        } else {
            // Permission already granted
            viewModel.startPublishing();      // sets isPublishing=true and connects
            startSensing();
            startSoundSensing();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.startPublishing();
                startSensing();
                startSoundSensing();
            } else {
                Toast.makeText(this,
                        "Microphone permission denied. Sound will not be measured.",
                        Toast.LENGTH_SHORT).show();
                // Still start MQTT + other sensors
                viewModel.startPublishing();
                startSensing();
            }
        }
    }

    // --------- SENSORS (light + accel) ---------

    private void startSensing() {
        if (sensorManager == null) return;

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        isSensing = true;
    }

    private void stopSensing() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        isSensing = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isSensing) return;

        int type = event.sensor.getType();

        if (type == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            viewModel.updateLight(lux);
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            viewModel.updateAccel(ax, ay, az);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    // --------- SOUND SENSING (microphone) ---------

    private void startSoundSensing() {
        if (isSoundSensing) return;

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            // Use a real temp file in cache dir
            File outputFile = File.createTempFile("sound_tmp", ".m4a", getCacheDir());
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();

            isSoundSensing = true;
            soundHandler.post(soundLevelRunnable);

            txtSoundValue.setText("Sound: measuring...");

        } catch (Exception e) {
            Log.e(TAG, "Error starting MediaRecorder", e);
            isSoundSensing = false;

            String msg = e.getClass().getSimpleName();
            if (e.getMessage() != null) msg += ": " + e.getMessage();
            txtSoundValue.setText("Sound error: " + msg);
        }
    }

    private void stopSoundSensing() {
        isSoundSensing = false;
        soundHandler.removeCallbacks(soundLevelRunnable);
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch (Exception ignored) {}
            try { mediaRecorder.reset(); } catch (Exception ignored) {}
            try { mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    private final Runnable soundLevelRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSoundSensing || mediaRecorder == null) return;
            try {
                int amp = mediaRecorder.getMaxAmplitude(); // 0..32767
                viewModel.updateSound(amp);
            } catch (Exception e) {
                Log.e(TAG, "Error reading sound level", e);
            }
            // Schedule next read
            soundHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        // On rotation or leaving the app temporarily:
        // stop local sensors/mic, but DO NOT change ViewModel's publishing state.
        stopSensing();
        stopSoundSensing();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If user really leaves this screen (back press), stop publishing + disconnect
        if (isFinishing()) {
            viewModel.stopPublishing();
            viewModel.disconnectFromBroker();
        }
    }

    private void applyAccessibilityMode(boolean enabled, ViewGroup root) {
        if (!enabled) return;

        if (root != null) {
            root.setBackgroundColor(Color.WHITE);
        }

        txtConnectionStatus.setTextColor(Color.BLACK);
        txtLightValue.setTextColor(Color.BLACK);
        txtAccelValue.setTextColor(Color.BLACK);
        txtSoundValue.setTextColor(Color.BLACK);

        txtConnectionStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        txtLightValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        txtAccelValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        txtSoundValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);

        btnStartPublishing.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        btnStopPublishing.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
    }
}