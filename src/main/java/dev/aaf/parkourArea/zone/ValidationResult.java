package dev.aaf.parkourArea.zone;

/** 区域操作校验结果。 */
public final class ValidationResult {

    private final boolean valid;
    private final String reason;

    private ValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult fail(String reason) {
        return new ValidationResult(false, reason);
    }

    public boolean valid() {
        return valid;
    }

    public String reason() {
        return reason == null ? "" : reason;
    }
}
