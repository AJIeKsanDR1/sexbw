package org.example.BedWarsLC.Lobby;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.BedWarsLC;
import org.example.BedWarsLC.Game.GameManager;
import org.example.BedWarsLC.Utils.TabManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LobbyManager {

    private final ArenaManager arenaManager;
    private final FileConfiguration config;
    private final GameManager gameManager;

    private final String prefix;         // Префикс из конфига
    private final int startCountdown;    // Таймер из конфига
    private final String waitingMessage; // Сообщение ожидания из конфига

    private int countdown;               // Текущий отсчёт времени
    private boolean countdownRunning = false; // Флаг, запущен ли таймер
    private int minPlayers;              // Минимальное количество игроков для старта

    // Задача для обратного отсчёта
    private BukkitRunnable countdownTask;

    // Дополнительное поле для хранения арены,
    // на которой сейчас идёт отсчёт
    private Arena countdownArena;

    // Конструктор
    public LobbyManager(ArenaManager arenaManager, GameManager gameManager, FileConfiguration config) {
        this.arenaManager = arenaManager;
        this.config = config;
        this.gameManager = gameManager;

        // Загружаем параметры из конфига
        this.prefix = ChatColor.translateAlternateColorCodes('&',
                config.getString("LobbySettings.Prefix", "&6BedWarsLC")); // По умолчанию
        this.startCountdown = config.getInt("LobbySettings.StartTimer", 30); // По умолчанию 30 сек
        this.waitingMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("LobbySettings.WaitingMessage", "Ожидание игроков")); // По умолчанию сообщение
        this.minPlayers = config.getInt("LobbySettings.MinimumPlayers", 2); // По умолчанию 2 игрока

        this.countdown = startCountdown; // Инициализируем таймер
    }

    // ======= ПЕРЕЗАГРУЗКА КОНФИГА =======
    public void reloadConfig() {
        config.options().copyDefaults(true);
        // Перечитываем значения при необходимости
        this.minPlayers = config.getInt("LobbySettings.MinimumPlayers", 2);
    }

    // ======= ВЫДАЧА ПРЕДМЕТОВ =======
    public void giveLobbyItems(Player player, Arena arena) {
        player.getInventory().clear();

        // Выбор команды
        ItemStack teamSelector = new ItemStack(Material.WHITE_BED);
        ItemMeta teamMeta = teamSelector.getItemMeta();
        teamMeta.setDisplayName(ChatColor.AQUA + "Выбор команды");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Выберите свою команду:");
        for (Map.Entry<String, Arena.TeamData> entry : arena.getTeams().entrySet()) {
            Arena.TeamData team = entry.getValue();
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "§7- " + team.getName() + " §8(Цвет: " + team.getColor() + ")"));
        }
        teamMeta.setLore(lore);
        teamSelector.setItemMeta(teamMeta);

        // Возврат в главное лобби
        ItemStack leaveLobby = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta leaveMeta = leaveLobby.getItemMeta();
        leaveMeta.setDisplayName(ChatColor.RED + "Вернуться в главное лобби");
        leaveLobby.setItemMeta(leaveMeta);

        player.getInventory().setItem(0, teamSelector);
        player.getInventory().setItem(8, leaveLobby);
    }

    // ======= ТЕЛЕПОРТАЦИЯ В ГЛАВНОЕ ЛОББИ =======
    public void teleportToMainLobby(Player player) {
        if (config.contains("MainLobby")) {
            String world = config.getString("MainLobby.world");
            double x = config.getDouble("MainLobby.x");
            double y = config.getDouble("MainLobby.y");
            double z = config.getDouble("MainLobby.z");
            float yaw = (float) config.getDouble("MainLobby.yaw");
            float pitch = (float) config.getDouble("MainLobby.pitch");

            if (Bukkit.getWorld(world) != null) {
                Location location = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                player.teleport(location);

                // Возвращаем параметры игрока
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(5.0f);
                player.getActivePotionEffects().clear();

                // Сбрасываем скорборд
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

                // Обновляем таб-лист для главного лобби
                TabManager tabManager = new TabManager(arenaManager);
                tabManager.updateTabList(player);

                player.sendMessage(ChatColor.GREEN + "Вы были телепортированы в главное лобби!");
            } else {
                player.sendMessage(ChatColor.RED + "Мир для главного лобби не найден!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Главное лобби ещё не установлено!");
        }
    }

    // ======= ПОДКЛЮЧЕНИЕ К ЛОББИ АРЕНЫ =======
    public void joinLobby(Player player, Arena arena) {
        Location lobbyLocation = arena.getLobbyLocation();
        if (lobbyLocation == null) {
            player.sendMessage(ChatColor.RED + "Лобби для арены '" + arena.getName() + "' не установлено!");
            return;
        }

        // Телепортируем игрока в лобби
        player.teleport(lobbyLocation);
        player.sendMessage(ChatColor.GREEN + "Вы были телепортированы в лобби арены '" + arena.getName() + "'!");

        // Сбрасываем состояние игрока
        arenaManager.setLobbyArena(player, arena);
        giveLobbyItems(player, arena);

        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.getActivePotionEffects().clear();

        // Обновляем таб-лист
        TabManager tabManager = new TabManager(arenaManager);
        tabManager.updateTabList(player);

        // Создаём скорборд
        setupScoreboard(player, arena);

        // После присоединения игрока – проверяем, не пора ли запустить отсчёт
        checkAndStartCountdown(arena);
    }

    // ======= СОЗДАНИЕ СКОРБОРДА =======
    public void setupScoreboard(Player player, Arena arena) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("lobby", "dummy");
        objective.setDisplayName(ChatColor.GOLD + "~~~ BedWarsLC ~~~");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Добавляем строки
        objective.getScore(ChatColor.YELLOW + "> Карта:").setScore(10);
        objective.getScore(ChatColor.GREEN + arena.getName()).setScore(9);

        objective.getScore(ChatColor.YELLOW + "> Игроков:").setScore(8);
        int players = getTotalPlayersInLobby(arena);
        String playerCountText = ChatColor.GREEN + String.valueOf(players)
                + "/"
                + String.valueOf(arena.getMaxPlayers());
        objective.getScore(playerCountText).setScore(7);
        objective.getScore(playerCountText).setScore(7);

        // Статус игры (по умолчанию ждём игроков)
        objective.getScore(ChatColor.GREEN + waitingMessage).setScore(5);

        objective.getScore("").setScore(4); // Пустая строка

        player.setScoreboard(scoreboard);
    }

    private void updateStatusForAll(Arena arena, String status) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(player) == arena) {
                updateStatus(player, status);
            }
        }
    }

    public void updateStatus(Player player, String status) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("lobby");
        if (objective == null) return;

        // Сбрасываем старую строку статуса
        scoreboard.resetScores(ChatColor.GREEN + waitingMessage);
        scoreboard.resetScores(ChatColor.GREEN + "Запуск игры...");

        // Добавляем новый статус
        objective.getScore(ChatColor.GREEN + status).setScore(5);
    }

    private void updateTimerForAll(Arena arena, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(player) == arena) {
                updateTimer(player, message);
            }
        }
    }

    // ======= ОБНОВЛЕНИЕ ИГРОКОВ =======
    public void updatePlayerCount(Player player, Arena arena) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("lobby");
        if (objective == null) return;

        int players = getTotalPlayersInLobby(arena);
        objective.getScore(ChatColor.AQUA + "Игроки: " + ChatColor.GREEN + players + "/" + arena.getMaxPlayers())
                .setScore(3);
    }

    // ======= ОБНОВЛЕНИЕ ТАЙМЕРА НА СКОРБОРДЕ =======
    public void updateTimer(Player player, String message) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("lobby");
        if (objective == null) return;

        // Сбрасываем старый таймер
        for (String entry : scoreboard.getEntries()) {
            if (entry.contains("Время до старта:")) {
                scoreboard.resetScores(entry);
            }
        }

        // Добавляем новый таймер
        objective.getScore(ChatColor.RED + "Время до старта: " + ChatColor.YELLOW + message).setScore(2);
    }

    // ======= ПОДСЧЁТ ИГРОКОВ =======
    public int getPlayerCount(Arena arena) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(player) == arena) {
                count++;
            }
        }
        return count;
    }

    // ======= ПОДСЧЁТ ВСЕХ ИГРОКОВ В ЛОББИ АРЕНЫ =======
    private int getTotalPlayersInLobby(Arena arena) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(player) == arena) {
                count++;
            }
        }
        return count;
    }

    // ======= ПРОВЕРКА НАЧАЛА ОТСЧЁТА =======
    private void checkAndStartCountdown(Arena arena) {
        int currentPlayers = getTotalPlayersInLobby(arena);

        // Если игроков достаточно и таймер не запущен
        if (currentPlayers >= minPlayers && !countdownRunning) {
            startCountdown(arena);
        }
    }

    // ======= ЗАПУСК ОТСЧЁТА =======
    private void startCountdown(Arena arena) {
        // Если почему-то арена оказалась null – выходим
        if (arena == null) {
            Bukkit.getLogger().warning("startCountdown > arena = null. Отменяем запуск таймера!");
            return;
        }

        // Сбрасываем, если вдруг что-то было запущено раньше
        stopCountdown();

        this.countdownArena = arena;
        this.countdown = startCountdown;
        this.countdownRunning = true;

        updateStatusForAll(countdownArena, "Запуск игры...");

        // Создаём таск
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Если плагин выгружен или арена стала null — отменяем
                if (countdownArena == null || !BedWarsLC.getInstance().isEnabled()) {
                    this.cancel();
                    return;
                }

                int currentPlayers = getTotalPlayersInLobby(countdownArena);

                // Если игроков меньше минимума
                if (currentPlayers < minPlayers) {
                    broadcastToArena(countdownArena, ChatColor.RED + "Недостаточно игроков. Запуск отменён!");
                    resetCountdown(countdownArena);
                    this.cancel();
                    return;
                }

                // Обновляем таймер
                updateTimerForAll(countdownArena, String.valueOf(countdown));

                // Когда таймер доходит до 0
                if (countdown <= 0) {
                    broadcastToArena(countdownArena, ChatColor.GREEN + "Игра начинается!");
                    this.cancel();
                    countdownRunning = false;

                    // Запуск игры
                    gameManager.startGame(countdownArena);
                }
                countdown--;
            }
        };

        // Запускаем задачу (первая отсрочка 0 тиков, повтор каждые 20 тиков = 1 сек)
        countdownTask.runTaskTimer(
                BedWarsLC.getInstance(),
                0L,
                20L
        );
    }

    // ======= СБРОС ОТСЧЁТА =======
    private void resetCountdown(Arena arena) {
        stopCountdown(); // Останавливаем таск
        this.countdown = startCountdown;
        this.countdownRunning = false;

        // Сбрасываем статус для всех (ждём игроков)
        updateStatusForAll(arena, waitingMessage);

        // Сбрасываем строку таймера, чтобы игроки не видели старые значения
        updateTimerForAll(arena, "");
    }

    // Метод для полной остановки/отмены текущего отсчёта
    private void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdownArena = null;
        countdownRunning = false;
    }

    private void broadcastToArena(Arena arena, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(player) == arena) {
                player.sendMessage(prefix + " " + message);
            }
        }
    }
}
