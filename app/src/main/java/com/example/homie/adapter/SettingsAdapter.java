
package com.example.homie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.R;
import com.example.homie.model.SettingsItem;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    private List<SettingsItem> settingsItems;
    private OnSettingClickListener listener;

    public interface OnSettingClickListener {
        void onSettingClick(SettingsItem item);
    }

    public SettingsAdapter(List<SettingsItem> settingsItems, OnSettingClickListener listener) {
        this.settingsItems = settingsItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settings, parent, false);
        return new SettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        SettingsItem item = settingsItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return settingsItems.size();
    }

    class SettingsViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView titleTextView;
        private TextView descriptionTextView;

        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.setting_icon);
            titleTextView = itemView.findViewById(R.id.setting_title);
            descriptionTextView = itemView.findViewById(R.id.setting_description);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSettingClick(settingsItems.get(position));
                }
            });
        }

        public void bind(SettingsItem item) {
            iconImageView.setImageResource(item.getIconResource());
            titleTextView.setText(item.getTitle());
            descriptionTextView.setText(item.getDescription());
        }
    }
}
