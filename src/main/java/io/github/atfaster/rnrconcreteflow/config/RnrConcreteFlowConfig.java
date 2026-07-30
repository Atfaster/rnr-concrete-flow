package io.github.atfaster.rnrconcreteflow.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RnrConcreteFlowConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CONNECTED_CONCRETE_FLOW;
    public static final ForgeConfigSpec.IntValue CONCRETE_SPREAD_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SINGLE_SIDE_RNR_ROAD_SUPPORT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PARTIAL_CONCRETE_MIXING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE_CONNECTED_CONCRETE_FLOW = builder
            .comment("Fill up to 25 nearest connected RNR road foundations instead of using the original radius.")
            .define("enableConnectedConcreteFlow", true);

        CONCRETE_SPREAD_INTERVAL_TICKS = builder
            .comment("Delay between concrete spread waves in ticks. RNR's original delay is 10 ticks.")
            .defineInRange("concreteSpreadIntervalTicks", 10, 1, 1200);

        ENABLE_SINGLE_SIDE_RNR_ROAD_SUPPORT = builder
            .comment("Keep RNR landslide-enabled road blocks stable when supported below and by at least one horizontal side.")
            .define("enableSingleSideRnrRoadSupport", true);

        ENABLE_PARTIAL_CONCRETE_MIXING = builder
            .comment("Allow RNR concrete powder to process when a TFC barrel contains excess water.")
            .define("enablePartialConcreteMixing", true);

        SPEC = builder.build();
    }

    private RnrConcreteFlowConfig() {
    }
}
