package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.item.Inventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryTest {

    @Test
    void addAndSerialize() {
        String inv = Inventory.add("", "筑基丹", 1);
        inv = Inventory.add(inv, "灵尘", 3);
        inv = Inventory.add(inv, "筑基丹", 2);
        assertEquals("筑基丹×3;灵尘×3", inv);
    }

    @Test
    void consumeReducesOrRemoves() {
        String inv = "筑基丹×2;灵尘×3";
        assertEquals("筑基丹×1;灵尘×3", Inventory.consume(inv, "筑基丹", 1));
        assertEquals("灵尘×3", Inventory.consume(inv, "筑基丹", 2));
    }

    @Test
    void consumeInsufficientReturnsNull() {
        assertNull(Inventory.consume("灵尘×1", "筑基丹", 1));
        assertNull(Inventory.consume("灵尘×1", "灵尘", 2));
    }

    @Test
    void displayEmpty() {
        assertEquals("空空如也", Inventory.display(""));
        assertEquals("灵尘×2", Inventory.display("灵尘×2"));
    }
}
