package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public class Autocraft {
    private static final Logger LOGGER = LogManager.getLogger(AutumnMod.MODID);

    private static final String CLICK_SOUND = "autumnmod:click";

    public static final KeyBinding[] AUTO_CRAFT_KEYS = new KeyBinding[] {
            new KeyBinding("金头", Keyboard.KEY_K, "AutumnMod"),
            new KeyBinding("简易金苹果", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("烈焰棒", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("无敌药", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("骷髅马", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("肃清", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("镰刀", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("龙剑", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("龙甲", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("聚变甲", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("锋利书", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("冶铁", Keyboard.KEY_NONE, "AutumnMod"),
            new KeyBinding("冶金", Keyboard.KEY_NONE, "AutumnMod")
    };

    private static final String[] CRAFT_COMMANDS = new String[] {
            "/internal_autocraftitem GOLDEN_HEAD",
            "/internal_autocraftitem LIGHT_APPLE",
            "/internal_autocraftitem BLAZE_ROD",
            "/internal_autocraftitem DEUS_EX_MACHINA",
            "/internal_autocraftitem DAREDEVIL",
            "/internal_autocraftitem FLASK_OF_CLEANSING",
            "/internal_autocraftitem DEATHS_SCYTHE",
            "/internal_autocraftitem DRAGON_SWORD",
            "/internal_autocraftitem DRAGON_ARMOR",
            "/internal_autocraftitem FUSION_ARMOR",
            "/internal_autocraftitem SHARP_ONE_BOOK",
            "/internal_autocraftitem IRON_INGOTS",
            "/internal_autocraftitem GOLD_PACK"
    };

    public static void init() {
        for (int i = 0; i < AUTO_CRAFT_KEYS.length; i++) {
            ClientRegistry.registerKeyBinding(AUTO_CRAFT_KEYS[i]);
        }
        MinecraftForge.EVENT_BUS.register(new Autocraft());
        LOGGER.info(LanguageLoader.format("log.autocraft.init"));
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            for (int i = 0; i < AUTO_CRAFT_KEYS.length; i++) {
                if (AUTO_CRAFT_KEYS[i].isPressed()) {
                    mc.thePlayer.sendChatMessage(CRAFT_COMMANDS[i]);
                    mc.thePlayer.playSound(CLICK_SOUND, 1.0F, 1.0F);
                    LOGGER.info(LanguageLoader.format("log.autocraft.trigger", CRAFT_COMMANDS[i]));
                }
            }
        }
    }
}