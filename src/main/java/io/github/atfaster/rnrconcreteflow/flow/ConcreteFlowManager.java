package io.github.atfaster.rnrconcreteflow.flow;

import io.github.atfaster.rnrconcreteflow.config.RnrConcreteFlowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntPredicate;

public final class ConcreteFlowManager {
    static final int MAX_BLOCKS = 25;
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL;
    private static final int MAX_PLANNING_POSITIONS = 65_536;
    private static final ResourceLocation WET_CONCRETE_ID =
        new ResourceLocation("rnr", "wet_concrete_road");
    private static final TagKey<Block> CONCRETE_SPREADABLE =
        TagKey.create(Registries.BLOCK, new ResourceLocation("rnr", "concrete_spreadable"));
    private static final TagKey<Block> WET_CONCRETE_ROADS =
        TagKey.create(Registries.BLOCK, new ResourceLocation("rnr", "wet_concrete_roads"));
    private static final Direction[] DIRECTIONS = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST
    };
    private static final Map<ServerLevel, List<FlowTask>> TASKS = new HashMap<>();

    public static void start(ServerLevel level, BlockPos origin) {
        if (!RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()) {
            return;
        }

        Block wetConcrete = ForgeRegistries.BLOCKS.getValue(WET_CONCRETE_ID);
        if (wetConcrete == null) {
            return;
        }

        BlockPos immutableOrigin = origin.immutable();
        if (!level.setBlock(immutableOrigin, wetConcrete.defaultBlockState(), UPDATE_FLAGS)) {
            return;
        }

        reserve(level, immutableOrigin, MAX_BLOCKS - 1, ignored -> true);
    }

    /**
     * Atomically plans unique reachable foundations, invokes payment for the
     * accepted block count, and commits the reservations only if payment
     * succeeds. This method must be called on the logical server thread.
     */
    public static int addCapacity(
        ServerLevel level,
        BlockPos wetOrigin,
        int requestedBlocks,
        IntPredicate payment
    ) {
        if (!RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()
            || requestedBlocks <= 0
            || !isWetRoad(level.getBlockState(wetOrigin))) {
            return 0;
        }
        return reserve(level, wetOrigin.immutable(), requestedBlocks, payment);
    }

    /**
     * Starts a paid pour on a foundation. The clicked foundation is included
     * in the accepted block count and costs the same 40 mB as every reserved
     * continuation block.
     */
    public static int startFromFoundation(
        ServerLevel level,
        BlockPos foundationOrigin,
        int requestedBlocks,
        IntPredicate payment
    ) {
        BlockPos origin = foundationOrigin.immutable();
        if (!RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()
            || requestedBlocks <= 0
            || !isSpreadable(level, origin)) {
            return 0;
        }

        PlanningContext context = PlanningContext.create(level);
        PlanningResult result = plan(level, origin, requestedBlocks - 1, context);
        int acceptedBlocks = result.newReservations.size() + 1;
        if (!payment.test(acceptedBlocks) || !isSpreadable(level, origin)) {
            return 0;
        }

        Block wetConcrete = ForgeRegistries.BLOCKS.getValue(WET_CONCRETE_ID);
        if (wetConcrete == null
            || !level.setBlock(origin, wetConcrete.defaultBlockState(), UPDATE_FLAGS)) {
            return 0;
        }

        commitReservations(level, result, origin);
        return acceptedBlocks;
    }

    public static boolean isWetRoad(BlockState state) {
        return state.is(WET_CONCRETE_ROADS);
    }

    public static boolean isSpreadableRoad(BlockState state) {
        return state.is(CONCRETE_SPREADABLE);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        List<FlowTask> levelTasks = TASKS.get(level);
        if (levelTasks == null) {
            return;
        }

        if (!RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()) {
            TASKS.remove(level);
            return;
        }

        Block wetConcrete = ForgeRegistries.BLOCKS.getValue(WET_CONCRETE_ID);
        if (wetConcrete == null) {
            TASKS.remove(level);
            return;
        }

        long gameTime = level.getGameTime();
        Iterator<FlowTask> iterator = levelTasks.iterator();
        while (iterator.hasNext()) {
            FlowTask task = iterator.next();
            task.placeDueReservations(level, wetConcrete, gameTime);
            if (task.reservations.isEmpty()) {
                iterator.remove();
            }
        }

        if (levelTasks.isEmpty()) {
            TASKS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TASKS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }

    private static int reserve(
        ServerLevel level,
        BlockPos origin,
        int requestedBlocks,
        IntPredicate payment
    ) {
        PlanningContext context = PlanningContext.create(level);
        PlanningResult result = plan(level, origin, requestedBlocks, context);
        int acceptedBlocks = result.newReservations.size();
        if (acceptedBlocks == 0 || !payment.test(acceptedBlocks)) {
            return 0;
        }

        commitReservations(level, result, null);
        return acceptedBlocks;
    }

    private static void commitReservations(
        ServerLevel level,
        PlanningResult result,
        BlockPos additionalOwnedWet
    ) {
        List<FlowTask> levelTasks = TASKS.computeIfAbsent(level, ignored -> new ArrayList<>());
        FlowTask target;
        if (result.connectedTasks.isEmpty()) {
            target = new FlowTask();
            levelTasks.add(target);
        } else {
            target = result.connectedTasks.iterator().next();
            for (FlowTask other : result.connectedTasks) {
                if (other != target) {
                    target.merge(other);
                    levelTasks.remove(other);
                }
            }
        }

        target.ownedWet.addAll(result.visitedWet);
        if (additionalOwnedWet != null) {
            target.ownedWet.add(additionalOwnedWet);
        }
        for (ReservedBlock reservation : result.newReservations.values()) {
            target.reservations.put(reservation.pos, reservation);
        }
    }

    private static PlanningResult plan(
        ServerLevel level,
        BlockPos origin,
        int requestedBlocks,
        PlanningContext context
    ) {
        long now = level.getGameTime();
        int interval = RnrConcreteFlowConfig.CONCRETE_SPREAD_INTERVAL_TICKS.get();
        Queue<PlanningNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> visitedWet = new HashSet<>();
        Set<FlowTask> connectedTasks = new HashSet<>();
        LinkedHashMap<BlockPos, ReservedBlock> planned = new LinkedHashMap<>();

        queue.add(new PlanningNode(origin, null, now, NodeType.WET));
        visited.add(origin);

        while (!queue.isEmpty() && visited.size() < MAX_PLANNING_POSITIONS) {
            PlanningNode current = queue.remove();
            if (current.type == NodeType.WET) {
                visitedWet.add(current.pos);
                FlowTask owner = context.wetOwners.get(current.pos);
                if (owner != null) {
                    connectedTasks.add(owner);
                }
            } else if (current.type == NodeType.RESERVED) {
                FlowTask owner = context.reservationOwners.get(current.pos);
                if (owner != null) {
                    connectedTasks.add(owner);
                }
            }

            for (Direction direction : DIRECTIONS) {
                BlockPos sameLevel = current.pos.relative(direction);
                PlanningNode next = resolveForwardStep(
                    level,
                    sameLevel,
                    current,
                    now,
                    interval,
                    context,
                    planned,
                    planned.size() < requestedBlocks
                );

                if (next == null && isBlockedLoaded(level, sameLevel)) {
                    BlockPos oneUp = sameLevel.above();
                    next = resolveExistingPath(level, oneUp, current, context);
                }

                if (next != null && visited.add(next.pos)) {
                    if (next.type == NodeType.PLANNED) {
                        planned.put(
                            next.pos,
                            new ReservedBlock(next.pos, next.parent, next.dueAt)
                        );
                    }
                    queue.add(next);
                }
            }
        }

        return new PlanningResult(planned, visitedWet, connectedTasks);
    }

    private static PlanningNode resolveForwardStep(
        ServerLevel level,
        BlockPos sameLevel,
        PlanningNode current,
        long now,
        int interval,
        PlanningContext context,
        Map<BlockPos, ReservedBlock> planned,
        boolean allowPlanning
    ) {
        PlanningNode existing = resolveExistingPath(level, sameLevel, current, context);
        if (existing != null) {
            return existing;
        }

        if (allowPlanning
            && isSpreadable(level, sameLevel)
            && !isReserved(sameLevel, context, planned)) {
            return plannedNode(sameLevel, current, now, interval);
        }

        if (!isAirLoaded(level, sameLevel)) {
            return null;
        }

        BlockPos oneDown = sameLevel.below();
        existing = resolveExistingPath(level, oneDown, current, context);
        if (existing != null) {
            return existing;
        }
        if (allowPlanning
            && isSpreadable(level, oneDown)
            && !isReserved(oneDown, context, planned)) {
            return plannedNode(oneDown, current, now, interval);
        }
        return null;
    }

    private static PlanningNode resolveExistingPath(
        ServerLevel level,
        BlockPos pos,
        PlanningNode current,
        PlanningContext context
    ) {
        if (isWetRoadLoaded(level, pos)) {
            return new PlanningNode(pos.immutable(), current.pos, level.getGameTime(), NodeType.WET);
        }

        ReservedBlock reservation = context.reservations.get(pos);
        if (reservation != null) {
            return new PlanningNode(
                reservation.pos,
                reservation.parent,
                reservation.dueAt,
                NodeType.RESERVED
            );
        }
        return null;
    }

    private static PlanningNode plannedNode(
        BlockPos pos,
        PlanningNode parent,
        long now,
        int interval
    ) {
        long dueAt = Math.max(now + interval, parent.dueAt + interval);
        return new PlanningNode(pos.immutable(), parent.pos, dueAt, NodeType.PLANNED);
    }

    private static boolean isReserved(
        BlockPos pos,
        PlanningContext context,
        Map<BlockPos, ReservedBlock> planned
    ) {
        return context.reservations.containsKey(pos) || planned.containsKey(pos);
    }

    private static boolean isWetRoadLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && isWetRoad(level.getBlockState(pos));
    }

    private static boolean isSpreadable(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).is(CONCRETE_SPREADABLE);
    }

    private static boolean isAirLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).isAir();
    }

    private static boolean isBlockedLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos)
            && !level.getBlockState(pos).isAir()
            && !isWetRoad(level.getBlockState(pos))
            && !level.getBlockState(pos).is(CONCRETE_SPREADABLE);
    }

    private static boolean isParentWet(ServerLevel level, BlockPos parent, Block wetConcrete) {
        if (!level.hasChunkAt(parent)) {
            return false;
        }
        BlockState state = level.getBlockState(parent);
        return state.is(wetConcrete) || state.is(WET_CONCRETE_ROADS);
    }

    private static final class FlowTask {
        private final Set<BlockPos> ownedWet = new HashSet<>();
        private final Map<BlockPos, ReservedBlock> reservations = new LinkedHashMap<>();

        private void merge(FlowTask other) {
            ownedWet.addAll(other.ownedWet);
            other.reservations.forEach(reservations::putIfAbsent);
        }

        private void placeDueReservations(ServerLevel level, Block wetConcrete, long gameTime) {
            List<ReservedBlock> due = reservations.values().stream()
                .filter(reservation -> reservation.dueAt <= gameTime)
                .sorted(Comparator.comparingLong(reservation -> reservation.dueAt))
                .toList();

            for (ReservedBlock reservation : due) {
                reservations.remove(reservation.pos);
                if (reservation.parent == null
                    || !isParentWet(level, reservation.parent, wetConcrete)
                    || !isSpreadable(level, reservation.pos)) {
                    continue;
                }

                if (level.setBlock(
                    reservation.pos,
                    wetConcrete.defaultBlockState(),
                    UPDATE_FLAGS
                )) {
                    ownedWet.add(reservation.pos);
                }
            }
        }
    }

    private record ReservedBlock(BlockPos pos, BlockPos parent, long dueAt) {
    }

    private record PlanningNode(BlockPos pos, BlockPos parent, long dueAt, NodeType type) {
    }

    private record PlanningResult(
        LinkedHashMap<BlockPos, ReservedBlock> newReservations,
        Set<BlockPos> visitedWet,
        Set<FlowTask> connectedTasks
    ) {
    }

    private record PlanningContext(
        Map<BlockPos, ReservedBlock> reservations,
        Map<BlockPos, FlowTask> reservationOwners,
        Map<BlockPos, FlowTask> wetOwners
    ) {
        private static PlanningContext create(ServerLevel level) {
            Map<BlockPos, ReservedBlock> reservations = new HashMap<>();
            Map<BlockPos, FlowTask> reservationOwners = new HashMap<>();
            Map<BlockPos, FlowTask> wetOwners = new HashMap<>();
            for (FlowTask task : TASKS.getOrDefault(level, List.of())) {
                for (Map.Entry<BlockPos, ReservedBlock> entry : task.reservations.entrySet()) {
                    reservations.put(entry.getKey(), entry.getValue());
                    reservationOwners.put(entry.getKey(), task);
                }
                for (BlockPos pos : task.ownedWet) {
                    wetOwners.put(pos, task);
                }
            }
            return new PlanningContext(reservations, reservationOwners, wetOwners);
        }
    }

    private enum NodeType {
        WET,
        RESERVED,
        PLANNED
    }

    private ConcreteFlowManager() {
    }
}
