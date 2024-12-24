package org.example.BedWarsLC.Menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;

import java.util.ArrayList;
import java.util.List;

public class ArenaSetupMenu {

    public static void openArenaMenu(Player player, ArenaManager arenaManager) {
        // Создаём инвентарь 27 слотов с названием
        Inventory menu = Bukkit.createInventory(null, 27, "§lУправление аренами");

        // Добавляем кнопку для создания новой арены
        menu.setItem(13, createMenuItem(
                Material.PAPER,
                "§aСоздать новую арену",
                "§7Нажмите, чтобы создать",
                "§7новую арену с параметрами."
        ));

        // Добавляем существующие арены с прокруткой
        int[] slots = {10, 11, 12, 14, 15, 16};
        int index = 0;

        for (Arena arena : arenaManager.getArenas().values()) {
            if (index >= slots.length) break; // Лимит отображения арен в меню

            menu.setItem(slots[index++], createMenuItem(
                    Material.MAP,
                    "§b" + arena.getName(),
                    "§7Режим: §f" + arena.getMode(),
                    "§7Игроков: §f" + arena.getMaxPlayers(),
                    "§eНажмите, чтобы редактировать."
            ));
        }

        // Добавляем кнопку закрытия
        menu.setItem(26, createMenuItem(
                Material.BARRIER,
                "§cЗакрыть",
                "§7Нажмите, чтобы закрыть меню."
        ));

        // Открываем меню для игрока
        player.openInventory(menu);
    }

    /**
     * Создание элемента меню с названием и описанием.
     */
    private static ItemStack createMenuItem(Material material, String name, String... loreText) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Устанавливаем название
        meta.setDisplayName(name);

        // Добавляем описание
        List<String> lore = new ArrayList<>();
        for (String line : loreText) {
            lore.add(line);
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
