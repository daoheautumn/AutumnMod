package com.daohe.autumnmod;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import java.util.Arrays;
import java.util.List;

public class AutumnCommand extends CommandBase {
    private final UhcCraftHelper craftHelper;

    public AutumnCommand(UhcCraftHelper craftHelper) {
        this.craftHelper = craftHelper;
    }

    @Override
    public String getCommandName() {
        return "autumn";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("au");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return LanguageLoader.format("command.autumn.usage");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new CommandException("Usage: " + getCommandUsage(sender));
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "togglebossbar":
                AutumnMod.isBossBarVisible = !AutumnMod.isBossBarVisible;
                String bossBarMessage = AutumnMod.isBossBarVisible ?
                        LanguageLoader.format("command.autumn.togglebossbar.on") :
                        LanguageLoader.format("command.autumn.togglebossbar.off");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(bossBarMessage));
                AutumnMod.saveConfig();
                break;

            case "reloadcrafthelper":
                craftHelper.reloadItems();
                sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.reloadcrafthelper.success")));
                break;

            case "togglecrafthelper":
                UhcCraftHelper.isCraftHelperVisible = !UhcCraftHelper.isCraftHelperVisible;
                String toggleMessage = UhcCraftHelper.isCraftHelperVisible ?
                        LanguageLoader.format("command.autumn.togglecrafthelper.on") :
                        LanguageLoader.format("command.autumn.togglecrafthelper.off");
                sender.addChatMessage(new ChatComponentText(toggleMessage));
                AutumnMod.saveConfig();
                break;

            case "toggleitemalert":
                AutumnMod.isItemAlertEnabled = !AutumnMod.isItemAlertEnabled;
                String itemAlertMessage = AutumnMod.isItemAlertEnabled ?
                        LanguageLoader.format("command.autumn.toggleitemalert.on") :
                        LanguageLoader.format("command.autumn.toggleitemalert.off");
                sender.addChatMessage(new ChatComponentText(itemAlertMessage));
                AutumnMod.saveConfig();
                break;

            case "toggleinventorycenter":
                AutumnMod.isInventoryCenterEnabled = !AutumnMod.isInventoryCenterEnabled;
                String inventoryCenterMessage = AutumnMod.isInventoryCenterEnabled ?
                        LanguageLoader.format("command.autumn.toggleinventorycenter.on") :
                        LanguageLoader.format("command.autumn.toggleinventorycenter.off");
                sender.addChatMessage(new ChatComponentText(inventoryCenterMessage));
                AutumnMod.saveConfig();
                break;

            case "toggletype":
                AutumnMod.isDynamicCraftMode = !AutumnMod.isDynamicCraftMode;
                String modeMessage = AutumnMod.isDynamicCraftMode ?
                        LanguageLoader.format("command.autumn.toggletype.dynamic") :
                        LanguageLoader.format("command.autumn.toggletype.fixed");
                sender.addChatMessage(new ChatComponentText(modeMessage));
                craftHelper.reloadItems();
                AutumnMod.saveConfig();
                break;

            case "lang":
                if (args.length != 2) {
                    throw new CommandException("Usage: /au lang <en/cn>");
                }
                String lang = args[1].toLowerCase();
                if ("en".equals(lang) || "cn".equals(lang)) {
                    LanguageLoader.setLanguage(lang);
                    AutumnMod.saveConfig();
                    sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.lang.success", lang)));
                } else {
                    throw new CommandException(LanguageLoader.format("command.autumn.lang.invalid", lang));
                }
                break;

            case "help":
                sendHelpMessage(sender);
                break;

            default:
                throw new CommandException(LanguageLoader.format("command.autumn.unknown", subCommand));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "togglebossbar", "reloadcrafthelper", "togglecrafthelper", "toggleitemalert", "toggleinventorycenter", "toggletype", "lang", "help");
        } else if (args.length == 2 && "lang".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "en", "cn");
        }
        return null;
    }

    private void sendHelpMessage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.title")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.togglebossbar")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.togglecrafthelper")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.toggleitemalert")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.toggleinventorycenter")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.toggletype")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.reloadcrafthelper")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.standalone")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.quickplay")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.1s")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.2s")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.3s")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.4s")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.4v4")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.solo")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.teams")));
        sender.addChatMessage(new ChatComponentText(LanguageLoader.format("command.autumn.help.footer")));
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