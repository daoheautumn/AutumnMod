package com.daohe;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class ToggleBossBarCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "togglebossbar";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/togglebossbar";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        AutumnMod.isBossBarVisible = !AutumnMod.isBossBarVisible;
        String message = AutumnMod.isBossBarVisible ? "§a开启了 boss bar" : "§c关闭了 boss bar";
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        AutumnMod.saveConfig();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}