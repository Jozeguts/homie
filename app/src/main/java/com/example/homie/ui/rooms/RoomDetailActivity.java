package com.example.homie.ui.rooms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.R;
import com.example.homie.adapter.DeviceAdapter;
import com.example.homie.model.Device;
import com.example.homie.ui.devices.DevicesViewModel;
import java.util.ArrayList;
import java.util.List;

public class RoomDetailActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    public static final String EXTRA_ROOM_ID = "extra_room_id";
    public static final String EXTRA_ROOM_NAME = "extra_room_name";

    private TextView roomNameText;
    private TextView roomInfoText;
    private RecyclerView devicesRecyclerView;
    private DeviceAdapter deviceAdapter;
    private DevicesViewModel devicesViewModel;
    private String roomId;
    private String roomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        // Get room data from intent
        Intent intent = getIntent();
        roomId = intent.getStringExtra(EXTRA_ROOM_ID);
        roomName = intent.getStringExtra(EXTRA_ROOM_NAME);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(roomName != null ? roomName : "Room Details");
        }

        initViews();
        setupRecyclerView();
        setupViewModel();
    }

    private void initViews() {
        roomNameText = findViewById(R.id.room_name);
        roomInfoText = findViewById(R.id.room_info);
        devicesRecyclerView = findViewById(R.id.devices_recycler_view);

        if (roomName != null) {
            roomNameText.setText(roomName);
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        devicesRecyclerView.setLayoutManager(layoutManager);

        // Initialize with empty list
        deviceAdapter = new DeviceAdapter(new ArrayList<>(), this);
        devicesRecyclerView.setAdapter(deviceAdapter);
    }

    private void setupViewModel() {
        devicesViewModel = new ViewModelProvider(this).get(DevicesViewModel.class);

        // Get all devices and filter by room
        devicesViewModel.getAllDevices().observe(this, allDevices -> {
            if (allDevices != null && roomName != null) {
                List<Device> roomDevices = new ArrayList<>();
                for (Device device : allDevices) {
                    // Match by room name instead of roomId since devices use room names
                    if (roomName.equals(device.getRoom())) {
                        roomDevices.add(device);
                    }
                }
                deviceAdapter.updateDevices(roomDevices);
                roomInfoText.setText(roomDevices.size() + " devices • " +
                        countActiveDevices(roomDevices) + " active");
            }
        });
    }

    private int countActiveDevices(List<Device> devices) {
        int count = 0;
        for (Device device : devices) {
            if (device.isActive()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onDeviceClick(Device device) {
        // Handle device click - navigate to device detail
        Intent intent = new Intent(this, com.example.homie.ui.devices.DeviceDetailActivity.class);
        intent.putExtra("device_id", device.getId());
        intent.putExtra("device_name", device.getName());
        startActivity(intent);
    }

    @Override
    public void onDeviceToggle(Device device) {
        // Handle device toggle
        devicesViewModel.toggleDevice(device.getId());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
