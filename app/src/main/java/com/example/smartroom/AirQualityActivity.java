package com.example.smartroom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class AirQualityActivity extends AppCompatActivity {

    private static final String TAG = "AirQualityActivity";

    // Madrid air-quality open data (real-time)
    private static final String AIR_QUALITY_URL =
            "https://ciudadesabiertas.madrid.es/dynamicAPI/API/query/calair_tiemporeal.json?pageSize=5000";

    private RecyclerView recyclerView;
    private AirQualityAdapter adapter;
    private ProgressBar progressBar;
    private TextView txtError;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality);

        recyclerView = findViewById(R.id.recyclerAirQuality);
        progressBar = findViewById(R.id.progressBar);
        txtError = findViewById(R.id.txtError);
        btnRefresh = findViewById(R.id.btnRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AirQualityAdapter();
        recyclerView.setAdapter(adapter);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAirQuality();
            }
        });

        // Load once when opening
        fetchAirQuality();
    }

    // ------------------- NETWORK -------------------

    private void fetchAirQuality() {
        progressBar.setVisibility(View.VISIBLE);
        txtError.setVisibility(View.GONE);

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

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            adapter.setItems(items);
                        });

                    } else {
                        Log.e(TAG, "HTTP error: " + responseCode);
                        showErrorOnUiThread("HTTP error: " + responseCode);
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error fetching air quality", e);
                    showErrorOnUiThread("Error: " + e.getMessage());
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    private void showErrorOnUiThread(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            txtError.setVisibility(View.VISIBLE);
            txtError.setText(message);
        });
    }

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

    // ------------------- JSON PARSING -------------------

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

            String estacion3 = pad3(estacion);

            String fullStationCode = provincia + municipio + estacion3;
            String stationName = mapStation(fullStationCode);

            // Skip any station that doesn't have a known name
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
                    String hourLabel = pad2(String.valueOf(h - 1)); // 0–23 style label
                    timestamp = year + "-" + pad2(month) + "-" + pad2(day)
                            + " " + hourLabel + ":00";
                    break;
                }
            }

            // If no valid value, skip this record
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

    // ------------------- HELPERS -------------------

    // Map MAGNITUD codes to human-readable pollutant names
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
            default:
                return null; // unknown → hide
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