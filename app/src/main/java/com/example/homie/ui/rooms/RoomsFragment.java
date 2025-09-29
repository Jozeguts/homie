package com.example.homie.ui.rooms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.R;
import com.example.homie.adapter.RoomAdapter;
import com.example.homie.model.Room;
import com.example.homie.ui.rooms.RoomsViewModel;

import java.util.ArrayList;

public class RoomsFragment extends Fragment implements RoomAdapter.OnRoomClickListener {

    private RoomsViewModel roomsViewModel;
    private RecyclerView roomsRecyclerView;
    private RoomAdapter roomAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        roomsViewModel = new ViewModelProvider(this).get(RoomsViewModel.class);

        View root = inflater.inflate(R.layout.fragment_rooms, container, false);

        roomsRecyclerView = root.findViewById(R.id.rooms_recycler_view);

        setupRecyclerView();
        observeRooms();

        return root;
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        roomsRecyclerView.setLayoutManager(layoutManager);

        roomAdapter = new RoomAdapter(new ArrayList<>(), this);

        roomsRecyclerView.setAdapter(roomAdapter);
    }

    private void observeRooms() {
        roomsViewModel.getAllRooms().observe(getViewLifecycleOwner(), rooms -> {
            if (rooms != null) {
                roomAdapter.updateRooms(rooms);
            }
        });
    }

    @Override
    public void onRoomClick(Room room) {
        Intent intent = new Intent(getActivity(), RoomDetailActivity.class);
        intent.putExtra(RoomDetailActivity.EXTRA_ROOM_ID, room.getId());
        intent.putExtra(RoomDetailActivity.EXTRA_ROOM_NAME, room.getName());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
