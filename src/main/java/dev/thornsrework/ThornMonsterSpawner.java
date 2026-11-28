package dev.thornsrework;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;

import java.util.ArrayList;
import java.util.List;

/** Adds one set of custom Thorns armor during eligible natural mob initialization. */
public final class ThornMonsterSpawner {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static final Item[][] BEYOND_ARMOR = {
            {Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS},
            {Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS},
            {Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE},
            {Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET}
    };
    private static final Item[] OVERLORD_ARMOR = {
            Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET
    };

    private ThornMonsterSpawner() {
    }

    public static void onNaturalSpawn(MobEntity mob, ServerWorldAccess access, Difficulty difficulty,
                                      SpawnReason reason) {
        if ((mob.getType() != EntityType.ZOMBIE && mob.getType() != EntityType.SKELETON)
                || difficulty != Difficulty.HARD || reason != SpawnReason.NATURAL) {
            return;
        }

        ServerWorld world = access.toServerWorld();
        Random random = access.getRandom();
        double beyond = DebugMode.beyondSpawnPercent() / 100.0;
        double overlord = DebugMode.overlordSpawnPercent() / 100.0;
        // Use independent rolls so changing one rate never silently changes the other.
        // If the very rare rolls overlap, the stronger Overlord tier takes precedence.
        boolean rollBeyond = random.nextDouble() < beyond;
        boolean rollOverlord = random.nextDouble() < overlord;

        if (!rollBeyond && !rollOverlord) {
            return;
        }

        boolean isOverlord = rollOverlord;
        Item[][] choices = isOverlord ? null : BEYOND_ARMOR;
        int pieces = isOverlord ? random.nextBetween(2, 4) : random.nextBetween(2, 3);
        int minimumLevel = isOverlord ? 3 : 1;
        int maximumLevel = isOverlord ? 3 : 2;
        var thornsEntry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.THORNS).orElseThrow();

        List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3));
        for (int index = slots.size() - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            int current = slots.get(index);
            slots.set(index, slots.get(other));
            slots.set(other, current);
        }

        for (int position = 0; position < pieces; position++) {
            int armorIndex = slots.get(position);
            Item item = isOverlord ? OVERLORD_ARMOR[armorIndex]
                    : choices[armorIndex][random.nextInt(choices[armorIndex].length)];
            int level = minimumLevel == maximumLevel ? minimumLevel
                    : random.nextBetween(minimumLevel, maximumLevel);
            ItemStack stack = new ItemStack(item);
            stack.addEnchantment(thornsEntry, level);
            EquipmentSlot slot = ARMOR_SLOTS[armorIndex];
            mob.equipStack(slot, stack);
            mob.setEquipmentDropChance(slot, 0.0F);
        }

        if (DebugMode.enabled()) {
            ThornsRework.LOGGER.info("Spawned Thorns Rework {}: entity={}, pieces={}, levelRange={}..{}",
                    isOverlord ? "Overlord" : "Beyond", mob.getType(), pieces, minimumLevel, maximumLevel);
        }
    }
}

