package org.example.BedWarsLC.Listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Game.GameManager;

import java.util.Map;
import java.util.Set;

public class RegionProtectionListener implements Listener {

    private final ArenaManager arenaManager;
    private final GameManager gameManager;
    private final Set<Location> preGameBlocks; // Блоки до начала игры
    private final Map<Location, String> teamBeds; // Кровати команд

    public RegionProtectionListener(ArenaManager arenaManager,
                                    GameManager gameManager,
                                    Set<Location> preGameBlocks,
                                    Map<Location, String> teamBeds) {
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
        this.preGameBlocks = preGameBlocks;
        this.teamBeds = teamBeds;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        // [Отладка]
        Bukkit.getLogger().info("onBlockBreak > player=" + player.getName()
                + ", blockType=" + block.getType()
                + ", blockLoc=" + blockLoc);

        // 1) Получаем арену игрока и проверяем статус
        Arena arena = arenaManager.getLobbyArena(player);
        if (arena == null || !"RUNNING".equalsIgnoreCase(arena.getStatus())) {
            // [Отладка]
            Bukkit.getLogger().info("onBlockBreak > arena == null или не RUNNING. Выходим.");
            return;
        }

        // 2) Проверка: является ли блок кроватью
        if (block.getType().name().endsWith("_BED")) {
            if (gameManager.handleBedBreak(blockLoc, player, arena)) {
                event.setCancelled(true); // Если кровать защищена
            } else {
                Bukkit.getLogger().info("onBlockBreak > Кровать не принадлежит арене. Разрешаем ломать.");
            }
            return;
        }

        // 3) Если не кровать, проверяем preGameBlocks
        if (preGameBlocks.contains(blockLoc)) {
            Bukkit.getLogger().info("onBlockBreak > Блок в preGameBlocks! Отменяем ломание.");
            event.setCancelled(true);
            sendMessageOnce(player, ChatColor.RED + "Нельзя ломать блоки, которые были поставлены до старта игры!");
        }
    }

    private void sendMessageOnce(Player player, String message) {
        if (!player.hasMetadata("block_break_message")) {
            player.sendMessage(message);
            player.setMetadata("block_break_message",
                    new FixedMetadataValue(gameManager.getPlugin(), true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("block_break_message", gameManager.getPlugin());
                }
            }.runTaskLater(gameManager.getPlugin(), 20L);
        }
    }
}
