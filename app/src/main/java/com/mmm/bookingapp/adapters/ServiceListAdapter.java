package com.mmm.bookingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.bookingapp.R;

import java.util.List;

/**
 * RecyclerView adapter for the Services screen (Screen 7).
 * Displays the fixed, hardcoded list of 7 services - informational only,
 * per the Blueprint (services are a fixed dropdown source elsewhere in
 * the app, e.g. Book Appointment, not user-editable data).
 */
public class ServiceListAdapter extends RecyclerView.Adapter<ServiceListAdapter.ServiceViewHolder> {

    private final List<String> services;

    public ServiceListAdapter(List<String> services) {
        this.services = services;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_card, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.tvServiceName.setText(services.get(position));
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
        }
    }
}