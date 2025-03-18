package com.daohe;

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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UhcCraftHelper {
    private static final Logger LOGGER = LogManager.getLogger(AutumnMod.MODID);

    public static boolean isCraftHelperVisible = true;

    private boolean shouldShowElements = false;
    private int gridBaseX;
    private int gridBaseY;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_ROWS = 4;
    private static final int GRID_COLS = 4;
    private static final int TOTAL_ROWS = 5;
    private ItemStack[][] gridItems;
    private String[][] clickCommands;
    private String[][] displayNames;
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 200;

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(AutumnMod.MODID, "textures/gui/background.png");
    private static final ResourceLocation DARK_BACKGROUND_TEXTURE = new ResourceLocation(AutumnMod.MODID, "textures/gui/darkbackground.png");

    private static final ResourceLocation GHEAD_TEXTURE = new ResourceLocation(AutumnMod.MODID, "textures/skin/ghead.png");
    private static final ResourceLocation BACKPACK_TEXTURE = new ResourceLocation(AutumnMod.MODID, "textures/skin/backpack.png");

    private int currentPage = 1;
    private int totalPages = 1;
    private int pageRowY;

    private static final String CLICK_SOUND = "autumnmod:click";

    private static File configFile;
    private static final ResourceLocation DEFAULT_CONFIG_RESOURCE = new ResourceLocation(AutumnMod.MODID, "config/uhc_craft_items.json");

    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private long mouseLockTime = 0;
    private static final long MOUSE_LOCK_DURATION = 1000;

    public UhcCraftHelper() {
        gridItems = new ItemStack[totalPages][GRID_ROWS * GRID_COLS];
        clickCommands = new String[totalPages][GRID_ROWS * GRID_COLS];
        displayNames = new String[totalPages][GRID_ROWS * GRID_COLS];
    }

    public void preInit(FMLPreInitializationEvent event) {
        configFile = new File(event.getModConfigurationDirectory(), "uhc_craft_items.json");
        loadItemsFromFile();
    }

    private void loadItemsFromFile() {
        if (!configFile.exists()) {
            copyDefaultConfigFromResources();
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<PageConfig>>() {}.getType();
            List<PageConfig> pages = gson.fromJson(reader, type);

            if (pages == null || pages.isEmpty()) {
                copyDefaultConfigFromResources();
                return;
            }

            totalPages = pages.size();
            gridItems = new ItemStack[totalPages][GRID_ROWS * GRID_COLS];
            clickCommands = new String[totalPages][GRID_ROWS * GRID_COLS];
            displayNames = new String[totalPages][GRID_ROWS * GRID_COLS];

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PageConfig page = pages.get(pageIndex);
                for (int slot = 0; slot < GRID_ROWS * GRID_COLS; slot++) {
                    ItemConfig itemConfig = page.items.get(slot);
                    ItemStack stack = null;
                    if (itemConfig.item != null && !itemConfig.item.isEmpty() && !itemConfig.item.equals("minecraft:air")) {
                        Item item = Item.getByNameOrId(itemConfig.item);
                        if (item != null) {
                            stack = new ItemStack(item);
                            stack.stackSize = Math.min(itemConfig.count, item.getItemStackLimit());

                            if (itemConfig.nbt != null && !itemConfig.nbt.isEmpty()) {
                                try {
                                    NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.getTagFromJson(itemConfig.nbt);
                                    stack.setTagCompound(nbt);
                                } catch (NBTException e) {
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
                        }
                    }
                    gridItems[pageIndex][slot] = stack;
                    clickCommands[pageIndex][slot] = itemConfig.command;
                    displayNames[pageIndex][slot] = itemConfig.display_name;
                }
            }
        } catch (IOException e) {
            copyDefaultConfigFromResources();
        }
    }

    public void reloadItems() {
        loadItemsFromFile();
        currentPage = Math.min(currentPage, totalPages);
    }

    private void copyDefaultConfigFromResources() {
        try (InputStream inputStream = Minecraft.class.getResourceAsStream("/assets/" + AutumnMod.MODID + "/config/uhc_craft_items.json")) {
            if (inputStream == null) {
                return;
            }

            try (FileOutputStream outputStream = new FileOutputStream(configFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                loadItemsFromFile();
            }
        } catch (IOException e) {
        }
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
                gridBaseX = gui.width - (GRID_COLS * SLOT_SIZE + 20 + 45);
                gridBaseY = (gui.height - (GRID_ROWS * SLOT_SIZE + SLOT_SIZE)) / 2 - 20;
                pageRowY = gridBaseY + GRID_ROWS * SLOT_SIZE;
                shouldShowElements = true;
            } else if (gui instanceof GuiInventory) {
                final int EXTRA_RIGHT_OFFSET = 5;
                final int EXTRA_DOWN_OFFSET = 30;

                boolean hasPotionEffects = !mc.thePlayer.getActivePotionEffects().isEmpty();

                int baseX = (gui.width - 176) / 2;
                int baseY = (gui.height - 166) / 2;

                if (hasPotionEffects) {
                    gridBaseX = baseX + 176 + 60 + 10 + EXTRA_RIGHT_OFFSET;
                } else {
                    gridBaseX = baseX + 176 + 10 + EXTRA_RIGHT_OFFSET;
                }

                gridBaseY = baseY + EXTRA_DOWN_OFFSET;
                pageRowY = gridBaseY + GRID_ROWS * SLOT_SIZE;
                shouldShowElements = true;
            } else {
                shouldShowElements = false;
            }

            if (mouseLockTime > 0 && System.currentTimeMillis() - mouseLockTime < MOUSE_LOCK_DURATION) {
                Mouse.setCursorPosition(lastMouseX, lastMouseY);
            }
        } else {
            shouldShowElements = false;
        }
    }

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
                ItemStack stack = gridItems[currentPage - 1][index];
                if (stack != null) {
                    int x = gridBaseX + col * SLOT_SIZE + 1;
                    int y = gridBaseY + row * SLOT_SIZE + 1;
                    String displayName = displayNames[currentPage - 1][index];
                    RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.enableDepth();
                    if ("金头".equals(displayName)) {
                        textureManager.bindTexture(GHEAD_TEXTURE);
                        mc.getRenderItem().renderItemIntoGUI(stack, x, y);
                    } else if ("背包".equals(displayName)) {
                        textureManager.bindTexture(BACKPACK_TEXTURE);
                        mc.getRenderItem().renderItemIntoGUI(stack, x, y);
                    } else {
                        mc.getRenderItem().renderItemIntoGUI(stack, x, y);
                    }
                    mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x, y, null);
                    GlStateManager.disableDepth();
                    RenderHelper.disableStandardItemLighting();
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
                    ItemStack stack = gridItems[currentPage - 1][index];
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

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

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

    private void drawBorder(GuiScreen gui, int x1, int y1, int x2, int y2, int color) {
        gui.drawRect(x1, y1, x2, y1 + 1, color);
        gui.drawRect(x1, y2 - 1, x2, y2, color);
        gui.drawRect(x1, y1, x1 + 1, y2, color);
        gui.drawRect(x2 - 1, y1, x2, y2, color);
    }

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

            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int x = gridBaseX + col * SLOT_SIZE;
                    int y = gridBaseY + row * SLOT_SIZE;
                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        int index = row * GRID_COLS + col;
                        String command = clickCommands[currentPage - 1][index];
                        if (command != null && !command.isEmpty()) {
                            mc.thePlayer.sendChatMessage(command);
                            mc.thePlayer.playSound(CLICK_SOUND, 1.0F, 1.0F);
                            lastMouseX = Mouse.getX();
                            lastMouseY = Mouse.getY();
                            mouseLockTime = System.currentTimeMillis();
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
        }
        return null;
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
        String nbt = "";
        int count = 1;
        @SerializedName("Enchant")
        String enchant = "false";
    }
}