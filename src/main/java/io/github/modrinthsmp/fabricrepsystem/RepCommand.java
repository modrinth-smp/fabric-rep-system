package io.github.modrinthsmp.fabricrepsystem;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.argument.GameProfileArgumentType.gameProfile;
import static net.minecraft.command.argument.GameProfileArgumentType.getProfileArgument;

public final class RepCommand {
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
            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("reload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> {
                    RepUtils.readConfig();
                    ctx.getSource().sendFeedback(Text.of("Successfully reloaded reputation config"), true);
                    return 1;
                })
            )
        );
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
            final ReputationData repData = FabricRepSystem.reputation.computeIfAbsent(profile.getId(), key -> new ReputationData());
            repData.setReputation(rep);
            ctx.getSource().sendFeedback(
                Text.of("Set " + profile.getName() + "'s reputation to " + rep + "."),
                true
            );
        }
        return profiles.size();
    }

    private static int repAdd(CommandContext<ServerCommandSource> ctx, Collection<GameProfile> profiles, int amount) {
        for (final GameProfile profile : profiles) {
            final ReputationData repData = FabricRepSystem.reputation.computeIfAbsent(profile.getId(), key -> new ReputationData());
            repData.addReputation(amount);
            ctx.getSource().sendFeedback(
                Text.of("Voted " + profile.getName() + " reputation " + (amount > 0 ? "+" : "") + amount + "."),
                true
            );
        }
        return profiles.size();
    }
}
