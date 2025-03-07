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

@Mod(modid = AutumnMod.MODID, version = "1.2", clientSideOnly = true)
public class AutumnMod {
    public static final String MODID = "autumnmod";
    public static final String VERSION = "1.0";
    public static final String CONFIG_FILE_NAME = "autumnmod.json";

    private static final Logger LOGGER = LogManager.getLogger(MODID);
    public static boolean isBossBarVisible = true;
    private static File configFile;

    private UhcCraftHelper uhcCraftHelper;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = new File(event.getModConfigurationDirectory(), CONFIG_FILE_NAME);
        loadConfig();

        uhcCraftHelper = new UhcCraftHelper();
        uhcCraftHelper.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        registerCommands();
        MinecraftForge.EVENT_BUS.register(new BossBarHandler());
        MinecraftForge.EVENT_BUS.register(uhcCraftHelper);
        Autocraft.init();
        LOGGER.info("AutumnMod initialized successfully.");
    }

    private void registerCommands() {
        ClientCommandHandler.instance.registerCommand(new ToggleBossBarCommand());
        ClientCommandHandler.instance.registerCommand(new CraftHelperReloadCommand(uhcCraftHelper));
        PlayCommands.registerCommands();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            LOGGER.warn("Config file not found, using default settings.");
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            Config config = gson.fromJson(reader, Config.class);
            if (config != null) {
                isBossBarVisible = config.isBossBarVisible;
                LOGGER.info("Config loaded successfully.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: {}", configFile.getAbsolutePath(), e);
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Config config = new Config();
            config.isBossBarVisible = isBossBarVisible;
            gson.toJson(config, writer);
            LOGGER.info("Config saved successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: {}", configFile.getAbsolutePath(), e);
        }
    }

    private static class Config {
        public boolean isBossBarVisible = true;
    }
}