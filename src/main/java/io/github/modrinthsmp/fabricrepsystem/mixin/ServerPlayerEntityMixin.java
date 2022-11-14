package io.github.modrinthsmp.fabricrepsystem.mixin;

import io.github.modrinthsmp.fabricrepsystem.RepUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "shouldDamagePlayer", at = @At("RETURN"), cancellable = true)
    public void fabricRepSystem$shouldDamagePlayer(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        final Integer minPvpRep = RepUtils.getConfig().getMinPvPRep();
        if (minPvpRep == null) return;
        if (RepUtils.getPlayerReputation(player.getUuid()).getReputation() < minPvpRep) {
            cir.setReturnValue(false);
        }
    }
}
