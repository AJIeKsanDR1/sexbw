package org.example.BedWarsLC.Menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LobbyMenu {

    public static void openLobbyMenu(Player player) {
        // Создаём меню с 27 слотами
        Inventory menu = Bukkit.createInventory(null, 27, "Настройка главного лобби");

        // Декоративные рамки
        ItemStack glassPane = createMenuItem(Material.WHITE_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || (i + 1) % 9 == 0) {
                menu.setItem(i, glassPane);
            }
        }

        // Опция: Установить лобби
        menu.setItem(11, createMenuItem(
                Material.ENDER_EYE,
                "§aУстановить лобби",
                "§7Устанавливает местоположение",
                "§7главного лобби в текущей точке."
        ));

        // Опция: Текущая информация о лобби
        menu.setItem(13, createMenuItem(
                Material.MAP,
                "§bИнформация о лобби",
                "§7Посмотрите текущие параметры",
                "§7главного лобби, включая координаты."
        ));

        // Кнопка выхода
        menu.setItem(15, createMenuItem(
                Material.ARROW,
                "§cНазад",
                "§7Вернуться в главное меню."
        ));

        // Открываем меню
        player.openInventory(menu);
    }

    private static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);

        item.setItemMeta(meta);
        return item;
    }
}
