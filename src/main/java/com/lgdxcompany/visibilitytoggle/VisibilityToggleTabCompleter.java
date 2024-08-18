package com.lgdxcompany.visibilitytoggle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@SuppressWarnings("ALL")

public class VisibilityToggleTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("vt".equalsIgnoreCase(command.getName()) || "visibilitytoggle".equalsIgnoreCase(command.getName())) {
                List<String> subcommands = Arrays.asList("help", "reload");
                for (String subcommand : subcommands) {
                    if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(subcommand);
                    }
                }
            }
        }
        return completions;
    }
}
