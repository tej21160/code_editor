package com.collabeditor.service;

import com.collabeditor.model.Operation;
import org.springframework.stereotype.Service;

@Service
public class OperationalTransformService {

    // Transform operation A against operation B
    // Returns transformed A that can be applied after B
    public Operation transform(Operation a, Operation b) {
        if (a.getType().equals("insert") && b.getType().equals("insert")) {
            return transformInsertInsert(a, b);
        } else if (a.getType().equals("insert") && b.getType().equals("delete")) {
            return transformInsertDelete(a, b);
        } else if (a.getType().equals("delete") && b.getType().equals("insert")) {
            return transformDeleteInsert(a, b);
        } else {
            return transformDeleteDelete(a, b);
        }
    }

    private Operation transformInsertInsert(Operation a, Operation b) {
        Operation transformed = copyOperation(a);
        if (a.getPosition() > b.getPosition()) {
            transformed.setPosition(a.getPosition() + b.getText().length());
        } else if (a.getPosition() == b.getPosition() && 
                   a.getUserId().compareTo(b.getUserId()) > 0) {
            transformed.setPosition(a.getPosition() + b.getText().length());
        }
        return transformed;
    }

    private Operation transformInsertDelete(Operation a, Operation b) {
        Operation transformed = copyOperation(a);
        if (a.getPosition() > b.getPosition()) {
            transformed.setPosition(
                Math.max(b.getPosition(), a.getPosition() - b.getLength())
            );
        }
        return transformed;
    }

    private Operation transformDeleteInsert(Operation a, Operation b) {
        Operation transformed = copyOperation(a);
        if (a.getPosition() >= b.getPosition()) {
            transformed.setPosition(a.getPosition() + b.getText().length());
        }
        return transformed;
    }

    private Operation transformDeleteDelete(Operation a, Operation b) {
        Operation transformed = copyOperation(a);
        if (a.getPosition() > b.getPosition()) {
            transformed.setPosition(
                Math.max(b.getPosition(), a.getPosition() - b.getLength())
            );
        } else if (a.getPosition() == b.getPosition()) {
            transformed.setLength(Math.max(0, a.getLength() - b.getLength()));
        }
        return transformed;
    }

    private Operation copyOperation(Operation op) {
        return new Operation(
            op.getType(), op.getPosition(), op.getText(),
            op.getLength(), op.getVersion(), op.getUserId(), op.getRoomId()
        );
    }

    // Apply operation to document content
    public String applyOperation(String content, Operation op) {
        if (content == null) content = "";
        try {
            if (op.getType().equals("insert")) {
                int pos = Math.min(op.getPosition(), content.length());
                return content.substring(0, pos) + op.getText() + 
                       content.substring(pos);
            } else if (op.getType().equals("delete")) {
                int start = Math.min(op.getPosition(), content.length());
                int end = Math.min(start + op.getLength(), content.length());
                return content.substring(0, start) + content.substring(end);
            }
        } catch (Exception e) {
            return content;
        }
        return content;
    }
}
