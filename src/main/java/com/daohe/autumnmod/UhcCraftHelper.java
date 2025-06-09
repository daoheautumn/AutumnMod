package com.daohe.autumnmod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UhcCraftHelper {
    private static final Logger LOGGER = LogManager.getLogger(AutumnMod.MODID);
    public static boolean isCraftHelperVisible = false;
    private boolean shouldShowElements = false;
    private int gridBaseX;
    private int gridBaseY;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_ROWS = 4;
    private static final int GRID_COLS = 4;
    private static final int TOTAL_ROWS = 5;
    private ItemStack[][][] gridItems;
    private String[][] clickCommands;
    private String[][] displayNames;
    private boolean[][] isFusion;
    private boolean[][] isMeal;
    private boolean[][] isModular;
    private boolean[][] isHoly;
    private boolean[][] isSwan;
    private boolean[][] isGhead;
    private boolean[][] isBackpack;
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 200;
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(AutumnMod.MODID, "textures/gui/background.png");
    private static final ResourceLocation DARK_BACKGROUND_TEXTURE = new ResourceLocation(AutumnMod.MODID, "textures/gui/darkbackground.png");
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageRowY;
    private static final String CLICK_SOUND = "autumnmod:click";
    private static File fixedConfigFile;
    private static File dynamicConfigFile;
    private static final ResourceLocation FIXED_CONFIG_RESOURCE = new ResourceLocation(AutumnMod.MODID, "config/uhc_craft_items.json");
    private static final ResourceLocation DYNAMIC_CONFIG_RESOURCE = new ResourceLocation(AutumnMod.MODID, "config/all_uhc_items.json");

    // 使用新的CoreMod替代原有的鼠标锁定机制
    /*
    private static boolean preventMouseReset = false;
    private static long preventMouseResetTime = 0;
    private static final long PREVENT_RESET_DURATION = 800;
    private static int savedMouseX = -1;
    private static int savedMouseY = -1;
     */

    private List<ItemConfig> recipes = new ArrayList<>();
    private ItemStack[] lastInventory = new ItemStack[36];

    public UhcCraftHelper() {
        gridItems = new ItemStack[totalPages][GRID_ROWS * GRID_COLS][];
        clickCommands = new String[totalPages][GRID_ROWS * GRID_COLS];
        displayNames = new String[totalPages][GRID_ROWS * GRID_COLS];
        isFusion = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isMeal = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isModular = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isHoly = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isSwan = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isGhead = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isBackpack = new boolean[totalPages][GRID_ROWS * GRID_COLS];
    }

    // 加载配置
    public void preInit(FMLPreInitializationEvent event) {
        fixedConfigFile = new File(event.getModConfigurationDirectory(), "uhc_craft_items.json");
        dynamicConfigFile = new File(event.getModConfigurationDirectory(), "all_uhc_items.json");
        copyDefaultConfigFromResources();
        reloadItems();
    }

    // 复制默认配置文件
    private void copyDefaultConfigFromResources() {
        // Copy uhc_craft_items.json
        if (!fixedConfigFile.exists()) {
            try (InputStream inputStream = Minecraft.class.getResourceAsStream("/assets/" + AutumnMod.MODID + "/config/uhc_craft_items.json")) {
                if (inputStream == null) {
                    LOGGER.error("Default uhc_craft_items config not found in resources.");
                } else {
                    try (FileOutputStream outputStream = new FileOutputStream(fixedConfigFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to copy default uhc_craft_items config: {}", e.getMessage());
            }
        }
        // Copy all_uhc_items.json
        if (!dynamicConfigFile.exists()) {
            try (InputStream inputStream = Minecraft.class.getResourceAsStream("/assets/" + AutumnMod.MODID + "/config/all_uhc_items.json")) {
                if (inputStream == null) {
                    LOGGER.error("Default all_uhc_items config not found in resources.");
                } else {
                    try (FileOutputStream outputStream = new FileOutputStream(dynamicConfigFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to copy default all_uhc_items config: {}", e.getMessage());
            }
        }
    }

    // 重新加载物品配置
    public void reloadItems() {
        if (AutumnMod.isDynamicCraftMode) {
            loadDynamicItems();
            updateGridItems();
        } else {
            loadFixedItems();
        }
        currentPage = Math.min(currentPage, totalPages);
    }

    // 加载固定模式（uhc_craft_items.json）
    private void loadFixedItems() {
        try (FileReader reader = new FileReader(fixedConfigFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<PageConfig>>() {}.getType();
            List<PageConfig> pages = gson.fromJson(reader, type);
            if (pages == null || pages.isEmpty()) {
                LOGGER.warn("UHC craft items config is empty or null, copying default.");
                copyDefaultConfigFromResources();
                return;
            }
            totalPages = pages.size();
            gridItems = new ItemStack[totalPages][GRID_ROWS * GRID_COLS][];
            clickCommands = new String[totalPages][GRID_ROWS * GRID_COLS];
            displayNames = new String[totalPages][GRID_ROWS * GRID_COLS];
            isFusion = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            isMeal = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            isModular = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            isHoly = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            isSwan = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            isGhead = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            isBackpack = new boolean[totalPages][GRID_ROWS * GRID_COLS];
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PageConfig page = pages.get(pageIndex);
                for (int slot = 0; slot < GRID_ROWS * GRID_COLS; slot++) {
                    ItemConfig itemConfig = page.items.get(slot);
                    ItemStack[] stacks = null;
                    if (itemConfig.fusion) {
                        stacks = new ItemStack[4];
                        Item[] diamondArmors = {
                                Item.getByNameOrId("minecraft:diamond_helmet"),
                                Item.getByNameOrId("minecraft:diamond_chestplate"),
                                Item.getByNameOrId("minecraft:diamond_leggings"),
                                Item.getByNameOrId("minecraft:diamond_boots")
                        };
                        for (int i = 0; i < 4; i++) {
                            if (diamondArmors[i] != null) {
                                ItemStack stack = new ItemStack(diamondArmors[i]);
                                stack.stackSize = Math.min(itemConfig.count, diamondArmors[i].getItemStackLimit());
                                stack.setItemDamage(itemConfig.damage);
                                if (itemConfig.nbt != null && !itemConfig.nbt.isEmpty()) {
                                    try {
                                        NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(itemConfig.nbt);
                                        stack.setTagCompound(nbt);
                                    } catch (NBTException e) {
                                        LOGGER.error("Failed to parse NBT for fusion item {}: {}", itemConfig.item, e.getMessage());
                                    }
                                }
                                if ("true".equalsIgnoreCase(itemConfig.enchant)) {
                                    NBTTagCompound nbt = stack.getTagCompound();
                                    if (nbt == null) {
                                        nbt = new NBTTagCompound();
                                        stack.setTagCompound(nbt);
                                    }
                                    NBTTagList enchantments = new NBTTagList();
                                    NBTTagCompound durabilityEnchant = new NBTTagCompound();
                                    durabilityEnchant.setShort("id", (short) 34);
                                    durabilityEnchant.setShort("lvl", (short) 1);
                                    enchantments.appendTag(durabilityEnchant);
                                    nbt.setTag("ench", enchantments);
                                }
                                if (itemConfig.display_name != null && !itemConfig.display_name.isEmpty()) {
                                    stack.setStackDisplayName(itemConfig.display_name);
                                }
                                stacks[i] = stack;
                            }
                        }
                        isFusion[pageIndex][slot] = true;
                    } else if (itemConfig.meal || itemConfig.holy || itemConfig.swan || itemConfig.modular ||
                            itemConfig.ghead || itemConfig.backpack ||
                            (itemConfig.item != null && !itemConfig.item.isEmpty() && !itemConfig.item.equals("minecraft:air"))) {
                        Item item = Item.getByNameOrId(itemConfig.item);
                        if (item != null) {
                            ItemStack stack = new ItemStack(item);
                            stack.stackSize = Math.min(itemConfig.count, item.getItemStackLimit());
                            stack.setItemDamage(itemConfig.damage);
                            if (itemConfig.modular) {
                                NBTTagCompound nbt = new NBTTagCompound();
                                NBTTagCompound display = new NBTTagCompound();
                                NBTTagList lore = new NBTTagList();
                                lore.appendTag(new NBTTagString("§aCurrent Mode: §6Lighting§8 [1]"));
                                display.setTag("Lore", lore);
                                nbt.setTag("display", display);
                                stack.setTagCompound(nbt);
                            } else if (itemConfig.ghead) {
                                try {
                                    NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(
                                            "{SkullOwner: {Id: \"b004b060-d04a-4a4d-83ca-78720929e1b4\", hypixelPopulated: 1b, Properties: {textures: [{Value: \"eyJ0aW1lc3RhbXAiOjE0ODUwMjM0NDEyNzAsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y5MzdlMWM0NWJiOGRhMjliMmM1NjRkZDlhN2RhNzgwZGQyZmU1NDQ2OGE1ZGZiNDExM2I0ZmY2NThmMDQzZTEifX19\"}]}}, display: {Lore: [\"Absorption (2:00)\", \"Regeneration II (0:05)\", \"*Given to all team members*\"], Name: \"Golden Head\"}, ExtraAttributes: {UHCid: 9001L}}"
                                    );
                                    stack.setTagCompound(nbt);
                                } catch (NBTException e) {
                                    LOGGER.error("Failed to parse NBT for ghead item {}: {}", itemConfig.item, e.getMessage());
                                }
                            } else if (itemConfig.backpack) {
                                try {
                                    NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(
                                            "{SkullOwner: {Id: \"e35b0038-972d-4bae-baf9-155bdbe03c7d\", hypixelPopulated: 1b, Properties: {textures: [{Value: \"eyJ0aW1lc3RhbXAiOjE1NjgyMTI5NjI1MzMsInByb2ZpbGVJZCI6IjgyYzYwNmM1YzY1MjRiNzk4YjkxYTEyZDNhNjE2OTc3IiwicHJvZmlsZU5hbWUiOiJOb3ROb3RvcmlvdXNOZW1vIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMWQ4MzdjYTIyMmNiYzBiYzEyNDI2ZjVkYTAxOGMzYTkzMWI0MDYwMDg4MDA5NjBhOWRmMTEyYTU5NmU3ZDYyIn19fQ==\"}]}, Name: \"e35b0038-972d-4bae-baf9-155bdbe03c7d\"}, display: {Lore: [\"Use this handy backpack to store\", \"some of those items which are\", \"cluttering up your inventory!\"], Name: \"Backpack\"}}"
                                    );
                                    stack.setTagCompound(nbt);
                                } catch (NBTException e) {
                                    LOGGER.error("Failed to parse NBT for backpack item {}: {}", itemConfig.item, e.getMessage());
                                }
                            } else if (itemConfig.nbt != null && !itemConfig.nbt.isEmpty()) {
                                try {
                                    NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(itemConfig.nbt);
                                    stack.setTagCompound(nbt);
                                } catch (NBTException e) {
                                    LOGGER.error("Failed to parse NBT for item {}: {}", itemConfig.item, e.getMessage());
                                }
                            }
                            if ("true".equalsIgnoreCase(itemConfig.enchant)) {
                                NBTTagCompound nbt = stack.getTagCompound();
                                if (nbt == null) {
                                    nbt = new NBTTagCompound();
                                    stack.setTagCompound(nbt);
                                }
                                boolean isEnchantedBook = itemConfig.item.equals("minecraft:enchanted_book");
                                String tagName = isEnchantedBook ? "StoredEnchantments" : "ench";
                                NBTTagList enchantments = new NBTTagList();
                                NBTTagCompound durabilityEnchant = new NBTTagCompound();
                                durabilityEnchant.setShort("id", (short) 34);
                                durabilityEnchant.setShort("lvl", (short) 1);
                                enchantments.appendTag(durabilityEnchant);
                                nbt.setTag(tagName, enchantments);
                            }
                            if (itemConfig.display_name != null && !itemConfig.display_name.isEmpty()) {
                                stack.setStackDisplayName(itemConfig.display_name);
                            }
                            stacks = new ItemStack[]{stack};
                        }
                        isMeal[pageIndex][slot] = itemConfig.meal;
                        isModular[pageIndex][slot] = itemConfig.modular;
                        isHoly[pageIndex][slot] = itemConfig.holy;
                        isSwan[pageIndex][slot] = itemConfig.swan;
                        isGhead[pageIndex][slot] = itemConfig.ghead;
                        isBackpack[pageIndex][slot] = itemConfig.backpack;
                    }
                    gridItems[pageIndex][slot] = stacks;
                    clickCommands[pageIndex][slot] = itemConfig.command;
                    displayNames[pageIndex][slot] = itemConfig.display_name;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load UHC craft items config: {}", e.getMessage());
            copyDefaultConfigFromResources();
        } catch (com.google.gson.JsonSyntaxException e) {
            LOGGER.error("Invalid JSON syntax in UHC craft items config: {}", e.getMessage());
            copyDefaultConfigFromResources();
        }
    }

    // 加载动态模式（all_uhc_items.json）
    private void loadDynamicItems() {
        try (FileReader reader = new FileReader(dynamicConfigFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<ItemConfig>>() {}.getType();
            recipes = gson.fromJson(reader, type);
            if (recipes == null || recipes.isEmpty()) {
                LOGGER.warn("All UHC items config is empty or null, copying default.");
                copyDefaultConfigFromResources();
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load all UHC items config: {}", e.getMessage());
            copyDefaultConfigFromResources();
        } catch (com.google.gson.JsonSyntaxException e) {
            LOGGER.error("Invalid JSON syntax in all UHC items config: {}", e.getMessage());
            copyDefaultConfigFromResources();
        }
    }

    // 检测玩家背包并更新
    private void updateGridItems() {
        if (!AutumnMod.isDynamicCraftMode) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        ItemStack[] currentInventory = mc.thePlayer.inventory.mainInventory;
        for (int i = 0; i < 36; i++) {
            lastInventory[i] = currentInventory[i] == null ? null : currentInventory[i].copy();
        }
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, Integer> itemTotalCounts = new HashMap<>();
        int diamondArmorCount = 0;
        int rawMeatCount = 0;
        int recordCount = 0;
        String[] recordItems = {
                "minecraft:record_ward", "minecraft:record_11", "minecraft:record_cat",
                "minecraft:record_far", "minecraft:record_wait", "minecraft:record_13",
                "minecraft:record_stal", "minecraft:record_blocks", "minecraft:record_mellohi",
                "minecraft:record_strd", "minecraft:record_mall", "minecraft:record_chirp"
        };
        for (ItemStack stack : currentInventory) {
            if (stack != null) {
                String itemId = Item.itemRegistry.getNameForObject(stack.getItem()).toString();
                int damage = stack.getItemDamage();
                String key = itemId + ":" + damage;
                // 排除金头和背包
                if (itemId.equals("minecraft:skull") && damage == 3 && stack.hasTagCompound() && isExcludedSkull(stack.getTagCompound())) {
                    continue;
                }
                itemCounts.merge(key, stack.stackSize, Integer::sum);
                itemTotalCounts.merge(itemId, stack.stackSize, Integer::sum);
                if (itemId.equals("minecraft:diamond_helmet") || itemId.equals("minecraft:diamond_chestplate") ||
                        itemId.equals("minecraft:diamond_leggings") || itemId.equals("minecraft:diamond_boots")) {
                    diamondArmorCount += stack.stackSize;
                }
                if (itemId.equals("minecraft:porkchop") || itemId.equals("minecraft:beef") ||
                        itemId.equals("minecraft:chicken") || itemId.equals("minecraft:mutton")) {
                    rawMeatCount += stack.stackSize;
                }
                for (String record : recordItems) {
                    if (itemId.equals(record)) {
                        recordCount += stack.stackSize;
                        break;
                    }
                }
            }
        }
        List<ItemConfig> availableRecipes = new ArrayList<>();
        for (ItemConfig recipe : recipes) {
            boolean hasMaterials = true;
            if (recipe.fusion) {
                if (diamondArmorCount < 5) {
                    hasMaterials = false;
                }
            } else if (recipe.meal) {
                int coalCount = itemCounts.getOrDefault("minecraft:coal:0", 0);
                if (coalCount < 1 || rawMeatCount < 8) {
                    hasMaterials = false;
                }
            } else if (recipe.holy) {
                int glassBottleCount = itemCounts.getOrDefault("minecraft:glass_bottle:0", 0);
                int redstoneBlockCount = itemCounts.getOrDefault("minecraft:redstone_block:0", 0);
                int goldIngotCount = itemCounts.getOrDefault("minecraft:gold_ingot:0", 0);
                if (glassBottleCount < 1 || redstoneBlockCount < 1 || goldIngotCount < 2 || recordCount < 1) {
                    hasMaterials = false;
                }
            } else if (recipe.swan) {
                int paperCount = itemCounts.getOrDefault("minecraft:paper:0", 0);
                int featherCount = itemCounts.getOrDefault("minecraft:feather:0", 0);
                if (paperCount < 3 || featherCount < 1 || recordCount < 1) {
                    hasMaterials = false;
                }
            } else {
                for (Ingredient ingredient : recipe.ingredients) {
                    String itemId = ingredient.item;
                    int requiredCount = ingredient.count;
                    int damage = ingredient.damage;
                    int availableCount;
                    if (damage == -1) {
                        availableCount = itemTotalCounts.getOrDefault(itemId, 0);
                    } else {
                        String key = itemId + ":" + damage;
                        availableCount = itemCounts.getOrDefault(key, 0);
                    }
                    if (availableCount < requiredCount) {
                        hasMaterials = false;
                        break;
                    }
                }
            }
            if (hasMaterials) {
                availableRecipes.add(recipe);
            }
        }

        totalPages = Math.max(1, (availableRecipes.size() + 15) / (GRID_ROWS * GRID_COLS));
        gridItems = new ItemStack[totalPages][GRID_ROWS * GRID_COLS][];
        clickCommands = new String[totalPages][GRID_ROWS * GRID_COLS];
        displayNames = new String[totalPages][GRID_ROWS * GRID_COLS];
        isFusion = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isMeal = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isModular = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isHoly = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isSwan = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isGhead = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        isBackpack = new boolean[totalPages][GRID_ROWS * GRID_COLS];
        for (int i = 0; i < availableRecipes.size(); i++) {
            int page = i / (GRID_ROWS * GRID_COLS);
            int slot = i % (GRID_ROWS * GRID_COLS);
            ItemConfig config = availableRecipes.get(i);
            ItemStack[] stacks = null;
            if (config.fusion) {
                stacks = new ItemStack[4];
                Item[] diamondArmors = {
                        Item.getByNameOrId("minecraft:diamond_helmet"),
                        Item.getByNameOrId("minecraft:diamond_chestplate"),
                        Item.getByNameOrId("minecraft:diamond_leggings"),
                        Item.getByNameOrId("minecraft:diamond_boots")
                };
                for (int j = 0; j < 4; j++) {
                    if (diamondArmors[j] != null) {
                        ItemStack stack = new ItemStack(diamondArmors[j]);
                        stack.stackSize = Math.min(config.count, diamondArmors[j].getItemStackLimit());
                        stack.setItemDamage(config.damage);
                        if (config.nbt != null && !config.nbt.isEmpty()) {
                            try {
                                NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(config.nbt);
                                stack.setTagCompound(nbt);
                            } catch (NBTException e) {
                                LOGGER.error("Failed to parse NBT for fusion item {}: {}", config.item, e.getMessage());
                            }
                        }
                        if ("true".equalsIgnoreCase(config.enchant)) {
                            NBTTagCompound nbt = stack.getTagCompound();
                            if (nbt == null) {
                                nbt = new NBTTagCompound();
                                stack.setTagCompound(nbt);
                            }
                            NBTTagList enchantments = new NBTTagList();
                            NBTTagCompound durabilityEnchant = new NBTTagCompound();
                            durabilityEnchant.setShort("id", (short) 34);
                            durabilityEnchant.setShort("lvl", (short) 1);
                            enchantments.appendTag(durabilityEnchant);
                            nbt.setTag("ench", enchantments);
                        }
                        if (config.display_name != null && !config.display_name.isEmpty()) {
                            stack.setStackDisplayName(config.display_name);
                        }
                        stacks[j] = stack;
                    }
                }
                isFusion[page][slot] = true;
            } else {
                Item item = Item.getByNameOrId(config.item);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    stack.stackSize = Math.min(config.count, item.getItemStackLimit());
                    stack.setItemDamage(config.damage);
                    if (config.modular) {
                        NBTTagCompound nbt = new NBTTagCompound();
                        NBTTagCompound display = new NBTTagCompound();
                        NBTTagList lore = new NBTTagList();
                        lore.appendTag(new NBTTagString("§aCurrent Mode: §6Lighting§8 [1]"));
                        display.setTag("Lore", lore);
                        nbt.setTag("display", display);
                        stack.setTagCompound(nbt);
                    } else if (config.ghead) {
                        try {
                            NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(
                                    "{SkullOwner: {Id: \"b004b060-d04a-4a4d-83ca-78720929e1b4\", hypixelPopulated: 1b, Properties: {textures: [{Value: \"eyJ0aW1lc3RhbXAiOjE0ODUwMjM0NDEyNzAsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y5MzdlMWM0NWJiOGRhMjliMmM1NjRkZDlhN2RhNzgwZGQyZmU1NDQ2OGE1ZGZiNDExM2I0ZmY2NThmMDQzZTEifX19\"}]}}, display: {Lore: [\"Absorption (2:00)\", \"Regeneration II (0:05)\", \"*Given to all team members*\"], Name: \"Golden Head\"}, ExtraAttributes: {UHCid: 9001L}}"
                            );
                            stack.setTagCompound(nbt);
                        } catch (NBTException e) {
                            LOGGER.error("Failed to parse NBT for ghead item {}: {}", config.item, e.getMessage());
                        }
                    } else if (config.backpack) {
                        try {
                            NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(
                                    "{SkullOwner: {Id: \"e35b0038-972d-4bae-baf9-155bdbe03c7d\", hypixelPopulated: 1b, Properties: {textures: [{Value: \"eyJ0aW1lc3RhbXAiOjE1NjgyMTI5NjI1MzMsInByb2ZpbGVJZCI6IjgyYzYwNmM1YzY1MjRiNzk4YjkxYTEyZDNhNjE2OTc3IiwicHJvZmlsZU5hbWUiOiJOb3ROb3RvcmlvdXNOZW1vIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMWQ4MzdjYTIyMmNiYzBiYzEyNDI2ZjVkYTAxOGMzYTkzMWI0MDYwMDg4MDA5NjBhOWRmMTEyYTU5NmU3ZDYyIn19fQ==\"}]}, Name: \"e35b0038-972d-4bae-baf9-155bdbe03c7d\"}, display: {Lore: [\"Use this handy backpack to store\", \"some of those items which are\", \"cluttering up your inventory!\"], Name: \"Backpack\"}}"
                            );
                            stack.setTagCompound(nbt);
                        } catch (NBTException e) {
                            LOGGER.error("Failed to parse NBT for backpack item {}: {}", config.item, e.getMessage());
                        }
                    } else if (config.nbt != null && !config.nbt.isEmpty()) {
                        try {
                            NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(config.nbt);
                            stack.setTagCompound(nbt);
                        } catch (NBTException e) {
                            LOGGER.error("Failed to parse NBT for item {}: {}", config.item, e.getMessage());
                        }
                    }
                    if ("true".equalsIgnoreCase(config.enchant)) {
                        NBTTagCompound nbt = stack.getTagCompound();
                        if (nbt == null) {
                            nbt = new NBTTagCompound();
                            stack.setTagCompound(nbt);
                        }
                        boolean isEnchantedBook = config.item.equals("minecraft:enchanted_book");
                        String tagName = isEnchantedBook ? "StoredEnchantments" : "ench";
                        NBTTagList enchantments = new NBTTagList();
                        NBTTagCompound durabilityEnchant = new NBTTagCompound();
                        durabilityEnchant.setShort("id", (short) 34);
                        durabilityEnchant.setShort("lvl", (short) 1);
                        enchantments.appendTag(durabilityEnchant);
                        nbt.setTag(tagName, enchantments);
                    }
                    if (config.display_name != null && !config.display_name.isEmpty()) {
                        stack.setStackDisplayName(config.display_name);
                    }
                    stacks = new ItemStack[]{stack};
                }
                isMeal[page][slot] = config.meal;
                isModular[page][slot] = config.modular;
                isHoly[page][slot] = config.holy;
                isSwan[page][slot] = config.swan;
                isGhead[page][slot] = config.ghead;
                isBackpack[page][slot] = config.backpack;
            }
            gridItems[page][slot] = stacks;
            clickCommands[page][slot] = config.internal_autocraftitem != null && !config.internal_autocraftitem.isEmpty()
                    ? "/internal_autocraftitem " + config.internal_autocraftitem
                    : config.command;
            displayNames[page][slot] = config.display_name;
        }
    }

    private boolean isExcludedSkull(NBTTagCompound nbt) {
        if (nbt == null) return false;

        if (nbt.hasKey("display")) {
            NBTTagCompound display = nbt.getCompoundTag("display");
            if (display.hasKey("Lore")) {
                NBTTagList lore = display.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String loreText = lore.getStringTagAt(i);
                    String cleanText = loreText.replaceAll("§[0-9a-fk-or]", "").toLowerCase();
                    if (cleanText.contains("use this handy backpack")) {
                        return true; // 排除背包
                    }
                    if (cleanText.contains("given to all team members")) {
                        return true; // 排除金头
                    }
                }
            }
        }

        if (!nbt.hasKey("SkullOwner")) return false;
        NBTTagCompound skullOwner = nbt.getCompoundTag("SkullOwner");
        String id = skullOwner.getString("Id");

        if (id.equals("b004b060-d04a-4a4d-83ca-78720929e1b4")) {
            if (!skullOwner.hasKey("Properties")) return false;
            NBTTagCompound properties = skullOwner.getCompoundTag("Properties");
            NBTTagList textures = properties.getTagList("textures", 10);
            if (textures.tagCount() == 0) return false;
            NBTTagCompound texture = textures.getCompoundTagAt(0);
            String textureValue = texture.getString("Value");
            return textureValue.equals("eyJ0aW1lc3RhbXAiOjE0ODUwMjM0NDEyNzAsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y5MzdlMWM0NWJiOGRhMjliMmM1NjRkZDlhN2RhNzgwZGQyZmU1NDQ2OGE1ZGZiNDExM2I0ZmY2NThmMDQzZTEifX19");
        }

        return false;
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (!isCraftHelperVisible) {
            shouldShowElements = false;
            return;
        }

        if (gui instanceof GuiContainer) {
            GuiContainer containerGui = (GuiContainer) gui;
            IInventory inventory = getLowerInventory(containerGui);
            Minecraft mc = Minecraft.getMinecraft();

            if (inventory != null && "Crafting Table".equals(inventory.getName())) {
                int xSize = getIntField(GuiContainer.class, "field_146999_f", "xSize", containerGui, 176);
                int guiLeft = getIntField(GuiContainer.class, "field_147003_i", "guiLeft", containerGui, (gui.width - xSize) / 2);
                int guiTop = getIntField(GuiContainer.class, "field_147009_r", "guiTop", containerGui, (gui.height - 166) / 2);
                final int EXTRA_RIGHT_OFFSET = 5;
                final int EXTRA_DOWN_OFFSET = 30;
                gridBaseX = guiLeft + xSize + 10 + EXTRA_RIGHT_OFFSET;
                gridBaseY = guiTop + EXTRA_DOWN_OFFSET;
                pageRowY = gridBaseY + GRID_ROWS * SLOT_SIZE;
                shouldShowElements = true;
            } else if (gui instanceof GuiInventory) {
                final int EXTRA_RIGHT_OFFSET = 5;
                final int EXTRA_DOWN_OFFSET = 30;
                int baseX = (gui.width - 176) / 2;
                int baseY = (gui.height - 166) / 2;
                gridBaseX = baseX + 176 + 10 + EXTRA_RIGHT_OFFSET;
                gridBaseY = baseY + EXTRA_DOWN_OFFSET;
                pageRowY = gridBaseY + GRID_ROWS * SLOT_SIZE;
                shouldShowElements = true;
            } else {
                shouldShowElements = false;
            }

        // 删除强行拉回鼠标的逻辑
        /*
        if (preventMouseReset &&
            System.currentTimeMillis() - preventMouseResetTime < PREVENT_RESET_DURATION &&
            savedMouseX != -1 && savedMouseY != -1) {

            final int finalSavedMouseX = savedMouseX;
            final int finalSavedMouseY = savedMouseY;

            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Mouse.setCursorPosition(finalSavedMouseX, finalSavedMouseY);
                    System.out.println("[UhcCraftHelper] Restored mouse position to: " + finalSavedMouseX + ", " + finalSavedMouseY);
                });
            });
        }
        */

            if (AutumnMod.isDynamicCraftMode) {
                updateGridItems();
            }
        } else {
            shouldShowElements = false;
        }
    }

    // 每4tick检查背包（动态模式）
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
        /*
        if (preventMouseReset &&
            System.currentTimeMillis() - preventMouseResetTime > PREVENT_RESET_DURATION) {
            preventMouseReset = false;
            savedMouseX = -1;
            savedMouseY = -1;
            System.out.println("[UhcCraftHelper] Mouse reset prevention expired");
        }
        */

            if (shouldShowElements && isCraftHelperVisible && AutumnMod.isDynamicCraftMode) {
                if (Minecraft.getMinecraft().theWorld != null && Minecraft.getMinecraft().thePlayer != null) {
                    if (Minecraft.getMinecraft().theWorld.getTotalWorldTime() % 4 == 0) {
                        updateGridItems();
                    }
                }
            }
        }
    }

    // 渲染合成助手界面
    @SubscribeEvent
    public void onGuiRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!shouldShowElements || !isCraftHelperVisible) return;
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = event.gui;
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        int totalWidth = GRID_COLS * SLOT_SIZE;
        int totalHeight = TOTAL_ROWS * SLOT_SIZE;
        TextureManager textureManager = mc.getTextureManager();
        textureManager.bindTexture(BACKGROUND_TEXTURE);
        GlStateManager.enableTexture2D();
        gui.drawTexturedModalRect(gridBaseX, gridBaseY, 0, 0, totalWidth, GRID_ROWS * SLOT_SIZE);
        int pageRowHeight = SLOT_SIZE;
        int buttonWidth = totalWidth / 3;
        textureManager.bindTexture(DARK_BACKGROUND_TEXTURE);
        gui.drawTexturedModalRect(gridBaseX, pageRowY, 0, 0, buttonWidth, pageRowHeight);
        String leftArrow = "<-";
        int leftArrowX = gridBaseX + (buttonWidth - mc.fontRendererObj.getStringWidth(leftArrow)) / 2;
        int leftArrowY = pageRowY + (pageRowHeight - mc.fontRendererObj.FONT_HEIGHT) / 2;
        mc.fontRendererObj.drawString(leftArrow, leftArrowX, leftArrowY, 0xFFFFFF);
        textureManager.bindTexture(BACKGROUND_TEXTURE);
        gui.drawTexturedModalRect(gridBaseX + buttonWidth, pageRowY, 0, 0, buttonWidth, pageRowHeight);
        String pageText = currentPage + "/" + totalPages;
        int pageTextX = gridBaseX + buttonWidth + (buttonWidth - mc.fontRendererObj.getStringWidth(pageText)) / 2;
        int pageTextY = pageRowY + (pageRowHeight - mc.fontRendererObj.FONT_HEIGHT) / 2;
        mc.fontRendererObj.drawString(pageText, pageTextX, pageTextY, 0xFFFFFF);
        textureManager.bindTexture(DARK_BACKGROUND_TEXTURE);
        gui.drawTexturedModalRect(gridBaseX + 2 * buttonWidth, pageRowY, 0, 0, buttonWidth, pageRowHeight);
        String rightArrow = "->";
        int rightArrowX = gridBaseX + 2 * buttonWidth + (buttonWidth - mc.fontRendererObj.getStringWidth(rightArrow)) / 2;
        int rightArrowY = pageRowY + (pageRowHeight - mc.fontRendererObj.FONT_HEIGHT) / 2;
        mc.fontRendererObj.drawString(rightArrow, rightArrowX, rightArrowY, 0xFFFFFF);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;
                ItemStack[] stacks = gridItems[currentPage - 1][index];
                if (stacks != null && stacks.length > 0) {
                    ItemStack stack;
                    if (isFusion[currentPage - 1][index]) {
                        long currentTime = System.currentTimeMillis();
                        int cycleIndex = (int) ((currentTime / 800) % 4);
                        stack = stacks[cycleIndex];
                    } else {
                        stack = stacks[0];
                    }
                    if (stack != null) {
                        int x = gridBaseX + col * SLOT_SIZE + 1;
                        int y = gridBaseY + row * SLOT_SIZE + 1;
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableDepth();
                        mc.getRenderItem().renderItemIntoGUI(stack, x, y);
                        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x, y, null);
                        GlStateManager.disableDepth();
                        RenderHelper.disableStandardItemLighting();
                    }
                }
            }
        }
        drawBorder(gui, gridBaseX - 2, gridBaseY - 2, gridBaseX + totalWidth + 2, gridBaseY + totalHeight + 2, 0xFF202020);
        drawBorder(gui, gridBaseX - 1, gridBaseY - 1, gridBaseX + totalWidth + 1, gridBaseY + totalHeight + 1, 0xFF404040);
        gui.drawRect(gridBaseX - 2, pageRowY - 1, gridBaseX + totalWidth + 2, pageRowY, 0xFF202020);
        gui.drawRect(gridBaseX - 1, pageRowY - 1, gridBaseX + totalWidth + 1, pageRowY, 0xFF404040);
        int mouseX = Mouse.getX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - (Mouse.getY() * gui.height / mc.displayHeight) - 1;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = gridBaseX + col * SLOT_SIZE;
                int y = gridBaseY + row * SLOT_SIZE;
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    int index = row * GRID_COLS + col;
                    ItemStack[] stacks = gridItems[currentPage - 1][index];
                    if (stacks != null && stacks.length > 0) {
                        ItemStack stack = isFusion[currentPage - 1][index]
                                ? stacks[(int) ((System.currentTimeMillis() / 800) % 4)]
                                : stacks[0];
                        if (stack != null) {
                            List<String> tooltip = new ArrayList<>();
                            String displayName = displayNames[currentPage - 1][index];
                            if (displayName != null && !displayName.isEmpty()) {
                                tooltip.add(displayName);
                            } else {
                                tooltip.add(stack.getDisplayName());
                            }
                            renderTooltip(gui, tooltip, mouseX, mouseY);
                        }
                    }
                }
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // 渲染物品提示信息
    private void renderTooltip(GuiScreen gui, List<String> tooltip, int mouseX, int mouseY) {
        if (tooltip.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        int maxWidth = 0;
        for (String line : tooltip) {
            int width = mc.fontRendererObj.getStringWidth(line);
            if (width > maxWidth) maxWidth = width;
        }
        int padding = 3;
        int tooltipWidth = maxWidth + padding * 2;
        int tooltipHeight = (mc.fontRendererObj.FONT_HEIGHT + 2) * tooltip.size() + padding * 2;
        int tooltipY = mouseY - tooltipHeight - 1;
        int tooltipX = mouseX + 7;
        if (tooltipX + tooltipWidth > gui.width) {
            tooltipX = gui.width - tooltipWidth;
        }
        if (tooltipY < 0) {
            tooltipY = 0;
        }
        if (tooltipY + tooltipHeight > gui.height) {
            tooltipY = gui.height - tooltipHeight;
        }
        GlStateManager.disableDepth();
        gui.drawRect(tooltipX - padding, tooltipY - padding, tooltipX + maxWidth + padding, tooltipY + tooltipHeight - padding, 0xC0101010);
        gui.drawRect(tooltipX - padding, tooltipY - padding, tooltipX - padding + 1, tooltipY + tooltipHeight - padding, 0xFF800080);
        gui.drawRect(tooltipX + maxWidth + padding - 1, tooltipY - padding, tooltipX + maxWidth + padding, tooltipY + tooltipHeight - padding, 0xFF800080);
        gui.drawRect(tooltipX - padding, tooltipY - padding, tooltipX + maxWidth + padding, tooltipY - padding + 1, 0xFF800080);
        gui.drawRect(tooltipX - padding, tooltipY + tooltipHeight - padding - 1, tooltipX + maxWidth + padding, tooltipY + tooltipHeight - padding, 0xFF800080);
        int textY = tooltipY;
        for (String line : tooltip) {
            mc.fontRendererObj.drawString(line, tooltipX, textY, 0xFFFFFF);
            textY += mc.fontRendererObj.FONT_HEIGHT + 2;
        }
        GlStateManager.enableDepth();
    }

    // 绘制界面边框
    private void drawBorder(GuiScreen gui, int x1, int y1, int x2, int y2, int color) {
        gui.drawRect(x1, y1, x2, y1 + 1, color); // Top
        gui.drawRect(x1, y2 - 1, x2, y2, color); // Bottom
        gui.drawRect(x1, y1, x1 + 1, y2, color); // Left
        gui.drawRect(x2 - 1, y1, x2, y2, color); // Right
    }

    // 处理鼠标点击事件
    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!shouldShowElements || !isCraftHelperVisible) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        GuiScreen gui = event.gui;
        if (gui == null) return;

        if (Mouse.isButtonDown(0)) {
            int mouseX = Mouse.getX() * gui.width / mc.displayWidth;
            int mouseY = gui.height - (Mouse.getY() * gui.height / mc.displayHeight) - 1;
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastClickTime < CLICK_COOLDOWN) return;

            // 检查物品格子点击
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int x = gridBaseX + col * SLOT_SIZE;
                    int y = gridBaseY + row * SLOT_SIZE;

                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        int index = row * GRID_COLS + col;
                        String command = clickCommands[currentPage - 1][index];

                        if (command != null && !command.isEmpty()) {
                            // 删除所有鼠标位置保存代码
                        /*
                        savedMouseX = Mouse.getX();
                        savedMouseY = Mouse.getY();
                        preventMouseReset = true;
                        preventMouseResetTime = System.currentTimeMillis();
                        System.out.println("[UhcCraftHelper] Saving mouse position: " + savedMouseX + ", " + savedMouseY);
                        */

                            System.out.println("[UhcCraftHelper] Executing command: " + command);
                            mc.thePlayer.sendChatMessage(command);
                            mc.thePlayer.playSound(CLICK_SOUND, 1.0F, 1.0F);
                        }

                        lastClickTime = currentTime;
                        event.setCanceled(true);
                        return;
                    }
                }
            }

            int pageRowHeight = SLOT_SIZE;
            int totalWidth = GRID_COLS * SLOT_SIZE;
            int buttonWidth = totalWidth / 3;

            if (mouseY >= pageRowY && mouseY < pageRowY + pageRowHeight) {
                if (mouseX >= gridBaseX && mouseX < gridBaseX + buttonWidth) {
                    currentPage--;
                    if (currentPage < 1) currentPage = totalPages;
                    mc.thePlayer.playSound(CLICK_SOUND, 1.0F, 1.0F);
                    lastClickTime = currentTime;
                    event.setCanceled(true);
                } else if (mouseX >= gridBaseX + 2 * buttonWidth && mouseX < gridBaseX + totalWidth) {
                    currentPage++;
                    if (currentPage > totalPages) currentPage = 1;
                    mc.thePlayer.playSound(CLICK_SOUND, 1.0F, 1.0F);
                    lastClickTime = currentTime;
                    event.setCanceled(true);
                }
            }
        }
    }

    private IInventory getLowerInventory(GuiContainer gui) {
        try {
            if (gui.inventorySlots != null) {
                return gui.inventorySlots.getSlot(0).inventory;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get lower inventory: {}", e.getMessage());
        }
        return null;
    }

    private int getIntField(Class<?> clazz, String srgFieldName, String mcpFieldName, Object instance, int defaultValue) {
        try {
            Field field = isObfuscatedEnv() ? clazz.getDeclaredField(srgFieldName) : clazz.getDeclaredField(mcpFieldName);
            field.setAccessible(true);
            return field.getInt(instance);
        } catch (Exception e) {
            LOGGER.error("Failed to get int field {}: {}", mcpFieldName, e.getMessage());
            return defaultValue;
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


    public static boolean shouldPreventMouseReset() {
        return false;
    }

    private static class PageConfig {
        List<ItemConfig> items = new ArrayList<>();
        public PageConfig() {
            for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
                items.add(new ItemConfig());
            }
        }
    }

    private static class ItemConfig {
        String item = "minecraft:air";
        String display_name = "";
        String command = "";
        String internal_autocraftitem = "";
        String nbt = "";
        int count = 1;
        @SerializedName("enchant")
        String enchant = "false";
        @SerializedName("damage")
        int damage = 0;
        @SerializedName("fusion")
        boolean fusion = false;
        @SerializedName("meal")
        boolean meal = false;
        @SerializedName("modular")
        boolean modular = false;
        @SerializedName("holy")
        boolean holy = false;
        @SerializedName("swan")
        boolean swan = false;
        @SerializedName("ghead")
        boolean ghead = false;
        @SerializedName("backpack")
        boolean backpack = false;
        List<Ingredient> ingredients = new ArrayList<>();
    }

    private static class Ingredient {
        String item = "minecraft:air";
        int count = 1;
        @SerializedName("damage")
        int damage = 0;
    }
}