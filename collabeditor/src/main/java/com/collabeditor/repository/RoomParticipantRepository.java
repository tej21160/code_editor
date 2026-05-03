package com.collabeditor.repository;

import com.collabeditor.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, UUID> {
    List<RoomParticipant> findByRoomId(UUID roomId);
}