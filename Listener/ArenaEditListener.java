package org.example.BedWarsLC.Listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Menu.SpawnSetupMenu;
import org.example.BedWarsLC.Menu.TeamSetupMenu;
import org.example.BedWarsLC.Menu.ArenaSetupMenu;

public class ArenaEditListener implements Listener {

    private final ArenaManager arenaManager;
    private final TeamSetupListener teamSetupListener; // Добавили listener

    public ArenaEditListener(ArenaManager arenaManager, TeamSetupListener teamSetupListener) {
        this.arenaManager = arenaManager;
        this.teamSetupListener = teamSetupListener; // Передаём listener
    }

    @EventHandler
    public void onEditMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Проверка на меню редактирования арены
        if (event.getView().getTitle().startsWith("Редактирование: ")) {
            event.setCancelled(true); // Блокируем перемещение предметов

            // Проверка на предмет
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Получаем арену по названию
            String arenaName = event.getView().getTitle().replace("Редактирование: ", "");
            Arena arena = arenaManager.getArena(arenaName);

            if (arena == null) return;

            switch (event.getCurrentItem().getType()) {
                case BLACK_BANNER: // Настройка команд
                    player.closeInventory();
                    teamSetupListener.setEditingArena(player, arena); // Устанавливаем арену для обработки
                    TeamSetupMenu.openTeamSetupMenu(player, arena);   // Открытие меню
                    break;

                case WOODEN_AXE: // Добавление региона
                    player.closeInventory();
                    arenaManager.setEditingArena(player, arena); // Привязываем арену к игроку
                    player.getInventory().addItem(new ItemStack(Material.GOLDEN_AXE)); // Выдаём инструмент
                    player.sendMessage("§aВы получили инструмент для выделения региона.");
                    player.sendMessage("§aНажмите ЛКМ и ПКМ для выделения региона.");
                    break;

                case BEACON: // Установка лобби арены
                    setArenaLobby(player, arena); // Сохраняем лобби
                    break;

                case CYAN_BED: // Настройка спавнов команд
                    player.closeInventory();
                    SpawnSetupMenu.openSpawnSetupMenu(player, arena);
                    break;

                case BARRIER: // Удалить арену
                    arenaManager.removeArena(arena.getName());
                    player.closeInventory();
                    player.sendMessage("§cАрена " + arena.getName() + " удалена!");
                    break;

                case ARROW: // Назад
                    player.closeInventory();
                    ArenaSetupMenu.openArenaMenu(player, arenaManager);
                    break;

                default:
                    break;
            }
        }
    }

    // Установка лобби для арены
    private void setArenaLobby(Player player, Arena arena) {
        Location loc = player.getLocation();

        // Сохраняем лобби в арене
        arena.setLobbyLocation(loc);
        arena.saveToConfig(); // Сохраняем сразу в конфиг

        player.sendMessage("§aЛобби для арены '" + arena.getName() + "' установлено!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        arenaManager.clearEditingArena(event.getPlayer());
    }

    @EventHandler
    public void onPlayerCloseInventory(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        arenaManager.clearEditingArena(player);
    }
}
