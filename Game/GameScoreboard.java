package org.example.BedWarsLC.Game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.example.BedWarsLC.Arena.Arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameScoreboard {

    private final JavaPlugin plugin;
    private final Map<Player, Scoreboard> playerScoreboards = new java.util.HashMap<>();
    private int timeLeft; // Сколько осталось секунд до конца
    private final int totalTime; // Исходное время (для расчёта прогресса в BossBar)

    /**
     * Храним BossBar как поле. Спидгот/Пэйпер 1.13+ позволяет это делать.
     */
    private final BossBar bossBar;

    public GameScoreboard(JavaPlugin plugin, int gameDuration) {
        this.plugin = plugin;
        this.timeLeft = gameDuration;
        this.totalTime = gameDuration;

        // Создаём BossBar, указываем начальный заголовок, цвет и стиль.
        // Например, зелёная полоса (BarColor.GREEN), без делений (BarStyle.SOLID).
        bossBar = Bukkit.createBossBar(
                ChatColor.GREEN + "До конца игры осталось: " + formatTime(timeLeft),
                BarColor.GREEN,
                BarStyle.SOLID
        );

        // Значение прогресса от 0.0 до 1.0
        // Пока только создали, не добавили игроков. Ставим стартовый прогресс = 1.0
        // (Потому что у нас timeLeft = totalTime).
        bossBar.setProgress(1.0);
        // Можно сделать, чтобы все игроки видели полосу (FLAG_DARKEN_SKY и прочие).
        // По умолчанию никакие флаги не включены.
    }

    /**
     * Отрисовка скорборда:
     * - Название арены,
     * - Несколько строк (цветные квадраты + число),
     * - Игроков добавляем в BossBar.
     */
    public void createScoreboard(
            Player player,
            String arenaName,
            Map<String, Arena.TeamData> teams,
            Map<String, Integer> teamCountMap,
            int currentPlayers,
            int maxPlayers
    ) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("GameInfo", "dummy");
        objective.setDisplayName(ChatColor.GOLD + "~~~ BedWarsLC ~~~");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int scoreIndex = 15;

        // Верхняя строка: имя арены
        Score arenaLine = objective.getScore(
                ChatColor.YELLOW + "Арена: " + ChatColor.GREEN + arenaName
        );
        arenaLine.setScore(scoreIndex--);

        // Пустая строка
        objective.getScore(" ").setScore(scoreIndex--);

        // Собираем список команд
        List<Map.Entry<String, Arena.TeamData>> teamList = new ArrayList<>(teams.entrySet());

        // Группируем порциями по 3-4 на строку (пример, как у вас)
        int i = 0;
        while (i < teamList.size()) {
            StringBuilder lineBuilder = new StringBuilder();

            // Например, формируем до 4 “пар” (цвет + число).
            for (int c = 0; c < 4; c++) {
                if (i >= teamList.size()) break;

                Map.Entry<String, Arena.TeamData> entry = teamList.get(i++);
                String teamKey = entry.getKey();
                Arena.TeamData data = entry.getValue();

                // Получаем ChatColor из data.getColor()
                ChatColor teamColor = convertColorName(data.getColor());
                // Количество игроков
                int count = teamCountMap.getOrDefault(teamKey, 0);

                // Добавляем: "§c█ §f2 "
                lineBuilder.append(teamColor).append("█ ")
                        .append(ChatColor.WHITE).append(count).append("  ");
            }

            String lineText = lineBuilder.toString().trim();
            objective.getScore(lineText).setScore(scoreIndex--);
            if (scoreIndex < 0) break;
        }

        // Ещё одна пустая строка
        objective.getScore("  ").setScore(scoreIndex--);

        // Применяем скорборд
        player.setScoreboard(board);
        playerScoreboards.put(player, board);

        // Теперь добавляем игрока в BossBar
        bossBar.addPlayer(player);
    }

    /**
     * Обновляем время, синхронизируем bossBar (прогресс и заголовок).
     */
    public void updateTime(int newTime) {
        this.timeLeft = newTime;

        // Меняем заголовок bossBar
        bossBar.setTitle(
                ChatColor.GREEN + "До конца игры осталось: " + ChatColor.YELLOW + formatTime(timeLeft)
        );

        // Прогресс: от 1.0 (в начале) до 0.0 (в конце)
        double progress = (double) timeLeft / (double) totalTime;
        if (progress < 0.0) progress = 0.0;
        bossBar.setProgress(progress);
    }


    /**
     * Вместо Title - используем BossBar, так что этот метод
     * можете оставить пустым или убрать, если не нужен.
     */
    public void updateTimerDisplay(Player player) {
        // Ничего не делаем, так как BossBar уже обновляется в updateTime(...)
    }

    public void removeBossBar(Player player) {
        if (bossBar != null) {
            bossBar.removePlayer(player); // Удаляем игрока из BossBar
        }
    }

    /**
     * Сброс. Удаляем BossBar у игроков и очищаем всё.
     */
    public void resetTimer() {
        timeLeft = 0;

        // Скрываем BossBar от всех игроков
        bossBar.removeAll();

        for (Player p : playerScoreboards.keySet()) {
            p.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        }
        playerScoreboards.clear();
    }

    /**
     * Преобразовать имя цвета (RED, BLUE, YELLOW ...) в ChatColor
     */
    private ChatColor convertColorName(String colorName) {
        if (colorName == null) {
            return ChatColor.GRAY;
        }
        switch (colorName.toUpperCase()) {
            case "WHITE":      return ChatColor.WHITE;
            case "RED":        return ChatColor.RED;
            case "ORANGE":     return ChatColor.GOLD;
            case "MAGENTA":    return ChatColor.LIGHT_PURPLE;
            case "LIGHT_BLUE": return ChatColor.AQUA;
            case "YELLOW":     return ChatColor.YELLOW;
            case "LIME":       return ChatColor.GREEN;
            case "PINK":       return ChatColor.LIGHT_PURPLE;
            case "GRAY":       return ChatColor.GRAY;
            case "SILVER":     return ChatColor.GRAY;
            case "CYAN":       return ChatColor.DARK_AQUA;
            case "PURPLE":     return ChatColor.DARK_PURPLE;
            case "BLUE":       return ChatColor.BLUE;
            case "BROWN":      return ChatColor.DARK_RED;
            case "GREEN":      return ChatColor.DARK_GREEN;
            case "BLACK":      return ChatColor.BLACK;
            default:           return ChatColor.GRAY;
        }
    }

    /**
     * Форматируем секунды в "MM:SS" или "M мин S сек" — как вам удобнее
     */
    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        // Пример: "2:05"
        return m + ":" + (s < 10 ? ("0" + s) : s);
    }
}
