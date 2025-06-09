package com.daohe.autumnmod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CraftHelperReloadCommand extends CommandBase {
    private final UhcCraftHelper craftHelper;

    public CraftHelperReloadCommand(UhcCraftHelper craftHelper) {
        this.craftHelper = craftHelper;
    }

    @Override
    public String getCommandName() {
        return "crafthelper";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return LanguageLoader.format("command.crafthelper.usage");
    }

    // 处理reload配置的命令
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            craftHelper.reloadItems();
            sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.crafthelper.reload.success")));
        } else {
            throw new CommandException("Usage: " + getCommandUsage(sender));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}