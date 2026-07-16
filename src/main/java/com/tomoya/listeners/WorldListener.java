package com.tomoya.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import com.tomoya.BeaconWaypointsReloaded;
import com.tomoya.LanguageManager;
import com.tomoya.gui.GUIs;
import com.tomoya.waypoints.Waypoint;
import com.tomoya.waypoints.WaypointCoord;
import com.tomoya.waypoints.WaypointManager;
import com.tomoya.waypoints.WaypointPlayer;

public class WorldListener implements Listener {

    // adds players to waypointPlayers map when they join if they are already not in
    // the map
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        BeaconWaypointsReloaded plugin = BeaconWaypointsReloaded.getPlugin();
        WaypointManager waypointManager = BeaconWaypointsReloaded.getWaypointManager();
        LanguageManager languageManager = BeaconWaypointsReloaded.getLanguageManager();
        WaypointPlayer waypointPlayer = waypointManager.getPlayer(e.getPlayer().getUniqueId());

        // add if not in map
        if (waypointPlayer == null)
            waypointManager.addPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        else if (waypointPlayer.getUsername() == null || !waypointPlayer.getUsername().equals(e.getPlayer().getName()))
            waypointPlayer.setUsername(e.getPlayer().getName());

    }

    // add new non-activated waypoint when one is placed, and make the player who
    // placed it the owner
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.BEACON) {
            UUID playerUUID = e.getPlayer().getUniqueId();
            Waypoint newWaypoint = new Waypoint(playerUUID, new WaypointCoord(e.getBlock().getLocation()));
            BeaconWaypointsReloaded.getWaypointManager().addInactiveWaypoint(newWaypoint);
        }
    }

    // delete waypoint when beacon is broken
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.BEACON) {
            WaypointManager waypointManager = BeaconWaypointsReloaded.getWaypointManager();
            LanguageManager languageManager = BeaconWaypointsReloaded.getLanguageManager();
            WaypointCoord waypointCoord = new WaypointCoord(e.getBlock().getLocation());

            // check if player has permission to break waypoint beacons and if the beacon
            // has waypoints
            Waypoint publicWaypoint1 = waypointManager.getPublicWaypoint(waypointCoord);
            List<Waypoint> privateWaypoints = waypointManager.getPrivateWaypointsAtCoord(waypointCoord);
            boolean ownsWaypoint = true;
            if (publicWaypoint1 != null && !publicWaypoint1.getOwnerUUID().equals(e.getPlayer().getUniqueId()))
                ownsWaypoint = false;
            if (ownsWaypoint && privateWaypoints.size() > 0) {
                for (Waypoint privateWaypoint : privateWaypoints) {
                    if (!privateWaypoint.getOwnerUUID().equals(e.getPlayer().getUniqueId())) {
                        ownsWaypoint = false;
                        break;
                    }
                }
            }
            if (!e.getPlayer().hasPermission("BeaconWaypoints.manageAllWaypoints")
                    && !e.getPlayer().hasPermission("BeaconWaypoints.breakWaypointBeacons")) {
                FileConfiguration config = BeaconWaypointsReloaded.getPlugin().getConfig();
                if (!config.contains("allow-beacon-break-by-owner"))
                    config.set("allow-beacon-break-by-owner", true);
                if (!(ownsWaypoint && config.getBoolean("allow-beacon-break-by-owner"))) {
                    e.getPlayer().sendMessage(ChatColor.RED + languageManager.getString("no-break-permission"));
                    e.setCancelled(true);
                }
            } else {
                // wait for one tick to see if plugins like WorldGuard restored the block
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        waypointManager.removeWaypointsAtCoord(waypointCoord);
                    }
                }.runTaskLater(BeaconWaypointsReloaded.getPlugin(), 1);
            }
        }
    }

    // deletes waypoints if they are removed with fill or set block commands
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent e) {
        if (e.getBlock().getType() != Material.AIR)
            return;

        WaypointManager waypointManager = BeaconWaypointsReloaded.getWaypointManager();
        WaypointCoord waypointCoord = new WaypointCoord(e.getBlock().getLocation());

        if (!waypointManager.hasWaypointAtCoord(waypointCoord))
            return;

        waypointManager.removeWaypointsAtCoord(waypointCoord);
    }

    // when player opens a beacon
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND
                && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.BEACON
                && !e.getPlayer().isSneaking()) {
            Player player = e.getPlayer();

            // TRICK LỎ: Kiểm tra xem player có đang muốn mở vanilla menu không
            if (GUIs.bypassVanillaBeacon.contains(player.getUniqueId())) {
                // Tha cho nó lần này để mở menu gốc, xong rồi xoá tên khỏi danh sách
                GUIs.bypassVanillaBeacon.remove(player.getUniqueId());
                return;
            }

            // check if player has permission to use waypoints
            if (player.hasPermission("BeaconWaypoints.useWaypoints")) {
                WaypointManager waypointManager = BeaconWaypointsReloaded.getWaypointManager();
                WaypointCoord waypointCoord = new WaypointCoord(e.getClickedBlock().getLocation());
                Waypoint waypoint = waypointManager.getPinnedWaypoint(waypointCoord);
                boolean isWaypointPublic = true;
                if (waypoint == null) {
                    waypoint = waypointManager.getPublicWaypoint(waypointCoord);
                }
                if (waypoint == null) {
                    isWaypointPublic = false;
                    waypoint = waypointManager.getPlayer(player.getUniqueId()).getWaypoint(waypointCoord);
                }
                if (waypoint != null) {
                    // Chặn menu gốc lại
                    e.setCancelled(true);

                    if (BeaconWaypointsReloaded.getPlugin().getConfig().getBoolean("discovery-mode")) {
                        // discovery mode
                        if (isWaypointPublic && !waypoint.playerDiscoveredWaypoint(player)) {
                            waypoint.addPlayerDiscovered(player);
                            if (!waypoint.getOwnerUUID().equals(player.getUniqueId()))
                                player.sendMessage(ChatColor.GREEN
                                        + BeaconWaypointsReloaded.getLanguageManager().getString("discovered-waypoint")
                                        + ": " + ChatColor.BOLD + waypoint.getName());
                        }
                    }
                    // Mở menu custom
                    GUIs.beaconMenu(player, waypoint);
                }
            }
        }
    }

    // when player throws an ender pearl and is teleporting, cancel it
    @EventHandler
    public void onProjectileThrow(ProjectileLaunchEvent e) {
        Projectile projectile = e.getEntity();
        if (projectile.getShooter() instanceof Player && projectile.getType() == EntityType.ENDER_PEARL) {
            WaypointPlayer waypointPlayer = BeaconWaypointsReloaded.getWaypointManager()
                    .getPlayer(((Player) projectile.getShooter()).getUniqueId());
            if (waypointPlayer != null && waypointPlayer.isTeleporting())
                e.setCancelled(true);
        }
    }

    // when player eats a chorus fruit and is teleporting, cancel it
    @EventHandler
    public void onEat(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.CHORUS_FRUIT) {
            WaypointPlayer waypointPlayer = BeaconWaypointsReloaded.getWaypointManager()
                    .getPlayer(e.getPlayer().getUniqueId());
            if (waypointPlayer != null && waypointPlayer.isTeleporting())
                e.setCancelled(true);
        }
    }

    // disable damage to player when teleporting
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity().getType() == EntityType.PLAYER && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            WaypointPlayer waypointPlayer = BeaconWaypointsReloaded.getWaypointManager()
                    .getPlayer(e.getEntity().getUniqueId());
            if (waypointPlayer != null && waypointPlayer.isTeleporting())
                e.setCancelled(true);
        }
    }
}