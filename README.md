# Fabric Reputation System

This is a mod to manage player reputation on a server. Mean players (e.g. griefers and thieves) can get downvoted, while nice players can get upvoted. You can configure some actions to happen based on a player's reputation. You can configure even more actions using a datapack.

## Command summary

+ `/rep view [player]` &mdash; View the reputation of a player.
+ `/rep set <rep>`, `/rep set <player> <rep>` (operator only) &mdash; Set the reputation of a player.
+ `/rep upvote <player> [reason]` &mdash; Upvote a player. The `reason` argument can be configured to be required.
+ `/rep downvote <player> [reason]` &mdash; Downvote a player. The `reason` argument can be configured to be required. 
+ `/rep reload` (operator only) &mdash; Reload the config file
+ `/rep wanted` &mdash; View the coordinates of a "wanted" player.

## Getting the reputation in a datapack

You can save a player's reputation into a scoreboard objective with the following command:

```
execute as <entity> store result score @s <objective> run rep view @s[type=player]
```

## Configuring the mod

Here's the mod's default config file:

```json5
{
	// Cooldown time is in seconds.
	// `/rep set` is not affected by cooldown, but `/rep upvote` and `/rep downvote` are.
	cooldown: 84600,
	// The minimum and maximum reputation.
	// All reputation changes will clamp between these values.
	minRep: null,
	maxRep: null,
	// Minimum required reputation to PvP. Anything lower than this will not be able to hit another player.
	// Can be set to null to always allow PvP.
	minPvPRep: null,
	// Minimum required reputation to modify within the range of spawn protection.
	// Can be set to null to always disallow modification.
	minSpawnBuildingRep: null,
	// Maximum required reputation to be wanted (have co-ords shown).
	// Can be set to null to not allow anyone to be wanted.
	maxWantedRep: null,
	// Require a reason to vote on a player.
	votingReasonRequired: true,
	// Show the upvote/downvote reason in notifications.
	showReason: false,
	// Notify the player when they get upvoted.
	upvoteNotifications: false,
	// Notify the player when they get downvoted.
	downvoteNotifications: false,
	// Webhook to log upvotes and downvotes.
	discordWebhookUrl: null
}
```

## Querying reputation from a mod

First, depend on the mod in your `build.gradle`:

```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}

dependencies {
    modImplementation "maven.modrinth:fabric-rep-system:1.0.0"
}
```

Then, you can query the API for reputation data using `RepUtils.getPlayerReputation(playerUuid)`.
