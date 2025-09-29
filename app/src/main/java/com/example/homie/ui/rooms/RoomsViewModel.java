package com.example.homie.ui.rooms;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.homie.model.Room;
import com.example.homie.repository.SmartHomeRepository;
import java.util.List;

public class RoomsViewModel extends ViewModel {

    private SmartHomeRepository repository;
    private LiveData<List<Room>> rooms;

    public RoomsViewModel() {
        repository = SmartHomeRepository.getInstance();
        rooms = repository.getRooms();
    }

    public LiveData<List<Room>> getRooms() {
        return rooms;
    }

    public LiveData<List<Room>> getAllRooms() {
        return rooms;
    }

}
