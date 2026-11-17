package dev.hammerandhearth;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/** Items for the slow bloomery-to-forged-weapon progression. */
public final class ForgingItems {
    public static final Item COPPER_CHUNK = item("native_copper", 64);
    public static final Item COPPER_BLOOM = item("copper_bloom", 64);
    public static final Item COPPER_INGOT = item("copper_ingot", 64);
    public static final Item IRON_BLOOM = item("iron_bloom", 64);
    public static final Item CRUCIBLE = crucible();
    public static final Item FORGE_HAMMER = durableItem("forge_hammer", 360);
    public static final Item COPPER_BLADE = item("copper_blade_blank", 16);
    public static final Item IRON_BLADE = item("iron_blade_blank", 16);
    public static final Item FORGED_COPPER_SWORD = sword("forged_copper_sword", ToolMaterial.COPPER, 2.5f, -2.8f, 160);
    public static final Item FORGED_IRON_SWORD = sword("forged_iron_sword", ToolMaterial.IRON, 3.5f, -2.6f, 250);

    private ForgingItems() {}

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(HammerAndHearth.MOD_ID, path));
    }

    private static Item item(String path, int stackSize) {
        ResourceKey<Item> key = key(path);
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(stackSize)));
    }

    private static Item durableItem(String path, int durability) {
        ResourceKey<Item> key = key(path);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new ForgeHammerItem(new Item.Properties().setId(key).stacksTo(1).durability(durability)));
    }

    private static Item crucible() {
        ResourceKey<Item> key = key("crucible");
        return Registry.register(BuiltInRegistries.ITEM, key,
                new CrucibleItem(new Item.Properties().setId(key).stacksTo(1).durability(16)));
    }

    private static Item sword(String path, ToolMaterial material, float damage, float speed, int durability) {
        ResourceKey<Item> key = key(path);
        Item.Properties settings = new Item.Properties().setId(key).sword(material, damage, speed).durability(durability);
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(settings));
    }

    public static void register() {
        // Class initialization registers the items before recipes are loaded.
    }
}
