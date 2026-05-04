package com.collabeditor.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Operation {
    private String type;      // "insert" or "delete"
    private int position;     // where in the document
    private String text;      // text to insert (for insert ops)
    private int length;       // length to delete (for delete ops)
    private long version;     // document version when op was created
    private String userId;    // who made this change
    private String roomId;
}
