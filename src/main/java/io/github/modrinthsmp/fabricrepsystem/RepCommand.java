package io.github.modrinthsmp.fabricrepsystem;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.quiltmc.parsers.json.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class RepCommand {
    private static final SimpleCommandExceptionType VOTE_YOURSELF_EXCEPTION = new SimpleCommandExceptionType(
        Component.literal("Cannot vote on yourself.")
    );
    private static final Dynamic2CommandExceptionType VOTE_FOR_AGAIN_EXCEPTION = new Dynamic2CommandExceptionType(
        (player, whenInMillis) -> Component.literal(
            "You can vote for " + player + " again " +
                Util.formatTimeDifference((long)whenInMillis)
        )
    );
    private static final DynamicCommandExceptionType LOOK_FOR_UNWANTED_EXCEPTION = new DynamicCommandExceptionType(
        player -> Component.empty()
            .append(((ServerPlayer)player).getDisplayName())
            .append(" is not currently wanted")
    );
    private static final SimpleCommandExceptionType REASON_REQUIRED_EXCEPTION = new SimpleCommandExceptionType(
        Component.literal("A reason is required, but you didn't specify one.")
    );
    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("0.0");

    private RepCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("rep")
            .then(literal("view")
                .executes(ctx -> repView(ctx, List.of(ctx.getSource().getPlayerOrException().getGameProfile())))
                .then(argument("player", GameProfileArgument.gameProfile())
                    .executes(ctx -> repView(ctx, GameProfileArgument.getGameProfiles(ctx, "player")))
                )
            )
            .then(literal("set")
                .requires(source -> source.hasPermission(2))
                .then(argument("rep", IntegerArgumentType.integer())
                    .executes(ctx -> repSet(ctx, List.of(ctx.getSource().getPlayerOrException().getGameProfile())))
                )
                .then(argument("player", GameProfileArgument.gameProfile())
                    .then(argument("rep", IntegerArgumentType.integer())
                        .executes(ctx -> repSet(ctx, GameProfileArgument.getGameProfiles(ctx, "player")))
                    )
                )
            )
            .then(literal("upvote")
                .then(argument("player", GameProfileArgument.gameProfile())
                    .executes(ctx -> repAdd(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), +1, null))
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> repAdd(
                            ctx,
                            GameProfileArgument.getGameProfiles(ctx, "player"),
                            +1,
                            StringArgumentType.getString(ctx, "reason")
                        ))
                    )
                )
            )
            .then(literal("downvote")
                .then(argument("player", GameProfileArgument.gameProfile())
                    .executes(ctx -> repAdd(ctx, GameProfileArgument.getGameProfiles(ctx, "player"), -1, null))
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> repAdd(
                            ctx,
                            GameProfileArgument.getGameProfiles(ctx, "player"),
                            -1,
                            StringArgumentType.getString(ctx, "reason")
                        ))
                    )
                )
            )
            .then(literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    RepUtils.readConfig();
                    ctx.getSource().sendSuccess(() -> Component.literal("Successfully reloaded reputation config"), true);
                    return 1;
                })
            )
            .then(literal("wanted")
                .then(argument("player", EntityArgument.player())
                    .executes(ctx -> getWanted(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
            )
        );
    }

    private static int getWanted(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        if (
            RepUtils.getConfig().getMaxWantedRep() == null ||
                FabricRepSystem.reputation.get(player.getUUID()).getReputation() > RepUtils.getConfig().getMaxWantedRep()
        ) {
            throw LOOK_FOR_UNWANTED_EXCEPTION.create(player);
        }
        final Vec3 pos = player.position();
        ctx.getSource().sendSuccess(
            () -> Component.empty()
                .append(player.getDisplayName())
                .append(
                    " is at X: " + COORD_FORMAT.format(pos.x) +
                        " Y: " + COORD_FORMAT.format(pos.y) +
                        " Z: " + COORD_FORMAT.format(pos.z)
                ),
            false
        );
        return 0;
    }

    private static int repView(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> profiles) {
        int rep = 0;
        for (final GameProfile profile : profiles) {
            final ReputationData repData = FabricRepSystem.reputation.get(profile.getId());
            final int thisRep = repData == null ? 0 : repData.getReputation();
            ctx.getSource().sendSuccess(
                () -> Component.literal(profile.getName() + " has " + thisRep + " reputation."),
                false
            );
            rep += thisRep;
        }
        return rep;
    }

    private static int repSet(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> profiles) {
        final int rep = IntegerArgumentType.getInteger(ctx, "rep");
        for (final GameProfile profile : profiles) {
            final ReputationData repData = RepUtils.getPlayerReputation(profile.getId());
            repData.setReputation(rep);
            ctx.getSource().sendSuccess(
                () -> Component.literal("Set " + profile.getName() + "'s reputation to " + repData.getReputation()),
                true
            );
        }
        return profiles.size();
    }

    private static int repAdd(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> profiles, int amount, String reason) throws CommandSyntaxException {
        if (RepUtils.getConfig().isVotingReasonRequired() && reason == null) {
            throw REASON_REQUIRED_EXCEPTION.create();
        }
        if (ctx.getSource().getEntity() != null) {
            final long time = System.currentTimeMillis();
            final ReputationData ownRepData = RepUtils.getPlayerReputation(ctx.getSource().getEntity().getUUID());
            for (final GameProfile profile : profiles) {
                if (!ctx.getSource().hasPermission(2) && profile.getId().equals(ctx.getSource().getEntity().getUUID())) {
                    throw VOTE_YOURSELF_EXCEPTION.create();
                }
                final Long lastVotedFor = ownRepData.getLastVotedFor().get(profile.getId());
                if (
                    lastVotedFor != null &&
                    lastVotedFor + RepUtils.getConfig().getCooldown() * 1000 > time
                ) {
                    throw VOTE_FOR_AGAIN_EXCEPTION.create(profile.getName(), lastVotedFor + RepUtils.getConfig().getCooldown() * 1000 - System.currentTimeMillis());
                }
            }
            for (final GameProfile profile : profiles) {
                ownRepData.getLastVotedFor().put(profile.getId(), time);
            }
        }
        for (final GameProfile profile : profiles) {
            final ReputationData repData = RepUtils.getPlayerReputation(profile.getId());
            repData.addReputation(amount);
            ctx.getSource().sendSuccess(
                () -> Component.literal("Voted " + profile.getName() + " reputation " + (amount > 0 ? "+" : "") + amount + "!"),
                false
            );
            FabricRepSystem.LOGGER.info(ctx.getSource().getTextName() + (amount > 0 ? " upvoted " : " downvoted ") + profile.getName() + " with reason: " + (reason == null ? "None Provided" : reason));
            if (RepUtils.getConfig().getDiscordWebhookUrl() != null) {
                final StringWriter stringWriter = new StringWriter();
                try (JsonWriter writer = JsonWriter.json(stringWriter)) {
                    writer.beginObject();
                    {
                        writer.name("embeds").beginArray();
                        {
                            writer.beginObject();
                            {
                                writer.name("title").value(
                                    ctx.getSource().getTextName() + (amount > 0 ? " upvoted " : " downvoted ") + profile.getName()
                                );
                                writer.name("description").value(reason);
                                writer.name("timestamp").value(Instant.now().toString());
                                writer.name("color").value(
                                    amount > 0 ? ChatFormatting.GREEN.getColor() : ChatFormatting.RED.getColor()
                                );
                                writer.name("footer").beginObject();
                                {
                                    writer.name("text").value(
                                        profile.getName() + "'s reputation is now " + repData.getReputation()
                                    );
                                    writer.name("icon_url").value(
                                        "https://crafatar.com/renders/head/" + profile.getId() + "?overlay=true"
                                    );
                                }
                                writer.endObject();
                            }
                            writer.endObject();
                        }
                        writer.endArray();
                    }
                    writer.endObject();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                FabricRepSystem.HTTP.sendAsync(
                    HttpRequest.newBuilder(RepUtils.getConfig().getDiscordWebhookUrl())
                        .header("User-Agent", FabricRepSystem.USER_AGENT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(stringWriter.toString()))
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                ).thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        FabricRepSystem.LOGGER.error("Failed to send Discord webhook (HTTP {}): {}", response.statusCode(), response.body());
                    }
                }).exceptionally(t -> {
                    FabricRepSystem.LOGGER.error("Failed to send Discord webhook", t);
                    return null;
                });
            }
            final ServerPlayer other = ctx.getSource().getServer().getPlayerList().getPlayer(profile.getId());
            if (other != null) {
                if (amount > 0 && RepUtils.getConfig().isUpvoteNotifications()) {
                    MutableComponent text = Component.literal("Your reputation was upvoted!");
                    if (RepUtils.getConfig().isShowReason()) {
                        text = text.append("Reason: " + (reason == null ? "None Provided" : reason));
                    }
                    other.sendSystemMessage(text);
                    other.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.5f, 1f);
                } else if (amount < 0 && RepUtils.getConfig().isDownvoteNotifications()) {
                    MutableComponent text = Component.literal("Your reputation was downvoted.");
                    if (RepUtils.getConfig().isShowReason()) {
                        text = text.append("Reason: " + (reason == null ? "None Provided" : reason));
                    }
                    other.sendSystemMessage(text);
                    other.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1, 1f);
                }
            }
        }
        return profiles.size();
    }
}
