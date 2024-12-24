package org.example.BedWarsLC.Listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.example.BedWarsLC.Menu.AdminMenu;
import org.bukkit.plugin.java.JavaPlugin;

public class MainLobbyListener implements Listener {

    private final JavaPlugin plugin;

    public MainLobbyListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Проверяем, в каком меню игрок
        if (event.getView().getTitle().equals("Настройка главного лобби")) {
            event.setCancelled(true); // Блокируем перемещение предметов

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            switch (event.getCurrentItem().getType()) {
                case ENDER_EYE: // Установить лобби
                    setLobbyLocation(player);
                    player.closeInventory();
                    break;
                case MAP: // Информация о лобби
                    showLobbyInfo(player);
                    break;
                case ARROW: // Назад
                    AdminMenu.openAdminMenu(player);
                    break;
            }
        }
    }

    // Метод для установки главного лобби
    private void setLobbyLocation(Player player) {
        Location loc = player.getLocation();

        // Сохранение в конфиг
        FileConfiguration config = plugin.getConfig();
        config.set("MainLobby.world", loc.getWorld().getName());
        config.set("MainLobby.x", loc.getX());
        config.set("MainLobby.y", loc.getY());
        config.set("MainLobby.z", loc.getZ());
        config.set("MainLobby.yaw", loc.getYaw());
        config.set("MainLobby.pitch", loc.getPitch());

        plugin.saveConfig();
        player.sendMessage("§aГлавное лобби установлено на текущей позиции!");
    }

    // Метод для отображения информации о текущем лобби
    private void showLobbyInfo(Player player) {
        FileConfiguration config = plugin.getConfig();

        if (config.contains("MainLobby")) {
            String world = config.getString("MainLobby.world");
            double x = config.getDouble("MainLobby.x");
            double y = config.getDouble("MainLobby.y");
            double z = config.getDouble("MainLobby.z");
            float yaw = (float) config.getDouble("MainLobby.yaw");
            float pitch = (float) config.getDouble("MainLobby.pitch");

            player.sendMessage("§bИнформация о лобби:");
            player.sendMessage("§7Мир: §a" + world);
            player.sendMessage("§7Координаты: §aX: " + x + " Y: " + y + " Z: " + z);
            player.sendMessage("§7Ориентация: §aYaw: " + yaw + " Pitch: " + pitch);
        } else {
            player.sendMessage("§cЛобби ещё не установлено!");
        }
    }
}
