package org.example.BedWarsLC.Listener;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Lobby.LobbyManager;
import org.example.BedWarsLC.Utils.TabManager;

import java.util.*;

public class LobbyItemListener implements Listener {

    private final ArenaManager arenaManager;
    private final LobbyManager lobbyManager;

    public LobbyItemListener(ArenaManager arenaManager, LobbyManager lobbyManager) {
        this.arenaManager = arenaManager;
        this.lobbyManager = lobbyManager;
    }

    // ======= ПЕРЕВОД В РЕЖИМ ПРИКЛЮЧЕНИЯ ПРИ ВХОДЕ =======
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getLobbyArena(player);

        if (arena != null) {
            // Устанавливаем режим приключения и выдаём предметы
            player.setGameMode(GameMode.ADVENTURE);
            lobbyManager.giveLobbyItems(player, arena);
        }
    }

    // ======= ЗАПРЕТ ВЫБРАСЫВАНИЯ =======
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (isLobbyItem(item)) {
            event.setCancelled(true); // Запрет выбрасывания
        }
    }

    // ======= ЗАПРЕТ ПЕРЕМЕЩЕНИЯ =======
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().equals(ChatColor.BOLD + "Выбор команды")) return;

        event.setCancelled(true); // Запрещаем перемещение предметов

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Arena arena = arenaManager.getLobbyArena(player);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в лобби арены!");
            return;
        }

        // Получаем имя команды
        String teamName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // Проверяем существование команды
        if (!arena.getTeams().containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "Команда не найдена!");
            return;
        }

        // Проверяем заполненность команды
        List<String> teamMembers = arenaManager.getPlayersInTeam(teamName);
        int maxPlayersPerTeam = arenaManager.getMaxPlayersPerTeam(arena);

        if (teamMembers.size() >= maxPlayersPerTeam) {
            player.sendMessage(ChatColor.RED + "Эта команда уже заполнена!");
            return;
        }

        // Добавляем игрока в команду
        arenaManager.addPlayerToTeam(player, arena, teamName);

        // Обновляем TAB-лист
        TabManager tabManager = new TabManager(arenaManager);
        tabManager.updateTabList(player);

        // Надеваем броню цвета команды
        setTeamArmor(player, arena.getTeams().get(teamName).getColor());

        // Подтверждение выбора
        player.sendMessage(ChatColor.GREEN + "Вы выбрали команду: " + ChatColor.AQUA + teamName);

        // Обновляем меню
        openTeamSelectionMenu(player);
    }

    // ======= ЗАПРЕТ ПЕРЕТАСКИВАНИЯ =======
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();

        for (ItemStack item : event.getNewItems().values()) {
            if (isLobbyItem(item)) {
                event.setCancelled(true); // Запрет перетаскивания
                break;
            }
        }
    }

    // ======= ОБРАБОТКА ВЗАИМОДЕЙСТВИЙ =======
    @EventHandler
    public void onItemClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem(); // Исправлено (более надёжный метод)

        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();

        // Обработка выбора команды
        if (item.getType() == Material.WHITE_BED && displayName.equals(ChatColor.AQUA + "Выбор команды")) {
            openTeamSelectionMenu(player);
        }

        // Возврат в главное лобби
        if (item.getType() == Material.REDSTONE_TORCH && displayName.equals(ChatColor.RED + "Вернуться в главное лобби")) {
            player.sendMessage(ChatColor.YELLOW + "Телепортация в главное лобби...");

            arenaManager.removeLobbyArena(player);
            arenaManager.removePlayerFromTeam(player);

            lobbyManager.teleportToMainLobby(player);
        }
    }

    private void openTeamSelectionMenu(Player player) {
        // Проверяем, находится ли игрок в лобби
        Arena arena = arenaManager.getLobbyArena(player);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в лобби арены!");
            return;
        }

        // Создаём меню (54 слота)
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.BOLD + "Выбор команды");

        // === Заполнение границ панелями стекла ===
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                ItemStack glassPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " "); // Серые панели
                menu.setItem(i, glassPane);
            }
        }

        // === Добавляем команды (блоки стекла) ===
        int[] slots = {10, 11, 12, 14, 15, 16, 19, 20, 21, 23, 24, 25, 28, 29, 30, 32, 33, 34};
        int index = 0;

        for (Map.Entry<String, Arena.TeamData> entry : arena.getTeams().entrySet()) {
            if (index >= slots.length) break;

            String teamName = entry.getKey();
            Arena.TeamData team = entry.getValue();

            // Проверяем заполненность команды
            List<String> teamMembers = arenaManager.getPlayersInTeam(teamName);
            int maxPlayersPerTeam = arenaManager.getMaxPlayersPerTeam(arena);
            boolean isFull = teamMembers.size() >= maxPlayersPerTeam;

            // Используем блок стекла
            Material glassMaterial = Material.valueOf(team.getColor().toUpperCase() + "_STAINED_GLASS");
            ItemStack item = new ItemStack(glassMaterial);

            // === Добавляем зачарование для заполненных команд ===
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + teamName);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Игроков: " + ChatColor.AQUA + teamMembers.size() + "/" + maxPlayersPerTeam);

            for (String member : teamMembers) {
                lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + member);
            }

            lore.add(ChatColor.YELLOW + "Нажмите, чтобы выбрать!");

            if (isFull) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                lore.add(ChatColor.RED + "Команда заполнена!");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            // Устанавливаем элемент в меню
            menu.setItem(slots[index], item);
            index++;
        }

        // Открываем меню для игрока
        player.openInventory(menu);
    }

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Задаём имя и скрываем характеристики
        meta.setDisplayName(name);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }


    private void setTeamArmor(Player player, String color) {
        Material material = Material.valueOf(color + "_WOOL");
        ItemStack armor = new ItemStack(material);
        player.getInventory().setHelmet(armor);
    }

    private boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        String name = item.getItemMeta().getDisplayName();
        return name.equals(ChatColor.AQUA + "Выбор команды") || name.equals(ChatColor.RED + "Вернуться в главное лобби");
    }
}
