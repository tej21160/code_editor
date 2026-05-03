package com.collabeditor.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeChangeMessage {
    private String roomId;
    private String userId;
    private String username;
    private String content;
    private Long version;
    private LocalDateTime timestamp;
}
