package io.github.modrinthsmp.fabricrepsystem;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public final class RepUtils {
    private static final Gson GSON = new GsonBuilder().create();
    private static final ReputationConfig CONFIG = new ReputationConfig();

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
            writeConfig();
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
}
