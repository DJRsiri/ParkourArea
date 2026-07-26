package dev.aaf.parkourArea.zone;

import java.util.EnumSet;
import java.util.Set;

/** 区域类型，含优先级与父子合法性。优先级：START=END > LEVEL > LOBBY > GLOBAL。 */
public enum ZoneType {

    GLOBAL,
    LOBBY,
    LEVEL,
    START,
    END;

    /** 数值越大优先级越高（同一点多个区域命中时取最高优先级者）。 */
    public int priority() {
        return switch (this) {
            case START, END -> 5;
            case LEVEL -> 4;
            case LOBBY -> 3;
            case GLOBAL -> 1;
        };
    }

    /** 该类型允许的直接子区域类型集合。 */
    public Set<ZoneType> allowedChildren() {
        return switch (this) {
            case GLOBAL -> EnumSet.of(LOBBY, LEVEL);
            case LOBBY -> EnumSet.of(LEVEL);
            case LEVEL -> EnumSet.of(START, END);
            case START, END -> EnumSet.noneOf(ZoneType.class);
        };
    }

    /** 是否允许同级兄弟共存（START/END 在同一 LEVEL 下可共存，其他不行）。 */
    public boolean allowsSiblingOfSameType() {
        return this == START || this == END;
    }

    public static ZoneType parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
