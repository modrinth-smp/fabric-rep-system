package io.github.modrinthsmp.fabricrepsystem.mixin;

import io.github.modrinthsmp.fabricrepsystem.RepUtils;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void fabricRepSystem$shouldDamagePlayer(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof PlayerEntity && RepUtils.getPlayerReputation(source.getAttacker().getUuid()).getReputation() < RepUtils.getConfig().getMinPvPRep()) {
            cir.setReturnValue(false);
        }
    }
}
