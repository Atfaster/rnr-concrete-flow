package io.github.atfaster.rnrconcreteflow.mixin;

import io.github.atfaster.rnrconcreteflow.config.RnrConcreteFlowConfig;
import net.dries007.tfc.common.recipes.InstantBarrelRecipe;
import net.dries007.tfc.common.recipes.inventory.BarrelInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InstantBarrelRecipe.class, remap = false)
public abstract class InstantBarrelRecipeMixin {
    private static final ResourceLocation RNR_CONCRETE_RECIPE =
        new ResourceLocation("rnr", "barrel/concrete_from_powder");

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void rnrConcreteFlow$allowExcessWater(
        BarrelInventory inventory,
        Level level,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValue() || !RnrConcreteFlowConfig.ENABLE_PARTIAL_CONCRETE_MIXING.get()) {
            return;
        }

        InstantBarrelRecipe recipe = (InstantBarrelRecipe) (Object) this;
        if (!RNR_CONCRETE_RECIPE.equals(recipe.getId())) {
            return;
        }

        if (recipe.getInputItem().test(inventory.getStackInSlot(2))
            && recipe.getInputFluid().test(inventory.getFluidInTank(0))) {
            callback.setReturnValue(true);
        }
    }
}
