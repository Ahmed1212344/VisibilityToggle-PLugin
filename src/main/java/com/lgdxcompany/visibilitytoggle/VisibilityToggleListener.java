package com.lgdxcompany.visibilitytoggle;

import de.HyChrod.Friends.Caching.Frienddata;
import de.HyChrod.Friends.Caching.FriendsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
@SuppressWarnings("ALL")

public class VisibilityToggleListener implements Listener {

    private final VisibilityToggle plugin;
    private final FriendsAPI friendsAPI;



    public VisibilityToggleListener(VisibilityToggle plugin) {
        this.plugin = plugin;
        this.friendsAPI = new FriendsAPI(); // Ensure you initialize FriendsAPI correctly
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getLogger().info("PlayerJoinEvent triggered for " + event.getPlayer().getName());
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig();

        if (!plugin.isInEnabledWorld(player)) {
            return;
        }

        if (config.getBoolean("Settings.ClearInvOnJoin")) {
            player.getInventory().clear();
        }

        if (config.getBoolean("Settings.GiveItemOnJoin")) {
            plugin.giveToggleItem(player);
        }

        if (config.getBoolean("Settings.JoinToggleMessage")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + config.getString("Messages.JoinMessage")));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        FileConfiguration config = plugin.getConfig();


        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Handle standard toggle items
            for (String key : config.getConfigurationSection("Items").getKeys(false)) {
                Material material = Material.matchMaterial(config.getString("Items." + key + ".Item"));
                if (material != null && item.getType() == material) {
                    handleToggleItemInteraction(player, key, false);
                    plugin.toggleVisibility(player); // Call the toggleVisibility method
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void handleToggleItemInteraction(Player player, String toggleType, boolean isCustom) {
        FileConfiguration config = plugin.getConfig();
        long cooldown = config.getLong( "Settings.Delay" ) * 1000;
        long lastToggle = plugin.getLastToggleTime().getOrDefault( player.getUniqueId(), 0L );
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastToggle < cooldown && cooldown != -1000) {
            player.sendMessage( ChatColor.translateAlternateColorCodes( '&', config.getString( "Prefix" ) + config.getString( "Messages.Delay" ).replace( "{cooldown}", String.valueOf( (cooldown - (currentTime - lastToggle)) / 1000 ) ) ) );
            return;
        }

        plugin.getLastToggleTime().put( player.getUniqueId(), currentTime );

        // Cycle through the toggle types
        String nextToggleType = getNextToggleType( toggleType, isCustom );
        plugin.setCurrentToggleType( player, nextToggleType );
        plugin.giveToggleItem( player, nextToggleType, isCustom );

        // Updated paths and logic to support custom items without relying on CustomItemsSettings
        String messageKey = isCustom ? "CustomMessages." : "Messages.";
        String titleKey = isCustom ? "CustomTitles." : "Titles.";
        String soundKey = isCustom ? "CustomToggleSound" : "ToggleSound";
        String toggleMessagePath = isCustom ? "Settings.ShowCustomToggleMessages" : "Settings.ShowToggleMessages";
        String toggleTitlePath = isCustom ? "Settings.ShowCustomToggleTitles" : "Settings.ShowToggleTitles";
        String toggleSoundPath = isCustom ? "Settings.CustomToggleSound.Enabled" : "Settings.ToggleSound.Enabled";

        if (config.getBoolean( toggleMessagePath )) {
            player.sendMessage( ChatColor.translateAlternateColorCodes( '&', config.getString( "Prefix" ) + config.getString( messageKey + nextToggleType ) ) );
        }

        if (config.getBoolean( toggleTitlePath )) {
            String title = ChatColor.translateAlternateColorCodes( '&', config.getString( titleKey + nextToggleType + ".Title" ) );
            String subtitle = ChatColor.translateAlternateColorCodes( '&', config.getString( titleKey + nextToggleType + ".Subtitle" ) );
            player.sendTitle( title, subtitle, 10, 70, 20 );
        }

        toggleSoundPath = "Settings.ToggleSound.Enabled";
        soundKey = "Settings.ToggleSound";

        if (config.getBoolean( toggleSoundPath )) {
            String soundName = config.getString( soundKey + ".Sound" );
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    Sound sound = Sound.valueOf( soundName );
                    float volume = (float) config.getDouble( soundKey + ".Vol" );
                    float pitch = (float) config.getDouble( soundKey + ".Pitch" );
                    player.playSound( player.getLocation(), sound, volume, pitch );
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning( "Invalid sound name in config: " + soundName );
                }
            } else {
                plugin.getLogger().warning( "Sound is not set or is empty in the config." );
            }
        }
    }

    private String getNextToggleType(String currentToggleType, boolean isCustom) {
        FileConfiguration config = plugin.getConfig();
        String[] toggleTypes = isCustom ?
                config.getConfigurationSection("CustomItems").getKeys(false).toArray(new String[0]) :
                config.getConfigurationSection("Items").getKeys(false).toArray(new String[0]);

        for (int i = 0; i < toggleTypes.length; i++) {
            if (toggleTypes[i].equals(currentToggleType)) {
                return toggleTypes[(i + 1) % toggleTypes.length];
            }
        }
        return toggleTypes[0];
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        boolean blockDropItem = config.getBoolean("Settings.BlockDropItem");

        if (blockDropItem && !player.hasPermission("visibilitytoggle.bypass")) {
            for (String key : config.getConfigurationSection("Items").getKeys(false)) {
                String toggleItemName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', config.getString("Items." + key + ".Name")));

                if (itemName.equals(toggleItemName)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + " You cannot drop this item."));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        boolean blockMovementItem = config.getBoolean("Settings.BlockMovementItem");

        if (blockMovementItem && !player.hasPermission("visibilitytoggle.bypass")) {
            for (String key : config.getConfigurationSection("Items").getKeys(false)) {
                String toggleItemName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', config.getString("Items." + key + ".Name")));

                if (itemName.equals(toggleItemName)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("Prefix") + " You cannot move this item."));
                    return;
                }
            }
        }
    }


    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.getLogger().info("PlayerChangedWorldEvent triggered for " + event.getPlayer().getName());
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig();

        if (config.getBoolean("Settings.ClearInvOnWorldChange")) {
            player.getInventory().clear();
        }

        if (plugin.isInEnabledWorld(player) && config.getBoolean("Settings.GiveItemOnWorldChange")) {
            plugin.giveToggleItem(event.getPlayer());
        }
    }

    private boolean isFriend(UUID playerUUID, UUID targetUUID, List<Frienddata.Friend> friends) {
        for (Frienddata.Friend friend : friends) {
            if (friend.getUuid().equals(targetUUID)) {
                return true;
            }
        }
        return false;
    }

    private void updateVisibilityBasedOnFriends(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<Frienddata.Friend> friends = friendsAPI.getFriends(playerUUID);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (isFriend(playerUUID, onlinePlayer.getUniqueId(), friends)) {
                player.showPlayer(plugin, onlinePlayer);
            } else {
                player.hidePlayer(plugin, onlinePlayer);
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLogger().info("PlayerQuitEvent triggered for " + event.getPlayer().getName());
        plugin.savePlayerState(event.getPlayer());
        Player player = event.getPlayer();
        if (player.hasMetadata("invisible")) {
            player.removeMetadata("invisible", plugin);
        }
        plugin.friendsVisible.remove(player.getUniqueId());
    }
}
