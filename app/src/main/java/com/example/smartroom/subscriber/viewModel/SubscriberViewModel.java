package com.example.smartroom.subscriber.viewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartroom.Constants;
import com.example.smartroom.subscriber.model.SensorData;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

public class SubscriberViewModel extends ViewModel {

    private static final String TAG = "SmartRoomSubscriber";

    private final MutableLiveData<SensorData> parsedData = new MutableLiveData<>();
    public LiveData<SensorData> getParsedData() { return parsedData; }

    private final String serverHost = Constants.serverIP;
    private final int serverPort = Constants.serverPort;
    private final String subscribeTopic = Constants.mqttTopics;

    private Mqtt3AsyncClient mqttClient;

    private final MutableLiveData<Boolean> isConnected =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> statusText =
            new MutableLiveData<>("Status: Disconnected");

    private final MutableLiveData<String> lastMessage =
            new MutableLiveData<>("No messages yet");

    public LiveData<Boolean> getIsConnected() { return isConnected; }
    public LiveData<String> getStatusText() { return statusText; }
    public LiveData<String> getLastMessage() { return lastMessage; }

    private void createClientIfNeeded() {
        if (mqttClient != null) return;

        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier("smartroom-subscriber-" + System.currentTimeMillis())
                .serverHost(serverHost)
                .serverPort(serverPort)
                .buildAsync();
    }

    public void connectAndSubscribe() {
        createClientIfNeeded();

        statusText.postValue("🔌 Connecting to MQTT broker…");

        mqttClient.connectWith()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {

                        Log.e(TAG, "❌ Connection failed", throwable);
                        statusText.postValue("❌ Connection FAILED. Check Wi-Fi or broker.");
                        isConnected.postValue(false);

                    } else {

                        Log.d(TAG, "✅ Connected to MQTT broker");
                        statusText.postValue("✅ Connected. Subscribing…");
                        isConnected.postValue(true);

                        subscribeToTopic();
                    }
                });
    }


    private void subscribeToTopic() {

        statusText.postValue("🎧 Subscribing to " + subscribeTopic + "…");

        mqttClient.subscribeWith()
                .topicFilter(subscribeTopic)
                .callback(publish -> {

                    byte[] payloadBytes = publish.getPayloadAsBytes();
                    if (payloadBytes == null) {
                        Log.e(TAG, "❌ Empty MQTT payload");
                        return;
                    }

                    String payload = new String(payloadBytes);
                    Log.d(TAG, "📩 Received MQTT message: " + payload);

                    // Парсим вручную
                    SensorData data = parseSensorData(payload);
                    if (data != null) {
                        parsedData.postValue(data);
                    }

                })
                .send()
                .whenComplete((subAck, throwable) -> {

                    if (throwable != null) {
                        Log.e(TAG, "❌ Subscribe failed", throwable);
                        statusText.postValue("❌ Subscribe FAILED");
                    } else {
                        statusText.postValue("🎧 Listening for sensor data…");
                    }
                });
    }


    private SensorData parseSensorData(String json) {
        try {
            // deleting '{' '}'
            json = json.replace("{", "").replace("}", "");

            // dividing into pairs "key:value"
            String[] parts = json.split(",");

            SensorData data = new SensorData();

            for (String part : parts) {
                String[] pair = part.split(":");
                if (pair.length < 2) continue;

                String key = pair[0].replace("\"", "").trim();
                String value = pair[1].trim();

                switch (key) {
                    case "light":
                        data.light = Float.parseFloat(value);
                        break;
                    case "ax":
                        data.ax = Float.parseFloat(value);
                        break;
                    case "ay":
                        data.ay = Float.parseFloat(value);
                        break;
                    case "az":
                        data.az = Float.parseFloat(value);
                        break;
                    case "sound":
                        data.sound = Float.parseFloat(value);
                        break;
                }
            }

            return data;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing JSON manually: " + e.getMessage());
            return null;
        }
    }




    public void disconnect() {
        if (mqttClient == null) return;

        mqttClient.disconnect()
                .whenComplete((result, throwable) -> {
                    statusText.postValue("Disconnected");
                    isConnected.postValue(false);
                });
    }

    @Override
    protected void onCleared() {
        disconnect();
        super.onCleared();
    }
}
