package io.github.atfaster.rnrconcreteflow.flow;

public final class ConcretePouringRules {
    public static final int MILLIBUCKETS_PER_BLOCK = 40;
    public static final int NORMAL_INTERACTION_LIMIT_MB = 1_000;

    public static int availableBlocks(int concreteAmountMb) {
        if (concreteAmountMb <= 0) {
            return 0;
        }

        int usableAmount = Math.min(concreteAmountMb, NORMAL_INTERACTION_LIMIT_MB);
        return usableAmount / MILLIBUCKETS_PER_BLOCK;
    }

    public static int concreteForBlocks(int blocks) {
        if (blocks <= 0) {
            return 0;
        }
        return Math.multiplyExact(blocks, MILLIBUCKETS_PER_BLOCK);
    }

    private ConcretePouringRules() {
    }
}
