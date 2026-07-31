package io.github.atfaster.rnrconcreteflow.flow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcretePouringRulesTest {
    @Test
    void roundsPartialAmountsDownToWholeBlocks() {
        assertEquals(0, ConcretePouringRules.availableBlocks(0));
        assertEquals(0, ConcretePouringRules.availableBlocks(39));
        assertEquals(1, ConcretePouringRules.availableBlocks(40));
        assertEquals(2, ConcretePouringRules.availableBlocks(100));
        assertEquals(24, ConcretePouringRules.availableBlocks(999));
        assertEquals(25, ConcretePouringRules.availableBlocks(1_000));
    }

    @Test
    void normalInteractionUsesAtMostOneBucket() {
        assertEquals(25, ConcretePouringRules.availableBlocks(10_000));
        assertEquals(1_000, ConcretePouringRules.concreteForBlocks(25));
    }

    @Test
    void everyInteractionIsLimitedToOneBucket() {
        assertEquals(25, ConcretePouringRules.availableBlocks(10_000));
        assertEquals(1_000, ConcretePouringRules.concreteForBlocks(25));
    }

    @Test
    void rejectsOverflowWhenConvertingAnInvalidBudget() {
        assertThrows(
            ArithmeticException.class,
            () -> ConcretePouringRules.concreteForBlocks(Integer.MAX_VALUE)
        );
    }
}
