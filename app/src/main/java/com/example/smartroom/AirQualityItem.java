package com.example.smartroom;

public class AirQualityItem {
    private final String station;
    private final String pollutant;
    private final String value;
    private final String timestamp;

    public AirQualityItem(String station, String pollutant, String value, String timestamp) {
        this.station = station;
        this.pollutant = pollutant;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getStation() {
        return station;
    }

    public String getPollutant() {
        return pollutant;
    }

    public String getValue() {
        return value;
    }

    public String getTimestamp() {
        return timestamp;
    }
}