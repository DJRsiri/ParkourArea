package dev.aaf.parkourArea.blocks;

import dev.aaf.parkourArea.ParkourArea;
import dev.aaf.parkourArea.concurrency.TaskHandle;
import dev.aaf.parkourArea.event.PlayerParkourTickEvent;
import dev.aaf.parkourArea.player.ParkourPlayer;
import dev.aaf.parkourArea.player.PlayerPhase;
import dev.aaf.parkourArea.util.Locations;
import dev.aaf.parkourArea.util.SilentCommandSender;
import dev.aaf.parkourArea.util.SoundPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 方块踩踏命令服务。检测玩家脚下方块（y-0.1），匹配 blocks.yml 配置时执行命令与音效。
 *
 * <p>非 repeat：方块变化时触发一次。repeat=true：持续站在方块上时按 interval（tick）重复触发（绑定玩家的 entity 周期任务，离开方块即取消）。</p>
 *
 * <p>命令以静默 sender 执行（反馈不刷屏、不动 gamerule），执行前清除玩家受伤冷却，
 * 保证 /damage 类命令不被原版受伤无敌帧吞掉。</p>
 */
public final class BlockCommandService {

    private final ParkourArea plugin;
    private final Map<String, BlockCommandConfig> configs = new HashMap<>();
    private final Map<UUID, Material> lastBlocks = new HashMap<>();
    private final Map<UUID, TaskHandle> repeatTasks = new HashMap<>();

    public BlockCommandService(ParkourArea plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.eventBus().subscribe(PlayerParkourTickEvent.class, this::onTick);
    }

    public void reloadConfigs() {
        configs.clear();
        FileConfiguration cfg = plugin.configService().blocks();
        ConfigurationSection blocks = cfg.getConfigurationSection("blocks");
        if (blocks == null) {
            return;
        }
        for (String key : blocks.getKeys(false)) {
            String path = "blocks." + key;
            List<String> command = cfg.getStringList(path + ".command");
            List<String> sound = cfg.getStringList(path + ".sound");
            boolean repeat = cfg.getBoolean(path + ".repeat", false);
            int interval = Math.max(1, cfg.getInt(path + ".interval", 20));
            configs.put(key.toUpperCase(Locale.ROOT), new BlockCommandConfig(command, sound, repeat, interval));
        }
    }

    private void onTick(PlayerParkourTickEvent e) {
        ParkourPlayer session = e.session();
        if (session.phase() == PlayerPhase.OUTSIDE || session.phase() == PlayerPhase.EDIT_MODE) {
            return;
        }
        Player player = Bukkit.getPlayer(e.playerId());
        if (player == null) {
            return;
        }
        Block below = Locations.blockBelowFeet(player);
        Material current = below.getType();
        Material last = lastBlocks.get(e.playerId());
        if (current == last) {
            return; // 脚下方块未变化
        }
        lastBlocks.put(e.playerId(), current);
        cancelRepeat(e.playerId());
        BlockCommandConfig cfg = configs.get(current.name());
        if (cfg == null) {
            return;
        }
        executeOnce(player, session, cfg);
        if (cfg.repeat()) {
            startRepeat(player, session, cfg);
        }
    }

    private void executeOnce(Player player, ParkourPlayer session, BlockCommandConfig cfg) {
        List<String> resolved = new java.util.ArrayList<>();
        for (String cmd : cfg.command()) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                resolved.add(VariableResolver.resolve(cmd, player));
            }
        }
        if (!resolved.isEmpty()) {
            // 先在 entity 线程清受伤冷却，再到 global 线程静默执行命令。
            // /damage 类命令受原版受伤无敌帧（noDamageTicks）限制，冷却内报
            // "Target is invulnerable" 结算失败——刚摔落/刚被机关伤到的玩家踩上去
            // 第一时间不生效；清冷却保证命令立即结算（repeat 间隔 < 20tick 的机关
            // 也依赖此举才能按 interval 稳定生效）。
            plugin.scheduler().runEntity(player, p -> {
                p.setNoDamageTicks(0);
                for (String r : resolved) {
                    plugin.scheduler().runGlobal(
                            () -> Bukkit.dispatchCommand(SilentCommandSender.console(), r));
                }
            }, () -> {});
        }
        // sound：玩家线程（受总开关与效果块子项控制）
        if (!cfg.sound().isEmpty() && session.shouldPlayBlockSound()) {
            plugin.scheduler().runEntity(player, p -> {
                for (String s : cfg.sound()) {
                    if (s != null && !s.trim().isEmpty()) {
                        SoundPlayer.play(p, s);
                    }
                }
            }, () -> {});
        }
    }

    private void startRepeat(Player player, ParkourPlayer session, BlockCommandConfig cfg) {
        UUID uuid = player.getUniqueId();
        TaskHandle task = plugin.scheduler().runEntityAtFixedRate(player,
                p -> executeOnce(p, session, cfg),
                () -> cancelRepeat(uuid),
                cfg.interval(), cfg.interval());
        repeatTasks.put(uuid, task);
    }

    private void cancelRepeat(UUID uuid) {
        TaskHandle t = repeatTasks.remove(uuid);
        if (t != null) {
            t.cancel();
        }
    }

    public void clear(UUID uuid) {
        cancelRepeat(uuid);
        lastBlocks.remove(uuid);
    }
}
