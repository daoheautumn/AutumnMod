package com.daohe;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BossBarHandler {
    @SubscribeEvent
    public void onRenderBossBar(RenderGameOverlayEvent event) {
        if (!AutumnMod.isBossBarVisible && event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
            event.setCanceled(true);
        }
    }
}