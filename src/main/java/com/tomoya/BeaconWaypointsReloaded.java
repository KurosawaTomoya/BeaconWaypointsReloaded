package com.tomoya;

import com.earth2me.essentials.IEssentials;
import com.tomoya.gui.MenuManager;
import com.tomoya.listeners.InventoryListener;
import com.tomoya.listeners.WorldListener;
import com.tomoya.waypoints.Waypoint;
import com.tomoya.waypoints.WaypointManager;
import com.tomoya.waypoints.WaypointPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

public class BeaconWaypointsReloaded extends JavaPlugin {
    private static BeaconWaypointsReloaded plugin;
    private static LanguageManager languageManager;
    private static WaypointManager waypointManager;
    private static MenuManager menuManager;

    private final WorldListener worldListener = new WorldListener();
    private final InventoryListener inventoryListener = new InventoryListener(this);

    private BukkitRunnable autoSave = new BukkitRunnable() {
        @Override
        public void run() {
            saveData();
        }
    };

    @Override
    public void onEnable() {
        plugin = this;
        waypointManager = new WaypointManager();
        menuManager = new MenuManager();

        // bStats
        Metrics metrics = new Metrics(this, 14276);
        metrics.addCustomChart(new Metrics.SingleLineChart("waypoints", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return waypointManager.getPublicWaypoints().size() + waypointManager.getNumPrivateWaypoints();
            }
        }));

        // register commands
        BWCommandExecutor commandExecutor = new BWCommandExecutor(this);
        Objects.requireNonNull(getCommand("waypoint")).setExecutor(commandExecutor);

        // register events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(worldListener, this);
        pm.registerEvents(inventoryListener, this);

        // create data folder if it doesn't exist
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        // create config file if it doesn't exist
        if (!new File(getDataFolder(), "config.yml").exists())
            saveDefaultConfig();

        // load language config
        loadLanguage();

        // config update checker
        try {
            ConfigUpdater.checkConfig(getConfig());
        } catch (IOException e) {
            getLogger().warning("Unable to run the update checker for config.yml");
        }

        try {
            ConfigUpdater.checkLanguageConfig(languageManager.getDefaults());
        } catch (IOException e) {
            getLogger().warning("Unable to run the update checker for language.yml");
        }

        // create folder for player waypoints if it doesn't exist
        File playerDir = new File(getDataFolder() + File.separator + "players");
        if (!playerDir.exists())
            playerDir.mkdirs();

        loadData();
        autoSave.runTaskTimer(plugin, 6000, 6000);


        // check if EssentialsX is installed
        IEssentials essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null)
            this.getLogger().warning(languageManager.getString("essentials-not-installed"));
    }

    @Override
    public void onDisable() {
        autoSave.cancel();
        saveData();
    }

    public void loadData() {
        this.reloadConfig();

        // read data from public file
        JSONParser parser = new JSONParser();
        try {
            JSONArray jsonWaypoints = (JSONArray) parser.parse(new InputStreamReader(Files.newInputStream(Paths.get("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "public.json")), StandardCharsets.UTF_8));
            for (JSONObject jsonWaypoint : (Iterable<JSONObject>) jsonWaypoints) {
                Waypoint waypoint = new Waypoint(jsonWaypoint);
                if (waypoint.getName() != null) {
                    if (waypoint.isPinned())
                        waypointManager.addPinnedWaypoint(waypoint);
                    else waypointManager.addPublicWaypoint(waypoint);
                }
            }
        } catch(IOException | ParseException e) {
            getLogger().info(e.getMessage());
        }

        // read data from player files
        try {
            File playerDir = new File("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "players");
            if (playerDir.listFiles() != null) {
                for (File playerFile : Objects.requireNonNull(playerDir.listFiles())) {
                    if (playerFile.isFile() && playerFile.getName().endsWith(".json")) {
                        JSONObject jsonPlayer = (JSONObject) parser.parse(new InputStreamReader(Files.newInputStream(Paths.get("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "players/" + File.separator + "" + playerFile.getName())), StandardCharsets.UTF_8));
                        Object uuid = jsonPlayer.get("uuid");
                        Object username = jsonPlayer.get("username");
                        if (waypointManager.getPlayer(UUID.fromString(uuid.toString())) == null) {
                            waypointManager.addPlayer(UUID.fromString(uuid.toString()), username == null ? null : username.toString());
                        }
                        for (JSONObject jsonWaypoint : (Iterable<JSONObject>) jsonPlayer.get("waypoints")) {
                            Waypoint waypoint = new Waypoint(jsonWaypoint);
                            if (waypoint.getName() != null)
                                waypointManager.addPrivateWaypoint(UUID.fromString(jsonPlayer.get("uuid").toString()), username != null ? username.toString() : null, waypoint);
                        }
                    }
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        // load inactive waypoints
        try {
            JSONArray jsonInactiveWaypoints = (JSONArray) parser.parse(new FileReader("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "inactive.json"));
            for (JSONObject jsonWaypoint : (Iterable<JSONObject>) jsonInactiveWaypoints) {
                Waypoint waypoint = new Waypoint(jsonWaypoint);
                if (waypoint.getName() != null)
                    waypointManager.addInactiveWaypoint(waypoint);
            }
        } catch(IOException | ParseException e) {
            getLogger().info(e.getMessage());
        }
    }

    public void saveData() {
        // save public waypoints
        JSONArray jsonWaypoints = new JSONArray();
        Collection<Waypoint> allPublicWaypoints = waypointManager.getPinnedWaypointsSortedAlphabetically();
        allPublicWaypoints.addAll(waypointManager.getPublicWaypointsSortedAlphabetically());
        for (Waypoint waypoint : allPublicWaypoints)
            if (waypoint != null) jsonWaypoints.add(waypoint.toJSON());

        try (Writer waypointFile = new OutputStreamWriter(Files.newOutputStream(Paths.get("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "public.json")), StandardCharsets.UTF_8)) {
            waypointFile.write(jsonWaypoints.toJSONString());
        } catch(IOException e) {
            getLogger().info(e.getMessage());
        }

        // save player data
        for (WaypointPlayer waypointPlayer : waypointManager.getWaypointPlayers().values()) {
            JSONObject playerData = waypointPlayer.toJSON();

            try (Writer playerWaypointFile = new OutputStreamWriter(Files.newOutputStream(Paths.get("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "players/" + File.separator + "" + waypointPlayer.getUUID().toString() + ".json")), StandardCharsets.UTF_8)) {
                playerWaypointFile.write(playerData.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // save inactive waypoints
        JSONArray jsonInactiveWaypoints = new JSONArray();
        for (Waypoint waypoint : waypointManager.getInactiveWaypoints().values())
            if (waypoint != null) jsonInactiveWaypoints.add(waypoint.toJSON());

        try (FileWriter inactiveWaypointFile = new FileWriter("plugins/" + File.separator + "BeaconWaypointsReloaded/" + File.separator + "inactive.json")) {
            inactiveWaypointFile.write(jsonInactiveWaypoints.toJSONString());
        } catch(IOException e) {
            getLogger().info(e.getMessage());
        }
    }

    public void loadLanguage() {
        File languageConfigFile = new File(getDataFolder(), "language.yml");
        YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(languageConfigFile);
        Reader configStream = new InputStreamReader(Objects.requireNonNull(getResource("language.yml")), StandardCharsets.UTF_8);
        YamlConfiguration defaultLanguageConfig = YamlConfiguration.loadConfiguration(configStream);
        languageConfig.setDefaults(defaultLanguageConfig);
        languageManager = new LanguageManager(defaultLanguageConfig);

        if (!new File(getDataFolder(), "language.yml").exists()) {
            try {
                defaultLanguageConfig.save(languageConfigFile);
            } catch (IOException e) {
                BeaconWaypointsReloaded.getPlugin().getLogger().severe(languageConfig.getString("cannot-save-default-language-config"));
                throw new RuntimeException(e);
            }
        }
        else
            languageManager = new LanguageManager(languageConfig);
    }

    public static BeaconWaypointsReloaded getPlugin() { return plugin; }
    public static LanguageManager getLanguageManager() { return languageManager; }
    public static WaypointManager getWaypointManager() { return waypointManager; }
    public static MenuManager getMenuManager() { return menuManager; }
}