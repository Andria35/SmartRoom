package com.example.smartroom;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AirQualityViewModel extends ViewModel {

    private static final String TAG = "AirQualityViewModel";

    // Madrid air-quality open data (real-time)
    private static final String AIR_QUALITY_URL =
            "https://ciudadesabiertas.madrid.es/dynamicAPI/API/query/calair_tiemporeal.json?pageSize=5000";

    private final MutableLiveData<List<AirQualityItem>> airQualityItems =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> isLoading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>(null);

    public LiveData<List<AirQualityItem>> getAirQualityItems() {
        return airQualityItems;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadAirQuality() {
        // Avoid parallel loads if already loading
        Boolean loading = isLoading.getValue();
        if (loading != null && loading) return;

        isLoading.postValue(true);
        errorMessage.postValue(null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(AIR_QUALITY_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = connection.getInputStream();
                        String json = readStream(inputStream);
                        List<AirQualityItem> items = parseJson(json);

                        airQualityItems.postValue(items);
                        isLoading.postValue(false);
                        errorMessage.postValue(null);

                    } else {
                        Log.e(TAG, "HTTP error: " + responseCode);
                        isLoading.postValue(false);
                        errorMessage.postValue("HTTP error: " + responseCode);
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error fetching air quality", e);
                    isLoading.postValue(false);
                    errorMessage.postValue("Error: " + e.getMessage());
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    // ------------------- HELPERS -------------------

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private List<AirQualityItem> parseJson(String json) throws JSONException {
        List<AirQualityItem> list = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray records = root.getJSONArray("records");

        for (int i = 0; i < records.length(); i++) {
            JSONObject obj = records.getJSONObject(i);

            String provincia = obj.optString("PROVINCIA", "");
            String municipio = obj.optString("MUNICIPIO", "");
            String estacion  = obj.optString("ESTACION", "");
            String magnitudCode = obj.optString("MAGNITUD", "");

            // ESTACION must be 3 digits (e.g. "11" -> "011")
            String estacion3 = pad3(estacion);

            // Full station code like 28079011 (2 + 3 + 3 digits)
            String fullStationCode = provincia + municipio + estacion3;
            String stationName = mapStation(fullStationCode);

            // Skip stations that we didn't map
            if (stationName == null) {
                continue;
            }

            String pollutant = mapPollutant(magnitudCode);

            String year  = obj.optString("ANO", "");
            String month = obj.optString("MES", "");
            String day   = obj.optString("DIA", "");

            String value = null;
            String timestamp = null;

            // Search from last hour of the day backwards for a valid value
            for (int h = 24; h >= 1; h--) {
                String hKey = String.format("H%02d", h); // H01..H24
                String vKey = String.format("V%02d", h); // V01..V24

                String v = obj.optString(vKey, "N");
                String hVal = obj.optString(hKey, "");

                if ("V".equals(v) && hVal != null && !hVal.isEmpty()) {
                    value = hVal;
                    String hourLabel = pad2(String.valueOf(h - 1)); // 0–23 style
                    timestamp = year + "-" + pad2(month) + "-" + pad2(day)
                            + " " + hourLabel + ":00";
                    break;
                }
            }

            if (value == null || timestamp == null) {
                continue;
            }

            String valueDisplay = value + " µg/m³";

            AirQualityItem item = new AirQualityItem(
                    stationName,
                    pollutant,
                    valueDisplay,
                    timestamp
            );
            list.add(item);
        }

        return list;
    }

    private String mapPollutant(String code) {
        if (code == null) return "Magnitud " + code;
        switch (code) {
            case "01": return "SO₂ (Dióxido de Azufre)";
            case "06": return "CO (Monóxido de Carbono)";
            case "07": return "NO (Monóxido de Nitrógeno)";
            case "08": return "NO₂ (Dióxido de Nitrógeno)";
            case "09": return "PM2.5 (Partículas < 2.5 µm)";
            case "10": return "PM10 (Partículas < 10 µm)";
            case "12": return "NOx (Óxidos de Nitrógeno)";
            case "14": return "O₃ (Ozono)";
            default:   return "Magnitud " + code;
        }
    }

    private String mapStation(String fullCode) {
        if (fullCode == null) return null;

        switch (fullCode) {
            case "28079001": return "Pza. Recoletos";
            case "28079003": return "Pza. del Carmen";
            case "28079004": return "Pza. de España";
            case "28079007": return "Pza. M. de Salamanca";
            case "28079008": return "Escuelas Aguirre";
            case "28079022": return "Pº Pontones";
            case "28079023": return "Final C/ Alcalá";
            case "28079026": return "Urb. Embajada (Barajas)";
            case "28079038": return "Pza. Castilla";
            case "28079039": return "Plaza de Fdez. Ladreda";
            case "28079040": return "Cuatro Caminos";
            case "28079047": return "Méndez Álvaro";
            case "28079048": return "Pza. Castilla II";
            case "28079049": return "Arturo Soria";
            case "28079050": return "Barrio del Pilar";
            case "28079054": return "Ensanche Vallecas";
            case "28079055": return "Plaza Elíptica";
            case "28079056": return "Moratalaz";
            case "28079057": return "Pza. Fernández Ladreda II";
            case "28079058": return "Sanchinarro";
            case "28079059": return "Parque Juan Carlos I";
            case "28079060": return "Tres Olivos";
            // add more if you want full coverage
            default:
                return null; // unknown: we hide it
        }
    }

    private String pad2(String s) {
        if (s == null) return "";
        return (s.length() == 1) ? "0" + s : s;
    }

    private String pad3(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.length() == 1) return "00" + s;
        if (s.length() == 2) return "0" + s;
        return s;
    }
}