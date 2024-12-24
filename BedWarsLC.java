    package org.example.BedWarsLC;

    import org.bukkit.Bukkit;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.example.BedWarsLC.Arena.ArenaCreationHandler;
    import org.example.BedWarsLC.Arena.ArenaManager;
    import org.example.BedWarsLC.Command.BedwarsCommand;
    import org.example.BedWarsLC.Game.GameManager;
    import org.example.BedWarsLC.Listener.*;
    import org.example.BedWarsLC.Lobby.LobbyManager;
    import org.example.BedWarsLC.Utils.TabManager;

    public class BedWarsLC extends JavaPlugin {

        private ArenaManager arenaManager;
        private LobbyManager lobbyManager;
        private ArenaCreationHandler creationHandler;
        private TabManager tabManager; // Добавляем TabManager
        private GameManager gameManager; // Добавляем GameManager
        private static BedWarsLC instance; // Статическая ссылка на текущий плагин

        @Override
        public void onEnable() {
            instance = this;
            saveDefaultConfig(); // Загружаем config.yml
            // Логика запуска плагина
            getLogger().info("BedWarsLC успешно запущен!");

            arenaManager = new ArenaManager(this);
            lobbyManager = new LobbyManager(arenaManager, gameManager, getConfig());
            creationHandler = new ArenaCreationHandler(arenaManager,this);
            tabManager = new TabManager(arenaManager); // Инициализируем TabManager
            gameManager = new GameManager(arenaManager, this, 3600); // 3600 секунд (1 час)

            // Регистрация команд и событий
            registerCommands();
            registerEvents();
        }

        @Override
        public void onDisable() {
            Bukkit.getScheduler().cancelTasks(this);
            // Логика выключения плагина
            getLogger().info("BedWarsLC выключен.");
        }

        private void registerCommands() {
            // Регистрация команды для управления плагином
            getCommand("bedwars").setExecutor(new BedwarsCommand(arenaManager, lobbyManager, gameManager));
            getCommand("bw").setExecutor(new BedwarsCommand(arenaManager, lobbyManager, gameManager)); // Алиас
        }

        private void registerEvents() {
            TeamSetupListener teamSetupListener = new TeamSetupListener(arenaManager);
            getServer().getPluginManager().registerEvents(new SpawnProtectionListener(gameManager, arenaManager), this);
            getServer().getPluginManager().registerEvents(new MenuListener(arenaManager, creationHandler), this);
            getServer().getPluginManager().registerEvents(new ArenaPvPListener(arenaManager, gameManager), this);
            getServer().getPluginManager().registerEvents(teamSetupListener, this); // Используем единый экземпляр
            getServer().getPluginManager().registerEvents(new ArenaChatListener(creationHandler), this);
            getServer().getPluginManager().registerEvents(new SpawnSetupListener(arenaManager), this);
            getServer().getPluginManager().registerEvents(new LobbyItemListener(arenaManager, lobbyManager), this);
            getServer().getPluginManager().registerEvents(new MainLobbyListener(this), this);
            getServer().getPluginManager().registerEvents(new RegionSelectionListener(arenaManager), this);
            getServer().getPluginManager().registerEvents(new GameListener(gameManager, arenaManager), this);
            getServer().getPluginManager().registerEvents(new ArenaEditListener(arenaManager, teamSetupListener), this);

            // Отладка (можно убрать после проверки)
            getLogger().info("Все события успешно зарегистрированы!");
        }
        public ArenaManager getArenaManager() {
            return arenaManager;
        }

        public GameManager getGameManager() {
            return gameManager;
        }

        public static BedWarsLC getInstance() {
            return instance;
        }

    }
