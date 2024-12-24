package org.example.BedWarsLC.Menu;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.BedWarsLC.Arena.Arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpawnSetupMenu {

    // Открытие меню спавнов команд
    public static void openSpawnSetupMenu(Player player, Arena arena) {
        Inventory menu = Bukkit.createInventory(null, 27, "Спавны команд: " + arena.getName());

        // Декоративные рамки
        ItemStack glassPane = createMenuItem(Material.WHITE_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || (i + 1) % 9 == 0) {
                menu.setItem(i, glassPane);
            }
        }

        // Вывод списка команд
        int slot = 10; // Начальный слот для отображения команд
        for (Map.Entry<String, Arena.TeamData> entry : arena.getTeams().entrySet()) {
            String teamName = entry.getKey(); // Имя команды
            Arena.TeamData team = entry.getValue(); // Данные команды

            // Добавляем кнопку команды
            menu.setItem(slot++, createTeamSpawnItem(teamName, team));
        }

        // Кнопка выхода
        menu.setItem(22, createMenuItem(
                Material.ARROW,
                "⮐ Назад",
                "§7Вернуться в меню редактирования арены"
        ));

        // Открываем меню
        player.openInventory(menu);
    }

    // Создание кнопки команды для установки спавна
    private static ItemStack createTeamSpawnItem(String teamName, Arena.TeamData team) {
        // Определяем цвет команды
        DyeColor color = DyeColor.valueOf(team.getColor().toUpperCase());
        Material wool = getWoolByColor(color); // Материал шерсти под 1.13+

        ItemStack item = new ItemStack(wool, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&" + getColorCode(color) + teamName));

        List<String> lore = new ArrayList<>();
        lore.add("§7Нажмите, чтобы установить спавн");
        lore.add("§7Текущий спавн: " + getFormattedLocation(team.getSpawnPoint()));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Получаем материал шерсти по цвету
    private static Material getWoolByColor(DyeColor color) {
        switch (color) {
            case WHITE: return Material.WHITE_WOOL;
            case ORANGE: return Material.ORANGE_WOOL;
            case MAGENTA: return Material.MAGENTA_WOOL;
            case LIGHT_BLUE: return Material.LIGHT_BLUE_WOOL;
            case YELLOW: return Material.YELLOW_WOOL;
            case LIME: return Material.LIME_WOOL;
            case PINK: return Material.PINK_WOOL;
            case GRAY: return Material.GRAY_WOOL;
            case LIGHT_GRAY: return Material.LIGHT_GRAY_WOOL; // Исправлено
            case CYAN: return Material.CYAN_WOOL;
            case PURPLE: return Material.PURPLE_WOOL;
            case BLUE: return Material.BLUE_WOOL;
            case BROWN: return Material.BROWN_WOOL;
            case GREEN: return Material.GREEN_WOOL;
            case RED: return Material.RED_WOOL;
            case BLACK: return Material.BLACK_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }

    // Возвращаем цветовой код для текста в меню
    private static String getColorCode(DyeColor color) {
        switch (color) {
            case WHITE: return "f";
            case ORANGE: return "6";
            case MAGENTA: return "d";
            case LIGHT_BLUE: return "b";
            case YELLOW: return "e";
            case LIME: return "a";
            case PINK: return "c";
            case GRAY: return "8";
            case LIGHT_GRAY: return "7"; // Светло-серый
            case CYAN: return "3";
            case PURPLE: return "5";
            case BLUE: return "9";
            case BROWN: return "4";
            case GREEN: return "2";
            case RED: return "c";
            case BLACK: return "0";
            default: return "f"; // Белый
        }
    }

    // Форматирование локации в текст
    private static String getFormattedLocation(org.bukkit.Location loc) {
        if (loc == null) return "§cНе установлено";
        return String.format("§aX: %.1f Y: %.1f Z: %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    // Метод для создания предметов меню
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
