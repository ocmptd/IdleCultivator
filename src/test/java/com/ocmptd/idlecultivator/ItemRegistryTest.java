package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.item.ItemRegistry;
import com.ocmptd.idlecultivator.game.item.ItemType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemRegistryTest {

    @Test
    void shopItemsAvailable() {
        var shopItems = ItemRegistry.shopItems();
        assertTrue(shopItems.size() >= 4, "Should have at least 4 shop items");
        assertTrue(shopItems.stream().anyMatch(i -> i.name().equals("加速丹")));
        assertTrue(shopItems.stream().anyMatch(i -> i.name().equals("聚灵丹")));
        assertTrue(shopItems.stream().anyMatch(i -> i.name().equals("筑基丹")));
        assertTrue(shopItems.stream().anyMatch(i -> i.name().equals("回气丹")));
    }

    @Test
    void overflowProtectDelisted() {
        assertFalse(ItemRegistry.exists("溢出保护符"));
        assertFalse(ItemRegistry.shopItems().stream().anyMatch(i -> i.name().equals("溢出保护符")));
    }

    @Test
    void materialsNotInShop() {
        var shopItems = ItemRegistry.shopItems();
        assertFalse(shopItems.stream().anyMatch(i -> i.name().equals("灵尘")));
        assertFalse(shopItems.stream().anyMatch(i -> i.name().equals("残破法宝")));
    }

    @Test
    void getItemReturnsDef() {
        ItemRegistry.ItemDef def = ItemRegistry.get("加速丹");
        assertNotNull(def);
        assertEquals("加速丹", def.name());
        assertEquals(ItemType.CONSUMABLE, def.type());
        assertEquals(200, def.price());
        assertTrue(def.shopAvailable());
    }

    @Test
    void unknownItemReturnsNull() {
        assertNull(ItemRegistry.get("不存在的道具"));
    }

    @Test
    void describeShopContainsItems() {
        String desc = ItemRegistry.describeShop();
        assertTrue(desc.contains("灵宝阁"));
        assertTrue(desc.contains("加速丹"));
        assertTrue(desc.contains("200"));
        assertFalse(desc.contains("灵尘"));
    }

    @Test
    void pillItemsHaveCorrectType() {
        assertEquals(ItemType.PILL, ItemRegistry.get("聚灵丹").type());
        assertEquals(ItemType.PILL, ItemRegistry.get("筑基丹").type());
        assertEquals(ItemType.PILL, ItemRegistry.get("回气丹").type());
    }

    @Test
    void materialItemsHaveCorrectType() {
        assertEquals(ItemType.MATERIAL, ItemRegistry.get("灵尘").type());
        assertEquals(ItemType.MATERIAL, ItemRegistry.get("残破法宝").type());
    }
}
