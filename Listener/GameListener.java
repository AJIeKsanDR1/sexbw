package org.example.BedWarsLC.Listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Game.GameManager;

public class GameListener implements Listener {

    private final GameManager gameManager;
    private final ArenaManager arenaManager;

    public GameListener(GameManager gameManager, ArenaManager arenaManager) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Отключаем стандартное сообщение о смерти
        event.setDeathMessage(null);

        // Получаем арену игрока с проверкой активной игры
        Arena arena = arenaManager.getActiveArena(player); // <-- Используем новый метод getActiveArena()
        if (arena == null) {
            Bukkit.getLogger().info("onPlayerDeath > Игрок " + player.getName() + " не в активной арене.");
            return;
        }

        // Логируем событие
        Bukkit.getLogger().info("onPlayerDeath > Смерть игрока: " + player.getName() + " на арене: " + arena.getName());

        // Передаём обработку смерти в GameManager
        gameManager.handlePlayerDeath(player, arena);
    }
}
