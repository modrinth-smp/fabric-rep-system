package io.github.modrinthsmp.fabricrepsystem;

public final class Util {
    private Util() {
    }

    public static String formatTimeDifference(long difference) {
        final String base = formatTimeDifferenceSimple(Math.abs(difference));
        return difference < 0 ? (base + " ago") : ("in " + base);
    }

    public static String formatTimeDifferenceSimple(long difference) {
        if (difference < 60 * 1000) {
            if (difference / 1000 == 1) {
                return "1 second";
            }
            return difference / 1000 + " seconds";
        }
        if (difference < 60 * 60 * 1000) {
            if (difference / (60 * 1000) == 1) {
                return "1 minute";
            }
            return difference / (60 * 1000) + " minutes";
        }
        if (difference < 24 * 60 * 60 * 1000) {
            if (difference / (60 * 60 * 1000) == 1) {
                return "1 hour";
            }
            return difference / (60 * 60 * 1000) + " hours";
        }
        if (difference < 365 * 24 * 60 * 60 * 1000L) {
            if (difference / (24 * 60 * 60 * 1000) == 1) {
                return "1 day";
            }
            return difference / (24 * 60 * 60 * 1000) + " days";
        }
        if (difference / (365 * 24 * 60 * 60 * 1000L) == 1) {
            return "1 year";
        }
        return difference / (365 * 24 * 60 * 60 * 1000L) + " years";
    }
}
