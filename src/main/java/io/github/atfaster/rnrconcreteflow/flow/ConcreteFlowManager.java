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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConcreteFlowManager {
    static final int MAX_BLOCKS = 25;
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL;
    private static final ResourceLocation WET_CONCRETE_ID =
        new ResourceLocation("rnr", "wet_concrete_road");
    private static final TagKey<Block> CONCRETE_SPREADABLE =
        TagKey.create(Registries.BLOCK, new ResourceLocation("rnr", "concrete_spreadable"));
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

        FlowTask task = new FlowTask();
        task.selected.add(immutableOrigin);
        task.frontier.add(immutableOrigin);
        advanceWave(level, task, wetConcrete);

        if (!task.frontier.isEmpty() && task.selected.size() < MAX_BLOCKS) {
            task.nextWaveAt = level.getGameTime() + RnrConcreteFlowConfig.CONCRETE_SPREAD_INTERVAL_TICKS.get();
            TASKS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(task);
        }
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
            if (gameTime < task.nextWaveAt) {
                continue;
            }

            advanceWave(level, task, wetConcrete);
            if (task.frontier.isEmpty() || task.selected.size() >= MAX_BLOCKS) {
                iterator.remove();
            } else {
                task.nextWaveAt = gameTime + RnrConcreteFlowConfig.CONCRETE_SPREAD_INTERVAL_TICKS.get();
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

    private static void advanceWave(ServerLevel level, FlowTask task, Block wetConcrete) {
        List<BlockPos> currentFrontier = task.frontier;
        List<BlockPos> nextFrontier = new ArrayList<>();

        for (BlockPos current : currentFrontier) {
            if (!level.hasChunkAt(current) || !level.getBlockState(current).is(wetConcrete)) {
                continue;
            }

            for (Direction direction : DIRECTIONS) {
                if (task.selected.size() >= MAX_BLOCKS) {
                    break;
                }

                RoadFlowRules.Position currentPosition =
                    new RoadFlowRules.Position(current.getX(), current.getY(), current.getZ());
                RoadFlowRules.findCandidate(
                    currentPosition,
                    direction.getStepX(),
                    direction.getStepZ(),
                    position -> classify(level, position)
                ).ifPresent(position -> selectAndFill(
                    level,
                    new BlockPos(position.x(), position.y(), position.z()),
                    task,
                    nextFrontier,
                    wetConcrete
                ));
            }
        }

        task.frontier = nextFrontier;
    }

    private static RoadFlowRules.Cell classify(ServerLevel level, RoadFlowRules.Position position) {
        BlockPos pos = new BlockPos(position.x(), position.y(), position.z());
        if (!level.hasChunkAt(pos)) {
            return RoadFlowRules.Cell.UNLOADED;
        }

        BlockState state = level.getBlockState(pos);
        if (state.is(CONCRETE_SPREADABLE)) {
            return RoadFlowRules.Cell.SPREADABLE;
        }
        return state.isAir() ? RoadFlowRules.Cell.AIR : RoadFlowRules.Cell.BLOCKED;
    }

    private static void selectAndFill(
        ServerLevel level,
        BlockPos candidate,
        FlowTask task,
        List<BlockPos> nextFrontier,
        Block wetConcrete
    ) {
        BlockPos immutableCandidate = candidate.immutable();
        if (!task.selected.add(immutableCandidate)) {
            return;
        }

        if (level.setBlock(immutableCandidate, wetConcrete.defaultBlockState(), UPDATE_FLAGS)) {
            nextFrontier.add(immutableCandidate);
        } else {
            task.selected.remove(immutableCandidate);
        }
    }

    private static final class FlowTask {
        private final Set<BlockPos> selected = new HashSet<>();
        private List<BlockPos> frontier = new ArrayList<>();
        private long nextWaveAt;
    }

    private ConcreteFlowManager() {
    }
}
