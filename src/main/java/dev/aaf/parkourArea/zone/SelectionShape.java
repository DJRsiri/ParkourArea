package dev.aaf.parkourArea.zone;

/** 选区形状。cuboid 所有类型可用；sphere 仅 START/END 可用。 */
public enum SelectionShape {

    CUBOID,
    SPHERE;

    public static SelectionShape parse(String s) {
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
