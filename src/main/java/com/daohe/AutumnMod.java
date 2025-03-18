package com.daohe;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Mod(modid = AutumnMod.MODID, version = "1.3", clientSideOnly = true)
public class AutumnMod {
    public static final String MODID = "autumnmod";
    public static final String VERSION = "1.0";
    public static final String CONFIG_FILE_NAME = "autumnmod.json";

    private static final Logger LOGGER = LogManager.getLogger(MODID);
    public static boolean isBossBarVisible = true;
    public static boolean isItemAlertEnabled = true;
    private static File configFile;

    private UhcCraftHelper uhcCraftHelper;
    private ItemChecker itemChecker;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = new File(event.getModConfigurationDirectory(), CONFIG_FILE_NAME);
        loadConfig();

        uhcCraftHelper = new UhcCraftHelper();
        uhcCraftHelper.preInit(event);

        itemChecker = new ItemChecker();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        registerCommands();
        MinecraftForge.EVENT_BUS.register(new BossBarHandler());
        MinecraftForge.EVENT_BUS.register(uhcCraftHelper);
        MinecraftForge.EVENT_BUS.register(itemChecker);
        Autocraft.init();
        LOGGER.info(LanguageLoader.format("log.init.success"));
    }

    private void registerCommands() {
        ClientCommandHandler.instance.registerCommand(new AutumnCommand(uhcCraftHelper));
        PlayCommands.registerCommands();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            LOGGER.warn(LanguageLoader.format("log.config.notfound"));
            saveConfig();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            Config config = gson.fromJson(reader, Config.class);
            if (config != null) {
                isBossBarVisible = config.isBossBarVisible;
                UhcCraftHelper.isCraftHelperVisible = config.isCraftHelperVisible;
                isItemAlertEnabled = config.isItemAlertEnabled;
                LanguageLoader.setLanguage(config.currentLanguage != null ? config.currentLanguage : "en");
                LOGGER.info(LanguageLoader.format("log.config.load.success"));
            }
        } catch (IOException e) {
            LOGGER.error(LanguageLoader.format("log.config.load.failed", configFile.getAbsolutePath()), e);
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Config config = new Config();
            config.isBossBarVisible = isBossBarVisible;
            config.isCraftHelperVisible = UhcCraftHelper.isCraftHelperVisible;
            config.isItemAlertEnabled = isItemAlertEnabled;
            config.currentLanguage = LanguageLoader.getCurrentLanguage();
            gson.toJson(config, writer);
            LOGGER.info(LanguageLoader.format("log.config.save.success"));
        } catch (IOException e) {
            LOGGER.error(LanguageLoader.format("log.config.save.failed", configFile.getAbsolutePath()), e);
        }
    }

    private static class Config {
        public boolean isBossBarVisible = true;
        public boolean isCraftHelperVisible = true;
        public boolean isItemAlertEnabled = true;
        public String currentLanguage = "en";
    }
}