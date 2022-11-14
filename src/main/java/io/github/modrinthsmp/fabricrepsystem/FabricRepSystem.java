package io.github.modrinthsmp.fabricrepsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.EntitySelector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;

public class FabricRepSystem implements ModInitializer {
    public static final Gson GSON = new GsonBuilder().create();
    public static final Logger LOGGER = LogUtils.getLogger();
    public Map<UUID, ReputationData> reputation;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try (final Reader reader = Files.newBufferedReader(getReputationPath(server), StandardCharsets.UTF_8)) {
                reputation = GSON.fromJson(reader, new TypeToken<Map<UUID, ReputationData>>() {}.getType());
            } catch (Exception e) {
                LOGGER.error("Failed to load reputation", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!reputation.containsKey(handler.getPlayer().getUuid())) {
                reputation.put(handler.getPlayer().getUuid(), new ReputationData().setReputation(0));
                LOGGER.info("Added a reputation of zero for player with UUID: " + handler.getPlayer().getUuid());
            } else {
                LOGGER.info("Player with UUID: " + handler.getPlayer().getUuid() + ", joined with reputation: " + reputation.get(handler.getPlayer().getUuid()));
            }
        });

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

    public static Path getReputationPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("reputation.json");
    }

    public static int repView(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity entity) {
        return 1;
    }
}
