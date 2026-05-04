package com.collabeditor.repository;

import com.collabeditor.entity.CodeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, UUID> {
    List<CodeSnapshot> findByRoomIdOrderBySavedAtDesc(UUID roomId);
    Optional<CodeSnapshot> findTopByRoomIdOrderBySavedAtDesc(UUID roomId);
}
