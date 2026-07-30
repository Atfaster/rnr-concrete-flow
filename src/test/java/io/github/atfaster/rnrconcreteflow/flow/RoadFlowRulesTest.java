package io.github.atfaster.rnrconcreteflow.flow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.github.atfaster.rnrconcreteflow.flow.RoadFlowRules.Cell.AIR;
import static io.github.atfaster.rnrconcreteflow.flow.RoadFlowRules.Cell.BLOCKED;
import static io.github.atfaster.rnrconcreteflow.flow.RoadFlowRules.Cell.SPREADABLE;
import static io.github.atfaster.rnrconcreteflow.flow.RoadFlowRules.Cell.UNLOADED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadFlowRulesTest {
    private static final RoadFlowRules.Position ORIGIN = new RoadFlowRules.Position(0, 10, 0);

    @Test
    void prefersFoundationOnTheSameLevel() {
        Map<RoadFlowRules.Position, RoadFlowRules.Cell> cells = new HashMap<>();
        cells.put(new RoadFlowRules.Position(1, 10, 0), SPREADABLE);
        cells.put(new RoadFlowRules.Position(1, 9, 0), SPREADABLE);

        assertEquals(
            new RoadFlowRules.Position(1, 10, 0),
            RoadFlowRules.findCandidate(ORIGIN, 1, 0, position -> cells.getOrDefault(position, BLOCKED)).orElseThrow()
        );
    }

    @Test
    void descendsOneStepThroughOpenAir() {
        Map<RoadFlowRules.Position, RoadFlowRules.Cell> cells = new HashMap<>();
        cells.put(new RoadFlowRules.Position(1, 10, 0), AIR);
        cells.put(new RoadFlowRules.Position(1, 9, 0), SPREADABLE);

        assertEquals(
            new RoadFlowRules.Position(1, 9, 0),
            RoadFlowRules.findCandidate(ORIGIN, 1, 0, position -> cells.getOrDefault(position, BLOCKED)).orElseThrow()
        );
    }

    @Test
    void doesNotPassThroughAnObstacle() {
        Map<RoadFlowRules.Position, RoadFlowRules.Cell> cells = new HashMap<>();
        cells.put(new RoadFlowRules.Position(1, 10, 0), BLOCKED);
        cells.put(new RoadFlowRules.Position(1, 9, 0), SPREADABLE);

        assertTrue(
            RoadFlowRules.findCandidate(ORIGIN, 1, 0, position -> cells.getOrDefault(position, BLOCKED)).isEmpty()
        );
    }

    @Test
    void doesNotCrossAnUnloadedChunk() {
        assertTrue(RoadFlowRules.findCandidate(ORIGIN, 1, 0, position -> UNLOADED).isEmpty());
    }

    @Test
    void neverClimbsUpward() {
        Map<RoadFlowRules.Position, RoadFlowRules.Cell> cells = new HashMap<>();
        cells.put(new RoadFlowRules.Position(1, 11, 0), SPREADABLE);
        cells.put(new RoadFlowRules.Position(1, 10, 0), AIR);

        assertTrue(
            RoadFlowRules.findCandidate(ORIGIN, 1, 0, position -> cells.getOrDefault(position, BLOCKED)).isEmpty()
        );
    }
}
