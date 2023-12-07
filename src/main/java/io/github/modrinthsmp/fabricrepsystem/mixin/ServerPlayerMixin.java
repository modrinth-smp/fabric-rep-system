package io.github.modrinthsmp.fabricrepsystem.mixin;

import io.github.modrinthsmp.fabricrepsystem.RepUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void fabricRepSystem$shouldDamagePlayer(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (RepUtils.getConfig().getMinPvPRep() == null) return; // No check
        if (
            source.getEntity() instanceof Player &&
                RepUtils.getPlayerReputation(source.getEntity().getUUID()).getReputation() < RepUtils.getConfig().getMinPvPRep()
        ) {
            cir.setReturnValue(false);
        }
    }
}
