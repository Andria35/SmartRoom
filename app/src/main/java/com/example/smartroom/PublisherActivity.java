package com.example.smartroom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.io.File;
import java.io.IOException;

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

    // Latest sensor values
    private float lastLux = 0f;
    private float lastAx = 0f;
    private float lastAy = 0f;
    private float lastAz = 0f;

    // ---- Sound (microphone) ----
    private static final int REQ_RECORD_AUDIO = 1001;
    private MediaRecorder mediaRecorder;
    private Handler soundHandler = new Handler(Looper.getMainLooper());
    private boolean isSoundSensing = false;
    private float lastSoundLevel = 0f;   // raw amplitude 0..32767

    // ---- MQTT (HiveMQ client) ----
    private static final String TAG = "SmartRoomMQTT";

    // TODO: set this to your Mac's IP
    private String serverHost = "192.168.1.131";
    private int serverPort = 1883;

    // Topic where sensor data will be published
    private String publishingTopic = "smartroom/test";

    private Mqtt3AsyncClient mqttClient;
    private boolean isMqttConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publisher);

        // ---- Find views ----
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus);
        txtLightValue       = findViewById(R.id.txtLightValue);
        txtAccelValue       = findViewById(R.id.txtAccelValue);
        txtSoundValue       = findViewById(R.id.txtSoundValue); // may be null if not in XML
        btnStartPublishing  = findViewById(R.id.btnStartPublishing);
        btnStopPublishing   = findViewById(R.id.btnStopPublishing);

        // ---- Sensors ----
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (lightSensor == null && accelerometer == null) {
            txtConnectionStatus.setText("No sensors available");
        } else {
            txtConnectionStatus.setText("Sensors ready, MQTT not connected");
        }

        // Create MQTT client object (doesn't connect yet)
        createMQTTclient();

        // Start button: request mic permission, connect MQTT + start sensing
        btnStartPublishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureAudioPermissionAndStart();
            }
        });

        // Stop button: stop sensing + disconnect MQTT
        btnStopPublishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSensing();
                stopSoundSensing();
                disconnectFromBroker();
            }
        });
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
            connectToBroker();
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
                connectToBroker();
                startSensing();
                startSoundSensing();
            } else {
                Toast.makeText(this,
                        "Microphone permission denied. Sound will not be measured.",
                        Toast.LENGTH_SHORT).show();
                // Still start MQTT + other sensors
                connectToBroker();
                startSensing();
            }
        }
    }

    // --------- MQTT SETUP ---------

    private void createMQTTclient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("smartroom-android-" + System.currentTimeMillis())
                .serverHost(serverHost)
                .serverPort(serverPort)
                .buildAsync();
    }

    private void connectToBroker() {
        if (mqttClient == null) {
            createMQTTclient();
        }

        txtConnectionStatus.setText("Connecting to MQTT...");

        mqttClient.connectWith()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        Log.d(TAG, "Problem connecting to server: " + throwable);
                        isMqttConnected = false;
                        runOnUiThread(() ->
                                txtConnectionStatus.setText("MQTT connection FAILED"));
                    } else {
                        Log.d(TAG, "Connected to server");
                        isMqttConnected = true;
                        runOnUiThread(() ->
                                txtConnectionStatus.setText("MQTT connected, sensing..."));
                    }
                });
    }

    private void disconnectFromBroker() {
        if (mqttClient != null) {
            mqttClient.disconnect()
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            Log.d(TAG, "Problem disconnecting: " + throwable);
                        } else {
                            Log.d(TAG, "Disconnected from server");
                        }
                        isMqttConnected = false;
                    });
        }
    }

    private void publishSensorData() {
        if (!isMqttConnected || mqttClient == null) {
            return;
        }

        // Simple JSON payload with latest values
        String payload = "{"
                + "\"light\":" + lastLux + ","
                + "\"ax\":" + lastAx + ","
                + "\"ay\":" + lastAy + ","
                + "\"az\":" + lastAz + ","
                + "\"sound\":" + lastSoundLevel
                + "}";

        mqttClient.publishWith()
                .topic(publishingTopic)
                .payload(payload.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.d(TAG, "Problem publishing sensor data: " + throwable);
                    } else {
                        Log.d(TAG, "Sensor data published: " + payload);
                    }
                });
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
        txtConnectionStatus.setText("Sensing (waiting for MQTT connect...)");
    }

    private void stopSensing() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        isSensing = false;
        txtConnectionStatus.setText("Sensing stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isSensing) return;

        int type = event.sensor.getType();

        if (type == Sensor.TYPE_LIGHT) {
            lastLux = event.values[0];
            txtLightValue.setText("Light: " + lastLux + " lx");
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            lastAx = event.values[0];
            lastAy = event.values[1];
            lastAz = event.values[2];

            txtAccelValue.setText(
                    "Accel:\n" +
                            "x = " + lastAx + "\n" +
                            "y = " + lastAy + "\n" +
                            "z = " + lastAz
            );
        }

        // Every time we get a new sensor value, publish full snapshot (incl. sound)
        publishSensorData();
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

            // More compatible format/encoder on modern Androids
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            // Use a real temp file in cache dir
            File outputFile = File.createTempFile("sound_tmp", ".m4a", getCacheDir());
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();

            isSoundSensing = true;
            soundHandler.post(soundLevelRunnable);

            if (txtSoundValue != null) {
                txtSoundValue.setText("Sound: measuring...");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error starting MediaRecorder", e);
            isSoundSensing = false;
            lastSoundLevel = 0f;

            String msg = e.getClass().getSimpleName();
            if (e.getMessage() != null) {
                msg += ": " + e.getMessage();
            }

            if (txtSoundValue != null) {
                txtSoundValue.setText("Sound error: " + msg);
            }
        }
    }

    private void stopSoundSensing() {
        isSoundSensing = false;
        soundHandler.removeCallbacks(soundLevelRunnable);
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception ignored) {}
            try {
                mediaRecorder.reset();
            } catch (Exception ignored) {}
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    private final Runnable soundLevelRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSoundSensing || mediaRecorder == null) return;
            try {
                int amp = mediaRecorder.getMaxAmplitude(); // 0..32767
                lastSoundLevel = amp;
                if (txtSoundValue != null) {
                    txtSoundValue.setText("Sound level: " + amp);
                }
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
        stopSensing();
        stopSoundSensing();
        disconnectFromBroker();
    }
}