package org.example.BedWarsLC.Arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<Player, Arena> editingArenas = new HashMap<>();
    private final Map<Player, Arena.TeamData> playerTeams = new HashMap<>(); // Храним TeamData
    private final Map<Player, Arena> playerLobbyArena = new HashMap<>();

    private FileConfiguration arenasConfig;
    private File arenasFile;

    // Конструктор
    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadArenasConfig();
        loadArenas();
    }

    // Загрузка arenas.yml
    private void loadArenasConfig() {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");

        // Создание файла, если его нет
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Загрузка данных из файла
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }

    // Сохранение arenas.yml
    public void saveArenasConfig() {
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить файл arenas.yml!");
            e.printStackTrace();
        }
    }

    public Arena getPlayerArena(Player player) {
        for (Arena arena : arenas.values()) {
            if (playerLobbyArena.containsKey(player) && playerLobbyArena.get(player).equals(arena)) {
                return arena;
            }
        }
        return null; // Если игрок не находится в арене
    }

    public Arena getActiveArena(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.getStatus().equalsIgnoreCase("RUNNING") && playerTeams.containsKey(player)) {
                Arena.TeamData teamData = playerTeams.get(player);
                if (arena.getTeams().containsKey(teamData.getName())) {
                    return arena; // Возвращаем арену, в которой игрок участвует
                }
            }
        }
        return null; // Если игрок не в активной арене
    }


    // Загрузка всех арен
    public void loadArenas() {
        arenas.clear(); // Очистка существующих арен

        if (arenasConfig.contains("arenas")) {
            for (String arenaName : arenasConfig.getConfigurationSection("arenas").getKeys(false)) {
                String mode = arenasConfig.getString("arenas." + arenaName + ".mode");
                int maxPlayers = arenasConfig.getInt("arenas." + arenaName + ".maxPlayers");

                // Загрузка команд
                Map<String, Arena.TeamData> teams = new HashMap<>();
                if (arenasConfig.contains("arenas." + arenaName + ".teams")) {
                    for (String team : arenasConfig.getConfigurationSection("arenas." + arenaName + ".teams").getKeys(false)) {
                        String teamName = arenasConfig.getString("arenas." + arenaName + ".teams." + team + ".name");
                        String teamColor = arenasConfig.getString("arenas." + arenaName + ".teams." + team + ".color");

                        // Создаём или получаем существующую команду
                        Arena.TeamData teamData = teams.getOrDefault(team, new Arena.TeamData(teamName, teamColor));

                        // Загружаем точку спавна
                        if (arenasConfig.contains("arenas." + arenaName + ".teams." + team + ".spawn")) {
                            String world = arenasConfig.getString("arenas." + arenaName + ".teams." + team + ".spawn.world");
                            double x = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".spawn.x");
                            double y = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".spawn.y");
                            double z = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".spawn.z");
                            float yaw = (float) arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".spawn.yaw");
                            float pitch = (float) arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".spawn.pitch");

                            Location spawn = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                            teamData.setSpawnPoint(spawn); // Устанавливаем точку спавна
                        }

                        // === ЗАГРУЗКА КРОВАТИ ===
                        if (arenasConfig.contains("arenas." + arenaName + ".teams." + team + ".bed")) {
                            String world = arenasConfig.getString("arenas." + arenaName + ".teams." + team + ".bed.world");
                            double x = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".bed.x");
                            double y = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".bed.y");
                            double z = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".bed.z");
                            float yaw = (float) arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".bed.yaw"); // Загрузка YAW

                            Location bedLocation = new Location(Bukkit.getWorld(world), x, y, z);
                            teamData.setBedLocation(bedLocation); // Устанавливаем координаты
                            teamData.setBedYaw(yaw); // Устанавливаем угол поворота
                        }

                        // === ЗАГРУЗКА МАЯКА ===
                        if (arenasConfig.contains("arenas." + arenaName + ".teams." + team + ".beacon")) {
                            String world = arenasConfig.getString("arenas." + arenaName + ".teams." + team + ".beacon.world");
                            double x = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".beacon.x");
                            double y = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".beacon.y");
                            double z = arenasConfig.getDouble("arenas." + arenaName + ".teams." + team + ".beacon.z");

                            Location beaconLocation = new Location(Bukkit.getWorld(world), x, y, z);
                            teamData.setBeaconLocation(beaconLocation); // Устанавливаем маяк
                        }

                        // Добавляем команду в список
                        teams.put(team, teamData);
                    }
                }

                // Создание арены
                Arena arena = new Arena(plugin, arenaName, mode, maxPlayers, teams, arenasConfig);

                // Загрузка лобби
                if (arenasConfig.contains("arenas." + arenaName + ".lobby")) {
                    String world = arenasConfig.getString("arenas." + arenaName + ".lobby.world");
                    double x = arenasConfig.getDouble("arenas." + arenaName + ".lobby.x");
                    double y = arenasConfig.getDouble("arenas." + arenaName + ".lobby.y");
                    double z = arenasConfig.getDouble("arenas." + arenaName + ".lobby.z");
                    float yaw = (float) arenasConfig.getDouble("arenas." + arenaName + ".lobby.yaw");
                    float pitch = (float) arenasConfig.getDouble("arenas." + arenaName + ".lobby.pitch");

                    Location lobbyLocation = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                    arena.setLobbyLocation(lobbyLocation);
                }

                // Загрузка региона
                if (arenasConfig.contains("arenas." + arenaName + ".region")) {
                    int minX = arenasConfig.getInt("arenas." + arenaName + ".region.minX");
                    int minY = arenasConfig.getInt("arenas." + arenaName + ".region.minY");
                    int minZ = arenasConfig.getInt("arenas." + arenaName + ".region.minZ");
                    int maxX = arenasConfig.getInt("arenas." + arenaName + ".region.maxX");
                    int maxY = arenasConfig.getInt("arenas." + arenaName + ".region.maxY");
                    int maxZ = arenasConfig.getInt("arenas." + arenaName + ".region.maxZ");

                    arena.setRegion(minX, minY, minZ, maxX, maxY, maxZ);
                }

                if (arenasConfig.contains("arenas." + arenaName + ".status")) {
                    arena.setStatus(arenasConfig.getString("arenas." + arenaName + ".status"));
                } else {
                    arena.setStatus("WAITING"); // Если статус не указан
                }

                // Добавляем арену в память
                arenas.put(arenaName, arena);
            }
        }
    }

    public List<Player> getPlayersInTeamByName(String teamName) {
        List<Player> teamPlayers = new ArrayList<>();
        for (Map.Entry<Player, Arena.TeamData> entry : playerTeams.entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(teamName)) {
                teamPlayers.add(entry.getKey()); // Добавляем объект Player
            }
        }
        return teamPlayers;
    }

    // Сохранение конкретной арены
    public void saveArena(Arena arena) {
        arena.saveToConfig();
        saveArenasConfig(); // Сохраняем в файл после изменений
    }

    // Получить все арены
    public Map<String, Arena> getArenas() {
        return arenas;
    }

    // Исправляем тип возвращаемого значения
    public Map<Player, Arena.TeamData> getPlayerTeams() {
        return playerTeams;
    }

    // Установить арену для игрока, находящегося в лобби
    public void setLobbyArena(Player player, Arena arena) {
        playerLobbyArena.put(player, arena);
    }

    // Получить арену, в лобби которой находится игрок
    public Arena getLobbyArena(Player player) {
        return playerLobbyArena.get(player);
    }

    // Получить список игроков в указанной команде
    public List<String> getPlayersInTeam(String teamName) {
        List<String> players = new ArrayList<>();
        // Исправляем логику цикла
        for (Map.Entry<Player, Arena.TeamData> entry : playerTeams.entrySet()) {
            // Проверяем имя команды в объекте TeamData
            if (entry.getValue().getName().equals(teamName)) {
                players.add(entry.getKey().getName());
            }
        }
        return players;
    }

    // Получить арену по имени
    public Arena getArena(String name) {
        return arenas.get(name);
    }

    // Добавить арену
    public void addArena(Arena arena) {
        arenas.put(arena.getName(), arena);
        saveArena(arena);
    }

    // Удалить арену
    public void removeArena(String name) {
        arenas.remove(name);
        arenasConfig.set("arenas." + name, null);
        saveArenasConfig();
    }

    // Добавление игрока в команду
    public void addPlayerToTeam(Player player, Arena arena, String teamName) {
        if (arena.getTeams().containsKey(teamName)) {
            Arena.TeamData teamData = arena.getTeams().get(teamName);
            playerTeams.put(player, teamData); // Сохраняем объект TeamData
        }
    }

    // Получение данных команды игрока
    public Arena.TeamData getPlayerTeamData(Player player) {
        return playerTeams.get(player); // Возвращаем объект TeamData
    }

    // Удаление игрока из команды
    public void removePlayerFromTeam(Player player) {
        playerTeams.remove(player);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    // Получить максимальное количество игроков в команде
    public int getMaxPlayersPerTeam(Arena arena) {
        switch (arena.getMode().toUpperCase()) {
            case "SOLO":
                return 1;
            case "DUO":
                return 2;
            case "SQUAD":
                return 4;
            default:
                return 1;
        }
    }

    // Установка редактируемой арены
    public void setEditingArena(Player player, Arena arena) {
        editingArenas.put(player, arena);
    }

    // Получение редактируемой арены
    public Arena getEditingArena(Player player) {
        return editingArenas.get(player);
    }

    // Очистка редактируемой арены
    public void clearEditingArena(Player player) {
        editingArenas.remove(player);
    }

    // Удалить арену, если игрок покидает лобби
    public void removeLobbyArena(Player player) {
        playerLobbyArena.remove(player);
    }
}
