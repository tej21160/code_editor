package com.collabeditor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "code_snapshots")
public class CodeSnapshot {
    @Id
    private UUID id;
    
    private UUID roomId;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private Long version;
    
    private LocalDateTime savedAt;
    
    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}
