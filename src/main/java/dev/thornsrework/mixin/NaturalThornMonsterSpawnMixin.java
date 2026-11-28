package dev.thornsrework.mixin;

import dev.thornsrework.ThornMonsterSpawner;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeletonEntity.class)
public abstract class NaturalThornMonsterSpawnMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private void thornsrework$applyNaturalThorns(ServerWorldAccess world, LocalDifficulty localDifficulty,
                                                 SpawnReason reason, EntityData entityData,
                                                 CallbackInfoReturnable<EntityData> callback) {
        ThornMonsterSpawner.onNaturalSpawn((MobEntity) (Object) this, world,
                localDifficulty.getGlobalDifficulty(), reason);
    }
}
