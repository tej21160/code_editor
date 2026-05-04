package com.collabeditor.service;

import com.collabeditor.entity.CodeSnapshot;
import com.collabeditor.repository.CodeSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SnapshotService {

    @Autowired
    private CodeSnapshotRepository codeSnapshotRepository;

    public CodeSnapshot saveSnapshot(String roomId, String content, Long version) {
        CodeSnapshot snapshot = new CodeSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setRoomId(UUID.fromString(roomId));
        snapshot.setContent(content);
        snapshot.setVersion(version);
        return codeSnapshotRepository.save(snapshot);
    }

    public Optional<CodeSnapshot> getLatestSnapshot(String roomId) {
        return codeSnapshotRepository.findTopByRoomIdOrderBySavedAtDesc(UUID.fromString(roomId));
    }

    public List<CodeSnapshot> getSnapshotHistory(String roomId) {
        return codeSnapshotRepository.findByRoomIdOrderBySavedAtDesc(UUID.fromString(roomId));
    }
}
