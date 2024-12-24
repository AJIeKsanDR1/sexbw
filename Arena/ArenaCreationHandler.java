package org.example.BedWarsLC.Arena;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ArenaCreationHandler {

    private final ArenaManager arenaManager;
    private final JavaPlugin plugin; // Добавляем ссылку на плагин

    private final Map<Player, String> currentStep = new HashMap<>();
    private final Map<Player, ArenaBuilder> arenaBuilders = new HashMap<>();

    public ArenaCreationHandler(ArenaManager arenaManager, JavaPlugin plugin) {
        this.arenaManager = arenaManager;
        this.plugin = plugin; // Инициализируем плагин
    }

    public void startCreation(Player player) {
        arenaBuilders.put(player, new ArenaBuilder());
        currentStep.put(player, "name");
        player.sendMessage("Введите имя новой арены:");
    }

    public void handleInput(Player player, String input) {
        String step = currentStep.get(player);
        ArenaBuilder builder = arenaBuilders.get(player);

        switch (step) {
            case "name":
                if (arenaManager.getArenas().containsKey(input)) {
                    player.sendMessage("Арена с таким именем уже существует. Введите другое имя:");
                } else {
                    builder.setName(input);
                    currentStep.put(player, "mode");
                    player.sendMessage("Выберите режим игры (SOLO, DUO, SQUAD):");
                }
                break;

            case "mode":
                if (input.equalsIgnoreCase("SOLO") || input.equalsIgnoreCase("DUO") || input.equalsIgnoreCase("SQUAD")) {
                    builder.setMode(input.toUpperCase());
                    currentStep.put(player, "maxPlayers");
                    player.sendMessage("Введите максимальное количество игроков:");
                } else {
                    player.sendMessage("Неверный режим. Введите SOLO, DUO или SQUAD:");
                }
                break;

            case "maxPlayers":
                try {
                    int maxPlayers = Integer.parseInt(input);
                    if (validateMaxPlayers(builder.getMode(), maxPlayers)) {
                        builder.setMaxPlayers(maxPlayers);
                        currentStep.remove(player);
                        saveArena(player, builder);
                    } else {
                        player.sendMessage("Число игроков не соответствует выбранному режиму. Попробуйте снова:");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("Введите корректное число игроков:");
                }
                break;
        }
    }

    private boolean validateMaxPlayers(String mode, int maxPlayers) {
        switch (mode) {
            case "SOLO":
                return maxPlayers % 1 == 0; // SOLO - 1 игрок в команде
            case "DUO":
                return maxPlayers % 2 == 0; // DUO - 2 игрока в команде
            case "SQUAD":
                return maxPlayers % 4 == 0; // SQUAD - 4 игрока в команде
            default:
                return false;
        }
    }

    private void saveArena(Player player, ArenaBuilder builder) {
        // Создаём арену с плагином, режимом и цветами по умолчанию
        Arena arena = new Arena(
                plugin,                  // Передаём ссылку на плагин
                builder.getName(),       // Имя арены
                builder.getMode(),       // Режим игры
                builder.getMaxPlayers()  // Максимальное количество игроков
        );

        arenaManager.addArena(arena);
        arenaManager.saveArena(arena);

        // Очистка данных
        arenaBuilders.remove(player);
        currentStep.remove(player);

        player.sendMessage("Арена '" + arena.getName() + "' успешно создана!");
        player.sendMessage("Теперь можно задать цвета команд в настройках.");
    }

    public boolean isCreatingArena(Player player) {
        return currentStep.containsKey(player);
    }
}
