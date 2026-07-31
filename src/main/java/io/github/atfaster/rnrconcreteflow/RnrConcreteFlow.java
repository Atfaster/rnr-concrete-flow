package io.github.atfaster.rnrconcreteflow;

import com.mojang.logging.LogUtils;
import io.github.atfaster.rnrconcreteflow.config.RnrConcreteFlowConfig;
import io.github.atfaster.rnrconcreteflow.flow.ConcreteFlowManager;
import io.github.atfaster.rnrconcreteflow.flow.WetConcreteInteractionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(RnrConcreteFlow.MOD_ID)
public final class RnrConcreteFlow {
    public static final String MOD_ID = "rnr_concrete_flow";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RnrConcreteFlow() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RnrConcreteFlowConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(ConcreteFlowManager.class);
        MinecraftForge.EVENT_BUS.register(new WetConcreteInteractionHandler());
        LOGGER.info("[RNR Concrete Flow] Concrete flow, road stability, and barrel mixing improvements are enabled.");
    }
}
