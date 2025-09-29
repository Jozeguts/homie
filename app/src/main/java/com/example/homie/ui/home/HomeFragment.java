package com.example.homie.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.MainActivity;
import com.example.homie.R;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private RecyclerView activeDevicesRecycler;
    private RecyclerView roomsRecycler;

    // Temperature UI elements
    private CardView mainTemperatureCard;
    private TextView mainTemperatureLabel;
    private TextView mainTemperatureValue;
    private TextView mainTemperatureStatus;
    private TextView noTemperatureMessage;

    // Connection indicators — exactly matching XML IDs
    private TextView wifiIndicator; // For Android Wi-Fi network
    private TextView websocketIndicator; // For ESP32 WebSocket connection

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(root);
        setupRecyclerViews();
        setupQuickActions(root);
        setupSeeAllButtons(root);
        setupObservers();

        return root;
    }

    private void initializeViews(View root) {
        // RecyclerViews
        activeDevicesRecycler = root.findViewById(R.id.active_devices_recycler);
        roomsRecycler = root.findViewById(R.id.rooms_recycler);

        // Temperature UI elements
        mainTemperatureCard = root.findViewById(R.id.main_temperature_card);
        mainTemperatureLabel = root.findViewById(R.id.main_temperature_label);
        mainTemperatureValue = root.findViewById(R.id.main_temperature_value);
        mainTemperatureStatus = root.findViewById(R.id.main_temperature_status);
        noTemperatureMessage = root.findViewById(R.id.no_temperature_message);

        // Connection indicators — MUST MATCH XML IDs
        wifiIndicator = root.findViewById(R.id.wifi_indicator); // ✅ Exists now
        websocketIndicator = root.findViewById(R.id.websocket_indicator); // ✅ Exists now
    }

    private void setupRecyclerViews() {
        if (activeDevicesRecycler != null) {
            activeDevicesRecycler.setLayoutManager(
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        if (roomsRecycler != null) {
            roomsRecycler.setLayoutManager(
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        }
    }

    private void setupQuickActions(View root) {
        View allLightsAction = root.findViewById(R.id.quick_action_all_lights);
        if (allLightsAction != null) {
            allLightsAction.setOnClickListener(v -> homeViewModel.toggleAllLights());
        }

        View climateAction = root.findViewById(R.id.quick_action_climate);
        if (climateAction != null) {
            climateAction.setOnClickListener(v -> homeViewModel.openClimateControl());
        }

        View securityAction = root.findViewById(R.id.quick_action_security);
        if (securityAction != null) {
            securityAction.setOnClickListener(v -> homeViewModel.toggleSecurity());
        }
    }

    private void setupSeeAllButtons(View root) {
        View seeAllRoomsButton = root.findViewById(R.id.see_all_rooms);
        if (seeAllRoomsButton != null) {
            seeAllRoomsButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToRooms();
                }
            });
        }

        View seeAllDevicesButton = root.findViewById(R.id.see_all_devices);
        if (seeAllDevicesButton != null) {
            seeAllDevicesButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToDevices();
                }
            });
        }
    }

    private void setupObservers() {
        // Temperature
        homeViewModel.getCurrentTemperature().observe(getViewLifecycleOwner(), temperature -> {
            if (temperature != null && temperature > 0) {
                updateTemperatureDisplay(temperature);
                showTemperatureCard(true);
            } else {
                showTemperatureCard(false);
            }
        });

        // WiFi Network Indicator (Android Wi-Fi)
        homeViewModel.getWifiConnected().observe(getViewLifecycleOwner(), connected -> {
            if (wifiIndicator != null) {
                wifiIndicator.setText(connected ? "WiFi: Connected" : "WiFi: Disconnected");
                wifiIndicator.setTextColor(
                        getResources().getColor(connected ? R.color.success : R.color.error, null));
            }
        });

        // ESP32 WebSocket Connection Indicator
        homeViewModel.getWebSocketConnected().observe(getViewLifecycleOwner(), connected -> {
            if (websocketIndicator != null) {
                websocketIndicator.setText(connected ? "ESP32: Connected" : "ESP32: Disconnected");
                websocketIndicator.setTextColor(
                        getResources().getColor(connected ? R.color.success : R.color.error, null));
            }
            if (noTemperatureMessage != null && !connected) {
                noTemperatureMessage.setText("No temperature data available.\nConnect to ESP32 to see readings.");
            }
        });
    }

    private void updateTemperatureDisplay(Float temperature) {
        if (mainTemperatureValue != null) {
            mainTemperatureValue.setText(String.format("%.1f°C", temperature));
        }
        if (mainTemperatureStatus != null) {
            String status = homeViewModel.getTemperatureStatus();
            mainTemperatureStatus.setText(status);
        }
        if (mainTemperatureLabel != null) {
            mainTemperatureLabel.setText("Living Room");
        }
    }

    private void showTemperatureCard(boolean show) {
        if (mainTemperatureCard != null) {
            mainTemperatureCard.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (noTemperatureMessage != null) {
            noTemperatureMessage.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}