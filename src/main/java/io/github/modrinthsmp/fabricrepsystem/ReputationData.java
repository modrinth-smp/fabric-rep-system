package io.github.modrinthsmp.fabricrepsystem;

public final class ReputationData {
    private int reputation = 0;

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int reputation) {
        this.reputation = reputation;
    }

    public void addReputation(int reputation) {
        this.reputation += reputation;
    }

    @Override
    public String toString() {
        return "ReputationData{" +
            "reputation=" + reputation +
            '}';
    }
}
