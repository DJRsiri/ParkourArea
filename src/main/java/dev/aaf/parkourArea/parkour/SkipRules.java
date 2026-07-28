package dev.aaf.parkourArea.parkour;

import dev.aaf.parkourArea.player.PlayerPhase;

/**
 * 跳关判定纯函数（可单测）。跳关检测维度（{@code settings.skip-detection} 总开关）：
 * <ol>
 *   <li>顺序：进入高于"第一个未通关关"的关卡起点（{@code allow-any-selectable} 开启时豁免）；</li>
 *   <li>计时：已选关但未从起点出发（AT_START/LEVEL_SELECTED）就到达该关终点；</li>
 *   <li>归属：途经/回走非所选关的起点与终点不做任何惩罚与状态切换（防误报）。</li>
 * </ol>
 */
public final class SkipRules {

    private SkipRules() {}

    /**
     * 进入 START 是否判跳关：检测开启、非自由选关、且关 id 超过第一个未通关关
     * （nextExpected=-1 表示全部通关，不判）。
     */
    public static boolean startIsSkip(boolean detectionEnabled, boolean allowAnySelectable,
                                      int levelId, int nextExpected) {
        return detectionEnabled && !allowAnySelectable
                && nextExpected != -1 && levelId > nextExpected;
    }

    /**
     * 进入 START 是否应忽略：RUNNING 中进入非所选关的起点
     * （回走低关/途经高关——不切换状态、不惩罚；本关起点不在此列）。
     */
    public static boolean startIgnored(PlayerPhase phase, Integer selectedLevelId, int levelId) {
        return phase == PlayerPhase.RUNNING
                && selectedLevelId != null && selectedLevelId.intValue() != levelId;
    }

    /** 进入 END 是否应完全忽略：未选关（闲逛），或终点不属于所选关（途经/回走）。 */
    public static boolean endIgnored(Integer selectedLevelId, int endLevelId) {
        return selectedLevelId == null || selectedLevelId.intValue() != endLevelId;
    }

    /**
     * 进入所选关 END 且非 RUNNING 是否判跳关：仅 AT_START/LEVEL_SELECTED
     * （选关后未从起点出发就到终点）；COMPLETED（通关后停留）/INVALIDATED 不惩罚。
     */
    public static boolean endIsSkip(boolean detectionEnabled, PlayerPhase phase) {
        return detectionEnabled
                && (phase == PlayerPhase.AT_START || phase == PlayerPhase.LEVEL_SELECTED);
    }
}
