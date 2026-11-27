package dev.thornsrework.mixin;

import dev.thornsrework.DebugMode;
import dev.thornsrework.RetaliationEffect;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.effect.AllOfEnchantmentEffects;
import net.minecraft.enchantment.effect.TargetedEnchantmentEffect;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Enchantment.class)
public abstract class EnchantmentDebugMixin {
    @Redirect(method = "applyTargetedEffect", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/enchantment/effect/TargetedEnchantmentEffect;test(Lnet/minecraft/loot/context/LootContext;)Z"))
    private static boolean thornsrework$observeRoll(TargetedEnchantmentEffect<?> effect, LootContext context) {
        boolean passed = effect.test(context);
        if (DebugMode.enabled() && effect.effect() instanceof AllOfEnchantmentEffects.EntityEffects all
                && all.effects().contains(RetaliationEffect.INSTANCE)) {
            DamageSource source = context.get(LootContextParameters.DAMAGE_SOURCE);
            int level = context.get(LootContextParameters.ENCHANTMENT_LEVEL);
            if (!passed) {
                DebugMode.trace(context.getWorld(), level, source.getAttacker(), "CHANCE_MISS", 0, 0,
                        0.15F + 0.15F * (level - 1), 0);
            } else if (source.getAttacker() == null) {
                DebugMode.trace(context.getWorld(), level, source.getSource(), "SKIP_NO_ATTACKER", 0, 0, 0, 0);
            }
        }
        return passed;
    }
}
