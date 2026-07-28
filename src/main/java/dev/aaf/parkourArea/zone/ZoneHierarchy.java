package dev.aaf.parkourArea.zone;

/**
 * 区域父子层级校验。
 *
 * <p>规则：
 * <ol>
 *   <li>无父级 → 只能是 GLOBAL，且不与同世界已有 GLOBAL 几何相交</li>
 *   <li>有父级 → 同世界、type ∈ parent.allowedChildren、完全几何包含于父级</li>
 *   <li>不与同父级任意子区域相交（避免重叠混乱）</li>
 * </ol>
 * 不限制同 type 子区域数量（支持 GLOBAL 下多个 LEVEL 多关）。</p>
 */
public final class ZoneHierarchy {

    private ZoneHierarchy() {}

    public static ValidationResult canCreate(Zone newZone, Zone parent, ZoneTree tree) {
        if (parent == null) {
            if (newZone.type() != ZoneType.GLOBAL) {
                return ValidationResult.fail("无父级时只能创建 GLOBAL 区域");
            }
            for (Zone z : tree.all()) {
                if (z.type() == ZoneType.GLOBAL
                        && java.util.Objects.equals(z.worldUid(), newZone.worldUid())
                        && ZoneContainment.intersects(z, newZone)) {
                    return ValidationResult.fail("与同世界 GLOBAL 区域 #" + z.id() + " 几何相交");
                }
            }
            return ValidationResult.ok();
        }

        if (!parent.worldUid().equals(newZone.worldUid())) {
            return ValidationResult.fail("子区域必须与父区域位于同一世界");
        }
        if (!parent.type().allowedChildren().contains(newZone.type())) {
            return ValidationResult.fail(parent.type() + " 下不允许创建 " + newZone.type() + " 区域");
        }
        if (!ZoneContainment.fullyContained(parent, newZone)) {
            return ValidationResult.fail("区域必须完全位于父区域 (#" + parent.id() + ") 内");
        }
        for (Zone sibling : tree.childrenOf(parent.id())) {
            if (sibling.id() == newZone.id()) {
                continue;
            }
            if (ZoneContainment.intersects(sibling, newZone)) {
                return ValidationResult.fail("与同级区域 #" + sibling.id() + " (" + sibling.type() + ") 几何相交");
            }
        }
        return ValidationResult.ok();
    }

    /**
     * resize 校验（edit resize 命令）。{@code newGeo} 为携带新几何的临时 Zone
     *（id/type/world/parent 与 current 相同，shape 已由调用方保证一致）。
     *
     * <p>规则：有父级 → 新范围仍 ⊆ 父级且不与同级相交；GLOBAL → 不与同世界其他
     * GLOBAL 相交（均排除自己）；所有子区域仍 ⊆ 新范围（resize 特有）。</p>
     */
    public static ValidationResult canResize(Zone current, Zone newGeo, ZoneTree tree) {
        if (current.parentId() != null) {
            Zone parent = tree.getById(current.parentId());
            if (parent != null && !ZoneContainment.fullyContained(parent, newGeo)) {
                return ValidationResult.fail("区域必须完全位于父区域 (#" + parent.id() + ") 内");
            }
            for (Zone sibling : tree.childrenOf(current.parentId())) {
                if (sibling.id() == current.id()) {
                    continue;
                }
                if (ZoneContainment.intersects(sibling, newGeo)) {
                    return ValidationResult.fail("与同级区域 #" + sibling.id() + " (" + sibling.type() + ") 几何相交");
                }
            }
        } else {
            for (Zone z : tree.all()) {
                if (z.id() == current.id()) {
                    continue;
                }
                if (z.type() == ZoneType.GLOBAL
                        && java.util.Objects.equals(z.worldUid(), current.worldUid())
                        && ZoneContainment.intersects(z, newGeo)) {
                    return ValidationResult.fail("与同世界 GLOBAL 区域 #" + z.id() + " 几何相交");
                }
            }
        }
        for (Zone child : tree.childrenOf(current.id())) {
            if (!ZoneContainment.fullyContained(newGeo, child)) {
                return ValidationResult.fail("子区域 #" + child.id() + " (" + child.type() + ") 将超出新范围");
            }
        }
        return ValidationResult.ok();
    }
}
