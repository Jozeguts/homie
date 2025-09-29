package com.example.homie.ui.devices;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.homie.R;
import com.example.homie.model.Device;
import com.example.homie.repository.SmartHomeRepository;

public class DeviceDetailActivity extends AppCompatActivity {

    private Device device;
    private SmartHomeRepository repository;

    private ImageView deviceIcon;
    private TextView deviceName;
    private TextView deviceRoom;
    private TextView deviceStatus;
    private Switch deviceSwitch;
    private SeekBar brightnessSeekBar;
    private TextView brightnessLabel;
    private TextView brightnessValue;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        repository = SmartHomeRepository.getInstance();



        // Get device ID from intent
        String deviceId = getIntent().getStringExtra("device_id");
        if (deviceId != null) {
            device = repository.getDeviceById(deviceId);
        }

        if (device == null) {
            Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(device.getName());
        }

        initViews();
        setupDevice();
        setupClickListeners();
    }

    private void initViews() {
        deviceIcon = findViewById(R.id.device_icon);
        deviceName = findViewById(R.id.device_name);
        deviceRoom = findViewById(R.id.device_room);
        deviceStatus = findViewById(R.id.device_status);
        deviceSwitch = findViewById(R.id.device_switch);
        brightnessSeekBar = findViewById(R.id.brightness_seekbar);
        brightnessLabel = findViewById(R.id.brightness_label);
        brightnessValue = findViewById(R.id.brightness_value);
        saveButton = findViewById(R.id.btn_save);
    }

    private void setupDevice() {
        deviceName.setText(device.getName());
        deviceRoom.setText(device.getRoom());
        deviceStatus.setText(device.isActive() ? "Online" : "Offline");
        deviceSwitch.setChecked(device.isActive());

        // Set appropriate icon based on device type
        int iconResource = getDeviceIcon(device.getType());
        deviceIcon.setImageResource(iconResource);

        // Show brightness control only for lights
        if (device.getType().toLowerCase().contains("light")) {
            brightnessSeekBar.setVisibility(android.view.View.VISIBLE);
            brightnessLabel.setVisibility(android.view.View.VISIBLE);
            brightnessValue.setVisibility(android.view.View.VISIBLE);

            // Set initial brightness value (assuming 70% default)
            int brightness = 70;
            brightnessSeekBar.setProgress(brightness);
            brightnessValue.setText(brightness + "%");
        } else {
            brightnessSeekBar.setVisibility(android.view.View.GONE);
            brightnessLabel.setVisibility(android.view.View.GONE);
            brightnessValue.setVisibility(android.view.View.GONE);
        }
    }

    private int getDeviceIcon(String deviceType) {
        String type = deviceType.toLowerCase();
        if (type.contains("light")) {
            return R.drawable.ic_lightbulb;
        } else if (type.contains("thermostat") || type.contains("temperature")) {
            return R.drawable.ic_thermostat;
        } else if (type.contains("lock")) {
            return R.drawable.ic_lock;
        } else {
            return R.drawable.ic_devices;
        }
    }

    private void setupClickListeners() {
        deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            device.setActive(isChecked);
            deviceStatus.setText(isChecked ? "Online" : "Offline");
        });

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    brightnessValue.setText(progress + "%");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        saveButton.setOnClickListener(v -> {
            // Save device settings
            repository.updateDevice(device);

            String message = "Settings saved for " + device.getName();
            if (brightnessSeekBar.getVisibility() == android.view.View.VISIBLE) {
                message += " (Brightness: " + brightnessSeekBar.getProgress() + "%)";
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
