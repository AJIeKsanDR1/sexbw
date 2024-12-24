package org.example.BedWarsLC.Arena;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.BedWarsLC.BedWarsLC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arena {

    private final JavaPlugin plugin;   // Ссылка на плагин
    private final String name;         // Название арены
    private final String mode;         // Режим игры
    private final int maxPlayers;      // Максимальное количество игроков
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private String status = "DISABLED"; // По умолчанию отключена
    private final FileConfiguration arenasConfig; // arenas.yml

    private final Map<String, TeamData> teams; // Команды
    private Location lobbyLocation;

    // ======= Вложенный класс TeamData =======
    public static class TeamData {
        private String name;        // Название команды
        private String color;       // Цвет команды
        private Location spawnPoint; // Точка спавна
        // Новые поля для кровати и маяка
        private Location bedLocation;
        private Location beaconLocation;
        private float bedYaw; // Добавляем угол поворота для кровати
        private boolean bedDestroyed = false; // По умолчанию кровать цела

        public TeamData(String name, String color) {
            this.name = name;
            this.color = color;
            this.spawnPoint = null; // По умолчанию нет спавна
        }

        // ====== Геттеры и сеттеры ======
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }
        public void setColor(String color) {
            this.color = color;
        }

        public Location getSpawnPoint() {
            return spawnPoint;
        }
        public void setSpawnPoint(Location spawnPoint) {
            this.spawnPoint = spawnPoint;
        }

        public float getBedYaw() {
            return bedYaw;
        }

        public void setBedYaw(float bedYaw) {
            this.bedYaw = bedYaw;
        }


        public Location getBedLocation() {
            return bedLocation;
        }
        public void setBedLocation(Location loc) {
            this.bedLocation = loc;
        }

        public Location getBeaconLocation() {
            return beaconLocation;
        }
        public void setBeaconLocation(Location loc) {
            this.beaconLocation = loc;
        }

        public boolean isBedDestroyed() {
            return bedDestroyed;
        }

        public void setBedDestroyed(boolean bedDestroyed) {
            this.bedDestroyed = bedDestroyed;
        }
    }
    // ======= Конец вложенного класса TeamData =======
    // ======= Конструкторы =======
    public Arena(JavaPlugin plugin, String name, String mode, int maxPlayers) {
        this.plugin = plugin;
        this.name = name;
        this.mode = mode;
        this.maxPlayers = maxPlayers;

        // Загружаем arenasConfig
        this.arenasConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "arenas.yml")
        );

        this.teams = new HashMap<>();

        // Автоматически создаём команды по умолчанию (например, Team 1, Team 2...)
        int teamCount = maxPlayers / getPlayersPerTeam(mode);
        for (int i = 1; i <= teamCount; i++) {
            String teamKey = "Team " + i;
            teams.put(teamKey, new TeamData(teamKey, "WHITE"));
        }
    }

    // Конструктор для загрузки из конфига
    public Arena(JavaPlugin plugin, String name, String mode,
                 int maxPlayers, Map<String, TeamData> teams,
                 FileConfiguration arenasConfig) {
        this.plugin = plugin;
        this.name = name;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.arenasConfig = arenasConfig;
        this.teams = new HashMap<>(teams);
    }

    // ======= Регион =======
    public void setRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }

    // ======= Статус =======
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;

        // Обновляем статус в конфиге
        arenasConfig.set("arenas." + name + ".status", status);
        try {
            arenasConfig.save(new File(plugin.getDataFolder(), "arenas.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<Player, Arena.TeamData> entry : BedWarsLC.getInstance().getArenaManager().getPlayerTeams().entrySet()) {
            if (BedWarsLC.getInstance().getArenaManager().getLobbyArena(entry.getKey()) == this) {
                players.add(entry.getKey());
            }
        }
        return players;
    }

    // ======= Геттеры/сеттеры прочие =======
    public String getName() { return name; }
    public String getMode() { return mode; }
    public int getMaxPlayers() { return maxPlayers; }
    public Map<String, TeamData> getTeams() { return teams; }

    public void setTeamName(String team, String newName) {
        if (teams.containsKey(team)) {
            teams.get(team).setName(newName);
            saveToConfig();
        }
    }
    public void setTeamColor(String team, String color) {
        if (teams.containsKey(team)) {
            teams.get(team).setColor(color);
            saveToConfig();
        }
    }

    // Лобби
    public Location getLobbyLocation() {
        return lobbyLocation;
    }
    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
    }

    // ======= Сохранение в конфиг =======
    public void saveToConfig() {
        String path = "arenas." + name;

        // Сохранение региона
        arenasConfig.set(path + ".region.minX", minX);
        arenasConfig.set(path + ".region.minY", minY);
        arenasConfig.set(path + ".region.minZ", minZ);
        arenasConfig.set(path + ".region.maxX", maxX);
        arenasConfig.set(path + ".region.maxY", maxY);
        arenasConfig.set(path + ".region.maxZ", maxZ);

        // Сохранение режима и игроков
        arenasConfig.set(path + ".mode", mode);
        arenasConfig.set(path + ".maxPlayers", maxPlayers);

        // Сохранение лобби
        saveLobbyToConfig(arenasConfig, path);

        // Сохранение команд
        arenasConfig.set(path + ".teams", null); // Сначала очищаем
        for (Map.Entry<String, TeamData> entry : teams.entrySet()) {
            String teamName = entry.getKey();                // Например, "Team 1"
            TeamData teamData = entry.getValue();
            String teamPath = path + ".teams." + teamName;   // arenas.ArenaName.teams.Team 1

            // Общие поля (name, color)
            arenasConfig.set(teamPath + ".name", teamData.getName());
            arenasConfig.set(teamPath + ".color", teamData.getColor());

            // Спаун
            Location spawn = teamData.getSpawnPoint();
            if (spawn != null) {
                arenasConfig.set(teamPath + ".spawn.world", spawn.getWorld().getName());
                arenasConfig.set(teamPath + ".spawn.x", spawn.getX());
                arenasConfig.set(teamPath + ".spawn.y", spawn.getY());
                arenasConfig.set(teamPath + ".spawn.z", spawn.getZ());
                arenasConfig.set(teamPath + ".spawn.yaw", spawn.getYaw());
                arenasConfig.set(teamPath + ".spawn.pitch", spawn.getPitch());
            }

            // Кровать
            Location bed = teamData.getBedLocation();
            if (bed != null) {
                arenasConfig.set(teamPath + ".bed.world", bed.getWorld().getName());
                arenasConfig.set(teamPath + ".bed.x", bed.getX());
                arenasConfig.set(teamPath + ".bed.y", bed.getY());
                arenasConfig.set(teamPath + ".bed.z", bed.getZ());
                arenasConfig.set(teamPath + ".bed.yaw", teamData.getBedYaw()); // Сохраняем угол поворота
            }

            // Маяк
            Location beacon = teamData.getBeaconLocation();
            if (beacon != null) {
                arenasConfig.set(teamPath + ".beacon.world", beacon.getWorld().getName());
                arenasConfig.set(teamPath + ".beacon.x", beacon.getX());
                arenasConfig.set(teamPath + ".beacon.y", beacon.getY());
                arenasConfig.set(teamPath + ".beacon.z", beacon.getZ());
            }
        }

        // Сохраняем arenas.yml
        try {
            arenasConfig.save(new File(plugin.getDataFolder(), "arenas.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Доп. метод сохранения лобби
    private void saveLobbyToConfig(FileConfiguration config, String path) {
        if (lobbyLocation != null) {
            config.set(path + ".lobby.world", lobbyLocation.getWorld().getName());
            config.set(path + ".lobby.x", lobbyLocation.getX());
            config.set(path + ".lobby.y", lobbyLocation.getY());
            config.set(path + ".lobby.z", lobbyLocation.getZ());
            config.set(path + ".lobby.yaw", lobbyLocation.getYaw());
            config.set(path + ".lobby.pitch", lobbyLocation.getPitch());
        }
    }

    // Определяем кол-во игроков в команде из режима
    private int getPlayersPerTeam(String mode) {
        switch (mode.toUpperCase()) {
            case "SOLO":  return 1;
            case "DUO":   return 2;
            case "SQUAD": return 4;
            default:      return 1; // По умолчанию SOLO
        }
    }
}
