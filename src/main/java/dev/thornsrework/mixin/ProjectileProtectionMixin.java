package dev.thornsrework.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentHelper.class)
public abstract class ProjectileProtectionMixin {
    @Unique
    private static final EquipmentSlot[] THORNSREWORK_SLOTS = EquipmentSlot.values();

    // This lambda is the per-entry consumer used by getProtectionAmount in 1.21.1.
    // Keep the first contribution in vanilla equipment order and leave other enchantments alone.
    @Inject(method = "method_60115", at = @At("HEAD"), cancellable = true)
    private static void thornsrework$applyOnce(ServerWorld world, LivingEntity wearer, DamageSource source,
                                              MutableFloat total, RegistryEntry<Enchantment> enchantment,
                                              int level, EnchantmentEffectContext context, CallbackInfo ci) {
        if (!enchantment.matchesKey(Enchantments.THORNS) || !source.isIn(DamageTypeTags.IS_PROJECTILE)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        for (EquipmentSlot slot : THORNSREWORK_SLOTS) {
            if (slot == context.slot()) {
                return;
            }
            if (enchantment.value().slotMatches(slot)
                    && EnchantmentHelper.getLevel(enchantment, wearer.getEquippedStack(slot)) > 0) {
                ci.cancel();
                return;
            }
        }
    }
}
