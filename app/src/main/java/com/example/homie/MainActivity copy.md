
package com.example.homie;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.homie.model.Device;
import com.example.homie.model.Room;
import com.example.homie.repository.SmartHomeRepository;
import com.example.homie.repository.ESP32Repository;
import com.example.homie.services.ESP32Service;
import com.example.homie.ui.settings.SettingsActivity;
import com.example.homie.voice.VoiceCommandManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements VoiceCommandManager.VoiceCommandListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private NavController navController;
    private FloatingActionButton fabVoice;
    private VoiceCommandManager voiceCommandManager;
    private SearchView searchView;
    private SmartHomeRepository repository;
    private ESP32Repository esp32Repository;

    // ESP32 Service integration
    private ESP32Service esp32Service;
    private boolean serviceBound = false;

    // Permission request launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    startESP32Service();
                    showMessage("Permissions granted. Connecting to ESP32...");
                } else {
                    showMessage("Permissions required for ESP32 connection");
                }
            });

    // Service connection for ESP32
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "ESP32 service connected");
            ESP32Service.ESP32Binder binder = (ESP32Service.ESP32Binder) service;
            esp32Service = binder.getService();
            serviceBound = true;

            setupServiceObservers();
            showMessage("Connected to ESP32 service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "ESP32 service disconnected");
            esp32Service = null;
            serviceBound = false;
            showMessage("ESP32 service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = SmartHomeRepository.getInstance();
        esp32Repository = new ESP32Repository(this);

        setupNavigation();
        setupVoiceButton();
        setupVoiceCommandManager();
        checkPermissions();

        Log.d(TAG, "MainActivity created with ESP32 integration");
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            Log.e("MainActivity", "NavHostFragment is null");
            return;
        }

        navController = navHostFragment.getNavController();
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_devices, R.id.navigation_rooms).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    private void setupVoiceButton() {
        fabVoice = findViewById(R.id.fab_voice);
        fabVoice.setOnClickListener(v -> toggleVoiceRecognition());
    }

    private void setupVoiceCommandManager() {
        voiceCommandManager = new VoiceCommandManager(this);
        voiceCommandManager.setVoiceCommandListener(this);
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startESP32Service();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void startESP32Service() {
        Intent serviceIntent = new Intent(this, ESP32Service.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupServiceObservers() {
        if (esp32Service == null)
            return;

        // Observe ESP32 connection state
        esp32Service.getEsp32Connected().observe(this, connected -> {
            updateConnectionIndicator(connected);

            if (connected) {
                showMessage("Connected to ESP32 controller");
            } else {
                showMessage("Disconnected from ESP32 controller");
            }
        });

        // Observe device updates from ESP32
        esp32Service.getDeviceUpdates().observe(this, devices -> {
            if (devices != null && !devices.isEmpty()) {
                Log.d(TAG, "Received device updates from ESP32: " + devices.size() + " devices");
                // Update local repository with ESP32 device states
                updateLocalDevicesFromESP32(devices);
            }
        });

        // Observe temperature data from ESP32
        esp32Service.getTemperatureData().observe(this, temperatureData -> {
            if (temperatureData != null && !temperatureData.isEmpty()) {
                Log.d(TAG, "Received temperature data: " + temperatureData.size() + " readings");
                updateDeviceTemperatures(temperatureData);
            }
        });
    }

    private void updateLocalDevicesFromESP32(List<Device> esp32Devices) {
        for (Device esp32Device : esp32Devices) {
            Device localDevice = repository.getDeviceById(esp32Device.getId());
            if (localDevice != null) {
                localDevice.setActive(esp32Device.isActive());
                localDevice.setOnline(esp32Device.isOnline());
                localDevice.setLastUpdate(esp32Device.getLastUpdate());
                repository.updateDevice(localDevice);
            }
        }
    }

    private void updateDeviceTemperatures(Map<String, Float> temperatureData) {
        for (Map.Entry<String, Float> entry : temperatureData.entrySet()) {
            Device device = repository.getDeviceById(entry.getKey());
            if (device != null) {
                device.setTemperature(entry.getValue().doubleValue());
                repository.updateDevice(device);
            }
        }
    }

    private void toggleVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        if (voiceCommandManager.isListening()) {
            voiceCommandManager.stopListening();
        } else {
            voiceCommandManager.startListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search devices and rooms...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        performSearch(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (newText.length() > 2) {
                            performSearch(newText);
                        }
                        return true;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_sync) {
            if (serviceBound && esp32Service != null) {
                esp32Service.syncDataWithESP32();
                showMessage("Syncing data with ESP32...");
            } else {
                showMessage("ESP32 service not connected");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performSearch(String query) {
        List<Device> allDevices = repository.getAllDevices();
        List<Room> allRooms = repository.getAllRooms();

        List<Device> matchedDevices = new ArrayList<>();
        List<Room> matchedRooms = new ArrayList<>();

        for (Device device : allDevices) {
            if (device.getName().toLowerCase().contains(query.toLowerCase()) ||
                    device.getType().toLowerCase().contains(query.toLowerCase()) ||
                    device.getRoom().toLowerCase().contains(query.toLowerCase())) {
                matchedDevices.add(device);
            }
        }

        for (Room room : allRooms) {
            if (room.getName().toLowerCase().contains(query.toLowerCase())) {
                matchedRooms.add(room);
            }
        }

        if (matchedDevices.isEmpty() && matchedRooms.isEmpty()) {
            Toast.makeText(this, "No match found for: " + query, Toast.LENGTH_SHORT).show();
        } else {
            StringBuilder result = new StringBuilder("Search Results:\n");

            if (!matchedDevices.isEmpty()) {
                result.append("\nDevices:\n");
                for (Device d : matchedDevices) {
                    result.append("• ").append(d.getName()).append(" (").append(d.getRoom()).append(")\n");
                }
            }

            if (!matchedRooms.isEmpty()) {
                result.append("\nRooms:\n");
                for (Room r : matchedRooms) {
                    result.append("• ").append(r.getName()).append("\n");
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Search Results")
                    .setMessage(result.toString())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    public void onCommandRecognized(String command) {
        runOnUiThread(() -> {
            executeVoiceCommand(command);
            new AlertDialog.Builder(this)
                    .setTitle("Voice Command Executed")
                    .setMessage("Command: " + command)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void executeVoiceCommand(String command) {
        String lower = command.toLowerCase();
        List<Device> devices = repository.getAllDevices();

        try {
            if (lower.contains("turn on") || lower.contains("turn off")) {
                boolean turnOn = lower.contains("turn on");
                String deviceName = matchDeviceNameFromCommand(lower, devices);
                if (deviceName != null) {
                    Device device = findDeviceByName(devices, deviceName);
                    if (device != null) {
                        // Use ESP32 service to control device if available
                        if (serviceBound && esp32Service != null && device.isESP32Controlled()) {
                            esp32Service.toggleDevice(device.getId(), turnOn);
                        } else {
                            device.setActive(turnOn);
                            repository.updateDevice(device);
                        }
                        Toast.makeText(this, device.getName() + " has been " + (turnOn ? "turned on" : "turned off"),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Device not found in command.", Toast.LENGTH_SHORT).show();
                }
            } else if (lower.contains("set") && lower.contains("temperature")) {
                String deviceName = matchDeviceNameFromCommand(lower, devices);
                int temp = extractNumberFromCommand(lower);
                if (deviceName != null && temp > 0) {
                    Device device = findDeviceByName(devices, deviceName);
                    if (device != null && device.getType().toLowerCase().contains("thermostat")) {
                        Toast.makeText(this, device.getName() + " temperature set to " + temp + "°F",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Command not recognized.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Command error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ... keep existing code (matchDeviceNameFromCommand, findDeviceByName,
    // extractNumberFromCommand methods) ...

    private String matchDeviceNameFromCommand(String command, List<Device> devices) {
        for (Device device : devices) {
            if (command.contains(device.getName().toLowerCase())) {
                return device.getName();
            }
        }
        return null;
    }

    private Device findDeviceByName(List<Device> devices, String name) {
        for (Device device : devices) {
            if (device.getName().equalsIgnoreCase(name)) {
                return device;
            }
        }
        return null;
    }

    private int extractNumberFromCommand(String command) {
        String[] words = command.split(" ");
        for (String word : words) {
            try {
                return Integer.parseInt(word.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    @Override
    public void onListeningStateChanged(boolean isListening) {
        runOnUiThread(() -> {
            fabVoice.setImageResource(isListening ? R.drawable.ic_mic_active : R.drawable.ic_mic);
            Toast.makeText(this, isListening ? "Listening..." : "Stopped listening", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Voice error: " + error, Toast.LENGTH_SHORT).show();
            fabVoice.setImageResource(R.drawable.ic_mic);
        });
    }

    // ESP32 integration methods
    public void toggleDeviceViaESP32(String deviceId, boolean state) {
        if (serviceBound && esp32Service != null) {
            esp32Service.toggleDevice(deviceId, state);
        } else {
            showMessage("ESP32 service not available");
        }
    }

    public void syncWithESP32() {
        if (serviceBound && esp32Service != null) {
            esp32Service.syncDataWithESP32();
        }
    }

    public boolean isESP32Connected() {
        return serviceBound && esp32Service != null &&
                Boolean.TRUE.equals(esp32Service.getEsp32Connected().getValue());
    }

    private void updateConnectionIndicator(boolean connected) {
        if (getSupportActionBar() != null) {
            String title = "Smart Home" + (connected ? " • ESP32 Connected" : " • ESP32 Disconnected");
            getSupportActionBar().setTitle(title);
        }
    }

    private void showMessage(String message) {
        Log.d(TAG, message);
        runOnUiThread(() -> {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
        });
    }

    public void navigateToRooms() {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setSelectedItemId(R.id.navigation_rooms);
    }

    public void navigateToDevices() {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setSelectedItemId(R.id.navigation_devices);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, new AppBarConfiguration.Builder().build())
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        if (voiceCommandManager != null) {
            voiceCommandManager.destroy();
        }

        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }

        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }
}
