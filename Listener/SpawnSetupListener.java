                        package org.example.BedWarsLC.Listener;

                        import org.bukkit.ChatColor;
                        import org.bukkit.Location;
                        import org.bukkit.Material;
                        import org.bukkit.entity.Player;
                        import org.bukkit.event.EventHandler;
                        import org.bukkit.event.Listener;
                        import org.bukkit.event.inventory.InventoryClickEvent;
                        import org.bukkit.inventory.ItemStack;
                        import org.example.BedWarsLC.Arena.Arena;
                        import org.example.BedWarsLC.Arena.ArenaManager;
                        import org.example.BedWarsLC.Menu.AdminMenu;
                        import org.example.BedWarsLC.Menu.ArenaEditMenu;
                        import org.example.BedWarsLC.Menu.SpawnSetupMenu;

                        public class SpawnSetupListener implements Listener {

                            private final ArenaManager arenaManager;

                            public SpawnSetupListener(ArenaManager arenaManager) {
                                this.arenaManager = arenaManager;
                            }

                            @EventHandler
                            public void onSpawnSetupClick(InventoryClickEvent event) {
                                Player player = (Player) event.getWhoClicked();

                                if (!event.getView().getTitle().startsWith("Спавны команд: ")) return;

                                event.setCancelled(true);

                                ItemStack item = event.getCurrentItem();
                                if (item == null || item.getType() == Material.AIR) return;

                                String arenaName = event.getView().getTitle().replace("Спавны команд: ", "");
                                Arena arena = arenaManager.getArena(arenaName);

                                if (arena == null) {
                                    player.sendMessage(ChatColor.RED + "Ошибка: арена не найдена!");
                                    return;
                                }

                                if (item.getType() == Material.ARROW) {
                                    ArenaEditMenu.openEditMenu(player, arena);
                                    return;
                                }

                                String teamName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                                Arena.TeamData team = arena.getTeams().get(teamName);

                                if (team == null) {
                                    player.sendMessage(ChatColor.RED + "Команда не найдена!");
                                    return;
                                }

                                Location location = player.getLocation();
                                team.setSpawnPoint(location); // Устанавливаем спавн
                                arena.saveToConfig(); // Сохраняем арену в конфиге
                                arenaManager.saveArenasConfig(); // Принудительное сохранение файла

                                player.sendMessage(ChatColor.GREEN + "Спавн для команды " + teamName + " установлен!");
                                SpawnSetupMenu.openSpawnSetupMenu(player, arena);
                            }
                        }
