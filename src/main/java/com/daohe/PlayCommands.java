package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.HashMap;
import java.util.Map;

public class PlayCommands extends CommandBase {
    private static final Map<String, String> COMMAND_MAP = new HashMap<String, String>() {{
        put("1s", "/play bedwars_eight_one");
        put("2s", "/play bedwars_eight_two");
        put("3s", "/play bedwars_four_three");
        put("4s", "/play bedwars_four_four");
        put("4v4", "/play bedwars_two_four");
        put("solo", "/play uhc_solo");
        put("teams", "/play uhc_teams");
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

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) sender;
        String playCommand = COMMAND_MAP.get(commandName);
        if (playCommand != null) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(playCommand);
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

    public static void registerCommands() {
        for (String command : COMMAND_MAP.keySet()) {
            ClientCommandHandler.instance.registerCommand(new PlayCommands(command));
        }
    }
}