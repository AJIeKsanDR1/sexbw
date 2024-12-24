package org.example.BedWarsLC.Menu;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.BedWarsLC.Arena.Arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArenaEditMenu {

    public static void openEditMenu(Player player, Arena arena) {
        Inventory menu = Bukkit.createInventory(null, 27, "Редактирование: " + arena.getName());

        // Декоративные рамки
        ItemStack glassPane = createMenuItem(Material.WHITE_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || (i + 1) % 9 == 0) {
                menu.setItem(i, glassPane);
            }
        }

        // Опция: Настройка команд
        menu.setItem(10, createMenuItem(
                Material.BLACK_BANNER,
                "⚙ Настройка команд",
                "§7Изменить имена и цвета команд"
        ));

        // Опция: Добавить регион
        menu.setItem(12, createMenuItem(
                Material.WOODEN_AXE,
                "➕ Добавить регион",
                "§7Выделите регион для привата блоков"
        ));

        // Опция: Удалить арену
        menu.setItem(14, createMenuItem(
                Material.BARRIER,
                "✖ Удалить арену",
                "§cУдалить арену полностью"
        ));

        // \uD83C\uDFE0 = Emoji “домик”
        // Опция: Установить лобби для арены
        menu.setItem(16, createMenuItem(
                Material.BEACON,
                "🏠 Установить лобби",
                "§7Сохранить текущее местоположение",
                "§7как точку лобби для арены."
        ));

        // Опция: Настройка спавнов
        menu.setItem(13, createMenuItem(
                Material.CYAN_BED,
                "⛺ Настройка спавнов",
                "§7Установить точки спавнов",
                "§7для каждой команды."
        ));

        // Информация об арене (Список команд)
        menu.setItem(4, createArenaInfoItem(arena)); // Добавляем информацию о командах

        // Кнопка выхода
        menu.setItem(22, createMenuItem(
                Material.ARROW,
                "⮐ Назад",
                "§7Вернуться в меню арен"
        ));

        player.openInventory(menu);
    }

    // Метод для создания информации об арене (Список команд)
    private static ItemStack createArenaInfoItem(Arena arena) {
        ItemStack item = new ItemStack(Material.MAP); // Карта как символ информации
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bИнформация об арене");

        List<String> lore = new ArrayList<>();
        lore.add("§7Название: §a" + arena.getName());
        lore.add("§7Режим: §a" + arena.getMode());
        lore.add("§7Макс. игроков: §a" + arena.getMaxPlayers());

        // Добавляем состояние арены
        String status = getArenaStatus(arena);
        lore.add("§7Состояние: " + status);

        // Добавляем список команд
        lore.add("§7Команды:");
        for (Map.Entry<String, Arena.TeamData> entry : arena.getTeams().entrySet()) {
            Arena.TeamData team = entry.getValue();

            // Определяем цвет команды
            DyeColor color = DyeColor.valueOf(team.getColor().toUpperCase());
            lore.add("§a" + team.getName() + " §7(Цвет: §" + getColorCode(color) + color.name() + "§7)");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String getArenaStatus(Arena arena) {
        switch (arena.getStatus().toUpperCase()) {
            case "DISABLED": return "§cОтключена";
            case "WAITING":  return "§eОжидание";
            case "RUNNING":  return "§aЗапущена";
            default:         return "§7Неизвестно";
        }
    }

    // Возвращает код цвета для отображения в тексте (1.12.2 поддержка)
    private static String getColorCode(DyeColor color) {
        switch (color) {
            case WHITE:      return "f";
            case ORANGE:     return "6";
            case MAGENTA:    return "d";
            case LIGHT_BLUE: return "b";
            case YELLOW:     return "e";
            case LIME:       return "a";
            case PINK:       return "c";
            case GRAY:       return "8";
            case LIGHT_GRAY:     return "7"; // LIGHT_GRAY называется SILVER
            case CYAN:       return "3";
            case PURPLE:     return "5";
            case BLUE:       return "9";
            case BROWN:      return "4";
            case GREEN:      return "2";
            case RED:        return "c";
            case BLACK:      return "0";
            default:         return "f"; // Белый по умолчанию
        }
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
