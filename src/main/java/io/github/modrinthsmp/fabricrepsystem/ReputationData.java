package io.github.modrinthsmp.fabricrepsystem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReputationData {
    private int reputation = 0;

    @SuppressWarnings("FieldMayBeFinal") // It can't, because Gson
    private Map<UUID, Long> lastVotedFor = new HashMap<>();

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int reputation) {
        final ReputationConfig config = RepUtils.getConfig();
        final Integer minRep = config.getMinRep();
        final Integer maxRep = config.getMaxRep();
        if (minRep != null && reputation < minRep) {
            reputation = minRep;
        } else if (maxRep != null && reputation > maxRep) {
            reputation = maxRep;
        }
        this.reputation = reputation;
    }

    public void addReputation(int reputation) {
        setReputation(this.reputation + reputation);
    }

    public Map<UUID, Long> getLastVotedFor() {
        return lastVotedFor;
    }

    @Override
    public String toString() {
        return "ReputationData{" +
            "reputation=" + reputation +
            ", lastVotedFor=" + lastVotedFor +
            '}';
    }
}
