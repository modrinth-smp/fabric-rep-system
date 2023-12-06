package io.github.modrinthsmp.fabricrepsystem.mixin;

import io.github.modrinthsmp.fabricrepsystem.RepUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftDedicatedServer.class)
public class MinecraftDedicatedServerMixin {
    @Inject(method = "isSpawnProtected", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getSpawnPos()Lnet/minecraft/util/math/BlockPos;"), cancellable = true)
    public void fabricRepSystem$isSpawnProtected(ServerWorld world, BlockPos pos, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (
            RepUtils.getConfig().getMinSpawnBuildingRep() != null &&
                RepUtils.getPlayerReputation(player.getUuid()).getReputation() >= RepUtils.getConfig().getMinSpawnBuildingRep()
        ) {
            cir.setReturnValue(false);
        }
    }
}
