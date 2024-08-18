package com.lgdxcompany.visibilitytoggle;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@SuppressWarnings("ALL")

public class ToggleCommand implements CommandExecutor {

    private final VisibilityToggle plugin;
    private final Map<UUID, Long> lastToggleTime = new HashMap<>();


    public ToggleCommand(VisibilityToggle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage( ChatColor.DARK_RED + "Only players can use this command.");
            Player player = (Player) sender;
            plugin.toggleVisibility(player); // Call the toggleVisibility method
            return true;
        }

        Player player = (Player) sender;

        // Log debug message
        if (plugin.getConfig().getBoolean("Settings.DebugMode")) {
            plugin.getLogger().info("ToggleCommand executed by: " + player.getName());
        }

        // Reload the configuration each time the command is executed to ensure we get the latest settings
        plugin.reloadConfig();
        plugin.loadCustomSettings(); // Reload custom settings
        FileConfiguration config = plugin.getConfig();

        boolean blockDropItem = config.getBoolean("Settings.BlockDropItem");
        boolean blockMovementItem = config.getBoolean("Settings.BlockMovementItem");

        // Log debug messages for configuration values
        plugin.getLogger().info("BlockDropItem setting: " + blockDropItem);
        plugin.getLogger().info("BlockMovementItem setting: " + blockMovementItem);


        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            if (player.hasPermission("visibilitytoggle.help")) {
                showHelpMenu(player);
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + " You do not have permission to use this command."));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("visibilitytoggle.reload")) {
                plugin.reloadPluginConfig();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + " Configuration reloaded."));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + " You do not have permission to use this command."));
            }
            return true;
        }

        if (args.length > 0) {
            String toggleType = args[0];
            player.sendMessage("Toggle type set to " + toggleType);
            return true;
        }

        if (args.length > 0) {
            String customToggleType = args[0];
            if (plugin.getCustomItems().containsKey(customToggleType)) {
                handleCustomToggle(player, customToggleType);
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + " Invalid usage command."));
            }
            return true;
        }

        long cooldown = config.getInt("Settings.Delay") * 1000L;
        long currentTime = System.currentTimeMillis();

        if (lastToggleTime.containsKey(player.getUniqueId())) {
            long lastTime = lastToggleTime.get(player.getUniqueId());
            if (currentTime - lastTime < cooldown) {
                long remainingTime = (cooldown - (currentTime - lastTime)) / 1000;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Messages.Delay").replace("{cooldown}", String.valueOf(remainingTime))));
                return true;
            }
        }

        lastToggleTime.put(player.getUniqueId(), currentTime);

        String toggleType = plugin.getCurrentToggleType(player);

        if (toggleType.equals("Show")) {
            toggleType = "Friends";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Messages.Friends")));
        } else if (toggleType.equals("Friends")) {
            toggleType = "Hide";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Messages.Hide")));
        } else {
            toggleType = "Show";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Messages.Show")));
        }

        plugin.giveToggleItem(player, toggleType, false);
        plugin.setCurrentToggleType(player, toggleType);

        if (config.getBoolean("Settings.ShowToggleTitles")) {
            String title = ChatColor.translateAlternateColorCodes('&', config.getString("Titles." + toggleType + ".Title"));
            String subtitle = ChatColor.translateAlternateColorCodes('&', config.getString("Titles." + toggleType + ".Subtitle"));
            player.sendTitle(title, subtitle, 10, 70, 20);
        }

        if (config.getBoolean("Settings.ToggleSound.Enabled")) {
            String soundName = config.getString("Settings.ToggleSound.Sound");
            float volume = (float) config.getDouble("Settings.ToggleSound.Vol");
            float pitch = (float) config.getDouble("Settings.ToggleSound.Pitch");

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound name: " + soundName);
            }
        }

        return true;
    }

    private void handleCustomToggle(Player player, String customToggleType) {
        FileConfiguration config = plugin.getConfig();

        if (config.getBoolean("CustomItemsSettings.ShowCustomToggleMessages")) {
            String customMessage = plugin.getCustomMessages().get(customToggleType);
            if (customMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', customMessage));
            }
        }

        if (config.getBoolean("CustomItemsSettings.ShowCustomToggleTitles")) {
            Map<String, String> customTitleMap = plugin.getCustomTitles().get(customToggleType);
            if (customTitleMap != null) {
                String title = customTitleMap.get("Title");
                String subtitle = customTitleMap.get("Subtitle");
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
        }

        if (config.getBoolean("CustomItemsSettings.CustomToggleSound.Enabled")) {
            String soundName = config.getString("CustomItemsSettings.CustomToggleSound.Sound");
            float volume = (float) config.getDouble("CustomItemsSettings.CustomToggleSound.Vol");
            float pitch = (float) config.getDouble("CustomItemsSettings.CustomToggleSound.Pitch");

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound name: " + soundName);
            }
        }

        ItemStack customItem = plugin.getCustomItems().get(customToggleType);
        player.getInventory().addItem(customItem);
    }

    private void showHelpMenu(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Prefix") + " &eHelp Menu:"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/vt &7- Toggle player visibility"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/vt reload &7- Reload the plugin configuration"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/vt help &7- Show this help menu"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/visibilitytoggle &7- Toggle player visibility"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/visibilitytoggle reload &7- Reload the plugin configuration"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/visibilitytoggle help &7- Show this help menu"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/vtreload &7- Reload the plugin configuration"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/vthelp &7- Show this help menu"));
    }
}
