package org.example.BedWarsLC.Menu;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.Arena.TeamData; // Импортируем TeamData

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamSetupMenu {

    public static void openTeamSetupMenu(Player player, Arena arena) {
        // Создаём меню 54 слота
        Inventory menu = Bukkit.createInventory(null, 54, "Команды - " + arena.getName());

        // Декоративные рамки
        ItemStack glassPane = createMenuItem(Material.WHITE_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                menu.setItem(i, glassPane);
            }
        }

        // Слоты для команд
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int index = 0;

        // Используем новый метод getTeams()
        for (Map.Entry<String, TeamData> entry : arena.getTeams().entrySet()) {
            if (index >= slots.length) break; // Ограничиваем количество кнопок

            String teamKey = entry.getKey();
            TeamData team = entry.getValue(); // Получаем объект TeamData
            String teamName = team.getName(); // Имя команды
            String colorName = team.getColor(); // Цвет команды

            // Определяем цвет шерсти
            DyeColor color = DyeColor.valueOf(colorName);
            Material woolMaterial = Material.valueOf(color.name() + "_WOOL"); // Преобразуем цвет в нужный тип шерсти

// Создаем ItemStack с нужным цветом шерсти
            ItemStack wool = new ItemStack(woolMaterial, 1);
            ItemMeta meta = wool.getItemMeta();
            meta.setDisplayName("§a" + teamName); // Имя команды
            wool.setItemMeta(meta);
            List<String> lore = new ArrayList<>();
            lore.add("§7Цвет: " + color.name());
            lore.add("§eНажмите, чтобы изменить цвет/имя.");
            meta.setLore(lore);

            wool.setItemMeta(meta);
            menu.setItem(slots[index++], wool);
        }

        // Кнопка назад
        menu.setItem(49, createMenuItem(Material.ARROW, "⮐ Назад", "§7Вернуться назад"));

        player.openInventory(menu);
    }

    private static ItemStack createMenuItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add(description);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
