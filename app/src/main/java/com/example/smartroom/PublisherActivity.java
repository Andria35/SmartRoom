package com.example.smartroom;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

public class PublisherActivity extends AppCompatActivity implements SensorEventListener {

    // ---- UI ----
    private TextView txtConnectionStatus;
    private TextView txtLightValue;
    private TextView txtAccelValue;
    private Button btnStartPublishing;
    private Button btnStopPublishing;

    // ---- Sensors ----
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor accelerometer;
    private boolean isSensing = false;

    // Latest sensor values (so we can send them together)
    private float lastLux = 0f;
    private float lastAx = 0f;
    private float lastAy = 0f;
    private float lastAz = 0f;

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

        // Start button: connect MQTT + start sensing
        btnStartPublishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToBroker();
                startSensing();
            }
        });

        // Stop button: stop sensing + disconnect MQTT
        btnStopPublishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSensing();
                disconnectFromBroker();
            }
        });
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
                + "\"az\":" + lastAz
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

    // --------- SENSORS ---------

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

        // Every time we get a new value, try to publish the latest snapshot
        publishSensorData();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSensing();
        disconnectFromBroker();
    }
}