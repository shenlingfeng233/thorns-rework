package dev.thornsrework;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public enum RetaliationEffect implements EnchantmentEntityEffect {
    INSTANCE;

    public static final MapCodec<RetaliationEffect> CODEC = MapCodec.unit(INSTANCE);
    public static final int DURABILITY_COST_PER_LEVEL = 3;
    public static final float MIN_DAMAGE = 1.0F;
    public static final float MAX_DAMAGE = 5.0F;

    @Override
    public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity target, Vec3d pos) {
        if (target instanceof PlayerEntity player && player.isCreative()) {
            DebugMode.trace(world, level, target, "SKIP_CREATIVE", 0, 0, 0, 0);
            return;
        }
        if (!(target instanceof LivingEntity attacker)) {
            DebugMode.trace(world, level, target, "SKIP_NON_LIVING", 0, 0, 0, 0);
            return;
        }
        ItemStack stack = attacker.getMainHandStack();
        if (stack.isEmpty()) {
            float before = attacker.getHealth();
            float expected = MathHelper.nextBetween(attacker.getRandom(), MIN_DAMAGE, MAX_DAMAGE);
            attacker.damage(world.getDamageSources().thorns(context.owner()), expected);
            float after = attacker.getHealth();
            DebugMode.trace(world, level, "minecraft:air", "B_HEALTH", before, after, expected, before - after);
            return;
        }
        if (stack.contains(DataComponentTypes.UNBREAKABLE)) {
            DebugMode.trace(world, level, target, "SKIP_UNBREAKABLE", stack.getDamage(), stack.getDamage(), 0, 0);
            return;
        }
        if (stack.isDamageable()) {
            damageMainHand(world, level, attacker, stack);
            return;
        }
        int before = stack.getCount();
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            boolean dropped = attacker.dropStack(stack.copyWithCount(1)) != null;
            if (dropped) {
                stack.decrement(1);
            }
            DebugMode.trace(world, level, "minecraft:enchanted_book", "C_PRIME_DROP", before,
                    stack.getCount(), 1, before - stack.getCount());
            return;
        }
        Item item = stack.getItem();
        stack.decrement(1);
        if (DebugMode.enabled()) {
            DebugMode.trace(world, level, Registries.ITEM.getId(item).toString(), "C_CONSUME", before,
                    stack.getCount(), 1, before - stack.getCount());
        }
    }

    private static void damageMainHand(ServerWorld world, int level, LivingEntity attacker, ItemStack stack) {
        Item item = stack.getItem();
        int before = stack.getDamage();
        int maximum = stack.getMaxDamage();
        int cost = DURABILITY_COST_PER_LEVEL * level;
        int after = before + Math.min(Math.max(0, maximum - before), cost);
        if (attacker instanceof ServerPlayerEntity player) {
            Criteria.ITEM_DURABILITY_CHANGED.trigger(player, stack, after);
        }
        stack.setDamage(after);
        boolean broke = after >= maximum;
        if (broke) {
            stack.decrement(1);
            attacker.sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND);
        }
        if (DebugMode.enabled()) {
            DebugMode.trace(world, level, Registries.ITEM.getId(item).toString(), broke ? "A_BREAK" : "A_DURABILITY",
                    before, after, cost, after - before);
        }
    }

    @Override
    public MapCodec<RetaliationEffect> getCodec() {
        return CODEC;
    }
}
