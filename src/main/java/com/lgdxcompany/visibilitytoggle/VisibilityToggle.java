package com.lgdxcompany.visibilitytoggle;

import de.HyChrod.Friends.Caching.Frienddata;
import de.HyChrod.Friends.Caching.FriendsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
@SuppressWarnings("ALL")

public class VisibilityToggle extends JavaPlugin implements Listener {

    private boolean supportsTitles;
    private static VisibilityToggle instance;
    private FriendsAPI friendsAPI;
    public final Set<UUID> friendsVisible = new HashSet<>();
    public Set<UUID> cooldowns = new HashSet<>();
    public HashMap<UUID, String> playerToggleStates = new HashMap<>();
    public Map<Player, String> playerToggleTypes = new HashMap<>();
    public final Map<UUID, Long> lastToggleTime = new HashMap<>();
    private Map<String, ItemStack> customItems = new HashMap<>();
    private Map<String, String> customMessages = new HashMap<>();
    private Map<String, Map<String, String>> customTitles = new HashMap<>();


    @Override
    public void onEnable() {
        getLogger().info("\u001B[33;1m[\u001B[34;1mVisibilityToggle\u001B[33;1m] \u001B[32;1mThe plugin has been enabled!\u001B[0m");
        saveDefaultConfig();
        reloadConfig();
        loadCustomSettings();
        this.friendsAPI = new FriendsAPI(); // Ensure you initialize FriendsAPI correctly
        instance = this;
        this.saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        this.reloadConfig();
        File titlesizeFile = new File(getDataFolder(), "titlesize.yml");
        if (!titlesizeFile.exists()) {
            saveResource("titlesize.yml", false);
        }
        getServer().getPluginManager().registerEvents(new EventHandlerToggleType(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VisibilityToggleListener(this), this);
        getCommand("visibilitytoggle").setExecutor(new ToggleCommand(this));
        getCommand("vt").setExecutor(new ToggleCommand(this));
        getCommand("vtreload").setExecutor(new ReloadCommand(this));
        getCommand("vtr").setExecutor(new ReloadCommand(this));
        getCommand("vthelp").setExecutor(new ToggleCommand(this));
        getCommand("visibilitytoggle").setTabCompleter(new VisibilityToggleTabCompleter());

        supportsTitles = checkTitleSupport();
    }

    public static VisibilityToggle getInstance() {
        return instance;
    }
    @Override
    public void onDisable() {
        getLogger().info("\u001B[33;1m[\u001B[34;1mVisibilityToggle\u001B[33;1m] \u001B[0mThe plugin has been disabled!");
    }

    public void loadCustomSettings() {
        FileConfiguration config = getConfig();

        // Load custom items
        if (config.isConfigurationSection( "CustomItems" )) {
            for (String key : config.getConfigurationSection( "CustomItems" ).getKeys( false )) {
                String name = ChatColor.translateAlternateColorCodes( '&', config.getString( "CustomItems." + key + ".Name" ) );
                Material material = Material.valueOf( config.getString( "CustomItems." + key + ".Item" ) );
                int data = config.getInt( "CustomItems." + key + ".Data" );
                List<String> lore = config.getStringList( "CustomItems." + key + ".Lore" );

                ItemStack item = new ItemStack( material, 1, (short) data );
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName( name );
                    meta.setLore( lore );
                    item.setItemMeta( meta );
                }

                customItems.put( key, item );
            }
        }

        // Load custom messages
        if (config.isConfigurationSection( "CustomMessages" )) {
            for (String key : config.getConfigurationSection( "CustomMessages" ).getKeys( false )) {
                String message = ChatColor.translateAlternateColorCodes( '&', config.getString( "CustomMessages." + key ) );
                customMessages.put( key, message );
            }
        }

        // Load custom titles
        if (config.isConfigurationSection( "CustomTitles" )) {
            for (String key : config.getConfigurationSection( "CustomTitles" ).getKeys( false )) {
                String title = ChatColor.translateAlternateColorCodes( '&', config.getString( "CustomTitles." + key + ".Title" ) );
                String subtitle = ChatColor.translateAlternateColorCodes( '&', config.getString( "CustomTitles." + key + ".Subtitle" ) );
                Map<String, String> titleMap = new HashMap<>();
                titleMap.put( "Title", title );
                titleMap.put( "Subtitle", subtitle );
                customTitles.put( key, titleMap );
            }
        }
    }

    public Map<String, ItemStack> getCustomItems() {
        return customItems;
    }

    public Map<String, String> getCustomMessages() {
        return customMessages;
    }

    public Map<String, Map<String, String>> getCustomTitles() {
        return customTitles;
    }


