package com.example.smartroom;

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

    public void setItems(List<AirQualityItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AQViewHolder extends RecyclerView.ViewHolder {
        TextView txtStation, txtPollutant, txtValue, txtTimestamp;

        AQViewHolder(@NonNull View itemView) {
            super(itemView);
            txtStation = itemView.findViewById(R.id.txtStation);
            txtPollutant = itemView.findViewById(R.id.txtPollutant);
            txtValue = itemView.findViewById(R.id.txtValue);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
        }
    }
}