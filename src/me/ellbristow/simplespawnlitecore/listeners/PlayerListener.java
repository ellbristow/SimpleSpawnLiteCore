package me.ellbristow.simplespawnlitecore.listeners;

import me.ellbristow.simplespawnlitecore.SimpleSpawnLiteCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {
    
    private SimpleSpawnLiteCore plugin;
    
    public PlayerListener(SimpleSpawnLiteCore instance) {
        plugin = instance;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            plugin.simpleTeleport(player, plugin.getDefaultSpawn());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(plugin.getDefaultSpawn());
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
        
        if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.COMMAND)) {

            Player player = event.getPlayer();
            Location toLoc = event.getTo();
            
            event.setCancelled(true);

            plugin.simpleTeleport(player, toLoc);
            
        }
    }
    
}
