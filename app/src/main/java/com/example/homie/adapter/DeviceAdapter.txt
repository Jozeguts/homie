package com.example.homie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.R;
import com.example.homie.model.Device;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Device> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);

        void onDeviceToggle(Device device);
    }

    public DeviceAdapter(List<Device> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_card, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView deviceName;
        private TextView deviceRoom;
        private TextView deviceStatus;
        private TextView deviceTemperature;
        private ImageView deviceIcon;
        private ImageView statusIndicator;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.device_card);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceRoom = itemView.findViewById(R.id.device_room);
            deviceStatus = itemView.findViewById(R.id.device_status);
            deviceTemperature = itemView.findViewById(R.id.device_temperature);
            deviceIcon = itemView.findViewById(R.id.device_icon);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        public void bind(Device device) {
            deviceName.setText(device.getName());
            deviceRoom.setText(device.getRoom());
            deviceStatus.setText(device.getStatus());
            deviceIcon.setImageResource(device.getIconResource());

            // Handle temperature display
            if (device.getTemperature() != null) {
                deviceTemperature.setVisibility(View.VISIBLE);
                deviceTemperature.setText(device.getTemperature().intValue() + "°");
            } else {
                deviceTemperature.setVisibility(View.GONE);
            }

            // Set status indicator color
            int statusColor = device.isActive() ? ContextCompat.getColor(itemView.getContext(), R.color.primary)
                    : ContextCompat.getColor(itemView.getContext(), R.color.secondary);
            statusIndicator.setColorFilter(statusColor);

            // Set card background based on status
            if (device.isActive()) {
                cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.device_active_background));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.surface));
            }

            // Click listeners
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });

            statusIndicator.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceToggle(device);
                }
            });
        }
    }
}
