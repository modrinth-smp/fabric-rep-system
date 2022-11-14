package io.github.modrinthsmp.fabricrepsystem;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class ReputationConfig {
    private long cooldown = 24 * 60 * 60;
    private Integer minPvPRep = null;

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public Integer getMinPvPRep() {
        return minPvPRep;
    }

    public void setMinPvPRep(Integer rep) {
        minPvPRep = rep;
    }

    public void writeConfig(JsonWriter writer) throws IOException {
        writer.beginObject(); {
            writer.blockComment("""
                Cooldown time is in seconds.
                `/rep set` is not affected by cooldown, but `/rep add` and `/rep remove` are.
                """);
            writer.name("cooldown").value(cooldown);
            writer.blockComment("""
                Minimum required reputation to PvP. Anything lower than this will not be able to hit another player.
                Can be set to null to always allow PvP.
                """);
            writer.name("minPvPRep").value(minPvPRep);
        } writer.endObject();
    }

    public void readConfig(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String key;
            switch (key = reader.nextName()) {
                case "cooldown" -> cooldown = reader.nextLong();
                case "minPvPRep" -> {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        minPvPRep = null;
                    } else {
                        minPvPRep = reader.nextInt();
                    }
                }
                default -> FabricRepSystem.LOGGER.warn("Unknown config key: " + key);
            }
        }
        reader.endObject();
    }
}
