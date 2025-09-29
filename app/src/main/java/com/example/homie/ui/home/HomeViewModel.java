package com.example.homie.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.homie.model.Device;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Boolean> allLightsState;
    private final MutableLiveData<Boolean> securityState;
    private final MutableLiveData<Float> currentTemperature;
    private final MutableLiveData<Float> currentHumidity;
    private final MutableLiveData<List<Device>> esp32Devices;
    private final MutableLiveData<Boolean> wifiConnected; // Android Wi-Fi network state
    private final MutableLiveData<Boolean> webSocketConnected; // ESP32 WebSocket connection state

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        allLightsState = new MutableLiveData<>();
        securityState = new MutableLiveData<>();
        currentTemperature = new MutableLiveData<>();
        currentHumidity = new MutableLiveData<>();
        esp32Devices = new MutableLiveData<>();
        wifiConnected = new MutableLiveData<>();
        webSocketConnected = new MutableLiveData<>();

        mText.setValue("Welcome Home");
        allLightsState.setValue(false);
        securityState.setValue(true);
        currentTemperature.setValue(22.0f);
        currentHumidity.setValue(50.0f);
        wifiConnected.setValue(false); // Initially not connected to WiFi
        webSocketConnected.setValue(false); // Initially not connected to ESP32
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Boolean> getAllLightsState() {
        return allLightsState;
    }

    public LiveData<Boolean> getSecurityState() {
        return securityState;
    }

    public LiveData<Float> getCurrentTemperature() {
        return currentTemperature;
    }

    public LiveData<Float> getCurrentHumidity() {
        return currentHumidity;
    }

    public LiveData<List<Device>> getESP32Devices() {
        return esp32Devices;
    }

    // ===== WiFi Network Connection (Android Wi-Fi) =====
    public LiveData<Boolean> getWifiConnected() {
        return wifiConnected;
    }

    public void setWifiConnected(boolean connected) {
        wifiConnected.setValue(connected);
    }

    // ===== WebSocket Connection (to ESP32 device) =====
    public LiveData<Boolean> getWebSocketConnected() {
        return webSocketConnected;
    }

    public void setWebSocketConnected(boolean connected) {
        webSocketConnected.setValue(connected);
    }

    public void toggleAllLights() {
        Boolean currentState = allLightsState.getValue();
        allLightsState.setValue(currentState != null ? !currentState : true);
    }

    public void toggleSecurity() {
        Boolean currentState = securityState.getValue();
        securityState.setValue(currentState != null ? !currentState : false);
    }

    public void updateTemperatureData(Map<String, Float> tempData) {
        if (tempData.containsKey("temperature")) {
            currentTemperature.setValue(tempData.get("temperature"));
        }
        if (tempData.containsKey("humidity")) {
            currentHumidity.setValue(tempData.get("humidity"));
        }
    }

    public void updateDevicesFromESP32(List<Device> devices) {
        esp32Devices.setValue(devices);
    }

    public String getTemperatureStatus() {
        Float temp = currentTemperature.getValue();
        if (temp == null)
            return "Unknown";
        if (temp < 18)
            return "Cold";
        if (temp < 25)
            return "Cool";
        if (temp < 30)
            return "Warm";
        return "Hot";
    }

    public String getFanControlStatus() {
        Float temp = currentTemperature.getValue();
        if (temp == null)
            return "Unknown";
        if (temp > 30)
            return "Auto fans ON";
        if (temp < 25)
            return "Auto fans OFF";
        return "Auto fans STANDBY";
    }

    public void openClimateControl() {
        // Hook for climate control
    }

    public void navigateToRooms() {
        // Hook for navigation
    }
}