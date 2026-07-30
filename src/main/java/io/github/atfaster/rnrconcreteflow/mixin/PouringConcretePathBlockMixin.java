package io.github.atfaster.rnrconcreteflow.mixin;

import com.therighthon.rnr.common.block.PouringConcretePathBlock;
import io.github.atfaster.rnrconcreteflow.config.RnrConcreteFlowConfig;
import io.github.atfaster.rnrconcreteflow.flow.ConcreteFlowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PouringConcretePathBlock.class)
public abstract class PouringConcretePathBlockMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void rnrConcreteFlow$scheduleConfiguredFirstWave(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState previousState,
        boolean movedByPiston,
        CallbackInfo callback
    ) {
        if (level.isClientSide || !RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()) {
            return;
        }

        callback.cancel();
        level.scheduleTick(
            pos,
            (Block) (Object) this,
            RnrConcreteFlowConfig.CONCRETE_SPREAD_INTERVAL_TICKS.get()
        );
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void rnrConcreteFlow$fillConnectedFoundation(
        BlockState state,
        ServerLevel level,
        BlockPos origin,
        RandomSource random,
        CallbackInfo callback
    ) {
        if (!RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()) {
            return;
        }

        callback.cancel();
        ConcreteFlowManager.start(level, origin);
    }
}
