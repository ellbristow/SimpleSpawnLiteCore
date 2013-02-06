package me.ellbristow.simplespawnlitecore;

import java.io.IOException;
import java.util.HashMap;
import me.ellbristow.simplespawnlitecore.events.SimpleSpawnChangeLocationEvent;
import me.ellbristow.simplespawnlitecore.events.SimpleSpawnTeleportEvent;
import me.ellbristow.simplespawnlitecore.utils.Metrics;
import me.ellbristow.simplespawnlitecore.utils.SQLBridge;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSpawnLiteCore extends JavaPlugin {

    private FileConfiguration config;
    private SimpleSpawnLiteCore plugin = this;
    private SQLBridge sql;
    private int teleportEffect;
    private int teleportSound;
    private boolean useTeleportEffect;
    private boolean useTeleportSound;
    
    private String[] spawnColumns = {"world", "x", "y", "z", "yaw", "pitch"};
    private String[] spawnDims = {"TEXT NOT NULL PRIMARY KEY", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sorry! Command " + commandLabel + " cannot be run from the console!");
            return false;
        }
        
        Player player = (Player) sender;
        
        if (commandLabel.equalsIgnoreCase("setspawn")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.set")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to set the spawn location!");
                    return false;
                }
                setWorldSpawn(player.getLocation());
                player.sendMessage(ChatColor.GOLD + "Spawn been set to this location for this world!");
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("*default")) {
                    if (!player.hasPermission("simplespawn.set.default")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to set the spawn location for new players!");
                        return false;
                    }
                    setDefaultSpawn(player.getLocation());
                    player.sendMessage(ChatColor.GOLD + "Spawn for new players been set to this location!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Command not recognised!");
                    player.sendMessage(ChatColor.RED + "Try: /setspawn OR /setspawn *default");
                    return false;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setspawn OR /setspawn *default");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("spawn")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.use")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                simpleTeleport(player, getWorldSpawn(player.getWorld().getName()));
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("*default")) {
                    if (!player.hasPermission("simplespawn.use.default")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                        return false;
                    }
                    simpleTeleport(player, getDefaultSpawn());
                    return true;
                }

                if (!player.hasPermission("simplespawn.use.world")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                World world = getServer().getWorld(args[0]);
                if (world == null) {
                    player.sendMessage(ChatColor.RED + "World '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                simpleTeleport(player, getWorldSpawn(world.getName()));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /spawn {worldName} or /spawn *default");
                return false;
            }

        }
        return false;
    }
    
    public void simpleTeleport(final Player player, final Location loc) {
        if (loc == null) {
            return;
        }
        
        Location fromLoc = player.getLocation();
        
        SimpleSpawnTeleportEvent e = new SimpleSpawnTeleportEvent(player, LocationType.WORLD_SPAWN, fromLoc, loc);
        getServer().getPluginManager().callEvent(e);
        
        if (e.isCancelled()) return;

        playSound(fromLoc);
        playEffect(fromLoc);
        
        if (!loc.getWorld().getChunkAt(loc).isLoaded()) {
            loc.getWorld().getChunkAt(loc).load();
        }
        
        player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        getLogger().finer("Player " + player.getName() + " teleported");
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override 
            public void run() {
                playSound(loc);
                playEffect(loc);
            }
        });
        player.sendMessage(ChatColor.GOLD + "WHOOSH!");
    }
    
    private void playSound(Location loc) {
        if (useTeleportSound) {
            switch (teleportSound) {
                case 0:
                    loc.getWorld().playSound(loc, Sound.AMBIENCE_THUNDER, 1, 1);
                    break;
                case 1:
                    loc.getWorld().playSound(loc, Sound.ENDERMAN_TELEPORT, 1, 1);
                    break;
                case 2:
                    loc.getWorld().playSound(loc, Sound.FIRE, 1, 1);
                    break;
                case 3:
                    loc.getWorld().playSound(loc, Sound.EXPLODE, 1, 1);
                    break;
                case 4:
                    loc.getWorld().playSound(loc, Sound.FIZZ, 1, 1);
                    break;
                case 5:
                    loc.getWorld().playSound(loc, Sound.PORTAL_TRIGGER, 1, 1);
                    break;
            }
        }
    }

    private void playEffect(Location loc) {
        loc = loc.clone();
        if (useTeleportEffect) {
            switch (teleportEffect) {
                case 0:
                    loc.getWorld().strikeLightningEffect(loc);
                    break;
                default:
                    loc.setY(loc.getY() + 1);
                    switch (teleportEffect) {
                        case 1:
                            loc.getWorld().playEffect(loc, Effect.ENDER_SIGNAL, 0);
                            break;
                        case 2:
                            loc.getWorld().playEffect(loc, Effect.SMOKE, 0);
                            break;
                        case 3:
                            loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
                            break;
                    }
                    break;
            }
        }
    }
    
    private void setDefaultSpawn(Location location) {
        SimpleSpawnChangeLocationEvent e = new SimpleSpawnChangeLocationEvent(location.getWorld().getName(), LocationType.WORLD_SPAWN, location);
        getServer().getPluginManager().callEvent(e);
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        sql.query("DELETE FROM DefaultSpawn");
        sql.query("INSERT INTO DefaultSpawn (world, x, y, z, yaw, pitch) VALUES ('" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
        location.getWorld().setSpawnLocation((int) x, (int) y, (int) z);
    }

    public Location getDefaultSpawn() {
        HashMap<Integer, HashMap<String, Object>> result = sql.select("world, x, y, z, yaw, pitch", "DefaultSpawn", null, null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            getLogger().finest("No default spawn found using spawn of first world (" + getServer().getWorlds().get(0).getName() + ").");
            location = getServer().getWorlds().get(0).getSpawnLocation();
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }
    
    private void setWorldSpawn(Location location) {
        SimpleSpawnChangeLocationEvent e = new SimpleSpawnChangeLocationEvent(location.getWorld().getName(), LocationType.WORLD_SPAWN, location);
        getServer().getPluginManager().callEvent(e);
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        sql.query("INSERT OR REPLACE INTO WorldSpawns (world, x, y, z, yaw, pitch) VALUES ('" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
        location.getWorld().setSpawnLocation((int) x, (int) y, (int) z);
    }

    private Location getWorldSpawn(String world) {
        HashMap<Integer, HashMap<String, Object>> result = sql.select("x, y, z, yaw, pitch", "WorldSpawns", "world = '" + world + "'", null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            getLogger().finest("No world spawn found for world (" + world + ") in db, using minecraft spawn location.");
            location = getServer().getWorld(world).getSpawnLocation();
        } else {
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }
    
    
    @Override
    public void onDisable() {
        sql.close();
    }

    @Override
    public void onEnable() {
        
        config = getConfig();
        
        useTeleportEffect = config.getBoolean("use_teleport_effect", true);
        getLogger().config("use_teleport_effect:" + useTeleportEffect);

        useTeleportSound = config.getBoolean("use_teleport_sound", true);
        getLogger().config("use_teleport_sound:" + useTeleportSound);

        config.set("use_teleport_effect", useTeleportEffect);
        config.set("use_teleport_sound", useTeleportSound);
        
        teleportEffect = config.getInt("teleport_effect_type", 1);
        config.set("teleport_effect_type", teleportEffect);

        teleportSound = config.getInt("sound_effect_type", 1);
        config.set("sound_effect_type", teleportSound);
        
        saveConfig();
                
        sql = new SQLBridge(plugin);
        sql.getConnection();
        
        if (!sql.checkTable("WorldSpawns")) {
            sql.createTable("WorldSpawns", spawnColumns, spawnDims);
        }
        
        if (!sql.checkTable("DefaultSpawn")) {
            sql.createTable("DefaultSpawn", spawnColumns, spawnDims);
            setDefaultSpawn(getServer().getWorlds().get(0).getSpawnLocation());
        }
        
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        
    }

}
