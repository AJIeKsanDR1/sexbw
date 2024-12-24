package org.example.BedWarsLC.Listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Arena.Arena.TeamData;
import org.example.BedWarsLC.Menu.TeamSetupMenu;
import org.example.BedWarsLC.Menu.ArenaEditMenu;

import java.util.*;

public class TeamSetupListener implements Listener {

    private final ArenaManager arenaManager;
    private final Map<Player, Arena> editingArena = new HashMap<>();
    private final Map<Player, String> selectedTeam = new HashMap<>();
    private final Map<Player, String> pendingRename = new HashMap<>();

    public TeamSetupListener(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    public void setEditingArena(Player player, Arena arena) {
        editingArena.put(player, arena);
        player.sendMessage("Арена установлена: " + arena.getName());
    }

    public void setSelectedTeam(Player player, String team) {
        selectedTeam.put(player, team);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Проверка, что игрок кликнул в меню команд
        if (event.getView().getTitle().startsWith("Команды - ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Arena arena = editingArena.get(player);
            if (arena == null) {
                player.sendMessage(ChatColor.RED + "Ошибка: арена не найдена!");
                return;
            }

            // Нажатие на команду
            if (event.getCurrentItem().getType().name().endsWith("_WOOL")) {
                String teamKey = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                setSelectedTeam(player, teamKey);
                openActionMenu(player, teamKey); // Переход к меню действий
            }

            // Кнопка назад
            if (event.getCurrentItem().getType() == Material.ARROW) {
                player.closeInventory();
                ArenaEditMenu.openEditMenu(player, arena);
            }
        }

        // Меню действий
        if (event.getView().getTitle().startsWith("Действие - ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Arena arena = editingArena.get(player);
            String teamKey = selectedTeam.get(player);
            if (arena == null || teamKey == null) {
                player.sendMessage(ChatColor.RED + "Ошибка: арена или команда не найдены!");
                return;
            }

            // Обработка действий
            Material clicked = event.getCurrentItem().getType();

            if (clicked == Material.NAME_TAG) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Введите новое имя для команды '" + teamKey + "':");
                pendingRename.put(player, teamKey);
            }

            if (clicked == Material.RED_WOOL) {
                openColorSelectionMenu(player); // Выбор цвета команды
            }

            if (clicked == Material.RED_BED) {
                Location bedLocation = player.getLocation();
                float yaw = bedLocation.getYaw(); // Сохраняем угол поворота
                arena.getTeams().get(teamKey).setBedLocation(bedLocation);
                arena.getTeams().get(teamKey).setBedYaw(yaw); // Устанавливаем угол поворота

                player.sendMessage(ChatColor.GREEN + "Кровать команды '" + teamKey + "' установлена!");
                player.closeInventory();
                arena.saveToConfig(); // Сохраняем изменения в конфиг
            }

            if (clicked == Material.BEACON) {
                arena.getTeams().get(teamKey).setBeaconLocation(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Маяк команды '" + teamKey + "' установлен!");
                player.closeInventory();
                arena.saveToConfig();
            }

            if (clicked == Material.ARROW) {
                player.closeInventory();
                TeamSetupMenu.openTeamSetupMenu(player, arena);
            }
        }

        // Выбор цвета
        if (event.getView().getTitle().equals("Выбор цвета")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Arena arena = editingArena.get(player);
            String teamKey = selectedTeam.get(player);
            if (arena == null || teamKey == null) {
                player.sendMessage(ChatColor.RED + "Ошибка: арена или команда не найдены!");
                return;
            }

            // Применение цвета команды
            DyeColor color = DyeColor.valueOf(event.getCurrentItem().getType().name().replace("_WOOL", ""));
            arena.getTeams().get(teamKey).setColor(color.name());
            arena.saveToConfig();
            player.sendMessage(ChatColor.GREEN + "Цвет команды '" + teamKey + "' изменён на " + color.name());
            player.closeInventory();
            TeamSetupMenu.openTeamSetupMenu(player, arena);
        }
    }


    /**
     * Меню действий для команды
     */
    private void openActionMenu(Player player, String teamName) {
        Inventory menu = Bukkit.createInventory(null, 27, "Действие - " + teamName);

        menu.setItem(11, createMenuItem(Material.NAME_TAG, "Изменить имя", "§eИзменить имя команды."));
        menu.setItem(15, createMenuItem(Material.RED_WOOL, "Изменить цвет", "§eИзменить цвет команды."));
        menu.setItem(12, createMenuItem(Material.RED_BED, "Установить кровать", "§eУстановить кровать на вашей локации."));
        menu.setItem(14, createMenuItem(Material.BEACON, "Установить маяк", "§eУстановить маяк на вашей локации."));
        menu.setItem(22, createMenuItem(Material.ARROW, "⮐ Назад", "§7Вернуться назад"));

        player.openInventory(menu);
    }

    /**
     * Создание элемента для меню
     */
    private ItemStack createMenuItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(description);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Меню выбора цвета команды
     */
    private void openColorSelectionMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "Выбор цвета");

        int slot = 0;
        for (DyeColor color : DyeColor.values()) {
            Material woolMaterial = Material.valueOf(color.name() + "_WOOL");
            ItemStack wool = new ItemStack(woolMaterial);
            ItemMeta meta = wool.getItemMeta();
            meta.setDisplayName("§a" + color.name());
            wool.setItemMeta(meta);
            menu.setItem(slot++, wool);
        }

        player.openInventory(menu);
    }

    @EventHandler
    public void onChatInput(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (pendingRename.containsKey(player)) {
            event.setCancelled(true);

            String newName = event.getMessage();
            String oldName = pendingRename.remove(player);

            Arena arena = editingArena.get(player);
            if (arena == null) {
                player.sendMessage("Ошибка: арена не найдена!");
                return;
            }

            TeamData team = arena.getTeams().remove(oldName);
            if (team != null) {
                team.setName(newName);
                arena.getTeams().put(newName, team);
            }

            arena.saveToConfig();
            player.sendMessage("Имя команды изменено с '" + oldName + "' на '" + newName + "'!");
            TeamSetupMenu.openTeamSetupMenu(player, arena);
        }
    }
}
