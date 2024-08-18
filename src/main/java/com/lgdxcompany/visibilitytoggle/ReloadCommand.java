package com.lgdxcompany.visibilitytoggle;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
@SuppressWarnings("ALL")

public class ReloadCommand implements CommandExecutor {
    private final VisibilityToggle plugin;

    public ReloadCommand(VisibilityToggle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("visibilitytoggle.reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Prefix") + " Configuration reloaded."));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou do not have permission to use this command."));
        }
        return true;
    }
}
