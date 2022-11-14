package io.github.modrinthsmp.fabricrepsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.argument.GameProfileArgumentType.gameProfile;
import static net.minecraft.command.argument.GameProfileArgumentType.getProfileArgument;

public class FabricRepSystem implements ModInitializer {
    public static final Gson GSON = new GsonBuilder().create();
    public static final Logger LOGGER = LogUtils.getLogger();
    public static Map<UUID, ReputationData> reputation = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try (final Reader reader = Files.newBufferedReader(getReputationPath(server), StandardCharsets.UTF_8)) {
                reputation = GSON.fromJson(reader, new TypeToken<Map<UUID, ReputationData>>() {}.getType());
            } catch (IOException e) {
                LOGGER.error("Failed to load reputation", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!reputation.containsKey(handler.getPlayer().getUuid())) {
                reputation.put(handler.getPlayer().getUuid(), new ReputationData());
                writeRep(server);
                LOGGER.info("Added a reputation of zero for player with UUID: " + handler.getPlayer().getUuid());
            } else {
                LOGGER.info("Player with UUID: " + handler.getPlayer().getUuid() + ", joined with reputation: " + reputation.get(handler.getPlayer().getUuid()));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> writeRep(server));

        ServerTickEvents.END_SERVER_TICK.register(server -> {

        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("rep")
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("view")
                    .executes(ctx -> repView(ctx, List.of(ctx.getSource().getPlayer().getGameProfile())))
                    .then(RequiredArgumentBuilder.<ServerCommandSource, GameProfileArgumentType.GameProfileArgument>argument("player", gameProfile())
                        .executes(ctx -> repView(ctx, getProfileArgument(ctx, "player")))
                    )
                )
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("set")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("rep", integer())
                        .executes(ctx -> repSet(ctx, List.of(ctx.getSource().getPlayer().getGameProfile())))
                    )
                    .then(RequiredArgumentBuilder.<ServerCommandSource, GameProfileArgumentType.GameProfileArgument>argument("player", gameProfile())
                        .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("rep", integer())
                            .executes(ctx -> repSet(ctx, getProfileArgument(ctx, "player")))
                        )
                    )
                )
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("add")
                    .then(RequiredArgumentBuilder.<ServerCommandSource, GameProfileArgumentType.GameProfileArgument>argument("player", gameProfile())
                        .executes(ctx -> repAdd(ctx, getProfileArgument(ctx, "player"), +1))
                    )
                )
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("remove")
                    .then(RequiredArgumentBuilder.<ServerCommandSource, GameProfileArgumentType.GameProfileArgument>argument("player", gameProfile())
                        .executes(ctx -> repAdd(ctx, getProfileArgument(ctx, "player"), -1))
                    )
                )
        ));
    }

    public static Path getReputationPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("reputation.json");
    }

    public static int repView(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles) {
        int rep = 0;
        for (final GameProfile profile : profiles) {
            final ReputationData repData = reputation.get(profile.getId());
            final int thisRep = repData == null ? 0 : repData.getReputation();
            ctx.getSource().sendFeedback(
                Text.of(profile.getName() + " has " + thisRep + " reputation."),
                false
            );
            rep += thisRep;
        }
        return rep;
    }

    public static int repSet(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles) {
        final int rep = getInteger(ctx, "rep");
        for (final GameProfile profile : profiles) {
            final ReputationData repData = reputation.computeIfAbsent(profile.getId(), key -> new ReputationData());
            repData.setReputation(rep);
            ctx.getSource().sendFeedback(
                Text.of("Set " + profile.getName() + "'s reputation to " + rep + "."),
                true
            );
        }
        return profiles.size();
    }

    public static int repAdd(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles, int amount) {
        for (final GameProfile profile : profiles) {
            final ReputationData repData = reputation.computeIfAbsent(profile.getId(), key -> new ReputationData());
            repData.addReputation(amount);
            ctx.getSource().sendFeedback(
                Text.of("Voted " + profile.getName() + " reputation " + (amount > 0 ? "+" : "") + amount + "."),
                true
            );
        }
        return profiles.size();
    }

    public static void writeRep(MinecraftServer server) {
        try (final Writer writer = Files.newBufferedWriter(getReputationPath(server), StandardCharsets.UTF_8)) {
            GSON.toJson(reputation, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write reputation", e);
        }
    }
}
