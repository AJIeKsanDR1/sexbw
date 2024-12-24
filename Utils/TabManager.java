package org.example.BedWarsLC.Utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Team;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;

import java.util.Objects;

public class TabManager {

    private final ArenaManager arenaManager;

    // Конструктор
    public TabManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    // Обновление TAB-листа для игрока
    public void updateTabList(Player player) {
        Arena arena = arenaManager.getLobbyArena(player);

        // Заголовок таба
        String header = ChatColor.GOLD + "" + ChatColor.BOLD + " ~~~ BedWarsLC ~~~ \n";

        // Подвал таба
        String footer;

        if (arena != null) {
            Arena.TeamData teamData = arenaManager.getPlayerTeamData(player);
            String teamName = (teamData != null) ? teamData.getName() : null;

            String teamDisplay = ChatColor.RED + "Не выбрана"; // Если команда не выбрана

            if (teamData != null && teamName != null && arena.getTeams().containsKey(teamName)) {
                ChatColor color = getColorByName(teamData.getColor());
                teamDisplay = color + teamName;
            }

            footer = ChatColor.YELLOW + "Арена: " + ChatColor.GREEN + arena.getName() + "\n" +
                    ChatColor.AQUA + "Режим: " + ChatColor.GREEN + arena.getMode() + "\n" +
                    ChatColor.DARK_GREEN + "Команда: " + teamDisplay + "\n" +
                    ChatColor.GRAY + "Игроков в лобби: " + ChatColor.GREEN + getOnlinePlayersInArena(arena) +
                    "/" + arena.getMaxPlayers() + "\n" +
                    ChatColor.YELLOW + "Ожидание игроков...";
        } else {
            footer = ChatColor.GRAY + "Вы находитесь вне игры.\n" +
                    ChatColor.YELLOW + "Выберите арену, чтобы присоединиться!";
        }

        // Обновляем таб-лист
        player.setPlayerListHeaderFooter(header, footer);

        // Обновляем имя игрока с цветом команды
        updatePlayerNameWithTeamColor(player);
    }

    // Обновление имени игрока в списке TAB с цветом команды
    private void updatePlayerNameWithTeamColor(Player player) {
        Arena arena = arenaManager.getLobbyArena(player);
        Arena.TeamData teamData = arenaManager.getPlayerTeamData(player);

        if (arena != null && teamData != null) {
            String teamName = teamData.getName();
            ChatColor color = getColorByName(teamData.getColor());

            // Устанавливаем имя в TAB в формате [Команда] Игрок
            player.setPlayerListName(color + "[" + teamName + "] " + ChatColor.WHITE + player.getName());
        } else {
            player.setPlayerListName(ChatColor.GRAY + "[Нет команды] " + ChatColor.WHITE + player.getName());
        }
    }

    // Получаем цвет по названию
    private ChatColor getColorByName(String color) {
        switch (color.toUpperCase()) {
            case "WHITE": return ChatColor.WHITE;
            case "ORANGE": return ChatColor.GOLD;
            case "MAGENTA": return ChatColor.LIGHT_PURPLE;
            case "LIGHT_BLUE": return ChatColor.AQUA;
            case "YELLOW": return ChatColor.YELLOW;
            case "LIME": return ChatColor.GREEN;
            case "PINK": return ChatColor.LIGHT_PURPLE;
            case "GRAY": return ChatColor.DARK_GRAY;
            case "SILVER": return ChatColor.GRAY;
            case "CYAN": return ChatColor.DARK_AQUA;
            case "PURPLE": return ChatColor.DARK_PURPLE;
            case "BLUE": return ChatColor.BLUE;
            case "BROWN": return ChatColor.GOLD;
            case "GREEN": return ChatColor.DARK_GREEN;
            case "RED": return ChatColor.DARK_RED;
            case "BLACK": return ChatColor.BLACK;
            default: return ChatColor.WHITE;
        }
    }

    // Подсчёт игроков в лобби
    private int getOnlinePlayersInArena(Arena arena) {
        int count = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (arenaManager.getLobbyArena(onlinePlayer) == arena) {
                count++;
            }
        }
        return count;
    }
}
