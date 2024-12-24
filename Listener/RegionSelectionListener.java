package org.example.BedWarsLC.Listener;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;

import java.util.HashMap;
import java.util.Map;

public class RegionSelectionListener implements Listener {

    private final ArenaManager arenaManager;
    private final Map<Player, Location> pos1 = new HashMap<>();
    private final Map<Player, Location> pos2 = new HashMap<>();

    public RegionSelectionListener(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onRegionSelection(PlayerInteractEvent event) {
        // Проверяем, что действие было выполнено только основной рукой
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Проверяем, что игрок держит инструмент для выделения
        if (item.getType() != Material.GOLDEN_AXE) return;

        // Получаем арену, которую редактирует игрок
        Arena arena = arenaManager.getEditingArena(player);
        if (arena == null) {
            player.sendMessage("§cВы не редактируете арену!");
            return;
        }

        // Определяем блок, по которому кликнул игрок
        Block block = event.getClickedBlock();
        if (block == null) return;

        // ЛКМ — Первая точка
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            pos1.put(player, block.getLocation());
            player.sendMessage("§aПервая точка установлена: " + formatLocation(block.getLocation()));
        }
        // ПКМ — Вторая точка
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Проверка на наличие первой точки
            if (!pos1.containsKey(player)) {
                player.sendMessage("§cСначала установите первую точку ЛКМ!");
                return; // Прекращаем выполнение, если первая точка не задана
            }

            pos2.put(player, block.getLocation());
            player.sendMessage("§aВторая точка установлена: " + formatLocation(block.getLocation()));

            // Сохраняем регион, если обе точки заданы
            saveRegion(player, arena);
        }
    }


    private void saveRegion(Player player, Arena arena) {
        Location p1 = pos1.get(player);
        Location p2 = pos2.get(player);

        if (p1 == null || p2 == null) {
            player.sendMessage("§cУстановите обе точки!");
            return;
        }

        // Определяем границы региона
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        // Растягиваем вверх и вниз
        int minY = 0;
        int maxY = 255;

        arena.setRegion(minX, minY, minZ, maxX, maxY, maxZ);
        arena.saveToConfig();

        player.sendMessage("§aРегион сохранён для арены '" + arena.getName() + "'!");

        // Очищаем точки после сохранения
        pos1.remove(player);
        pos2.remove(player);
    }

    private String formatLocation(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
