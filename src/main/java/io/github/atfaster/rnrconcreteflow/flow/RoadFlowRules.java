package io.github.atfaster.rnrconcreteflow.flow;

import java.util.Optional;

public final class RoadFlowRules {
    public static Optional<Position> findCandidate(
        Position current,
        int stepX,
        int stepZ,
        CellLookup cells
    ) {
        Position sameLevel = current.offset(stepX, 0, stepZ);
        Cell sameLevelCell = cells.get(sameLevel);
        if (sameLevelCell == Cell.SPREADABLE) {
            return Optional.of(sameLevel);
        }
        if (sameLevelCell != Cell.AIR) {
            return Optional.empty();
        }

        Position oneLevelDown = sameLevel.offset(0, -1, 0);
        return cells.get(oneLevelDown) == Cell.SPREADABLE
            ? Optional.of(oneLevelDown)
            : Optional.empty();
    }

    public enum Cell {
        SPREADABLE,
        AIR,
        BLOCKED,
        UNLOADED
    }

    @FunctionalInterface
    public interface CellLookup {
        Cell get(Position position);
    }

    public record Position(int x, int y, int z) {
        public Position offset(int xOffset, int yOffset, int zOffset) {
            return new Position(x + xOffset, y + yOffset, z + zOffset);
        }
    }

    private RoadFlowRules() {
    }
}
