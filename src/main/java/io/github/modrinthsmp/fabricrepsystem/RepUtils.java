package io.github.modrinthsmp.fabricrepsystem;

import club.minnced.discord.webhook.WebhookClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RepUtils {
    private static final Gson GSON = new GsonBuilder().create();
    private static final ReputationConfig CONFIG = new ReputationConfig();
    private static WebhookClient webhookClient = null;
    private static URL lastWebhookUrl = null;

    private RepUtils() {
    }

    public static void readRep(MinecraftServer server) {
        try (final Reader reader = Files.newBufferedReader(getReputationPath(server), StandardCharsets.UTF_8)) {
            FabricRepSystem.reputation = GSON.fromJson(reader, new TypeToken<Map<UUID, ReputationData>>() {}.getType());
        } catch (IOException e) {
            FabricRepSystem.LOGGER.error("Failed to load reputation", e);
        }
    }

    public static void readConfig() {
        try (final JsonReader reader = JsonReader.json5(getConfigPath())) {
            CONFIG.readConfig(reader);
        } catch (IOException e) {
            FabricRepSystem.LOGGER.error("Failed to load config", e);
        }
        writeConfig();
        if (!Objects.equals(lastWebhookUrl, CONFIG.getDiscordWebhookUrl())) {
            closeWebhookClient();
            lastWebhookUrl = CONFIG.getDiscordWebhookUrl();
            if (lastWebhookUrl != null) {
                webhookClient = WebhookClient.withUrl(lastWebhookUrl.toExternalForm());
                FabricRepSystem.LOGGER.info("Started Discord webhook client.");
            }
        }
    }

    public static void writeRep(MinecraftServer server) {
        try (final Writer writer = Files.newBufferedWriter(getReputationPath(server), StandardCharsets.UTF_8)) {
            GSON.toJson(FabricRepSystem.reputation, writer);
        } catch (IOException e) {
            FabricRepSystem.LOGGER.error("Failed to write reputation", e);
        }
    }

    public static void writeConfig() {
        try (final JsonWriter writer = JsonWriter.json5(getConfigPath())) {
            CONFIG.writeConfig(writer);
        } catch (IOException e) {
            FabricRepSystem.LOGGER.error("Failed to save config", e);
        }
    }

    public static Path getReputationPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("reputation.json");
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("fabric-rep-system.json5");
    }

    public static ReputationConfig getConfig() {
        return CONFIG;
    }

    public static WebhookClient getWebhookClient() {
        return webhookClient;
    }

    static void closeWebhookClient() {
        lastWebhookUrl = null;
        if (webhookClient != null) {
            webhookClient.close();
            FabricRepSystem.LOGGER.info("Closed Discord webhook client.");
        }
        webhookClient = null;
    }

    public static ReputationData getPlayerReputation(UUID uuid) {
        return FabricRepSystem.reputation.computeIfAbsent(uuid, key -> new ReputationData());
    }
}
