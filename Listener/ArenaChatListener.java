package org.example.BedWarsLC.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.example.BedWarsLC.Arena.ArenaCreationHandler;

public class ArenaChatListener implements Listener {

    private final ArenaCreationHandler creationHandler;

    public ArenaChatListener(ArenaCreationHandler creationHandler) {
        this.creationHandler = creationHandler;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Если игрок в процессе создания арены
        if (creationHandler.isCreatingArena(player)) {
            event.setCancelled(true); // Блокируем стандартный чат
            String input = event.getMessage();

            // Передаём ввод в обработчик создания
            creationHandler.handleInput(player, input);
        }
    }
}