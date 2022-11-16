package io.github.modrinthsmp.fabricrepsystem;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class ReputationConfig {
    private long cooldown = 24 * 60 * 60;
    private Integer minPvPRep = null;
    private Integer minSpawnBuildingRep = null;
    private Integer maxWantedRep = null;
    private boolean votingReasonRequired = true;
    private boolean showReason = false;
    private boolean upvoteNotifications = false;
    private boolean downvoteNotifications = false;

    public long getCooldown() {
        return cooldown;
    }

    public Integer getMinPvPRep() {
        return minPvPRep;
    }

    public Integer getMinSpawnBuildingRep() {
        return minSpawnBuildingRep;
    }

    public Integer getMaxWantedRep() {
        return maxWantedRep;
    }

    public boolean isVotingReasonRequired() {
        return votingReasonRequired;
    }

    public boolean isShowReason() {
        return showReason;
    }

    public boolean isUpvoteNotifications() {
        return upvoteNotifications;
    }

    public boolean isDownvoteNotifications() {
        return downvoteNotifications;
    }

    public void writeConfig(JsonWriter writer) throws IOException {
        writer.beginObject(); {
            writer.blockComment("""
                Cooldown time is in seconds.
                `/rep set` is not affected by cooldown, but `/rep upvote` and `/rep downvote` are.
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

            writer.blockComment("""
                    Maximum required reputation to be wanted (have co-ords shown).
                    Can be set to null to not allow anyone to be wanted.
                    """);
            writer.name("maxWantedRep").value(maxWantedRep);

            writer.comment("Require a reason to vote on a player.");
            writer.name("votingReasonRequired").value(votingReasonRequired);

            writer.comment("Show the upvote/downvote reason in notification.");
            writer.name("showReason").value(showReason);

            writer.comment("Notify the player when they get upvoted.");
            writer.name("upvoteNotifications").value(upvoteNotifications);

            writer.comment("Notify the player when they get downvoted.");
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
                case "maxWantedRep" -> {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        maxWantedRep = null;
                    } else {
                        maxWantedRep = reader.nextInt();
                    }
                }
                case "votingReasonRequired" -> votingReasonRequired = reader.nextBoolean();
                case "showReason" -> showReason = reader.nextBoolean();
                case "upvoteNotifications" -> upvoteNotifications = reader.nextBoolean();
                case "downvoteNotifications" -> downvoteNotifications = reader.nextBoolean();
                default -> FabricRepSystem.LOGGER.warn("Unknown config key: " + key);
            }
        }
        reader.endObject();
    }
}
