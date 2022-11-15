package io.github.modrinthsmp.fabricrepsystem;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class ReputationConfig {
    private long cooldown = 24 * 60 * 60;
    private Integer minPvPRep = null;
    private Integer minSpawnBuildingRep = null;
    private boolean upvoteNotifications = false;
    private boolean downvoteNotifications = false;

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

    public Integer getMinSpawnBuildingRep() {
        return minSpawnBuildingRep;
    }

    public void setMinSpawnBuildingRep(Integer minSpawnBuildingRep) {
        this.minSpawnBuildingRep = minSpawnBuildingRep;
    }

    public boolean isUpvoteNotifications() {
        return upvoteNotifications;
    }

    public void setUpvoteNotifications(boolean upvoteNotifications) {
        this.upvoteNotifications = upvoteNotifications;
    }

    public boolean isDownvoteNotifications() {
        return downvoteNotifications;
    }

    public void setDownvoteNotifications(boolean downvoteNotifications) {
        this.downvoteNotifications = downvoteNotifications;
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

            writer.blockComment("""
                    Minimum required reputation to modify within the range of spawn protection.
                    Can be set to null to always disallow modification.
                    """);
            writer.name("minSpawnBuildingRep").value(minSpawnBuildingRep);

            writer.blockComment("Notify the player when they get upvoted.");
            writer.name("upvoteNotifications").value(upvoteNotifications);

            writer.blockComment("Notify the player when they get downvoted.");
            writer.name("downvoteNotifications").value(downvoteNotifications);
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
                case "minSpawnBuildingRep" -> {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        minSpawnBuildingRep = null;
                    } else {
                        minSpawnBuildingRep = reader.nextInt();
                    }
                }
                case "upvoteNotifications" -> upvoteNotifications = reader.nextBoolean();
                case "downvoteNotifications" -> downvoteNotifications = reader.nextBoolean();
                default -> FabricRepSystem.LOGGER.warn("Unknown config key: " + key);
            }
        }
        reader.endObject();
    }
}
