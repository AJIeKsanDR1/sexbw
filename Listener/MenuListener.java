package org.example.BedWarsLC.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Menu.ArenaEditMenu;
import org.example.BedWarsLC.Menu.ArenaSetupMenu;
import org.example.BedWarsLC.Arena.ArenaCreationHandler;
import org.example.BedWarsLC.Menu.LobbyMenu;

public class MenuListener implements Listener {

    private final ArenaManager arenaManager;
    private final ArenaCreationHandler creationHandler;

    public MenuListener(ArenaManager arenaManager, ArenaCreationHandler creationHandler) {
        this.arenaManager = arenaManager;
        this.creationHandler = creationHandler;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        // Получаем игрока
        Player player = (Player) event.getWhoClicked();

        // Проверяем название меню
        String menuTitle = event.getView().getTitle(); // Получаем название текущего меню
        if (menuTitle == null) return; // Проверка на null

        // ======== Меню "BedWars Управление" ========
        if (menuTitle.equals("§lBedWars Управление")) { // Проверка по форматированному названию
            event.setCancelled(true); // Блокируем перемещение предметов

            // Проверка на пустой предмет
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Обработка нажатий
            switch (event.getCurrentItem().getType()) {
                case CYAN_BED: // Управление аренами
                    player.sendMessage("§aВы выбрали управление аренами.");
                    ArenaSetupMenu.openArenaMenu(player, arenaManager);
                    break;
                case BEACON: // Управление лобби
                    player.sendMessage("§aВы выбрали управление лобби.");
                    LobbyMenu.openLobbyMenu(player); // Открываем меню настройки лобби
                    break;
                case BARRIER: // Закрытие меню
                    player.closeInventory();
                    break;
                default:
                    break;
            }
        }

        // ======== Меню "Управление аренами" ========
        if (menuTitle.equals("§lУправление аренами")) { // Обновлённое название с форматированием
            event.setCancelled(true); // Блокируем перемещение предметов

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Нажатие на арену для редактирования
            if (event.getCurrentItem().getType() == Material.MAP) {
                String arenaName = event.getCurrentItem().getItemMeta().getDisplayName().replace("§b", ""); // Убираем цвет
                Arena arena = arenaManager.getArena(arenaName); // Получаем арену по имени

                if (arena != null) {
                    player.closeInventory();
                    ArenaEditMenu.openEditMenu(player, arena); // Открываем меню редактирования
                } else {
                    player.sendMessage("§cОшибка: арена не найдена!");
                }
            }

            // Нажатие на кнопку "Создать новую арену"
            if (event.getCurrentItem().getType() == Material.PAPER) { // Создание новой арены
                player.closeInventory();
                creationHandler.startCreation(player); // Запускаем процесс создания
            }

            // Нажатие на кнопку "Закрыть"
            if (event.getCurrentItem().getType() == Material.BARRIER) { // Закрытие меню
                player.closeInventory();
            }
        }
    }
}
