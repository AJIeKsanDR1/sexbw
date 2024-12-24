package org.example.BedWarsLC.Menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminMenu {

    public static void openAdminMenu(Player player) {
        // Создаём инвентарь 9 слотов с названием
        Inventory menu = Bukkit.createInventory(null, 9, "§lBedWars Управление");

        // Добавляем кнопки с красивыми описаниями
        menu.setItem(0, createMenuItem(
                Material.CYAN_BED,
                "§aУправление аренами",
                "§7Настройка арен,",
                "§7создание и редактирование."
        ));

        menu.setItem(1, createMenuItem(
                Material.BEACON,
                "§bНастройка главного лобби",
                "§7Изменение главного лобби и",
                "§7его параметров."
        ));

        // Пустая кнопка-заполнитель для симметрии
        menu.setItem(4, createMenuItem(
                Material.NETHER_STAR,
                "§6Основные настройки",
                "§7Изменение глобальных",
                "§7параметров игры."
        ));

        // Кнопка для выхода
        menu.setItem(8, createMenuItem(
                Material.BARRIER,
                "§cЗакрыть меню",
                "§7Нажмите, чтобы закрыть это меню."
        ));

        // Открываем меню
        player.openInventory(menu);
    }

    // Метод для создания элементов меню
    private static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Устанавливаем имя кнопки
        meta.setDisplayName(name);

        // Добавляем описание кнопки
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);

        item.setItemMeta(meta);
        return item;
    }
}
