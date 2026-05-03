package com.collabeditor.service;

import com.collabeditor.entity.Room;
import com.collabeditor.entity.RoomParticipant;
import com.collabeditor.repository.RoomParticipantRepository;
import com.collabeditor.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository participantRepository;

    public Room createRoom(String name, UUID createdBy) {
        Room room = Room.builder().name(name).createdBy(createdBy).createdAt(LocalDateTime.now()).isActive(true).build();
        return roomRepository.save(room);
    }

    public RoomParticipant joinRoom(UUID roomId, UUID userId) {
        RoomParticipant participant = RoomParticipant.builder().roomId(roomId).userId(userId).joinedAt(LocalDateTime.now()).build();
        return participantRepository.save(participant);
    }

    public Map<String, Object> getRoomDetails(UUID roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            List<RoomParticipant> participants = participantRepository.findByRoomId(roomId);
            return Map.of("room", roomOpt.get(), "participants", participants);
        }
        return null;
    }
}