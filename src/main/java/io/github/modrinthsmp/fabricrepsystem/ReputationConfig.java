package io.github.modrinthsmp.fabricrepsystem;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonToken;
import org.quiltmc.parsers.json.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public final class ReputationConfig {
    private long cooldown = 24 * 60 * 60;
    @Nullable
    private Integer minRep = null;
    @Nullable
    private Integer maxRep = null;
    @Nullable
    private Integer minPvPRep = null;
    @Nullable
    private Integer minSpawnBuildingRep = null;
    @Nullable
    private Integer maxWantedRep = null;
    private boolean votingReasonRequired = true;
    private boolean showReason = false;
    private boolean upvoteNotifications = false;
    private boolean downvoteNotifications = false;
    @Nullable
    private URI discordWebhookUrl = null;

    public long getCooldown() {
        return cooldown;
    }

    @Nullable
    public Integer getMinRep() {
        return minRep;
    }

    @Nullable
    public Integer getMaxRep() {
        return maxRep;
    }

    @Nullable
    public Integer getMinPvPRep() {
        return minPvPRep;
    }

    @Nullable
    public Integer getMinSpawnBuildingRep() {
        return minSpawnBuildingRep;
    }

    @Nullable
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

    @Nullable
    public URI getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public void writeConfig(JsonWriter writer) throws IOException {
        writer.beginObject(); {
            writer.blockComment("""
                Cooldown time is in seconds.
                `/rep set` is not affected by cooldown, but `/rep upvote` and `/rep downvote` are.
                """);
            writer.name("cooldown").value(cooldown);

            writer.blockComment("""
                The minimum and maximum reputation.
                All reputation changes will clamp between these values.
                """);
            writer.name("minRep").value(minRep);
            writer.name("maxRep").value(maxRep);

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

            writer.comment("Show the upvote/downvote reason in notifications.");
            writer.name("showReason").value(showReason);

            writer.comment("Notify the player when they get upvoted.");
            writer.name("upvoteNotifications").value(upvoteNotifications);

            writer.comment("Notify the player when they get downvoted.");
            writer.name("downvoteNotifications").value(downvoteNotifications);

            writer.comment("Webhook to log upvotes and downvotes.");
            writer.name("discordWebhookUrl").value(discordWebhookUrl != null ? discordWebhookUrl.toString() : null);
        } writer.endObject();
    }

    public void readConfig(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String key;
            switch (key = reader.nextName()) {
                case "cooldown" -> cooldown = reader.nextLong();
                case "minRep" -> minRep = nextNullableInt(reader);
                case "maxRep" -> maxRep = nextNullableInt(reader);
                case "minPvPRep" -> minPvPRep = nextNullableInt(reader);
                case "minSpawnBuildingRep" -> minSpawnBuildingRep = nextNullableInt(reader);
                case "maxWantedRep" -> maxWantedRep = nextNullableInt(reader);
                case "votingReasonRequired" -> votingReasonRequired = reader.nextBoolean();
                case "showReason" -> showReason = reader.nextBoolean();
                case "upvoteNotifications" -> upvoteNotifications = reader.nextBoolean();
                case "downvoteNotifications" -> downvoteNotifications = reader.nextBoolean();
                case "discordWebhookUrl" -> {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        discordWebhookUrl = null;
                    } else {
                        final String url = reader.nextString();
                        try {
                            discordWebhookUrl = new URI(url);
                        } catch (URISyntaxException e) {
                            FabricRepSystem.LOGGER.warn("Invalid URI: {}", e.getMessage());
                        }
                    }
                }
                default -> FabricRepSystem.LOGGER.warn("Unknown config key: {}", key);
            }
        }
        reader.endObject();
    }

    @Nullable
    private static Integer nextNullableInt(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        } else {
            return reader.nextInt();
        }
    }
}
