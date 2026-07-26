package dev.aaf.parkourArea.zone;

import dev.aaf.parkourArea.concurrency.Scheduler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * zones.yml 持久化。所有写操作在 global region 线程发起（改 tree），磁盘写盘在 async 线程。
 * 保存采用「写 .tmp 再原子替换」避免半写。reload 时整体替换 tree 引用（COW）。
 */
public final class ZoneRepository {

    private final JavaPlugin plugin;
    private final Scheduler scheduler;
    private final File file;
    private volatile ZoneTree tree = new ZoneTree();
    private final ZoneIdAllocator idAllocator = new ZoneIdAllocator(1);

    public ZoneRepository(JavaPlugin plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.file = new File(plugin.getDataFolder(), "zones.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("zones.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ZoneTree newTree = new ZoneTree();
        ZoneIdAllocator newAlloc = new ZoneIdAllocator(cfg.getInt("next-zone-id", 1));

        List<Map<?, ?>> raw = cfg.getMapList("zones");
        for (Map<?, ?> m : raw) {
            try {
                Zone z = parseZone(m);
                if (z != null) {
                    newTree.add(z);
                    newAlloc.reserve(z.id());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("跳过无法解析的区域: " + e.getMessage());
            }
        }
        this.tree = newTree;
        this.idAllocator.setNext(newAlloc.peek());
        plugin.getLogger().info("已加载 " + newTree.size() + " 个区域");
    }

    public void reload() {
        load();
    }

    @SuppressWarnings("unchecked")
    private Zone parseZone(Map<?, ?> m) {
        int id = toInt(m.get("id"), -1);
        if (id < 0) {
            throw new IllegalArgumentException("无效 id");
        }
        Object nameObj = m.get("name");
        String name = nameObj == null ? "" : String.valueOf(nameObj);
        ZoneType type = ZoneType.parse(String.valueOf(m.get("type")));
        SelectionShape shape = SelectionShape.parse(String.valueOf(m.get("shape")));
        if (type == null || shape == null) {
            throw new IllegalArgumentException("无效 type/shape");
        }
        UUID world = parseUuid(m.get("world"));
        Integer parent = parseParent(m.get("parent"));
        if (shape == SelectionShape.CUBOID) {
            int[] p1 = coords(m.get("pos1"));
            int[] p2 = coords(m.get("pos2"));
            return Zone.cuboid(id, name, type, world, parent, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]);
        }
        double[] c = coordsD(m.get("center"));
        double r = toDouble(m.get("radius"), 1.0);
        return Zone.sphere(id, name, type, world, parent, c[0], c[1], c[2], r);
    }

    @SuppressWarnings("unchecked")
    private int[] coords(Object o) {
        if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            return new int[]{toInt(m.get("x"), 0), toInt(m.get("y"), 0), toInt(m.get("z"), 0)};
        }
        return new int[]{0, 0, 0};
    }

    @SuppressWarnings("unchecked")
    private double[] coordsD(Object o) {
        if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            return new double[]{toDouble(m.get("x"), 0), toDouble(m.get("y"), 0), toDouble(m.get("z"), 0)};
        }
        return new double[]{0, 0, 0};
    }

    private UUID parseUuid(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(o));
        } catch (Exception e) {
            throw new IllegalArgumentException("无效 world uuid: " + o);
        }
    }

    private Integer parseParent(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        String s = String.valueOf(o);
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public ZoneTree tree() {
        return tree;
    }

    public ZoneIdAllocator idAllocator() {
        return idAllocator;
    }

    /** 创建区域（global region 线程）。校验通过后分配 id、加入 tree、异步保存。 */
    public ValidationResult add(ZoneSpec spec, Consumer<Zone> onCreate) {
        Zone temp = spec.toTempZone();
        Zone parent = spec.parentId() == null ? null : tree.getById(spec.parentId());
        ValidationResult r = ZoneHierarchy.canCreate(temp, parent, tree);
        if (!r.valid()) {
            return r;
        }
        int id = idAllocator.next();
        Zone real = spec.toZone(id);
        tree.add(real);
        saveAsync();
        if (onCreate != null) {
            onCreate.accept(real);
        }
        return ValidationResult.ok();
    }

    /** 删除区域及其所有后代。返回删除的总数（含自身），0 表示不存在。 */
    public int delete(int id) {
        Zone z = tree.getById(id);
        if (z == null) {
            return 0;
        }
        List<Zone> desc = tree.descendentsOf(id);
        for (Zone d : desc) {
            tree.remove(d.id());
        }
        saveAsync();
        return desc.size();
    }

    public boolean rename(int id, String newName) {
        Zone z = tree.getById(id);
        if (z == null) {
            return false;
        }
        tree.remove(id);
        z.rename(newName);
        tree.add(z);
        saveAsync();
        return true;
    }

    private void saveAsync() {
        scheduler.runAsync(this::saveNow);
    }

    private synchronized void saveNow() {
        FileConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> zoneList = new ArrayList<>();
        for (Zone z : tree.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", z.id());
            m.put("name", z.name());
            m.put("type", z.type().name());
            m.put("shape", z.shape().name());
            m.put("world", z.worldUid() == null ? "" : z.worldUid().toString());
            m.put("parent", z.parentId());
            if (z.shape() == SelectionShape.CUBOID) {
                m.put("pos1", coordsMap(z.minX(), z.minY(), z.minZ()));
                m.put("pos2", coordsMap(z.maxX(), z.maxY(), z.maxZ()));
            } else {
                m.put("center", coordsMapD(z.centerX(), z.centerY(), z.centerZ()));
                m.put("radius", z.radius());
            }
            zoneList.add(m);
        }
        cfg.set("zones", zoneList);
        cfg.set("next-zone-id", idAllocator.peek());

        File tmp = new File(file.getParentFile(), "zones.yml.tmp");
        try {
            cfg.save(tmp);
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // ATOMIC_MOVE 在某些 FS 不支持，回退普通替换
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                plugin.getLogger().severe("保存 zones.yml 失败: " + e2.getMessage());
            }
        }
    }

    private Map<String, Object> coordsMap(int x, int y, int z) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        return m;
    }

    private Map<String, Object> coordsMapD(double x, double y, double z) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        return m;
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double toDouble(Object o, double def) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o == null) {
            return def;
        }
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
