package io.github.modrinthsmp.fabricrepsystem;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class ReputationConfig {
    private long cooldown = 24 * 60 * 60;

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public void writeConfig(JsonWriter writer) throws IOException {
        writer.beginObject(); {
            writer.blockComment("""
                Cooldown time is in seconds.
                `/rep set` is not affected by cooldown, but `/rep add` and `/rep remove` are.
                """);
            writer.name("cooldown").value(cooldown);
        } writer.endObject();
    }

    public void readConfig(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String key;
            //noinspection SwitchStatementWithTooFewBranches
            switch (key = reader.nextName()) {
                case "cooldown" -> cooldown = reader.nextLong();
                default -> FabricRepSystem.LOGGER.warn("Unknown config key: " + key);
            }
        }
        reader.endObject();
    }
}
