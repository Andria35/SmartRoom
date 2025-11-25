package com.example.smartroom;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

public class PublisherViewModel extends ViewModel {

    private static final String TAG = "SmartRoomMQTT";

    // ---- Periodic Publishing ----
    private final Handler publishHandler = new Handler(Looper.getMainLooper());
    private static final long PUBLISH_INTERVAL_MS = 2000; // Publish every 2 seconds (adjust as needed)

    // MQTT config
    private String serverHost = "192.168.0.103";  // your Mac IP
    private int serverPort = 1883;
    private String publishingTopic = "smartroom/test";

    private Mqtt3AsyncClient mqttClient;

    // Connection state
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusText =
            new MutableLiveData<>("Status: Disconnected");

    public LiveData<Boolean> getIsConnected() { return isConnected; }
    public LiveData<String> getStatusText() { return statusText; }

    // Publishing state (should we be sensing/publishing?)
    private final MutableLiveData<Boolean> isPublishing = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsPublishing() { return isPublishing; }

    public void startPublishing() {
        isPublishing.postValue(true);
        // connectToBroker() will now handle starting the publish timer upon success
        connectToBroker();

        // --- REMOVE THESE LINES ---
        // publishHandler.removeCallbacks(publishRunnable);
        // publishHandler.post(publishRunnable);
    }

    public void stopPublishing() {
        isPublishing.postValue(false);

        // --- ADDED: Stop the periodic publishing timer ---
        publishHandler.removeCallbacks(publishRunnable);
    }

    // ---- Sensor values (persist across rotation) ----
    private float lastLux = 0f;
    private float lastAx = 0f;
    private float lastAy = 0f;
    private float lastAz = 0f;
    private float lastSound = 0f;

    private final MutableLiveData<Float> lightLiveData = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> accelXLiveData = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> accelYLiveData = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> accelZLiveData = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> soundLiveData   = new MutableLiveData<>(0f);

    public LiveData<Float> getLightLiveData()   { return lightLiveData; }
    public LiveData<Float> getAccelXLiveData()  { return accelXLiveData; }
    public LiveData<Float> getAccelYLiveData()  { return accelYLiveData; }
    public LiveData<Float> getAccelZLiveData()  { return accelZLiveData; }
    public LiveData<Float> getSoundLiveData()   { return soundLiveData; }

    // Called by Activity when light sensor changes
    public void updateLight(float lux) {
        lastLux = lux;
        lightLiveData.postValue(lux);
//        publishCurrentSnapshot();
    }

    // Called by Activity when accelerometer changes
    public void updateAccel(float ax, float ay, float az) {
        lastAx = ax;
        lastAy = ay;
        lastAz = az;
        accelXLiveData.postValue(ax);
        accelYLiveData.postValue(ay);
        accelZLiveData.postValue(az);
//        publishCurrentSnapshot();
    }

    // Called by Activity when sound level changes
    public void updateSound(float sound) {
        lastSound = sound;
        soundLiveData.postValue(sound);
//        publishCurrentSnapshot();
    }

    // ---- MQTT helpers ----

    private void createMQTTclientIfNeeded() {
        if (mqttClient != null) return;

        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("smartroom-android-" + System.currentTimeMillis())
                .serverHost(serverHost)
                .serverPort(serverPort)
                .buildAsync();
    }

    public void connectToBroker() {
        Boolean connected = isConnected.getValue();
        if (connected != null && connected) {
            // already connected, ensure the timer is running
            publishHandler.removeCallbacks(publishRunnable);
            publishHandler.post(publishRunnable);
            return;
        }

        createMQTTclientIfNeeded();

        statusText.postValue("Connecting to MQTT...");

        mqttClient.connectWith()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        Log.d(TAG, "Problem connecting to server: " + throwable);
                        isConnected.postValue(false);
                        statusText.postValue("MQTT connection FAILED");
                    } else {
                        Log.d(TAG, "Connected to server");
                        isConnected.postValue(true);
                        statusText.postValue("MQTT connected, sensing...");

                        // --- THE KEY FIX IS HERE ---
                        // Only start the publishing timer after a successful connection.
                        Boolean publishing = isPublishing.getValue();
                        if (publishing != null && publishing) {
                            publishHandler.removeCallbacks(publishRunnable);
                            publishHandler.post(publishRunnable);
                        }
                    }
                });
    }

    public void disconnectFromBroker() {
        if (mqttClient == null) {
            isConnected.postValue(false);
            statusText.postValue("Status: Disconnected");
            return;
        }

        mqttClient.disconnect()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Log.d(TAG, "Problem disconnecting: " + throwable);
                    } else {
                        Log.d(TAG, "Disconnected from server");
                    }
                    isConnected.postValue(false);
                    statusText.postValue("Status: Disconnected");
                });
    }

    // Publish whatever the latest snapshot is
// Publish whatever the latest snapshot is
    private void publishCurrentSnapshot() {
        Boolean connected = isConnected.getValue();

        // Only require client + connection
        if (mqttClient == null || connected == null || !connected) {
            return;
        }

        String payload = "{"
                + "\"light\":" + lastLux + ","
                + "\"ax\":" + lastAx + ","
                + "\"ay\":" + lastAy + ","
                + "\"az\":" + lastAz + ","
                + "\"sound\":" + lastSound
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

    private final Runnable publishRunnable = new Runnable() {
        @Override
        public void run() {
            // Only publish if the Activity has requested publishing AND we are connected
            Boolean publishing = isPublishing.getValue();
            Boolean connected = isConnected.getValue();

            if (publishing != null && publishing && connected != null && connected) {
                // This is the only place we now call the publishing logic
                publishCurrentSnapshot();
            }

            // Schedule the next execution
            publishHandler.postDelayed(this, PUBLISH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();
        // Best-effort disconnect when ViewModel is finally destroyed
        disconnectFromBroker();

        publishHandler.removeCallbacks(publishRunnable);
    }
}