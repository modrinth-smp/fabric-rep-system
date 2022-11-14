package io.github.modrinthsmp.fabricrepsystem;

public final class ReputationData {
    private int reputation;

    public int getReputation() {
        return reputation;
    }

    public ReputationData setReputation(int reputation) {
        this.reputation = reputation;
        return this;
    }

    @Override
    public String toString() {
        return "ReputationData{" +
            "reputation=" + reputation +
            '}';
    }
}
