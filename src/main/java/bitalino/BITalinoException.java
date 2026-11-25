package bitalino;

public class BITalinoException extends Exception {

    private static final long serialVersionUID = 1L;

    public int code;

    private final BITalinoErrorTypes errorType;

    public BITalinoException(BITalinoErrorTypes errorType) {
        this(errorType, null);
    }

    /** Keeps the original low-level error (e.g. the Bluetooth IOException) as the cause. */
    public BITalinoException(BITalinoErrorTypes errorType, Throwable cause) {
        super(errorType.getName(), cause);
        this.errorType = errorType;
        this.code = errorType.getValue();
    }

    public BITalinoErrorTypes getErrorType() {
        return errorType;
    }
}
