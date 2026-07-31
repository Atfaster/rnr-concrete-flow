package io.github.atfaster.rnrconcreteflow.flow;

import io.github.atfaster.rnrconcreteflow.config.RnrConcreteFlowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;

public final class WetConcreteInteractionHandler {
    private static final ResourceLocation CONCRETE_FLUID_ID =
        new ResourceLocation("rnr", "concrete");

    public static InteractionResult tryHandle(
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand
    ) {
        if (!RnrConcreteFlowConfig.ENABLE_CONNECTED_CONCRETE_FLOW.get()
            || player.isShiftKeyDown()) {
            return null;
        }

        boolean wetRoad = ConcreteFlowManager.isWetRoad(level.getBlockState(pos));
        boolean foundation = ConcreteFlowManager.isSpreadableRoad(level.getBlockState(pos));
        if (!wetRoad && !foundation) {
            return null;
        }

        Fluid concrete = ForgeRegistries.FLUIDS.getValue(CONCRETE_FLUID_ID);
        if (concrete == null) {
            return null;
        }

        ItemStack heldStack = player.getItemInHand(hand);
        IFluidHandlerItem handler = heldStack
            .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
            .resolve()
            .orElse(null);
        if (handler == null) {
            return null;
        }

        int availableConcrete = availableConcrete(handler, concrete);
        if (availableConcrete == 0) {
            return null;
        }

        int requestedBlocks = ConcretePouringRules.availableBlocks(availableConcrete);
        if (requestedBlocks == 0 || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (wetRoad) {
            ConcreteFlowManager.addCapacity(
                serverLevel,
                pos,
                requestedBlocks,
                acceptedBlocks -> payForReservations(
                    player,
                    hand,
                    handler,
                    concrete,
                    acceptedBlocks
                )
            );
        } else {
            ConcreteFlowManager.startFromFoundation(
                serverLevel,
                pos,
                requestedBlocks,
                acceptedBlocks -> payForReservations(
                    player,
                    hand,
                    handler,
                    concrete,
                    acceptedBlocks
                )
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int availableConcrete(IFluidHandlerItem handler, Fluid concrete) {
        long total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack contents = handler.getFluidInTank(tank);
            if (!contents.isEmpty() && contents.getFluid().isSame(concrete)) {
                total += contents.getAmount();
                if (total >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return (int) total;
    }

    private static boolean payForReservations(
        Player player,
        InteractionHand hand,
        IFluidHandlerItem handler,
        Fluid concrete,
        int acceptedBlocks
    ) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        int requiredAmount = ConcretePouringRules.concreteForBlocks(acceptedBlocks);
        FluidStack request = new FluidStack(concrete, requiredAmount);
        FluidStack simulated = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.getAmount() != requiredAmount || !simulated.getFluid().isSame(concrete)) {
            return false;
        }

        FluidStack drained = handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != requiredAmount || !drained.getFluid().isSame(concrete)) {
            return false;
        }

        player.setItemInHand(hand, handler.getContainer());
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = tryHandle(
            event.getLevel(),
            event.getPos(),
            event.getEntity(),
            event.getHand()
        );
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    public WetConcreteInteractionHandler() {
    }
}
