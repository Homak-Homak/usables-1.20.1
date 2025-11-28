package silly.homak.usables.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import silly.homak.usables.UsablesMain;
import silly.homak.usables.custom.item.PocketBarrierItem;

public class UsableItems {
    public static final Item POCKET_BARRIER = registerItem("pocket_barrier",
            new PocketBarrierItem(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(UsablesMain.MOD_ID, name), item);
    }
    private static void addToItemGroup(RegistryKey<ItemGroup> group, Item item) {
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> {
            entries.add(item);
        });
    }
    public static void init() {
        addToItemGroup(ItemGroups.COMBAT, POCKET_BARRIER);
    }
}
