package io.github.modrinthsmp.fabricrepsystem.mixin;

import io.github.modrinthsmp.fabricrepsystem.RepUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
    @Inject(
        method = "isUnderSpawnProtection",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getSharedSpawnPos()Lnet/minecraft/core/BlockPos;"
        ),
        cancellable = true
    )
    public void fabricRepSystem$isSpawnProtected(ServerLevel world, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (
            RepUtils.getConfig().getMinSpawnBuildingRep() != null &&
                RepUtils.getPlayerReputation(player.getUUID()).getReputation() >= RepUtils.getConfig().getMinSpawnBuildingRep()
        ) {
            cir.setReturnValue(false);
        }
    }
}
