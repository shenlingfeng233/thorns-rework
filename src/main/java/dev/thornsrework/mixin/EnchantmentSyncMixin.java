package dev.thornsrework.mixin;

import net.minecraft.component.ComponentMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.SerializableRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SerializableRegistries.class)
public abstract class EnchantmentSyncMixin {
    // Vanilla clients decode the full enchantment record but do not execute its server effects.
    @Redirect(method = "method_56595", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/registry/entry/RegistryEntry$Reference;value()Ljava/lang/Object;"))
    private static Object thornsrework$omitServerEffects(RegistryEntry.Reference<?> entry) {
        Object value = entry.value();
        if (value instanceof Enchantment enchantment && entry.registryKey().equals(Enchantments.THORNS)) {
            return new Enchantment(enchantment.description(), enchantment.definition(), enchantment.exclusiveSet(),
                    ComponentMap.EMPTY);
        }
        return value;
    }
}
