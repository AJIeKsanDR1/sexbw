package org.example.BedWarsLC.Listener;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.example.BedWarsLC.Arena.Arena;
import org.example.BedWarsLC.Arena.ArenaManager;
import org.example.BedWarsLC.Game.GameManager;

public class ArenaPvPListener implements Listener {

    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    public ArenaPvPListener(ArenaManager arenaManager, GameManager gameManager) {
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
    }

    // ======= ОБРАБОТКА АТАКИ =======
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Проверяем, что атакующий и жертва являются игроками
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager(); // Атакующий
        Player victim = (Player) event.getEntity();  // Жертва

        // Получаем арену жертвы
        Arena arena = arenaManager.getLobbyArena(victim);

        // Если жертва находится в арене и игра идёт
        if (arena != null && "RUNNING".equalsIgnoreCase(arena.getStatus())) {

            // Проверяем, неуязвим ли игрок
            if (victim.isInvulnerable()) {
                event.setCancelled(true); // Отменяем урон
                return;
            }

            // Дополнительно: защита от нападения на членов своей команды
            Arena.TeamData damagerTeam = arenaManager.getPlayerTeamData(damager);
            Arena.TeamData victimTeam = arenaManager.getPlayerTeamData(victim);

            if (damagerTeam != null && victimTeam != null && damagerTeam.getName().equalsIgnoreCase(victimTeam.getName())) {
                event.setCancelled(true); // Блокируем урон по членам своей команды
            }
        } else {
            // Если игрок не в арене или игра не началась — блокируем урон
            event.setCancelled(true);
        }
    }
}
