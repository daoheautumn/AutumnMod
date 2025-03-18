package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class ItemChecker {
    private final Map<String, String> playerItemStatus = new HashMap<>();

    private static final Map<String, ItemInfo> ITEM_LIST = new HashMap<String, ItemInfo>() {{
        put("Dragon Sword", new ItemInfo("§5", item -> item != null && item.getItem() == Items.diamond_sword && hasUHCid(item, 2004)));
        put("Anduril", new ItemInfo("§d", item -> item != null && item.getItem() == Items.iron_sword && hasUHCid(item, 50008)));
        put("Apprentice Bow", new ItemInfo("§9", item -> item != null && item.getItem() == Items.bow && hasUHCid(item, 1003)));
        put("Apprentice Sword", new ItemInfo("§8", item -> item != null && item.getItem() == Items.iron_sword && hasUHCid(item, 1002)));
        put("Artemis Bow", new ItemInfo("§5", item -> item != null && item.getItem() == Items.bow && hasUHCid(item, 50001)));
        put("Axe of Perun", new ItemInfo("§6", item -> item != null && item.getItem() == Items.diamond_axe && hasUHCid(item, 50006)));
        put("Bloodlust", new ItemInfo("§d", item -> item != null && item.getItem() == Items.diamond_sword && hasUHCid(item, 50023)));
        put("Cornucopia", new ItemInfo("§5", item -> item != null && item.getItem() == Items.golden_carrot && hasUHCid(item, 50011)));
        put("Deaths Scythe", new ItemInfo("§6", item -> item != null && item.getItem() == Items.iron_hoe && hasUHCid(item, 50009)));
        put("Excalibur", new ItemInfo("§6", item -> item != null && item.getItem() == Items.diamond_sword && hasUHCid(item, 50007)));
        put("Vorpal Sword", new ItemInfo("§8", item -> item != null && item.getItem() == Items.iron_sword && hasUHCid(item, 2001)));
        put("Lucky Shears", new ItemInfo("§8", item -> item != null && item.getItem() == Items.shears && hasUHCid(item, 13001)));
        put("Masters Compass", new ItemInfo("§9", item -> item != null && item.getItem() == Items.compass && hasUHCid(item, 1004)));
        put("Philosophers Pickaxe", new ItemInfo("§8", item -> item != null && item.getItem() == Items.diamond_pickaxe && hasUHCid(item, 6004)));
        put("Deus Ex Machina", new ItemInfo("§6", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 50014)));
        put("Fate Potion", new ItemInfo("§d", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 50010)));
        put("Flask of Cleansing", new ItemInfo("§6", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 50018)));
        put("Flask of Ichor", new ItemInfo("§6", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 50002)));
        put("Holy Water", new ItemInfo("§5", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 8003)));
        put("Nectar", new ItemInfo("§9", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 4003)));
        put("Panacea", new ItemInfo("§d", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 9003)));
        put("Potion of Toughness", new ItemInfo("§d", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 5002)));
        put("Potion of Velocity", new ItemInfo("§9", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 10003)));
        put("Potion of Vitality", new ItemInfo("§5", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 50020)));
        put("Golden Apple", new ItemInfo("§2", item -> item != null && item.getItem() == Items.golden_apple && item.getItemDamage() == 0));
        put("Notch Apple", new ItemInfo("§6", item -> item != null && item.getItem() == Items.golden_apple && item.getItemDamage() == 1));
        put("Golden Head", new ItemInfo("§2", item -> item != null && item.getItem() == Items.skull && hasUHCid(item, 9001)));
    }};

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || !AutumnMod.isItemAlertEnabled) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) continue;

            EntityPlayer otherPlayer = (EntityPlayer) entity;
            String playerName = otherPlayer.getName();
            ItemStack heldItem = otherPlayer.getHeldItem();
            String currentItem = getSpecialItem(heldItem);
            String previousItem = playerItemStatus.getOrDefault(playerName, "");

            if (heldItem != null) {
                if (currentItem == null && (heldItem.getItem() == Items.diamond_sword || heldItem.getItem() == Items.iron_sword)) {
                    currentItem = heldItem.getItem() == Items.diamond_sword ? "Diamond Sword" : "Iron Sword";
                }

                if (currentItem != null && !currentItem.equals(previousItem)) {
                    String color = getItemColor(currentItem);
                    String enchantments = getEnchantmentsString(heldItem);
                    int distance = (int) mc.thePlayer.getDistanceToEntity(otherPlayer);
                    int stackSize = shouldShowStackSize(currentItem) ? heldItem.stackSize : -1;
                    String message = LanguageLoader.format("itemchecker.alert",
                            playerName,
                            stackSize > 0 ? color + stackSize + " " : "",
                            color + currentItem,
                            enchantments.isEmpty() ? "" : " §e(" + enchantments + ")",
                            distance);
                    mc.thePlayer.addChatMessage(new ChatComponentText(message));
                }
            }

            playerItemStatus.put(playerName, currentItem != null ? currentItem : "");
        }
    }

    private String getSpecialItem(ItemStack item) {
        for (Map.Entry<String, ItemInfo> entry : ITEM_LIST.entrySet()) {
            if (entry.getValue().checker.check(item)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String getItemColor(String itemName) {
        if ("Diamond Sword".equals(itemName)) return "§9";
        if ("Iron Sword".equals(itemName)) return "§8";
        return ITEM_LIST.containsKey(itemName) ? ITEM_LIST.get(itemName).color : "§f";
    }

    private boolean shouldShowStackSize(String itemName) {
        return "Golden Apple".equals(itemName) || "Notch Apple".equals(itemName) || "Golden Head".equals(itemName);
    }

    private static boolean hasUHCid(ItemStack item, int uhcId) {
        NBTTagCompound nbt = item.getTagCompound();
        if (nbt == null || !nbt.hasKey("ExtraAttributes")) return false;

        NBTTagCompound extraAttributes = nbt.getCompoundTag("ExtraAttributes");
        return extraAttributes.hasKey("UHCid") && extraAttributes.getInteger("UHCid") == uhcId;
    }

    private String getEnchantmentsString(ItemStack item) {
        if (item == null || !item.isItemEnchanted()) return "";

        NBTTagList enchantments = item.getEnchantmentTagList();
        if (enchantments == null || enchantments.tagCount() == 0) return "";

        StringBuilder enchants = new StringBuilder();
        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound enchant = enchantments.getCompoundTagAt(i);
            short id = enchant.getShort("id");
            short lvl = enchant.getShort("lvl");

            Enchantment enchantment = Enchantment.getEnchantmentById(id);
            if (enchantment != null) {
                if (enchants.length() > 0) enchants.append(" , ");
                enchants.append(enchantment.getTranslatedName(lvl));
            }
        }
        return enchants.toString();
    }

    private static class ItemInfo {
        String color;
        ItemCheckerFunction checker;

        ItemInfo(String color, ItemCheckerFunction checker) {
            this.color = color;
            this.checker = checker;
        }
    }

    @FunctionalInterface
    private interface ItemCheckerFunction {
        boolean check(ItemStack item);
    }
}