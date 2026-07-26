package dev.aaf.parkourArea;

import dev.aaf.parkourArea.actionbar.ActionBarService;
import dev.aaf.parkourArea.blocks.BlockCommandService;
import dev.aaf.parkourArea.command.CreateSubCommand;
import dev.aaf.parkourArea.command.DeleteSubCommand;
import dev.aaf.parkourArea.command.EditModeSubCommand;
import dev.aaf.parkourArea.command.EditSubCommand;
import dev.aaf.parkourArea.command.InfoSubCommand;
import dev.aaf.parkourArea.command.ListSubCommand;
import dev.aaf.parkourArea.command.ParkourCommand;
import dev.aaf.parkourArea.command.ReloadSubCommand;
import dev.aaf.parkourArea.command.ResetSaveSubCommand;
import dev.aaf.parkourArea.command.SoundSubCommand;
import dev.aaf.parkourArea.command.ToggleSoundSubCommand;
import dev.aaf.parkourArea.command.GuiSubCommand;
import dev.aaf.parkourArea.concurrency.Scheduler;
import dev.aaf.parkourArea.concurrency.UnifiedScheduler;
import dev.aaf.parkourArea.config.ConfigService;
import dev.aaf.parkourArea.config.Messages;
import dev.aaf.parkourArea.editmode.BuildGuard;
import dev.aaf.parkourArea.editmode.EditModeService;
import dev.aaf.parkourArea.event.EventBus;
import dev.aaf.parkourArea.hooks.protocollib.NoProtocolLib;
import dev.aaf.parkourArea.hooks.protocollib.ProtocolLibPresent;
import dev.aaf.parkourArea.hooks.protocollib.VisibilityHook;
import dev.aaf.parkourArea.hooks.worldedit.NoWorldEdit;
import dev.aaf.parkourArea.hooks.worldedit.WorldEditHook;
import dev.aaf.parkourArea.hooks.worldedit.WorldEditPresent;
import dev.aaf.parkourArea.hotbar.HotbarService;
import dev.aaf.parkourArea.listeners.ConnectionListener;
import dev.aaf.parkourArea.menu.MenuListener;
import dev.aaf.parkourArea.parkour.AntiIdleService;
import dev.aaf.parkourArea.parkour.CheckpointService;
import dev.aaf.parkourArea.parkour.LevelProgressService;
import dev.aaf.parkourArea.parkour.ParkourStateService;
import dev.aaf.parkourArea.parkour.RatingService;
import dev.aaf.parkourArea.parkour.TimerService;
import dev.aaf.parkourArea.persistence.CheckpointDao;
import dev.aaf.parkourArea.persistence.Database;
import dev.aaf.parkourArea.persistence.PlayerPreferenceDao;
import dev.aaf.parkourArea.persistence.PlayerBestDao;
import dev.aaf.parkourArea.persistence.PlayerProgressDao;
import dev.aaf.parkourArea.persistence.PlayerTimeDao;
import dev.aaf.parkourArea.player.PlayerRegionTracker;
import dev.aaf.parkourArea.player.PlayerSessionService;
import dev.aaf.parkourArea.util.ConfirmFlow;
import dev.aaf.parkourArea.visibility.PreferenceService;
import dev.aaf.parkourArea.visibility.VisibilityService;
import dev.aaf.parkourArea.zone.ZoneRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ParkourArea 主类。仅做装配与生命周期管理，业务逻辑在各 Service 中。
 *
 * <p>兼容 Paper 与 Folia：使用 {@link UnifiedScheduler} 桥接两平台共有的调度器 API。</p>
 */
public final class ParkourArea extends JavaPlugin {

    // ---- 基础设施 ----
    private Scheduler scheduler;
    private EventBus eventBus;
    private ConfirmFlow confirmFlow;
    private ConfigService configService;
    private Database database;

    // ---- DAO ----
    private PlayerTimeDao timeDao;
    private PlayerBestDao bestDao;
    private PlayerProgressDao progressDao;
    private CheckpointDao checkpointDao;
    private PlayerPreferenceDao preferenceDao;

    // ---- 区域 / 编辑 / 选区 ----
    private ZoneRepository zoneRepository;
    private EditModeService editModeService;
    private WorldEditHook worldEditHook;

