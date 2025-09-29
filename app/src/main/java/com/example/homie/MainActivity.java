package com.example.homie;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.homie.databinding.ActivityMainBinding;
import com.example.homie.model.Device;
import com.example.homie.services.WebSocketService;
import com.example.homie.ui.home.HomeViewModel;
import com.example.homie.utils.SecurityManager;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // Match the ESP32 AP SSID here
    private static final String ESP32_SSID = "SmartHome_ESP32";

    private ActivityMainBinding binding;
    private WebSocketService webSocketService;
    private boolean webSocketServiceBound = false;
    private HomeViewModel homeViewModel;
    private SecurityManager securityManager;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private Network esp32Network = null;
    private boolean isWifiConnected = false; // Tracks Android Wi-Fi network connection (SSID match)
    private boolean isESP32NetworkBound = false;
    private View wifiStatusLight;
    private View esp32StatusLight;
    private Gson gson = new Gson();

    private final ConnectivityManager.NetworkCallback esp32NetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "Network available: " + network);
            // Only bind when SSID matches and network is wifi
            if (isConnectedToESP32WiFi()) {
                checkAndBindNetworkIfMatches(network);
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            if (network.equals(esp32Network)) {
                Log.w(TAG, "ESP32 network lost");
                esp32Network = null;
                isESP32NetworkBound = false;
                runOnUiThread(() -> {
                    isWifiConnected = false;
                    updateWifiStatusIndicator(false);
                    updateESP32StatusIndicator(false);
                    Toast.makeText(MainActivity.this, "ESP32 WiFi network lost", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            if (network.equals(esp32Network)) {
                Log.d(TAG, "ESP32 network capabilities changed");
            }
        }
    };

    private final ServiceConnection webSocketConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            WebSocketService.WebSocketBinder binder = (WebSocketService.WebSocketBinder) service;
            webSocketService = binder.getService();
            webSocketServiceBound = true;
            Log.d(TAG, "WebSocketService connected");
            Toast.makeText(MainActivity.this, "WebSocket Service Connected", Toast.LENGTH_SHORT).show();
            observeWebSocketStatus();
            // If we already bound to ESP32 network, tell service to try socket connect now
            if (isESP32NetworkBound && webSocketService != null) {
                webSocketService.requestInitialData();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            webSocketServiceBound = false;
            webSocketService = null;
            updateESP32StatusIndicator(false);
            Log.d(TAG, "WebSocketService disconnected");
        }
    };

    private final ActivityResultLauncher<String> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Location permission granted");
                    checkAndHandleWiFiConnection();
                } else {
                    Log.w(TAG, "Location permission denied");
                    Toast.makeText(this, "Location permission required for WiFi detection", Toast.LENGTH_LONG).show();
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Location access is needed to detect the ESP32 WiFi. Grant it in settings.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        securityManager = new SecurityManager(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiStatusLight = findViewById(R.id.wifi_status_light);
        esp32StatusLight = findViewById(R.id.esp32_status_light);

        if (!securityManager.isUserAuthenticated()) {
            redirectToAuth();
            return;
        }

        setupNavigation();
        setupEnhancedWifiMonitoring();
        startWebSocketService();
        requestLocationPermission();
        logActivity("App started", "User opened the application");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndHandleWiFiConnection();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission already granted");
            checkAndHandleWiFiConnection();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void setupEnhancedWifiMonitoring() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        try {
            connectivityManager.registerNetworkCallback(builder.build(), esp32NetworkCallback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback", e);
        }
    }

    private void checkAndHandleWiFiConnection() {
        boolean wasConnected = isWifiConnected;
        isWifiConnected = isConnectedToESP32WiFi();

        if (isWifiConnected != wasConnected) {
            updateWifiStatusIndicator(isWifiConnected);

            // 👇 CRITICAL: Update ViewModel with current WiFi network state
            homeViewModel.setWifiConnected(isWifiConnected);

            if (isWifiConnected) {
                Log.d(TAG, "Connected to ESP32 WiFi network");
                findAndBindESP32Network();
            } else {
                Log.d(TAG, "Disconnected from ESP32 WiFi network");
                unbindFromESP32Network();
                updateESP32StatusIndicator(false); // Also disconnect ESP32 if WiFi drops
            }
        }
    }

    private boolean isConnectedToESP32WiFi() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                String currentSSID = wifiInfo.getSSID().replace("\"", "");
                boolean isESP32 = currentSSID.equals(ESP32_SSID);
                Log.d(TAG, "Current SSID: " + currentSSID + ", is ESP32: " + isESP32);
                return isESP32;
            }
        }
        return false;
    }

    private void findAndBindESP32Network() {
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // Verify this network maps to current wifi SSID before binding
                if (isConnectedToESP32WiFi()) {
                    checkAndBindNetworkIfMatches(network);
                    return;
                }
            }
        }
        Log.w(TAG, "No matching ESP32 network instance found yet");
    }

    // Bind only if the active network resolves the ESP32 gateway IP or if SSID
    // matches
    private void checkAndBindNetworkIfMatches(Network network) {
        try {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return;
            }
            // Proceed to bind process to this network
            esp32Network = network;
            bindToESP32Network(network);
            // After binding, kick service to request data
            if (webSocketServiceBound && webSocketService != null) {
                webSocketService.requestInitialData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAndBindNetworkIfMatches", e);
        }
    }

    private void bindToESP32Network(Network network) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean success = connectivityManager.bindProcessToNetwork(network);
                if (success) {
                    isESP32NetworkBound = true;
                    Log.d(TAG, "Successfully bound app to ESP32 network");
                    runOnUiThread(() -> {
                        isWifiConnected = true;
                        updateWifiStatusIndicator(true);
                    });
                } else {
                    Log.e(TAG, "Failed to bind app to ESP32 network");
                    runOnUiThread(
                            () -> Toast.makeText(this, "Failed to bind to ESP32 network", Toast.LENGTH_SHORT).show());
                }
            } else {
                ConnectivityManager.setProcessDefaultNetwork(network);
                isESP32NetworkBound = true;
                Log.d(TAG, "Set ESP32 as default network (legacy method)");
                runOnUiThread(() -> {
                    isWifiConnected = true;
                    updateWifiStatusIndicator(true);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding to ESP32 network", e);
            runOnUiThread(() -> Toast.makeText(this, "Failed to bind to ESP32 network", Toast.LENGTH_SHORT).show());
        }
    }

    private void unbindFromESP32Network() {
        if (isESP32NetworkBound) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.bindProcessToNetwork(null);
                } else {
                    ConnectivityManager.setProcessDefaultNetwork(null);
                }
                isESP32NetworkBound = false;
                Log.d(TAG, "Unbound app from ESP32 network");
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding from ESP32 network", e);
            }
        }
    }

    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_home, R.id.navigation_devices, R.id.navigation_rooms)
                        .build();
                setSupportActionBar(binding.toolbar);
                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
                Log.d(TAG, "Navigation setup completed successfully");
            } else {
                Log.e(TAG, "NavHostFragment not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation", e);
        }
    }

    private void updateWifiStatusIndicator(boolean connected) {
        if (wifiStatusLight != null) {
            wifiStatusLight.setBackgroundResource(connected ? R.drawable.circle_green : R.drawable.circle_red);
        }
    }

    private void updateESP32StatusIndicator(boolean connected) {
        if (esp32StatusLight != null) {
            esp32StatusLight.setBackgroundResource(connected ? R.drawable.circle_green : R.drawable.circle_red);
        }
    }

    private void startWebSocketService() {
        try {
            Intent serviceIntent = new Intent(this, WebSocketService.class);
            startService(serviceIntent);
            bindService(serviceIntent, webSocketConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "WebSocketService start and bind initiated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start WebSocketService", e);
            Toast.makeText(this, "Failed to start WebSocket Service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void observeWebSocketStatus() {
        if (webSocketService != null) {
            webSocketService.getConnected().observe(this, connected -> {
                if (connected != null) {
                    updateESP32StatusIndicator(connected);
                    String status = connected ? "Connected to ESP32" : "Disconnected";
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show();

                    // 👇 CRITICAL: Update ViewModel with WebSocket connection state
                    homeViewModel.setWebSocketConnected(connected);

                    // 👇 ALSO update WiFi state based on current network — ensures UI stays in sync
                    if (isConnectedToESP32WiFi()) {
                        homeViewModel.setWifiConnected(true);
                    } else {
                        homeViewModel.setWifiConnected(false);
                    }

                    logActivity("ESP32 Status", status);
                }
            });

            webSocketService.getDeviceUpdates().observe(this, devices -> {
                if (devices != null && !devices.isEmpty()) {
                    Log.d(TAG, "Received " + devices.size() + " device updates from ESP32");
                    homeViewModel.updateDevicesFromESP32(devices);
                    saveDevicesToLocal(devices);
                }
            });

            webSocketService.getTemperatureData().observe(this, tempData -> {
                if (tempData != null && !tempData.isEmpty()) {
                    Log.d(TAG, "Temperature data received: " + tempData.toString());
                    homeViewModel.updateTemperatureData(tempData);
                }
            });
        }
    }

    private void saveDevicesToLocal(List<Device> devices) {
        SharedPreferences prefs = getSharedPreferences("device_sync", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (Device device : devices) {
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("id", device.getId());
            deviceData.put("name", device.getName());
            deviceData.put("type", device.getType());
            deviceData.put("room", device.getRoom());
            deviceData.put("isActive", device.isActive());
            deviceData.put("pin", device.getPin());
            deviceData.put("timestamp", System.currentTimeMillis());
            editor.putString("device_" + device.getId(), gson.toJson(deviceData));
        }
        editor.apply();
        Log.d(TAG, "Saved " + devices.size() + " devices to local storage");
    }

    private void logActivity(String action, String details) {
        Log.d(TAG, "Activity logged: " + action + " - " + details);
    }

    private void redirectToAuth() {
        Intent authIntent = new Intent(this, com.example.homie.ui.auth.AuthActivity.class);
        startActivity(authIntent);
        finish();
    }

    public WebSocketService getWebSocketService() {
        return webSocketServiceBound ? webSocketService : null;
    }

    public boolean isWebSocketServiceConnected() {
        return webSocketServiceBound && webSocketService != null
                && Boolean.TRUE.equals(webSocketService.getConnected().getValue());
    }

    public boolean isWifiConnected() {
        return isWifiConnected;
    }

    public boolean isESP32NetworkBound() {
        return isESP32NetworkBound;
    }

    public void navigateToDevices() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.navigation_devices);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to devices", e);
        }
    }

    public void navigateToRooms() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.navigation_rooms);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to rooms", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            performFullSync();
            return true;
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, com.example.homie.ui.settings.SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performFullSync() {
        if (isWebSocketServiceConnected()) {
            if (webSocketService != null) {
                webSocketService.requestInitialData();
                Toast.makeText(this, "Sync initiated...", Toast.LENGTH_SHORT).show();
                logActivity("Full Sync", "User initiated full sync with ESP32");
            }
        } else {
            Toast.makeText(this, "ESP32 Not Connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                return NavigationUI.navigateUp(navController, new AppBarConfiguration.Builder().build())
                        || super.onSupportNavigateUp();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSupportNavigateUp", e);
        }
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(esp32NetworkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callbacks", e);
            }
        }
        unbindFromESP32Network();
        if (webSocketServiceBound) {
            unbindService(webSocketConnection);
            webSocketServiceBound = false;
        }
        logActivity("App closed", "User closed the application");
    }

    // Additional methods preserved from original
    public void toggleDevice(String deviceId, boolean state) {
        if (webSocketService != null && isWebSocketServiceConnected()) {
            webSocketService.toggleDevice(deviceId, state);
        }
    }

    public void updateDevice(Device device) {
        if (webSocketService != null && isWebSocketServiceConnected()) {
            webSocketService.updateDevice(device);
        }
    }

    public Map<String, Object> getStoredSettings() {
        Map<String, Object> settings = new HashMap<>();
        SharedPreferences notificationPrefs = getSharedPreferences("notification_prefs", MODE_PRIVATE);
        SharedPreferences securityPrefs = getSharedPreferences("security_prefs", MODE_PRIVATE);

        settings.put("device_alerts", notificationPrefs.getBoolean("device_alerts", true));
        settings.put("security_alerts", notificationPrefs.getBoolean("security_alerts", true));
        settings.put("smart_suggestions", notificationPrefs.getBoolean("smart_suggestions", false));
        settings.put("two_factor_auth", securityPrefs.getBoolean("two_factor_auth", false));
        settings.put("data_collection", securityPrefs.getBoolean("data_collection", true));
        return settings;
    }
}