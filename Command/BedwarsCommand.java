package org.example.BedWarsLC.Command;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Game.GameManager;
import org.example.BedWarsLC.Lobby.LobbyManager;
import org.example.BedWarsLC.Menu.AdminMenu;

public class BedwarsCommand implements CommandExecutor {

    private final ArenaManager arenaManager;
    private final LobbyManager lobbyManager;
    private final GameManager gameManager;

    public BedwarsCommand(ArenaManager arenaManager, LobbyManager lobbyManager, GameManager gameManager) {
        this.arenaManager = arenaManager;
        this.lobbyManager = lobbyManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка, что команду выполняет игрок
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только для игроков.");
            return true;
        }

        Player player = (Player) sender;

        // Если команда без аргументов — показываем список команд
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        // Получаем название арены, если указано
        String action = args[0].toLowerCase();
        String arenaName = args.length > 1 ? args[1] : null;

        // Если команда требует арену, но она не указана
        if ((action.equals("start") || action.equals("stop") || action.equals("joinlobby")) && arenaName == null) {
            player.sendMessage(ChatColor.RED + "Используйте: /bw " + action + " <имя арены>");
            return true;
        }

        // Получаем арену, если указано имя
        Arena arena = arenaName != null ? arenaManager.getArena(arenaName) : null;

        // Если арена не найдена
        if ((action.equals("start") || action.equals("stop") || action.equals("joinlobby")) && arena == null) {
            player.sendMessage(ChatColor.RED + "Арена '" + arenaName + "' не найдена!");
            return true;
        }

        switch (action) {
            case "start":
                handleStartGame(player, arena);
                break;

            case "stop":
                handleStopGame(player, arena);
                break;

            case "info":
                showInfo(player);
                break;

            case "reload":
                reloadConfig(player);
                break;

            case "setup":
                AdminMenu.openAdminMenu(player);
                break;

            case "joinlobby":
                handleJoinLobby(player, arena);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Неизвестная команда. Используйте: /bw help");
                break;
        }
        return true;
    }

    // ====== Обработка запуска игры ======
    private void handleStartGame(Player player, Arena arena) {
        if (arena.getStatus().equalsIgnoreCase("RUNNING")) {
            player.sendMessage(ChatColor.RED + "Арена '" + arena.getName() + "' уже запущена!");
            return;
        }

        if (gameManager.startGame(arena)) {
            arena.setStatus("RUNNING"); // Устанавливаем статус в конфиге
            player.sendMessage(ChatColor.GREEN + "Игра на арене '" + arena.getName() + "' успешно запущена!");
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось запустить игру. Проверьте настройки арены.");
        }
    }

    private void handleStopGame(Player player, Arena arena) {
        if (!arena.getStatus().equalsIgnoreCase("RUNNING")) {
            player.sendMessage(ChatColor.RED + "Арена '" + arena.getName() + "' не запущена!");
            return;
        }

        if (gameManager.stopGame(arena)) {
            arena.setStatus("WAITING"); // Возвращаем статус в конфиге
            player.sendMessage(ChatColor.RED + "Игра на арене '" + arena.getName() + "' успешно остановлена!");
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось остановить игру. Попробуйте снова.");
        }
    }

    // ====== Вход в лобби ======
    private void handleJoinLobby(Player player, Arena arena) {
        if (!arena.getStatus().equalsIgnoreCase("WAITING")) {
            player.sendMessage(ChatColor.RED + "Вы не можете войти в лобби, так как арена '" + arena.getName() + "' запущена!");
            return;
        }

        lobbyManager.joinLobby(player, arena);

        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().clear();

        player.sendMessage(ChatColor.GREEN + "Вы вошли в лобби арены '" + arena.getName() + "'!");
    }

    // ======= ПОМОЩЬ =======
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "BedWars Управление" + ChatColor.GOLD + " =====");
        player.sendMessage(ChatColor.AQUA + "/bw start <арена>" + ChatColor.GRAY + " - Запустить игру.");
        player.sendMessage(ChatColor.AQUA + "/bw stop <арена>" + ChatColor.GRAY + " - Остановить игру.");
        player.sendMessage(ChatColor.AQUA + "/bw info" + ChatColor.GRAY + " - Информация о плагине.");
        player.sendMessage(ChatColor.AQUA + "/bw reload" + ChatColor.GRAY + " - Перезагрузить конфигурацию.");
        player.sendMessage(ChatColor.AQUA + "/bw setup" + ChatColor.GRAY + " - Открыть меню настроек.");
        player.sendMessage(ChatColor.AQUA + "/bw joinlobby <арена>" + ChatColor.GRAY + " - Войти в лобби арены.");
        player.sendMessage(ChatColor.GOLD + "=================================");
    }

    // ======= ИНФОРМАЦИЯ =======
    private void showInfo(Player player) {
        player.sendMessage(ChatColor.YELLOW + "===== " + ChatColor.GOLD + "Информация о плагине" + ChatColor.YELLOW + " =====");
        player.sendMessage(ChatColor.GREEN + "Плагин: " + ChatColor.AQUA + "BedWarsLC");
        player.sendMessage(ChatColor.GREEN + "Версия: " + ChatColor.AQUA + "1.0");
        player.sendMessage(ChatColor.GREEN + "Автор: " + ChatColor.AQUA + "Loorec");
        player.sendMessage(ChatColor.YELLOW + "=================================");
    }

    // ======= ПЕРЕЗАГРУЗКА =======
    private void reloadConfig(Player player) {
        arenaManager.loadArenas();
        lobbyManager.reloadConfig();
        player.sendMessage(ChatColor.AQUA + "Конфигурация успешно перезагружена!");
    }
}
