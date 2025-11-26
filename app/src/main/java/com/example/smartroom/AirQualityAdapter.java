package com.example.smartroom;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AirQualityAdapter extends RecyclerView.Adapter<AirQualityAdapter.AQViewHolder> {

    private final List<AirQualityItem> items = new ArrayList<>();

    // NEW: flag to know if accessibility mode is ON
    private boolean accessibilityEnabled = false;

    public void setItems(List<AirQualityItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    // NEW: called from Activity when accessibility pref changes
    public void setAccessibilityEnabled(boolean enabled) {
        this.accessibilityEnabled = enabled;
        notifyDataSetChanged(); // redraw rows with new font sizes
    }

    @NonNull
    @Override
    public AQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_air_quality, parent, false);
        return new AQViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AQViewHolder holder, int position) {
        AirQualityItem item = items.get(position);

        holder.txtStation.setText(item.getStation());
        holder.txtPollutant.setText("Pollutant: " + item.getPollutant());
        holder.txtValue.setText("Value: " + item.getValue());
        holder.txtTimestamp.setText("Updated: " + item.getTimestamp());

        // ---- Apply font sizes depending on accessibility mode ----
        float stationSize   = accessibilityEnabled ? 28f : 16f;  // main line
        float secondarySize = accessibilityEnabled ? 28f : 14f;  // other lines

        holder.txtStation.setTextSize(TypedValue.COMPLEX_UNIT_SP, stationSize);
        holder.txtPollutant.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondarySize);
        holder.txtValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondarySize);
        holder.txtTimestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondarySize);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AQViewHolder extends RecyclerView.ViewHolder {
        TextView txtStation, txtPollutant, txtValue, txtTimestamp;

        AQViewHolder(@NonNull View itemView) {
            super(itemView);
            txtStation   = itemView.findViewById(R.id.txtStation);
            txtPollutant = itemView.findViewById(R.id.txtPollutant);
            txtValue     = itemView.findViewById(R.id.txtValue);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
        }
    }
}