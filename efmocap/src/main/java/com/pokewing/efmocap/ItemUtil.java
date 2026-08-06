package com.pokewing.efmocap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Converts visible equipment between live entities and recorded registry names. */
public final class ItemUtil {
    private ItemUtil() {}

    public static String nameOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    public static ItemStack stackOf(String name) {
        if (name == null || name.isEmpty()) return ItemStack.EMPTY;
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /** Copy the entity's visible equipment into a frame. */
    public static void record(LivingEntity e, MocapFrame f) {
        f.mainHand = nameOf(e.getItemBySlot(EquipmentSlot.MAINHAND));
        f.offHand  = nameOf(e.getItemBySlot(EquipmentSlot.OFFHAND));
        f.head     = nameOf(e.getItemBySlot(EquipmentSlot.HEAD));
        f.chest    = nameOf(e.getItemBySlot(EquipmentSlot.CHEST));
        f.legs     = nameOf(e.getItemBySlot(EquipmentSlot.LEGS));
        f.feet     = nameOf(e.getItemBySlot(EquipmentSlot.FEET));
    }

    /** Apply a frame's equipment onto a clone, only when it actually changed. */
    public static void apply(LivingEntity e, MocapFrame f) {
        set(e, EquipmentSlot.MAINHAND, f.mainHand);
        set(e, EquipmentSlot.OFFHAND, f.offHand);
        set(e, EquipmentSlot.HEAD, f.head);
        set(e, EquipmentSlot.CHEST, f.chest);
        set(e, EquipmentSlot.LEGS, f.legs);
        set(e, EquipmentSlot.FEET, f.feet);
    }

    private static void set(LivingEntity e, EquipmentSlot slot, String name) {
        if (nameOf(e.getItemBySlot(slot)).equals(name == null ? "" : name)) return;
        e.setItemSlot(slot, stackOf(name));
    }
}
