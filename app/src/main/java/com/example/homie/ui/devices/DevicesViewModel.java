package com.example.homie.ui.devices;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.homie.model.Device;
import com.example.homie.repository.SmartHomeRepository;
import java.util.List;

public class DevicesViewModel extends ViewModel {

    private SmartHomeRepository repository;
    private LiveData<List<Device>> devices;
    private MutableLiveData<String> filterType;

    public DevicesViewModel() {
        repository = SmartHomeRepository.getInstance();
        devices = repository.getDevices();
        filterType = new MutableLiveData<>();
        filterType.setValue("all");
    }

    public LiveData<List<Device>> getDevices() {
        return devices;
    }

    public LiveData<String> getFilterType() {
        return filterType;
    }

    public void setFilterType(String filter) {
        filterType.setValue(filter);
    }

    public void toggleDevice(String deviceId) {
        repository.toggleDevice(deviceId);
    }

    public List<Device> getFilteredDevices(String filter) {
        List<Device> allDevices = devices.getValue();
        if (allDevices == null)
            return null;

        switch (filter) {
            case "active":
                return repository.getActiveDevices();
            case "inactive":
                List<Device> inactiveDevices = new java.util.ArrayList<>();
                for (Device device : allDevices) {
                    if (!device.isActive()) {
                        inactiveDevices.add(device);
                    }
                }
                return inactiveDevices;
            default:
                return allDevices;
        }
    }

    public LiveData<List<Device>> getAllDevices() {
        return devices;
    }


    private final MutableLiveData<List<Device>> searchResults = new MutableLiveData<>();

    public LiveData<List<Device>> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<Device> results) {
        searchResults.setValue(results);
    }

}
