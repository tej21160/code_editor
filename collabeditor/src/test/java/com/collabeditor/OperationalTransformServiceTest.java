package com.collabeditor;

import com.collabeditor.service.OperationalTransformService;
import com.collabeditor.model.Operation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OperationalTransformServiceTest {

    private final OperationalTransformService otService = new OperationalTransformService();

    @Test
    public void testInsertInsert_differentPositions() {
        Operation a = new Operation("insert", 5, "A", 0, 1, "user1", "room1");
        Operation b = new Operation("insert", 10, "B", 0, 1, "user2", "room1");
        Operation transformed = otService.transform(a, b);
        assertEquals(5, transformed.getPosition());
    }

    @Test
    public void testInsertInsert_samePosition() {
        Operation a = new Operation("insert", 5, "A", 0, 1, "user2", "room1");
        Operation b = new Operation("insert", 5, "B", 0, 1, "user1", "room1");
        Operation transformed = otService.transform(a, b);
        assertEquals(5 + "B".length(), transformed.getPosition());
    }

    @Test
    public void testInsertDelete_insertAfterDelete() {
        Operation a = new Operation("insert", 8, "X", 0, 1, "user1", "room1");
        Operation b = new Operation("delete", 2, "", 3, 1, "user2", "room1");
        Operation transformed = otService.transform(a, b);
        assertEquals(Math.max(b.getPosition(), a.getPosition() - b.getLength()), transformed.getPosition());
    }

    @Test
    public void testDeleteDelete_samePosition() {
        Operation a = new Operation("delete", 5, "", 4, 1, "user1", "room1");
        Operation b = new Operation("delete", 5, "", 2, 1, "user2", "room1");
        Operation transformed = otService.transform(a, b);
        assertEquals(Math.max(0, a.getLength() - b.getLength()), transformed.getLength());
    }

    @Test
    public void testApplyOperation_insert() {
        String result = otService.applyOperation("hello world",
                new Operation("insert", 5, " beautiful", 0, 1, "user1", "room1"));
        assertEquals("hello beautiful world", result);
    }

    @Test
    public void testApplyOperation_delete() {
        String result = otService.applyOperation("hello world",
                new Operation("delete", 0, "", 6, 1, "user1", "room1"));
        assertEquals("world", result);
    }
}
