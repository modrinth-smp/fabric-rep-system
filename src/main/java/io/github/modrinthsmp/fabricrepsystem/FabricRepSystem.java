package io.github.modrinthsmp.fabricrepsystem;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FabricRepSystem implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static Map<UUID, ReputationData> reputation = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RepUtils.readRep(server);
            RepUtils.readConfig();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            RepUtils.closeWebhookClient();
            RepUtils.writeConfig();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            if (!reputation.containsKey(uuid)) {
                reputation.put(uuid, new ReputationData());
                RepUtils.writeRep(server);
                LOGGER.info("Added a reputation of zero for player with UUID: " + uuid);
            } else {
                LOGGER.info("Player with UUID: " + uuid + ", joined with reputation: " + reputation.get(uuid));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RepUtils.writeRep(server));

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> RepCommand.register(dispatcher));
    }
}