    public void reloadPluginConfig() {
        reloadConfig();
        super.reloadConfig();
        loadCustomSettings();
        this.reloadConfig();
        getLogger().info("Configuration reloaded.");
    }

    public void giveToggleItem(Player player) {
        String toggleType = playerToggleStates.getOrDefault(player.getUniqueId(), "Show");
        giveToggleItem(player, toggleType, false);
    }

    public void giveToggleItem(Player player, String toggleType, boolean isCustom) {
        FileConfiguration config = getConfig();
        if (config.getBoolean("Settings.GiveItemOnJoin")) {
            if (isCustom) {
                ItemStack customItem = customItems.get(toggleType);
                if (customItem != null) {
                    player.getInventory().setItem(config.getInt("Settings.Slot"), customItem);
                } else {
                    getLogger().warning("Custom item for toggle type " + toggleType + " not found.");
                }
            } else {
                playerToggleStates.put(player.getUniqueId(), toggleType);
                String itemName = ChatColor.translateAlternateColorCodes('&', config.getString("Items." + toggleType + ".Name"));
                Material material = Material.matchMaterial(config.getString("Items." + toggleType + ".Item"));
                int dataValue = config.getInt("Items." + toggleType + ".Data");

                ItemStack item = new ItemStack(material, 1, (short) dataValue);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(itemName);
                    List<String> lore = config.getStringList("Items." + toggleType + ".Lore");
                    for (int i = 0; i < lore.size(); i++) {
                        lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                player.getInventory().setItem(config.getInt("Settings.Slot"), item);
            }
        }
    }

    // This method cycles through the toggle types and gives the next one to the player
    public void cycleToggleItem(Player player) {
        String currentToggleType = playerToggleStates.getOrDefault(player.getUniqueId(), "Show");
        String nextToggleType = getNextToggleType(currentToggleType);
        giveToggleItem(player, nextToggleType, false);
    }

    // Helper method to get the next toggle type
    private String getNextToggleType(String currentToggleType) {
        List<String> toggleTypes = Arrays.asList("Show", "Friends", "Hide");
        int index = toggleTypes.indexOf(currentToggleType);
        if (index == -1 || index + 1 >= toggleTypes.size()) {
            return toggleTypes.get(0);
        }
        return toggleTypes.get(index + 1);
    }

    public void savePlayerState(Player player) {
        playerToggleStates.put(player.getUniqueId(), getCurrentToggleType(player));
    }

    public String getCurrentToggleType(Player player) {
        return playerToggleStates.getOrDefault(player.getUniqueId(), "Show");
    }

    public void setCurrentToggleType(Player player, String toggleType) {
        playerToggleTypes.put(player, toggleType);
    }

    public boolean isToggleItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        FileConfiguration config = getConfig();
        for (String toggleType : Arrays.asList("Show", "Friends", "Hide")) {
            String itemName = config.getString("Items." + toggleType + ".Name");
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(itemName))) {
                return true;
            }
        }
        return false;
    }

    public Map<UUID, Long> getLastToggleTime() {
        return lastToggleTime;
    }

    public enum ToggleState {
        VISIBLE, HIDDEN, FRIENDS_ONLY
    }

    public void toggleVisibility(Player player) {
        ToggleState currentState = getCurrentState(player);

        switch (currentState) {
            case VISIBLE:
                hideFromAllPlayers(player);
                player.setMetadata("invisible", new FixedMetadataValue(this, true));
                playToggleSound(player, "HIDDEN");
                break;

            case HIDDEN:
                hideFromNonFriends(player);
                friendsVisible.add(player.getUniqueId());
                player.setMetadata("visibleonly", new FixedMetadataValue(this, "friends"));
                playToggleSound(player, "FRIENDS_ONLY");
                break;

            case FRIENDS_ONLY:
                showAllPlayers(player);
                player.removeMetadata("invisible", this);
                player.removeMetadata("visibleonly", this);
                friendsVisible.remove(player.getUniqueId());
                playToggleSound(player, "VISIBLE");
                break;

            default:
                // Handle custom toggle types separately
                if (isCustomToggleType(currentState)) {
                    applyRankSpecificVisibility(player, currentState);
                } else {
                    showAllPlayers(player);
                }
                break;
        }
    }

    private boolean isCustomToggleType(ToggleState state) {
        // Check if the toggle state corresponds to a custom toggle type
        return getConfig().getConfigurationSection("specificsRanksThatCustomToggleItemWillShow").contains(state.name());
    }

    private void applyRankSpecificVisibility(Player player, ToggleState customToggleType) {
        List<String> requiredRanks = getConfig().getStringList("specificsRanksThatCustomToggleItemWillShow." + customToggleType.name() + ".TheRankWillBeShownInThisType");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            boolean shouldShow = false;
            for (String rank : requiredRanks) {
                if (onlinePlayer.hasPermission("group." + rank)) {  // Assuming LuckPerms uses "group.rankname" as the permission node
                    shouldShow = true;
                    break;
                }
            }
            if (shouldShow) {
                player.showPlayer(this, onlinePlayer);
            } else {
                player.hidePlayer(this, onlinePlayer);
            }
        }
        playToggleSound(player, customToggleType.name());
    }


    private ToggleState getCurrentState(Player player) {
        if (player.hasMetadata("invisible")) {
            return ToggleState.HIDDEN;
        } else if (friendsVisible.contains(player.getUniqueId())) {
            return ToggleState.FRIENDS_ONLY;
        } else {
            return ToggleState.VISIBLE;
        }
    }

    public void showAllPlayers(Player player) {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            player.showPlayer(this, p);
        }
    }

    public void hideFromAllPlayers(Player player) {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (!p.equals(player)) {
                player.hidePlayer(this, p);
            }
        }
    }

    public void hideFromNonFriends(Player player) {
        List<Frienddata.Friend> friends = friendsAPI.getFriends(player.getUniqueId());
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (!isFriend(player, p, friends)) {
                player.hidePlayer(this, p);
            }
        }
    }

    private boolean isFriend(Player player, Player potentialFriend, List<Frienddata.Friend> friends) {
        UUID friendUUID = potentialFriend.getUniqueId();
        return friends.stream().anyMatch(friend -> friend.getUuid().equals(friendUUID));
    }

    public void sendToggleMessages(Player player, String state) {
        FileConfiguration config = getConfig();
        if (config.getBoolean("Settings.ShowToggleMessages")) {
            String message = config.getString("Messages." + state, "&eVisibility " + state.toLowerCase() + ".");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public void sendToggleTitles(Player player, String state) {
        FileConfiguration config = getConfig();
        if (config.getBoolean("Settings.ShowToggleTitles") && supportsTitles) {
            String title = config.getString("Titles." + state + ".Title", "&e" + state)
                    .replace("%state%", state);
            String subtitle = config.getString("Titles." + state + ".Subtitle", "&eYou are now " + state.toLowerCase())
                    .replace("%state%", state);

            File titleSizeFile = new File(getDataFolder(), "titlesize.yml");
            FileConfiguration titleSizeConfig = YamlConfiguration.loadConfiguration(titleSizeFile);

            int fadeIn = titleSizeConfig.getInt("title-size.fadeIn", 10);
            int stay = titleSizeConfig.getInt("title-size.stay", 70);
            int fadeOut = titleSizeConfig.getInt("title-size.fadeOut", 20);

            sendTitle(player, ChatColor.translateAlternateColorCodes('&', title),
                    ChatColor.translateAlternateColorCodes('&', subtitle), fadeIn, stay, fadeOut);
        }
    }

    public void playToggleSound(Player player, String state) {
        FileConfiguration config = getConfig();
        if (config.getBoolean("Settings.ToggleSound.Enabled")) {
            String soundName = config.getString("ToggleSound." + state, "BLOCK_NOTE_BLOCK_PLING");
            try {
                Sound sound = Sound.valueOf(soundName);
                float volume = (float) config.getDouble("Settings.ToggleSound.Vol", 1.0);
                float pitch = (float) config.getDouble("Settings.ToggleSound.Pitch", 1.0);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid sound name in config: " + soundName);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
            }
        }
    }



    public @Nullable Logger getLogger() {
        return super.getLogger();
    }

    private void startCooldown(Player player) {
        int delay = getConfig().getInt("Settings.Delay", 5);
        cooldowns.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(this, () -> cooldowns.remove(player.getUniqueId()), delay * 20L);
    }

    public boolean isInEnabledWorld(Player player) {
        List<String> enabledWorlds = getConfig().getStringList("EnabledWorlds");
        return enabledWorlds.contains(player.getWorld().getName());
    }

    private boolean checkTitleSupport() {
        String version = Bukkit.getVersion();
        return version.contains("1.9") || version.contains("1.10") || version.contains("1.11") || version.contains("1.12") ||
                version.contains("1.13") || version.contains("1.14") || version.contains("1.15") || version.contains("1.16") ||
                version.contains("1.17") || version.contains("1.18") || version.contains("1.19") || version.contains("1.20");
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Method sendTitle = Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            sendTitle.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
        } catch (ReflectiveOperationException e) {
            getLogger().warning("Failed to send title to player " + player.getName());
        }
    }
}