    // ---- 玩家会话 / 工具栏 ----
    private HotbarService hotbarService;
    private PlayerSessionService sessionService;
    private PlayerRegionTracker regionTracker;

    // ---- 跑酷玩法（阶段 D） ----
    private LevelProgressService levelProgressService;
    private TimerService timerService;
    private RatingService ratingService;
    private ParkourStateService parkourStateService;
    private CheckpointService checkpointService;
    private AntiIdleService antiIdleService;
    private ActionBarService actionBarService;
    private BlockCommandService blockCommandService;
    private PreferenceService preferenceService;
    private VisibilityHook visibilityHook;
    private VisibilityService visibilityService;

    // ---- 命令 ----
    private ParkourCommand commandRoot;

    @Override
    public void onEnable() {
        this.scheduler = new UnifiedScheduler(this);
        this.eventBus = new EventBus();
        this.confirmFlow = new ConfirmFlow();

        this.configService = new ConfigService(this);
        this.configService.load();

        try {
            this.database = new Database(this);
            int topN = configService.settings().topRecordCount();
            this.timeDao = new PlayerTimeDao(database, topN);
            this.bestDao = new PlayerBestDao(database);
            this.progressDao = new PlayerProgressDao(database);
            this.checkpointDao = new CheckpointDao(database);
            this.preferenceDao = new PlayerPreferenceDao(database);
        } catch (Exception e) {
            getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.zoneRepository = new ZoneRepository(this, scheduler);
        this.zoneRepository.load();

        this.editModeService = new EditModeService(configService.settings().editModeDefault());
        WorldEditPresent weHook = WorldEditPresent.tryCreate();
        this.worldEditHook = weHook != null ? weHook : new NoWorldEdit();
        if (weHook != null) {
            getLogger().info("已接入 WorldEdit/FAWE 选区，create 命令可用选区");
        } else {
            getLogger().info("未检测到 WorldEdit/FAWE，create 命令需手动填写坐标参数");
        }

        this.commandRoot = new ParkourCommand(this);
        registerCommands();
        PluginCommand cmd = getCommand("parkour");
        if (cmd != null) {
            cmd.setExecutor(commandRoot);
            cmd.setTabCompleter(commandRoot);
        } else {
            getLogger().severe("parkour 命令未在 plugin.yml 注册！");
        }

        // 玩家会话、工具栏
        this.hotbarService = new HotbarService(this);
        this.sessionService = new PlayerSessionService(this, eventBus, zoneRepository, hotbarService);
        this.sessionService.init();

        // 跑酷玩法服务
        this.levelProgressService = new LevelProgressService(this);
        this.timerService = new TimerService(this);
        this.timerService.init();
        this.ratingService = new RatingService(this);
        this.parkourStateService = new ParkourStateService(this, levelProgressService);
        this.parkourStateService.init();
        this.checkpointService = new CheckpointService(this);
        this.checkpointService.init();
        this.antiIdleService = new AntiIdleService(this);
        this.antiIdleService.init();
        this.blockCommandService = new BlockCommandService(this);
        this.blockCommandService.reloadConfigs();
        this.blockCommandService.init();
        this.preferenceService = new PreferenceService(this);
        this.actionBarService = new ActionBarService(this, ratingService, timerService);

        // 区域跟踪与渲染（最后启动，确保所有事件订阅者已注册）
        this.regionTracker = new PlayerRegionTracker(this, sessionService);
        this.regionTracker.start();
        this.actionBarService.start();

        // 监听器
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new BuildGuard(this), this);

        // PlaceholderAPI 软依赖注册（独立类加载，避免主类硬依赖 PlaceholderExpansion）
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                dev.aaf.parkourArea.hooks.papi.PapiRegistration.register(this);
                getLogger().info("已注册 PlaceholderAPI 变量 (%parkour_*%)");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI 注册失败: " + t.getMessage());
            }
        }

        // ProtocolLib 软依赖（玩家可见性缩小挡位）
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            try {
                this.visibilityHook = new ProtocolLibPresent(this);
                if (visibilityHook.supportsScaling()) {
                    getLogger().info("已接入 ProtocolLib，可见性缩小挡位可用");
                } else {
                    getLogger().info("ProtocolLib 已加载但 scale 包探测失败，缩小挡位退化为不可见");
                }
            } catch (Throwable t) {
                getLogger().warning("ProtocolLib 接入失败，缩小退化为不可见: " + t.getMessage());
                this.visibilityHook = new NoProtocolLib(this);
            }
        } else {
            this.visibilityHook = new NoProtocolLib(this);
            getLogger().info("未检测到 ProtocolLib，可见性缩小挡位将退化为不可见");
        }

        // 可见性服务（事件驱动刷新关卡区域内其他玩家的可见性）
        this.visibilityService = new VisibilityService(this);
        this.visibilityService.init();

        getLogger().info("ParkourArea v" + getPluginMeta().getVersion()
                + " 已启用 (Paper/Folia 兼容, 区域数=" + zoneRepository.tree().size() + ")");
    }

    private void registerCommands() {
        commandRoot.register(new ReloadSubCommand(this));
        commandRoot.register(new GuiSubCommand(this));
        commandRoot.register(new ResetSaveSubCommand(this));
        commandRoot.register(new ToggleSoundSubCommand(this));
        commandRoot.register(new SoundSubCommand(this));
        commandRoot.register(new CreateSubCommand(this));
        commandRoot.register(new DeleteSubCommand(this));
        commandRoot.register(new InfoSubCommand(this, "info", false));
        commandRoot.register(new InfoSubCommand(this, "info-all", true));
        commandRoot.register(new ListSubCommand(this));
        commandRoot.register(new EditSubCommand(this));
        commandRoot.register(new EditModeSubCommand(this));
    }

    public void reloadAll() {
        configService.load();
        zoneRepository.reload();
        if (blockCommandService != null) {
            blockCommandService.reloadConfigs();
        }
    }

    @Override
    public void onDisable() {
        if (actionBarService != null) {
            actionBarService.stop();
        }
        if (regionTracker != null) {
            regionTracker.stop();
        }
        if (sessionService != null) {
            sessionService.shutdown();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("ParkourArea 已禁用");
    }

    // ---- getters ----

    public Scheduler scheduler() {
        return scheduler;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public ConfirmFlow confirmFlow() {
        return confirmFlow;
    }

    public ConfigService configService() {
        return configService;
    }

    public Messages messages() {
        return configService.messages();
    }

    public Database database() {
        return database;
    }

    public PlayerTimeDao timeDao() {
        return timeDao;
    }

    public PlayerBestDao bestDao() {
        return bestDao;
    }

    public PlayerProgressDao progressDao() {
        return progressDao;
    }

    public CheckpointDao checkpointDao() {
        return checkpointDao;
    }

    public PlayerPreferenceDao preferenceDao() {
        return preferenceDao;
    }

    public ZoneRepository zoneRepository() {
        return zoneRepository;
    }

    public EditModeService editModeService() {
        return editModeService;
    }

    public WorldEditHook worldEditHook() {
        return worldEditHook;
    }

    public HotbarService hotbarService() {
        return hotbarService;
    }

    public PlayerSessionService sessionService() {
        return sessionService;
    }

    public PlayerRegionTracker regionTracker() {
        return regionTracker;
    }

    public LevelProgressService progressService() {
        return levelProgressService;
    }

    public TimerService timerService() {
        return timerService;
    }

    public RatingService ratingService() {
        return ratingService;
    }

    public ParkourStateService parkourStateService() {
        return parkourStateService;
    }

    public CheckpointService checkpointService() {
        return checkpointService;
    }

    public AntiIdleService antiIdleService() {
        return antiIdleService;
    }

    public ActionBarService actionBarService() {
        return actionBarService;
    }

    public BlockCommandService blockCommandService() {
        return blockCommandService;
    }

    public PreferenceService preferenceService() {
        return preferenceService;
    }

    public VisibilityHook visibilityHook() {
        return visibilityHook;
    }

    public VisibilityService visibilityService() {
        return visibilityService;
    }

    public ParkourCommand commandRoot() {
        return commandRoot;
    }

    public boolean debug() {
        return configService != null && configService.settings().debug();
    }
}
