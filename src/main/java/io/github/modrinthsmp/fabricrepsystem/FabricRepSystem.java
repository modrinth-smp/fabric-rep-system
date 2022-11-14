package io.github.modrinthsmp.fabricrepsystem;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.EntitySelector;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;

public class FabricRepSystem implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("rep")
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("view")
                    .executes(ctx -> repView(ctx, ctx.getSource().getPlayer()))
                    .then(RequiredArgumentBuilder.<ServerCommandSource, EntitySelector>argument("player", player())
                        .executes(ctx -> repView(ctx, getPlayer(ctx, "player")))
                    )
                )
            );
        });
    }

    public static int repView(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity entity) {
        return 1;
    }
}
