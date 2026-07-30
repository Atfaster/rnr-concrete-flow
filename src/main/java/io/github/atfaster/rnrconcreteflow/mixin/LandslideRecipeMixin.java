package io.github.atfaster.rnrconcreteflow.mixin;

import io.github.atfaster.rnrconcreteflow.config.RnrConcreteFlowConfig;
import net.dries007.tfc.common.entities.misc.TFCFallingBlockEntity;
import net.dries007.tfc.common.recipes.LandslideRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LandslideRecipe.class, remap = false)
public abstract class LandslideRecipeMixin {
    @Inject(method = "getLandslidePos", at = @At("HEAD"), cancellable = true)
    private static void rnrConcreteFlow$acceptOneHorizontalSupport(
        Level level,
        BlockPos pos,
        BlockState state,
        CallbackInfoReturnable<BlockPos> callback
    ) {
        if (!RnrConcreteFlowConfig.ENABLE_SINGLE_SIDE_RNR_ROAD_SUPPORT.get()) {
            return;
        }

        LandslideRecipe recipe = LandslideRecipe.getRecipe(state);
        if (recipe == null || !"rnr".equals(recipe.getId().getNamespace())) {
            return;
        }

        if (TFCFallingBlockEntity.canFallThrough(level, pos.below(), Direction.DOWN, state)) {
            return;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (LandslideRecipe.isSupportedOnSide(level, pos, direction)) {
                callback.setReturnValue(null);
                return;
            }
        }
    }
}
