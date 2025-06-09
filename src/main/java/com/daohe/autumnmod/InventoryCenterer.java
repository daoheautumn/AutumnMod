package com.daohe.autumnmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.lang.reflect.Field;
import java.util.List;

public class InventoryCenterer {
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!AutumnMod.isInventoryCenterEnabled) return;

        if (event.phase == TickEvent.Phase.START) {
            if (mc.currentScreen != null && mc.currentScreen instanceof InventoryEffectRenderer) {
                try {
                    InventoryEffectRenderer ier = (InventoryEffectRenderer) mc.currentScreen;
                    // 检查是否有药水效果
                    boolean hasActivePotionEffects = getBooleanField(InventoryEffectRenderer.class, "field_147045_u", "hasActivePotionEffects", ier, false);
                    if (hasActivePotionEffects) {
                        // 居中物品栏
                        int xSize = getIntField(GuiContainer.class, "field_146999_f", "xSize", ier, 176);
                        int guiLeft = (ier.width - xSize) / 2;
                        setIntField(GuiContainer.class, "field_147003_i", "guiLeft", ier, guiLeft);
                    }

                    // 调整位置
                    List<?> buttonList = getListField(GuiScreen.class, "field_146292_n", "buttonList", ier);
                    if (buttonList != null) {
                        for (Object o : buttonList) {
                            if (o instanceof GuiButton) {
                                GuiButton button = (GuiButton) o;
                                int xSize = getIntField(GuiContainer.class, "field_146999_f", "xSize", ier, 176);
                                int guiLeft = (ier.width - xSize) / 2;
                                if (button.id == 101) { // 药水效果
                                    button.xPosition = guiLeft;
                                } else if (button.id == 102) { // 配方书
                                    button.xPosition = guiLeft + xSize - 20;
                                } else if (button.id == 55) {
                                    button.xPosition = guiLeft + 66;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    AutumnMod.LOGGER.error("Failed to center inventory: {}", e.getMessage());
                }
            }
        }
    }

    private boolean getBooleanField(Class<?> clazz, String srgFieldName, String mcpFieldName, Object instance, boolean defaultValue) {
        try {
            Field field = isObfuscatedEnv() ? clazz.getDeclaredField(srgFieldName) : clazz.getDeclaredField(mcpFieldName);
            field.setAccessible(true);
            return field.getBoolean(instance);
        } catch (Exception e) {
            AutumnMod.LOGGER.error("Failed to get boolean field {}: {}", mcpFieldName, e.getMessage());
            return defaultValue;
        }
    }

    private int getIntField(Class<?> clazz, String srgFieldName, String mcpFieldName, Object instance, int defaultValue) {
        try {
            Field field = isObfuscatedEnv() ? clazz.getDeclaredField(srgFieldName) : clazz.getDeclaredField(mcpFieldName);
            field.setAccessible(true);
            return field.getInt(instance);
        } catch (Exception e) {
            AutumnMod.LOGGER.error("Failed to get int field {}: {}", mcpFieldName, e.getMessage());
            return defaultValue;
        }
    }

    private void setIntField(Class<?> clazz, String srgFieldName, String mcpFieldName, Object instance, int newValue) {
        try {
            Field field = isObfuscatedEnv() ? clazz.getDeclaredField(srgFieldName) : clazz.getDeclaredField(mcpFieldName);
            field.setAccessible(true);
            field.setInt(instance, newValue);
        } catch (Exception e) {
            AutumnMod.LOGGER.error("Failed to set int field {}: {}", mcpFieldName, e.getMessage());
        }
    }

    private List<?> getListField(Class<?> clazz, String srgFieldName, String mcpFieldName, Object instance) {
        try {
            Field field = isObfuscatedEnv() ? clazz.getDeclaredField(srgFieldName) : clazz.getDeclaredField(mcpFieldName);
            field.setAccessible(true);
            return (List<?>) field.get(instance);
        } catch (Exception e) {
            AutumnMod.LOGGER.error("Failed to get list field {}: {}", mcpFieldName, e.getMessage());
            return null;
        }
    }

    private boolean isObfuscatedEnv() {
        try {
            Class.forName("net.minecraft.init.Blocks").getField("air");
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}