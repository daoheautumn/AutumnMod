package com.daohe.autumnmod;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemChecker {
    private final Map<String, String> playerItemStatus = new HashMap<>();
    private final Map<String, String> playerEnchantStatus = new HashMap<>();
    private final Map<String, Long> playerSoundCooldown = new HashMap<>();
    private final Map<String, Float> playerLastHealth = new HashMap<>();
    private static final long SOUND_COOLDOWN = 30 * 1000;
    private static final String WARNING_SOUND = "autumnmod:bass";
    private final Minecraft mc = Minecraft.getMinecraft();
    private static final Item ENDER_CHEST_ITEM = Item.getItemFromBlock(Blocks.ender_chest);

    // 需要检测的物品列表
    private static final Map<String, ItemInfo> ITEM_LIST = new HashMap<String, ItemInfo>() {{
        put("Dragon Sword", new ItemInfo("§5", item -> item != null && item.getItem() == Items.diamond_sword && hasUHCid(item, 2004)));
        put("Anduril", new ItemInfo("§d", item -> item != null && item.getItem() == Items.iron_sword && hasUHCid(item, 50008)));
        put("Apprentice Bow", new ItemInfo("§9", item -> item != null && item.getItem() == Items.bow && hasUHCid(item, 1003)));
        put("Apprentice Sword", new ItemInfo("§9", item -> item != null && item.getItem() == Items.iron_sword && hasUHCid(item, 1002)));
        put("Artemis Bow", new ItemInfo("§d", item -> item != null && item.getItem() == Items.bow && hasUHCid(item, 50001)));
        put("Axe of Perun", new ItemInfo("§6", item -> item != null && item.getItem() == Items.diamond_axe && hasUHCid(item, 50006)));
        put("Bloodlust", new ItemInfo("§d", item -> item != null && item.getItem() == Items.diamond_sword && hasUHCid(item, 50023)));
        put("Cornucopia", new ItemInfo("§2", item -> item != null && item.getItem() == Items.golden_carrot && hasUHCid(item, 50011)));
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
        put("Splash Potion of Poison", new ItemInfo("§6", item -> item != null && item.getItem() == Items.potionitem &&
                (item.getItemDamage() == 16388 || item.getItemDamage() == 16420 || item.getItemDamage() == 16452)));
        put("Bow", new ItemInfo("§9", item -> item != null && item.getItem() == Items.bow && !hasSpecialBowUHCid(item)));
        put("Speed II Potion", new ItemInfo("§5", item -> item != null && item.getItem() == Items.potionitem && item.getItemDamage() == 8226));
        put("Strength II Potion", new ItemInfo("§5", item -> item != null && item.getItem() == Items.potionitem && hasUHCid(item, 8233)));
        put("Cow Spawn Egg", new ItemInfo("§9", item -> item != null && item.getItem() == Items.spawn_egg && item.getItemDamage() == 92));
        put("Modular Bow", new ItemInfo("§6", item -> item != null && item.getItem() == Items.bow && hasUHCid(item, 50024)));
        put("Expert Seal", new ItemInfo("§6", item -> item != null && item.getItem() == Items.nether_star && hasUHCid(item, 50025)));
        put("Miner's Blessing", new ItemInfo("§d", item -> item != null && item.getItem() == Items.diamond_pickaxe && hasUHCid(item, 50021)));
        put("The Mark", new ItemInfo("§d", item -> item != null && item.getItem() == Items.snowball && hasUHCid(item, 50029)));
        put("Void Box", new ItemInfo("§5", item -> item != null && item.getItem() == ENDER_CHEST_ITEM && hasUHCid(item, 50013)));
        put("Wolf Spawn Egg", new ItemInfo("§9", item -> item != null && item.getItem() == Items.spawn_egg && item.getItemDamage() == 95));
    }};

    // debug cmd
    public ItemChecker() {
        ClientCommandHandler.instance.registerCommand(new WhatsMyHealthCommand());
    }

    // 检查玩家手持物品
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (mc.thePlayer == null || mc.theWorld == null || !AutumnMod.isItemAlertEnabled) return;

        float maxHealth = mc.thePlayer.getMaxHealth();
        if (maxHealth != 40.0F && maxHealth != 60.0F && maxHealth != 30.0F) return; //增加了对Less Health Modifier的支持

        List<Entity> entities = new ArrayList<>(mc.theWorld.loadedEntityList);
        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) continue;

            EntityPlayer otherPlayer = (EntityPlayer) entity;
            String playerName = otherPlayer.getName();
            ItemStack heldItem = otherPlayer.getHeldItem();
            String currentItem = getSpecialItem(heldItem);
            String previousItem = playerItemStatus.getOrDefault(playerName, "");
            String currentEnchantments = heldItem != null ? getEnchantmentsString(heldItem) : "";
            String previousEnchantments = playerEnchantStatus.getOrDefault(playerName, "");

            if (heldItem != null) {
                if (currentItem == null && (heldItem.getItem() == Items.diamond_sword || heldItem.getItem() == Items.iron_sword)) {
                    currentItem = heldItem.getItem() == Items.diamond_sword ? "Diamond Sword" : "Iron Sword";
                }

                if (currentItem != null && (!currentItem.equals(previousItem) || !currentEnchantments.equals(previousEnchantments))) {
                    String color = getItemColor(currentItem);
                    String enchantments = getEnchantmentsString(heldItem);
                    int distance = (int) mc.thePlayer.getDistanceToEntity(otherPlayer);
                    String message = LanguageLoader.format("itemchecker.alert",
                            playerName,
                            "",
                            color + currentItem,
                            enchantments.isEmpty() ? "" : " §e(" + enchantments + ")",
                            distance);

                    int messageId = (playerName + currentItem + enchantments).hashCode();

                    IChatComponent chatMessage = new ChatComponentText(message);
                    mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(chatMessage, messageId);

                    if ("§6".equals(color)) {
                        String key = playerName + ":" + currentItem;
                        long currentTime = System.currentTimeMillis();
                        Long lastPlayedTime = playerSoundCooldown.get(key);
                        if (lastPlayedTime == null || (currentTime - lastPlayedTime >= SOUND_COOLDOWN)) {
                            mc.thePlayer.playSound(WARNING_SOUND, 1.0F, 1.0F);
                            playerSoundCooldown.put(key, currentTime);
                        }
                    }
                }
            }

            playerItemStatus.put(playerName, currentItem != null ? currentItem : "");
            playerEnchantStatus.put(playerName, currentEnchantments);
        }

        if (mc.theWorld.getScoreboard() != null) {
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective healthObjective = scoreboard.getObjectiveInDisplaySlot(0);
            if (healthObjective != null) {
                for (String playerName : scoreboard.getObjectiveNames()) {
                    Score score = scoreboard.getValueFromObjective(playerName, healthObjective);
                    if (score != null) {
                        playerLastHealth.put(playerName, (float) score.getScorePoints());
                    }
                }
            }
        }
    }

    private float getTabListHealth(String playerName) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.theWorld.getScoreboard() == null) return -1;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective healthObjective = scoreboard.getObjectiveInDisplaySlot(0);

        if (healthObjective == null) return -1;

        Score score = scoreboard.getValueFromObjective(playerName, healthObjective);
        if (score != null) {
            return score.getScorePoints();
        }

        return -1;
    }

    // 查找物品是否在要检测的物品列表里
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

    // 检查UHCID方法
    private static boolean hasUHCid(ItemStack item, int uhcId) {
        NBTTagCompound nbt = item.getTagCompound();
        if (nbt == null || !nbt.hasKey("ExtraAttributes")) return false;

        NBTTagCompound extraAttributes = nbt.getCompoundTag("ExtraAttributes");
        return extraAttributes.hasKey("UHCid") && extraAttributes.getInteger("UHCid") == uhcId;
    }

    // 检查弓的类型, 自瞄弓/变幻弓/丘比特
    private static boolean hasSpecialBowUHCid(ItemStack item) {
        NBTTagCompound nbt = item.getTagCompound();
        if (nbt == null || !nbt.hasKey("ExtraAttributes")) return false;

        NBTTagCompound extraAttributes = nbt.getCompoundTag("ExtraAttributes");
        if (!extraAttributes.hasKey("UHCid")) return false;

        int uhcId = extraAttributes.getInteger("UHCid");
        return uhcId == 1003 || uhcId == 50001 || uhcId == 50024;
    }

    // 获取物品附魔信息
    private String getEnchantmentsString(ItemStack item) {
        if (item == null || !item.isItemEnchanted()) return "";

        NBTTagList enchantments = item.getEnchantmentTagList();
        if (enchantments == null || enchantments.tagCount() == 0) return "";

        int unbreakingOneCount = 0;
        StringBuilder enchants = new StringBuilder();

        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound enchant = enchantments.getCompoundTagAt(i);
            short id = enchant.getShort("id");
            short lvl = enchant.getShort("lvl");

            if (id == 34 && lvl == 1) {
                unbreakingOneCount++;
            } else {
                Enchantment enchantment = Enchantment.getEnchantmentById(id);
                if (enchantment != null) {
                    if (enchants.length() > 0) enchants.append(" , ");
                    enchants.append(enchantment.getTranslatedName(lvl));
                }
            }
        }

        if (unbreakingOneCount > 0) {
            if (enchants.length() > 0) enchants.append(" , ");
            if (unbreakingOneCount == 1) {
                enchants.append("Enchanted");
            } else {
                enchants.append(unbreakingOneCount).append(" Enchantments");
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

    private static class WhatsMyHealthCommand extends net.minecraft.command.CommandBase {
        @Override
        public String getCommandName() {
            return "whatsmyhealth";
        }

        @Override
        public String getCommandUsage(net.minecraft.command.ICommandSender sender) {
            return "/whatsmyhealth";
        }

        @Override
        public void processCommand(net.minecraft.command.ICommandSender sender, String[] args) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) {
                sender.addChatMessage(new ChatComponentText("§cUnable to retrieve health."));
                return;
            }

            float myHealth = getTabListHealth(mc.thePlayer.getName());
            if (myHealth >= 0) {
                sender.addChatMessage(new ChatComponentText("§fYour health: §a" + myHealth));
            } else {
                sender.addChatMessage(new ChatComponentText("§cCould not parse your health from TAB list."));
            }

            sender.addChatMessage(new ChatComponentText("§6=== TAB List ==="));
            if (mc.theWorld.getScoreboard() != null) {
                Scoreboard scoreboard = mc.theWorld.getScoreboard();
                ScoreObjective healthObjective = scoreboard.getObjectiveInDisplaySlot(0);
                if (healthObjective != null) {
                    for (String playerName : scoreboard.getObjectiveNames()) {
                        Score score = scoreboard.getValueFromObjective(playerName, healthObjective);
                        ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
                        String teamName = team != null ? team.getRegisteredName() : "N/A";
                        String message = String.format(
                                "§fName: §a%s\n" +
                                        "§fHealth: §a%s\n" +
                                        "§fTeam: §d%s",
                                playerName,
                                score != null ? String.valueOf(score.getScorePoints()) : "Unknown",
                                teamName
                        );
                        sender.addChatMessage(new ChatComponentText(message));
                        sender.addChatMessage(new ChatComponentText("§7---"));
                    }
                }
            }
            sender.addChatMessage(new ChatComponentText("§6=============="));
        }

        private float getTabListHealth(String playerName) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null || mc.theWorld.getScoreboard() == null) return -1;

            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective healthObjective = scoreboard.getObjectiveInDisplaySlot(0);

            if (healthObjective == null) return -1;

            Score score = scoreboard.getValueFromObjective(playerName, healthObjective);
            if (score != null) {
                return score.getScorePoints();
            }

            return -1;
        }

        @Override
        public boolean canCommandSenderUseCommand(net.minecraft.command.ICommandSender sender) {
            return true;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
}