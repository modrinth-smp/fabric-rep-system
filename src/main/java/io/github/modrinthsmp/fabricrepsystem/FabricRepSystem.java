package io.github.modrinthsmp.fabricrepsystem;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FabricRepSystem implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final HttpClient HTTP = HttpClient.newHttpClient();
    public static final String VERSION = FabricLoader.getInstance()
        .getModContainer("fabric-rep-system")
        .orElseThrow()
        .getMetadata()
        .getVersion()
        .getFriendlyString();
    public static final String USER_AGENT = "FabricRepSystem/" + VERSION;
    public static Map<UUID, ReputationData> reputation = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RepUtils.readRep(server);
            RepUtils.readConfig();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RepUtils.writeConfig());

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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RepCommand.register(dispatcher));
    }
}
