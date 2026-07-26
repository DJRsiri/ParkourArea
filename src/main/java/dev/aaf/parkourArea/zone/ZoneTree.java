package dev.aaf.parkourArea.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 区域内存索引。按 world 分桶存储，提供优先级查询与父子链查询。
 *
 * <p><b>线程约定</b>：仅在 global region 线程访问（增删改查）。
 * reload 时由 {@code ZoneRepository} 整体替换引用（COW 语义），不影响并发查询。</p>
 */
public final class ZoneTree {

    private final Map<Integer, Zone> byId = new LinkedHashMap<>();
    private final Map<String, Zone> byName = new java.util.HashMap<>();
    private final Map<UUID, List<Zone>> byWorld = new java.util.HashMap<>();

    public void add(Zone zone) {
        byId.put(zone.id(), zone);
        if (!zone.name().isEmpty()) {
            byName.put(zone.name().toLowerCase(Locale.ROOT), zone);
        }
        byWorld.computeIfAbsent(zone.worldUid(), k -> new ArrayList<>()).add(zone);
    }

    public void remove(int id) {
        Zone z = byId.remove(id);
        if (z == null) {
            return;
        }
        if (!z.name().isEmpty()) {
            byName.remove(z.name().toLowerCase(Locale.ROOT));
        }
        List<Zone> list = byWorld.get(z.worldUid());
        if (list != null) {
            list.remove(z);
        }
    }

    public Zone getById(int id) {
        return byId.get(id);
    }

    public Zone getByName(String name) {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    /** 根据 id 或名称查找（名称优先匹配，再尝试数字 id）。 */
    public Zone resolve(String idOrName) {
        if (idOrName == null || idOrName.isEmpty()) {
            return null;
        }
        Zone byNameZone = getByName(idOrName);
        if (byNameZone != null) {
            return byNameZone;
        }
        try {
            return getById(Integer.parseInt(idOrName));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Collection<Zone> all() {
        return byId.values();
    }

    public List<Zone> byWorld(UUID world) {
        return byWorld.getOrDefault(world, Collections.emptyList());
    }

    public int size() {
        return byId.size();
    }

    /**
     * 返回某点优先级最高的命中区域（START=END > LEVEL > LOBBY > GLOBAL）。
     * 同优先级时返回插入顺序中先遇到者（START/END 同物理区域只应创建一个）。
     */
    public Zone findMostSpecific(UUID world, double px, double py, double pz) {
        List<Zone> zones = byWorld.get(world);
        if (zones == null || zones.isEmpty()) {
            return null;
        }
        Zone best = null;
        int bestPri = -1;
        for (Zone z : zones) {
            if (z.containsPoint(px, py, pz)) {
                int pri = z.type().priority();
                if (pri > bestPri) {
                    bestPri = pri;
                    best = z;
                }
            }
        }
        return best;
    }

    /** 返回某点从最具体到 GLOBAL 的区域链（info-all 用）。 */
    public List<Zone> findChain(UUID world, double px, double py, double pz) {
        Zone most = findMostSpecific(world, px, py, pz);
        List<Zone> chain = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Zone cur = most;
        while (cur != null && visited.add(cur.id())) {
            chain.add(cur);
            cur = cur.parentId() == null ? null : byId.get(cur.parentId());
        }
        return chain;
    }

    /** 某区域的直接子区域。 */
    public List<Zone> childrenOf(int parentId) {
        List<Zone> out = new ArrayList<>();
        for (Zone z : byId.values()) {
            if (z.parentId() != null && z.parentId() == parentId) {
                out.add(z);
            }
        }
        return out;
    }

    /** 某区域的所有后代（含自身），删除时用。 */
    public List<Zone> descendentsOf(int rootId) {
        List<Zone> out = new ArrayList<>();
        collectDescendents(rootId, out);
        return out;
    }

    private void collectDescendents(int id, List<Zone> out) {
        Zone z = byId.get(id);
        if (z == null) {
            return;
        }
        out.add(z);
        for (Zone child : childrenOf(id)) {
            collectDescendents(child.id(), out);
        }
    }
}
