package org.example.BedWarsLC.Listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Game.GameManager;

import java.util.Set;

public class SpawnProtectionListener implements Listener {

    private final GameManager gameManager;
    private final ArenaManager arenaManager;

    public SpawnProtectionListener(GameManager gameManager, ArenaManager arenaManager) {
        this.gameManager = gameManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Проверяем арену
        Arena arena = arenaManager.getLobbyArena(player);
        if (arena == null || !"RUNNING".equalsIgnoreCase(arena.getStatus())) {
            return;
        }

        Location placedLoc = event.getBlockPlaced().getLocation();

        // 1) Если это точка спавн-защиты
        if (gameManager.getSpawnProtectedBlocks().contains(placedLoc)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Здесь нельзя ставить блоки! (Spawn protection)");
            return;
        }

        // 2) Удаляем этот блок из preGameBlocks (теперь это “новый” блок)
        int bx = placedLoc.getBlockX();
        int by = placedLoc.getBlockY();
        int bz = placedLoc.getBlockZ();

        if (bx >= arena.getMinX() && bx <= arena.getMaxX()
                && by >= arena.getMinY() && by <= arena.getMaxY()
                && bz >= arena.getMinZ() && bz <= arena.getMaxZ()) {

            gameManager.getPreGameBlocks().remove(placedLoc);
            // Теперь ломать этот блок будет можно
        }
    }
}
