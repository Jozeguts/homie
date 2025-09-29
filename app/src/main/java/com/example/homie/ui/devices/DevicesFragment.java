package com.example.homie.ui.devices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.R;
import com.example.homie.adapter.DeviceAdapter;
import com.example.homie.databinding.FragmentDevicesBinding;
import com.example.homie.model.Device;
import java.util.ArrayList;

public class DevicesFragment extends Fragment implements DeviceAdapter.OnDeviceClickListener {

    private FragmentDevicesBinding binding;
    private DevicesViewModel devicesViewModel;
    private DeviceAdapter deviceAdapter;
    private RecyclerView devicesRecyclerView;
    private Button btnAll, btnActive, btnInactive;
    private String currentFilter = "all";

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        devicesViewModel = new ViewModelProvider(this).get(DevicesViewModel.class);

        binding = FragmentDevicesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupViews(root);
        setupRecyclerView();
        setupFilterButtons();
        observeViewModel();

        return root;
    }

    private void setupViews(View root) {
        devicesRecyclerView = root.findViewById(R.id.devices_recycler_view);
        btnAll = root.findViewById(R.id.btn_filter_all);
        btnActive = root.findViewById(R.id.btn_filter_active);
        btnInactive = root.findViewById(R.id.btn_filter_inactive);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        devicesRecyclerView.setLayoutManager(layoutManager);

        deviceAdapter = new DeviceAdapter(new ArrayList<>(), this);
        devicesRecyclerView.setAdapter(deviceAdapter);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateFilterButtons();
            updateDevicesList();
        });

        btnActive.setOnClickListener(v -> {
            currentFilter = "active";
            updateFilterButtons();
            updateDevicesList();
        });

        btnInactive.setOnClickListener(v -> {
            currentFilter = "inactive";
            updateFilterButtons();
            updateDevicesList();
        });

        updateFilterButtons();
    }

    private void updateFilterButtons() {
        // Reset all buttons
        btnAll.setSelected(false);
        btnActive.setSelected(false);
        btnInactive.setSelected(false);

        // Set selected button
        switch (currentFilter) {
            case "all":
                btnAll.setSelected(true);
                break;
            case "active":
                btnActive.setSelected(true);
                break;
            case "inactive":
                btnInactive.setSelected(true);
                break;
        }
    }

    private void observeViewModel() {
        devicesViewModel.getDevices().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                updateDevicesList();
            }
        });
    }

    private void updateDevicesList() {
        deviceAdapter.updateDevices(devicesViewModel.getFilteredDevices(currentFilter));
    }

    @Override
    public void onDeviceClick(Device device) {
        // Handle device detail click - could navigate to device detail fragment
        // For now, just toggle the device
        onDeviceToggle(device);
    }

    @Override
    public void onDeviceToggle(Device device) {
        devicesViewModel.toggleDevice(device.getId());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
