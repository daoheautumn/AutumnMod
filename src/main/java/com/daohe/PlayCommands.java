package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.HashMap;
import java.util.Map;

public class PlayCommands extends CommandBase {
    // 定义对应游戏模式的/play命令
    private static final Map<String, String> COMMAND_MAP = new HashMap<String, String>() {{
        put("1s", "/play bedwars_eight_one");
        put("2s", "/play bedwars_eight_two");
        put("3s", "/play bedwars_four_three");
        put("4s", "/play bedwars_four_four");
        put("4v4", "/play bedwars_two_four");
        put("solo", "/play uhc_solo");
        put("teams", "/play uhc_teams");
    }};

    // 定义游戏模式名称
    private static final Map<String, String> GAME_NAMES = new HashMap<String, String>() {{
        put("1s", "Bedwars 1s");
        put("2s", "Bedwars 2s");
        put("3s", "Bedwars 3s");
        put("4s", "Bedwars 4s");
        put("4v4", "Bedwars 4v4");
        put("solo", "UHC Solo");
        put("teams", "UHC Teams");
    }};

    // 定义游戏模式颜色代码
    private static final Map<String, String> GAME_COLORS = new HashMap<String, String>() {{
        put("1s", "§c");
        put("2s", "§c");
        put("3s", "§c");
        put("4s", "§c");
        put("4v4", "§c");
        put("solo", "§6");
        put("teams", "§6");
    }};

    private final String commandName;

    public PlayCommands(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + commandName;
    }

    // 执行/play命令并显示提示
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) sender;
        String playCommand = COMMAND_MAP.get(commandName);
        if (playCommand != null) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(playCommand);
            String gameName = GAME_NAMES.get(commandName);
            String gameColor = GAME_COLORS.get(commandName);
            String message = String.format("§dSeeding You to a game of %s%s", gameColor, gameName);
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    // 注册所有命令
    public static void registerCommands() {
        for (String command : COMMAND_MAP.keySet()) {
            ClientCommandHandler.instance.registerCommand(new PlayCommands(command));
        }
    }
}