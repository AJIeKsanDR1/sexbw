package org.example.BedWarsLC.Game;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Listener.RegionProtectionListener;
import org.example.BedWarsLC.Lobby.LobbyManager;
import org.example.BedWarsLC.Utils.TabManager;

import java.util.*;

public class GameManager {

    private final ArenaManager arenaManager;
    private final JavaPlugin plugin;

    private final int gameTimer;
    private int timeLeft;

    private final List<Player> activePlayers = new ArrayList<>();
    private final Set<Location> preGameBlocks = new HashSet<>();
    private final Set<Location> spawnProtectedBlocks = new HashSet<>();
    private final Map<Location, String> teamBeds = new HashMap<>();
    private final Map<Location, String> teamBeacons = new HashMap<>();
    private final Map<String, ChatColor> colorMap = new HashMap<>();

    private final GameScoreboard gameScoreboard;

    public GameManager(ArenaManager arenaManager, JavaPlugin plugin, int gameTimer) {
        this.arenaManager = arenaManager;
        this.plugin = plugin;
        this.gameTimer = gameTimer;
        this.gameScoreboard = new GameScoreboard(plugin, gameTimer);

        initializeColorMap();
    }

    public boolean startGame(Arena arena) {
        if (arena == null || "RUNNING".equalsIgnoreCase(arena.getStatus())) {
            return false;
        }

        Bukkit.getLogger().info("startGame > Запуск игры на арене: " + arena.getName());

        arena.setStatus("RUNNING");
        activePlayers.clear();
        timeLeft = gameTimer;

        for (Map.Entry<Player, Arena.TeamData> entry : arenaManager.getPlayerTeams().entrySet()) {
            Player player = entry.getKey();
            Arena.TeamData teamData = entry.getValue();

            if (arenaManager.getLobbyArena(player) != arena) {
                continue; // Игрок не в этой арене
            }

            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            player.getActivePotionEffects().clear();
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

            if (teamData != null && teamData.getSpawnPoint() != null) {
                Location spawn = teamData.getSpawnPoint();
                player.teleport(spawn);
                player.sendMessage(ChatColor.GREEN + "Игра началась! Вас телепортировали на спавн команды.");
                activePlayers.add(player);

                Bukkit.getLogger().info("startGame > " + player.getName()
                        + " телепортирован на спавн команды " + teamData.getName());

                // === ДОБАВЛЕНО: СОЗДАНИЕ СКОРБОРДА И BOSSBAR ===
                gameScoreboard.createScoreboard(
                        player,
                        arena.getName(),
                        arena.getTeams(),
                        getPlayerCountsByTeam(arena),
                        activePlayers.size(),
                        arena.getMaxPlayers()
                );

            } else {
                player.sendMessage(ChatColor.RED + "Не задана точка спавна для вашей команды!");
            }
        }

        scanPreGameBlocks(arena);

        for (Map.Entry<String, Arena.TeamData> entry : arena.getTeams().entrySet()) {
            String teamKey = entry.getKey();
            Arena.TeamData teamData = entry.getValue();

            if (teamData.getBedLocation() != null) {
                Location bedHeadLocation = teamData.getBedLocation().clone();
                bedHeadLocation.setX(Math.floor(bedHeadLocation.getX()));
                bedHeadLocation.setY(Math.floor(bedHeadLocation.getY()));
                bedHeadLocation.setZ(Math.floor(bedHeadLocation.getZ()));

                BlockFace direction = getDirectionFromYaw(teamData.getBedYaw());
                Location bedFootLocation = getFootLocation(bedHeadLocation.clone(), direction);

                Material bedMaterial = getBedMaterialByColor(teamData.getColor());

                bedHeadLocation.getBlock().setType(bedMaterial);
                org.bukkit.block.data.type.Bed headData = (org.bukkit.block.data.type.Bed) bedHeadLocation.getBlock().getBlockData();
                headData.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
                headData.setFacing(direction);
                bedHeadLocation.getBlock().setBlockData(headData);

                bedFootLocation.getBlock().setType(bedMaterial);
                org.bukkit.block.data.type.Bed footData = (org.bukkit.block.data.type.Bed) bedFootLocation.getBlock().getBlockData();
                footData.setPart(org.bukkit.block.data.type.Bed.Part.FOOT);
                footData.setFacing(direction);
                bedFootLocation.getBlock().setBlockData(footData);

                teamBeds.put(bedHeadLocation, teamKey);
                teamBeds.put(bedFootLocation, teamKey);

                Bukkit.getLogger().info("startGame > Кровать команды " + teamKey + " добавлена в teamBeds: "
                        + "Head=" + bedHeadLocation + ", Foot=" + bedFootLocation);
            }

            if (teamData.getBeaconLocation() != null) {
                Location beaconLocation = teamData.getBeaconLocation();
                beaconLocation.getBlock().setType(Material.BEACON);

                DyeColor teamColor = DyeColor.valueOf(teamData.getColor());
                Material glassMaterial = Material.valueOf(teamColor.name() + "_STAINED_GLASS");
                beaconLocation.clone().add(0, 1, 0).getBlock().setType(glassMaterial);

                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        beaconLocation.clone().add(x, -1, z).getBlock().setType(Material.IRON_BLOCK);
                    }
                }

                teamBeacons.put(beaconLocation, teamKey);
                Bukkit.getLogger().info("startGame > Спавним маяк команды " + teamKey + " по адресу: " + beaconLocation);
            }
        }

        buildBarrier(arena);
        startGameTimer(arena);

        plugin.getServer().getPluginManager().registerEvents(
                new RegionProtectionListener(arenaManager, this, getPreGameBlocks(), teamBeds),
                plugin
        );

        Bukkit.getLogger().info("startGame > Игра на арене " + arena.getName() + " успешно запущена!");
        return true;
    }

    public boolean handleBedBreak(Location blockLoc, Player player, Arena arena) {
        // Округляем координаты блока
        blockLoc.setX(Math.floor(blockLoc.getX()));
        blockLoc.setY(Math.floor(blockLoc.getY()));
        blockLoc.setZ(Math.floor(blockLoc.getZ()));

        // Проверяем, есть ли кровать в карте teamBeds
        if (!teamBeds.containsKey(blockLoc)) {
            Bukkit.getLogger().info("handleBedBreak > Кровать не найдена в teamBeds: " + blockLoc);
            return false;
        }

        // Получаем команду, чья это кровать
        String brokenTeamName = teamBeds.get(blockLoc);
        Arena.TeamData brokenTeamData = arena.getTeams().get(brokenTeamName);

        if (brokenTeamData == null) {
            return false;
        }

        // Проверяем команду игрока
        Arena.TeamData playerTeam = arenaManager.getPlayerTeamData(player);
        if (playerTeam != null && playerTeam.getName().equalsIgnoreCase(brokenTeamName)) {
            player.sendMessage(ChatColor.RED + "Вы не можете сломать свою кровать!");
            return true;
        }

        // Устанавливаем флаг разрушенной кровати
        brokenTeamData.setBedDestroyed(true);
        teamBeds.remove(blockLoc);

        // Удаляем ножную часть кровати
        Location footLoc = getFootLocation(blockLoc.clone(), getDirectionFromYaw(brokenTeamData.getBedYaw()));
        footLoc.setX(Math.floor(footLoc.getX()));
        footLoc.setY(Math.floor(footLoc.getY()));
        footLoc.setZ(Math.floor(footLoc.getZ()));

        teamBeds.remove(footLoc);
        blockLoc.getBlock().setType(Material.AIR);
        footLoc.getBlock().setType(Material.AIR);

        // Гасим маяк (убираем стекло и центральный блок железа)
        Location beaconLocation = brokenTeamData.getBeaconLocation();
        if (beaconLocation != null) {
            beaconLocation.clone().add(0, 1, 0).getBlock().setType(Material.AIR); // Стекло
            beaconLocation.clone().add(0, -1, 0).getBlock().setType(Material.AIR); // Центральный блок железа
        }

        // Получаем цвет команды
        ChatColor teamColor = colorMap.getOrDefault(brokenTeamData.getColor().toUpperCase(), ChatColor.WHITE);

        // Сообщение в чат с окрашенным названием команды
        Bukkit.broadcastMessage(ChatColor.RED + player.getName()
                + " разрушил кровать команды " + teamColor + brokenTeamData.getName() + ChatColor.RED + "!");

        // Звук для всех
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        List<Player> teamPlayers = arenaManager.getPlayersInTeamByName(brokenTeamName);
        for (Player teamPlayer : teamPlayers) {
            teamPlayer.playSound(teamPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        }

        checkForVictory(arena);

        return true;
    }

    // ======= ПРОВЕРКА ПОБЕДЫ =======
    private void checkForVictory(Arena arena) {
        Set<String> aliveTeams = new HashSet<>();

        // Перебираем всех активных игроков
        for (Player player : activePlayers) {
            Arena.TeamData teamData = arenaManager.getPlayerTeamData(player);

            if (teamData != null && (!teamData.isBedDestroyed() || hasAlivePlayers(teamData))) {
                aliveTeams.add(teamData.getName());
            }
        }

        // Если осталась только одна команда
        if (aliveTeams.size() == 1) {
            String winningTeam = aliveTeams.iterator().next();
            ChatColor teamColor = colorMap.getOrDefault(winningTeam.toUpperCase(), ChatColor.WHITE);

            Bukkit.broadcastMessage(ChatColor.GOLD + "Победа команды " + teamColor + winningTeam + ChatColor.GOLD + "!");

            for (Player player : activePlayers) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            // Останавливаем игру через 10 секунд
            new BukkitRunnable() {
                @Override
                public void run() {
                    stopGame(arena);
                }
            }.runTaskLater(plugin, 200L);
        }
    }

    private boolean hasAlivePlayers(Arena.TeamData teamData) {
        for (Player player : activePlayers) {
            Arena.TeamData playerTeam = arenaManager.getPlayerTeamData(player);
            if (playerTeam != null && playerTeam.getName().equalsIgnoreCase(teamData.getName())) {
                return true;
            }
        }
        return false;
    }

    // ======= ВОЗВРАТ ИГРОКОВ В ЛОББИ =======
    private void returnPlayersToLobby(Arena arena) {
        LobbyManager lobbyManager = new LobbyManager(arenaManager, this, plugin.getConfig());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(player) == arena || activePlayers.contains(player)) {
                Location lobbyLocation = arena.getLobbyLocation();
                if (lobbyLocation != null) {
                    player.teleport(lobbyLocation);
                    player.sendMessage(ChatColor.GREEN + "Вы были возвращены в лобби арены '" + arena.getName() + "'!");
                } else {
                    player.sendMessage(ChatColor.RED + "Лобби арены не установлено!");
                }

                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(5.0f);
                player.getActivePotionEffects().clear();

                TabManager tabManager = new TabManager(arenaManager);
                tabManager.updateTabList(player);

                lobbyManager.giveLobbyItems(player, arena);
                lobbyManager.setupScoreboard(player, arena);
            }
        }
        activePlayers.clear();
    }

    private Map<String, Integer> getPlayerCountsByTeam(Arena arena) {
        Map<String, Integer> countMap = new HashMap<>();
        for (Map.Entry<Player, Arena.TeamData> entry : arenaManager.getPlayerTeams().entrySet()) {
            String teamName = entry.getValue().getName();
            countMap.put(teamName, countMap.getOrDefault(teamName, 0) + 1);
        }
        return countMap;
    }

    private void initializeColorMap() {
        colorMap.put("WHITE", ChatColor.WHITE);
        colorMap.put("ORANGE", ChatColor.GOLD);
        colorMap.put("MAGENTA", ChatColor.LIGHT_PURPLE);
        colorMap.put("LIGHT_BLUE", ChatColor.AQUA);
        colorMap.put("YELLOW", ChatColor.YELLOW);
        colorMap.put("LIME", ChatColor.GREEN);
        colorMap.put("PINK", ChatColor.LIGHT_PURPLE);
        colorMap.put("GRAY", ChatColor.DARK_GRAY);
        colorMap.put("SILVER", ChatColor.GRAY);
        colorMap.put("CYAN", ChatColor.DARK_AQUA);
        colorMap.put("PURPLE", ChatColor.DARK_PURPLE);
        colorMap.put("BLUE", ChatColor.BLUE);
        colorMap.put("BROWN", ChatColor.GOLD);
        colorMap.put("GREEN", ChatColor.DARK_GREEN);
        colorMap.put("RED", ChatColor.RED);
        colorMap.put("BLACK", ChatColor.BLACK);
    }

    private void scanPreGameBlocks(Arena arena) {
        preGameBlocks.clear();

        int minX = arena.getMinX();
        int maxX = arena.getMaxX();
        int minY = arena.getMinY();
        int maxY = arena.getMaxY();
        int minZ = arena.getMinZ();
        int maxZ = arena.getMaxZ();

        Location lobbyLoc = arena.getLobbyLocation();
        World world = lobbyLoc.getWorld();
        if (world == null) {
            Bukkit.getLogger().info("scanPreGameBlocks > Мир не найден у арены " + arena.getName());
            return;
        }

        Bukkit.getLogger().info("scanPreGameBlocks > Начинаем сканирование региона арены: " + arena.getName());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);

                    // Пропускаем кровать
                    if (isBedLocation(loc)) {
                        continue;
                    }

                    // Пропускаем маяк
                    if (isBeaconLocation(loc)) {
                        preGameBlocks.add(loc);
                        continue;
                    }

                    // Остальные блоки добавляем в preGameBlocks
                    preGameBlocks.add(loc);
                }
            }
        }
        Bukkit.getLogger().info("scanPreGameBlocks > Добавлено блоков: " + preGameBlocks.size());
    }

    private boolean isBeaconLocation(Location loc) {
        if (teamBeacons.containsKey(loc)) {
            return true;
        }
        for (Location beaconLocation : teamBeacons.keySet()) {
            if (loc.equals(beaconLocation.clone().add(0, 1, 0))) {
                return true;
            }
            if (isBeaconBase(loc, beaconLocation)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBedLocation(Location loc) {
        if (teamBeds.containsKey(loc)) {
            return true;
        }
        for (Location headLocation : teamBeds.keySet()) {
            Arena arena = arenaManager.getArena(teamBeds.get(headLocation));
            if (arena == null) continue;

            Arena.TeamData td = arena.getTeams().get(teamBeds.get(headLocation));
            if (td == null) continue;

            Location footLocation = getFootLocation(headLocation.clone(), getDirectionFromYaw(td.getBedYaw()));
            if (footLocation.equals(loc)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBeaconBase(Location loc, Location beaconLocation) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location base = beaconLocation.clone().add(x, -1, z);
                if (base.equals(loc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void buildBarrier(Arena arena) {
        int minX = arena.getMinX();
        int maxX = arena.getMaxX();
        int minY = arena.getMinY();
        int maxY = arena.getMaxY();
        int minZ = arena.getMinZ();
        int maxZ = arena.getMaxZ();

        Location lobbyLoc = arena.getLobbyLocation();
        World world = lobbyLoc.getWorld();
        if (world == null) return;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                world.getBlockAt(x, y, minZ).setType(Material.BARRIER);
                world.getBlockAt(x, y, maxZ).setType(Material.BARRIER);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                world.getBlockAt(minX, y, z).setType(Material.BARRIER);
                world.getBlockAt(maxX, y, z).setType(Material.BARRIER);
            }
        }

        Bukkit.getLogger().info("buildBarrier > Установлен барьер по периметру арены: " + arena.getName());
    }

    private void removeBarrier(Arena arena) {
        int minX = arena.getMinX();
        int maxX = arena.getMaxX();
        int minY = arena.getMinY();
        int maxY = arena.getMaxY();
        int minZ = arena.getMinZ();
        int maxZ = arena.getMaxZ();

        Location lobbyLoc = arena.getLobbyLocation();
        World world = lobbyLoc.getWorld();
        if (world == null) return;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                world.getBlockAt(x, y, minZ).setType(Material.AIR);
                world.getBlockAt(x, y, maxZ).setType(Material.AIR);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                world.getBlockAt(minX, y, z).setType(Material.AIR);
                world.getBlockAt(maxX, y, z).setType(Material.AIR);
            }
        }

        Bukkit.getLogger().info("removeBarrier > Барьеры на арене " + arena.getName() + " удалены.");
    }

    private Material getBedMaterialByColor(String color) {
        switch (color.toUpperCase()) {
            case "WHITE": return Material.WHITE_BED;
            case "ORANGE": return Material.ORANGE_BED;
            case "MAGENTA": return Material.MAGENTA_BED;
            case "LIGHT_BLUE": return Material.LIGHT_BLUE_BED;
            case "YELLOW": return Material.YELLOW_BED;
            case "LIME": return Material.LIME_BED;
            case "PINK": return Material.PINK_BED;
            case "GRAY": return Material.GRAY_BED;
            case "LIGHT_GRAY": return Material.LIGHT_GRAY_BED;
            case "CYAN": return Material.CYAN_BED;
            case "PURPLE": return Material.PURPLE_BED;
            case "BLUE": return Material.BLUE_BED;
            case "BROWN": return Material.BROWN_BED;
            case "GREEN": return Material.GREEN_BED;
            case "RED": return Material.RED_BED;
            case "BLACK": return Material.BLACK_BED;
            default:
                Bukkit.getLogger().info("getBedMaterialByColor > Неизвестный цвет: " + color + ". Возвращаем WHITE_BED.");
                return Material.WHITE_BED;
        }
    }

    private void startGameTimer(Arena arena) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    stopGame(arena);
                    cancel();
                    return;
                }
                gameScoreboard.updateTime(timeLeft);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public boolean stopGame(Arena arena) {
        if (arena == null || !"RUNNING".equalsIgnoreCase(arena.getStatus())) {
            return false;
        }

        Bukkit.getLogger().info("stopGame > Останавливаем игру на арене: " + arena.getName());

        arena.setStatus("WAITING");
        activePlayers.clear();
        gameScoreboard.resetTimer(); // Сброс BossBar и Scoreboard

        for (Player player : Bukkit.getOnlinePlayers()) {
            gameScoreboard.removeBossBar(player);
        }

        // Удаляем кровати
        for (Location bedLocation : teamBeds.keySet()) {
            if (bedLocation.getBlock().getType().name().endsWith("_BED")) {
                bedLocation.getBlock().setType(Material.AIR);
            }
        }
        teamBeds.clear();

        // Удаляем маяки
        for (Location beaconLocation : teamBeacons.keySet()) {
            beaconLocation.getBlock().setType(Material.AIR);

            Location glassLocation = beaconLocation.clone().add(0, 1, 0);
            if (glassLocation.getBlock().getType().name().endsWith("_GLASS")) {
                glassLocation.getBlock().setType(Material.AIR);
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location base = beaconLocation.clone().add(x, -1, z);
                    if (base.getBlock().getType() == Material.IRON_BLOCK) {
                        base.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        teamBeacons.clear();

        preGameBlocks.clear();
        spawnProtectedBlocks.clear();

        removeBarrier(arena);
        returnPlayersToLobby(arena);

        Bukkit.getLogger().info("stopGame > Игра на арене " + arena.getName() + " остановлена.");
        return true;
    }

    // Геттеры/прочие методы:
    public Set<Location> getPreGameBlocks() {
        return preGameBlocks;
    }

    public Set<Location> getSpawnProtectedBlocks() {
        return spawnProtectedBlocks;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Location getFootLocation(Location headLocation, BlockFace direction) {
        switch (direction) {
            case NORTH:
                return headLocation.add(0, 0, 1);
            case SOUTH:
                return headLocation.add(0, 0, -1);
            case EAST:
                return headLocation.add(-1, 0, 0);
            case WEST:
                return headLocation.add(1, 0, 0);
            default:
                return headLocation;
        }
    }

    public void handlePlayerDeath(Player player, Arena arena) {
        Arena.TeamData teamData = arenaManager.getPlayerTeamData(player);
        if (teamData != null && !teamData.isBedDestroyed()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.YELLOW + "Вы возродитесь через 5 секунд!");

            Location spawnPoint = teamData.getSpawnPoint();
            if (spawnPoint == null) {
                Bukkit.getLogger().warning("handlePlayerDeath > Ошибка: Точка возрождения для команды " +
                        teamData.getName() + " не задана! Телепортация отменена.");
                player.sendMessage(ChatColor.RED + "Ошибка: Точка возрождения вашей команды не задана!");
                return;
            }

            new BukkitRunnable() {
                int countdown = 5;

                @Override
                public void run() {
                    if (countdown > 0) {
                        player.sendTitle(ChatColor.RED + "Возрождение через",
                                ChatColor.YELLOW + String.valueOf(countdown) + " сек.", 0, 20, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                        countdown--;
                    } else {
                        if (!activePlayers.contains(player)) {
                            Bukkit.getLogger().info("handlePlayerDeath > Игрок " + player.getName() + " уже не в игре.");
                            cancel();
                            return;
                        }

                        player.spigot().respawn();
                        player.teleport(spawnPoint);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.setHealth(20.0);
                        player.setFoodLevel(20);
                        player.setSaturation(5.0f);
                        player.sendMessage(ChatColor.GREEN + "Вы возродились, потому что ваша кровать цела!");

                        setTemporaryInvulnerability(player, 5);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        } else {
            // Кровать разрушена
            activePlayers.remove(player);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "Вы были устранены, так как ваша кровать разрушена!");

            checkForVictory(arena);
        }
    }

    private void setTemporaryInvulnerability(Player player, int seconds) {
        player.setInvulnerable(true);
        player.sendMessage(ChatColor.GOLD + "Вы неуязвимы в течение " + seconds + " секунд!");

        new BukkitRunnable() {
            int countdown = seconds;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendTitle("", ChatColor.YELLOW + "Неуязвимость: " + countdown + " сек.", 0, 20, 0);
                    countdown--;
                } else {
                    player.setInvulnerable(false);
                    player.sendMessage(ChatColor.RED + "Вы больше не неуязвимы!");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public BlockFace getDirectionFromYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135) {
            return BlockFace.EAST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.SOUTH;
        } else if (yaw >= 225 && yaw < 315) {
            return BlockFace.WEST;
        } else {
            return BlockFace.NORTH;
        }
    }
}
