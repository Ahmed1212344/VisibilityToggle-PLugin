package com.lgdxcompany.visibilitytoggle;

import de.HyChrod.Friends.Caching.Frienddata;
import de.HyChrod.Friends.Caching.FriendsAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;
@SuppressWarnings("ALL")

public class EventHandlerToggleType implements Listener {

    private final VisibilityToggle plugin;
    private final LuckPerms luckPerms;
    private final FriendsAPI friendsAPI;


    public EventHandlerToggleType(VisibilityToggle plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
        this.friendsAPI = new FriendsAPI(); // Ensure you initialize FriendsAPI correctly
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        handlePlayerVisibility(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player's view direction has changed significantly
        if (event.getFrom().getPitch() != event.getTo().getPitch() ||
                event.getFrom().getYaw() != event.getTo().getYaw()) {

            // Immediately apply the toggle type based on the current state
            handlePlayerVisibility(player);
        }
    }

    @EventHandler
    public void onPlayerInteract( PlayerInteractEvent event ) {
        Player player = event.getPlayer();
        if (event.getItem() != null && plugin.isToggleItem(event.getItem())) {
            event.setCancelled(true);

            String toggleType = plugin.getCurrentToggleType(player);

            if (toggleType.equals("Show")) {
                plugin.showAllPlayers(player);
                plugin.sendToggleMessages(player, "Show");
                plugin.playToggleSound(player, "Show");
                plugin.sendToggleTitles(player, "Show");
            } else if (toggleType.equals("Friends")) {
                for (Player targetPlayer : player.getWorld().getPlayers()) {
                    if (isFriend(player, targetPlayer)) {
                        player.showPlayer(plugin, targetPlayer);
                    } else {
                        player.hidePlayer(plugin, targetPlayer);
                    }
                }
                plugin.sendToggleMessages(player, "Friends");
                plugin.playToggleSound(player, "Friends");
                plugin.sendToggleTitles(player, "Friends");
            } else if (toggleType.equals("Hide")) {
                plugin.hideFromAllPlayers(player);
                plugin.sendToggleMessages(player, "Hide");
                plugin.playToggleSound(player, "Hide");
                plugin.sendToggleTitles(player, "Hide");
            } else {
                // Handle custom toggle types
                if (hasRequiredRank(player, toggleType)) {
                    plugin.showAllPlayers(player);
                } else {
                    plugin.hideFromAllPlayers(player);
                }
                plugin.sendToggleMessages(player, toggleType);
                plugin.playToggleSound(player, toggleType);
                plugin.sendToggleTitles(player, toggleType);
            }
        }
    }

    public void handlePlayerVisibility(Player togglingPlayer) {
        String toggleType = plugin.getCurrentToggleType(togglingPlayer);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (toggleType.equals("Show")) {
                togglingPlayer.showPlayer(plugin, player);
            } else if (toggleType.equals("Hide")) {
                if (hasRequiredRank(player, toggleType)) {
                    togglingPlayer.showPlayer(plugin, player);
                } else {
                    togglingPlayer.hidePlayer(plugin, player);
                }
            } else if (toggleType.equals("Friends")) {
                if (isFriend(togglingPlayer, player)) {
                    togglingPlayer.showPlayer(plugin, player);
                } else {
                    togglingPlayer.hidePlayer(plugin, player);
                }
            } else {
                // Handle custom toggle types
                if (hasRequiredRank(player, toggleType)) {
                    togglingPlayer.showPlayer(plugin, player);
                } else {
                    togglingPlayer.hidePlayer(plugin, player);
                }
            }
        }
    }

    private boolean isFriend(Player player, Player potentialFriend) {
        UUID playerUUID = player.getUniqueId();
        UUID friendUUID = potentialFriend.getUniqueId();
        List<Frienddata.Friend> friends = friendsAPI.getFriends(playerUUID);
        for (Frienddata.Friend friend : friends) {
            if (friend.getUuid().equals(friendUUID)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRequiredRank(Player player, String toggleType) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return false;
        }

        // Get the configuration section for specificsRanksThatCustomToggleItemWillShow
        ConfigurationSection specificRanksSection = plugin.getConfig().getConfigurationSection("specificsRanksThatCustomToggleItemWillShow." + toggleType);
        if (specificRanksSection == null) {
            return false; // No specific ranks found for this toggleType
        }

        List<String> requiredRanks = specificRanksSection.getStringList("TheRankWillBeShownInThisType");
        for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
            if (requiredRanks.contains(node.getGroupName())) {
                return true;
            }
        }
        return false;
    }

}
