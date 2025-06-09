package com.daohe.autumnmod;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BossBarHandler {
    // 控制Boss Bar是否显示
    @SubscribeEvent
    public void onRenderBossBar(RenderGameOverlayEvent event) {
        if (!AutumnMod.isBossBarVisible && event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
            event.setCanceled(true);
        }
    }
}