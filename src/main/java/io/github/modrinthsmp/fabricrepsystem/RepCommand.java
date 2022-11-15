package io.github.modrinthsmp.fabricrepsystem;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.command.argument.GameProfileArgumentType.gameProfile;
import static net.minecraft.command.argument.GameProfileArgumentType.getProfileArgument;

public final class RepCommand {
    private static final SimpleCommandExceptionType VOTE_YOURSELF_EXCEPTION = new SimpleCommandExceptionType(
        Text.of("Cannot vote on yourself.")
    );
    private static final Dynamic2CommandExceptionType VOTE_FOR_AGAIN_EXCEPTION = new Dynamic2CommandExceptionType(
        (player, whenInMillis) -> new LiteralText(
            "You can vote for " + player + " again " +
                Util.formatTimeDifference((long)whenInMillis)
        )
    );
    private static final DynamicCommandExceptionType LOOK_FOR_UNWANTED_EXCEPTION = new DynamicCommandExceptionType(
            (player) -> new LiteralText(
                    ((ServerPlayerEntity)player).getName() +
                            " is currently unwanted."
            )
    );

    private RepCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
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
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("upvote")
                .then(RequiredArgumentBuilder.<ServerCommandSource, GameProfileArgumentType.GameProfileArgument>argument("player", gameProfile())
                        .then(voteConfigReader())
                            .executes(ctx -> repAdd(ctx, getProfileArgument(ctx, "player"), +1))
                )
            )
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("downvote")
                .then(RequiredArgumentBuilder.<ServerCommandSource, GameProfileArgumentType.GameProfileArgument>argument("player", gameProfile())
                    .executes(ctx -> repAdd(ctx, getProfileArgument(ctx, "player"), -1))
                )
            )
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("reload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> {
                    RepUtils.readConfig();
                    ctx.getSource().sendFeedback(Text.of("Successfully reloaded reputation config"), true);
                    return 1;
                })
            )
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("wanted")
                    .then(RequiredArgumentBuilder.argument("player", player()))
                    .executes(ctx -> getWanted(ctx, getPlayer(ctx, "player")))
            )
        );
    }

    private static ArgumentBuilder<ServerCommandSource,?> voteConfigReader() {
        if (RepUtils.getConfig().isVotingReasonRequired()) {
            return RequiredArgumentBuilder.argument("reason", string());
        }
        return CommandManager.argument("reason", string());
    }

    private static int getWanted(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) throws CommandSyntaxException {
        if (RepUtils.getConfig().getMaxWantedRep() == null)
            throw NOT_ALLOWED_EXCEPTION.create();
        if (FabricRepSystem.reputation.get(player.getUuid()).getReputation() > RepUtils.getConfig().getMaxWantedRep())
            throw LOOK_FOR_UNWANTED_EXCEPTION.create(player);
        ctx.getSource().sendFeedback(
                Text.of(player.getPos().toString()), false
        );
        return 0;
    }

    private static int repView(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles) {
        int rep = 0;
        for (final GameProfile profile : profiles) {
            final ReputationData repData = FabricRepSystem.reputation.get(profile.getId());
            final int thisRep = repData == null ? 0 : repData.getReputation();
            ctx.getSource().sendFeedback(
                Text.of(profile.getName() + " has " + thisRep + " reputation."),
                false
            );
            rep += thisRep;
        }
        return rep;
    }

    private static int repSet(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles) {
        final int rep = getInteger(ctx, "rep");
        for (final GameProfile profile : profiles) {
            RepUtils.getPlayerReputation(profile.getId()).setReputation(rep);
            ctx.getSource().sendFeedback(
                Text.of("Set " + profile.getName() + "'s reputation to " + rep),
                true
            );
        }
        return profiles.size();
    }

    private static int repAdd(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles, int amount) throws CommandSyntaxException {
        if (ctx.getSource().getEntity() != null) {
            final long time = System.currentTimeMillis();
            final ReputationData ownRepData = RepUtils.getPlayerReputation(ctx.getSource().getEntity().getUuid());
            for (final GameProfile profile : profiles) {
                if (profile.getId().equals(ctx.getSource().getEntity().getUuid())) {
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
            ctx.getSource().sendFeedback(
                Text.of("Voted " + profile.getName() + " reputation " + (amount > 0 ? "+" : "") + amount + "!"),
                false
            );
            final ServerPlayerEntity other = ctx.getSource().getServer().getPlayerManager().getPlayer(profile.getId());
            if (other != null) {
                String reason = ctx.getArgument("reason", String.class);
                if (amount > 0 && RepUtils.getConfig().isUpvoteNotifications()) {
                    other.sendSystemMessage(Text.of("Your reputation was upvoted!\nReason: " + (reason == null ? "None Provided" : reason)), net.minecraft.util.Util.NIL_UUID);
                    FabricRepSystem.LOGGER.info(other.getUuidAsString() + "was upvoted with reason: " + (reason == null ? "None Provided" : reason));
                    other.networkHandler.sendPacket(new PlaySoundS2CPacket(
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.MASTER,
                        other.getX(),
                        other.getY(),
                        other.getZ(),
                        0.5f, 1f
                    ));
                } else if (amount < 0 && RepUtils.getConfig().isDownvoteNotifications()) {
                    other.sendSystemMessage(Text.of("Your reputation was downvoted.\nReason: " + (reason == null ? "None Provided" : reason)), net.minecraft.util.Util.NIL_UUID);
                    FabricRepSystem.LOGGER.info(other.getUuidAsString() + "was downvoted with reason: " + (reason == null ? "None Provided" : reason));
                    other.networkHandler.sendPacket(new PlaySoundS2CPacket(
                        SoundEvents.ENTITY_VILLAGER_NO,
                        SoundCategory.MASTER,
                        other.getX(),
                        other.getY(),
                        other.getZ(),
                        1f, 1f
                    ));
                }
            }
        }
        return profiles.size();
    }
}
