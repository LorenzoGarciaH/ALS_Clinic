package alsclinic.patient;

/**
 * Sensors used to convert their raw Bitalino values
 * into physical units.
 *
 * Transfer function from the Bitalino sensor datasheets:
 *   value = ((ADC / 2^n) - 1/2) * VCC / G_sensor
 * with n = 10 bits, VCC = 3.3 V.
 * check sensors
 */
public enum SensorType {
    EMG("EMG", 0, 1009.0, 1e3, "mV"),
    ECG("ECG", 1, 1100.0, 1e3, "mV"),
    EEG("EEG", 3, 41782.0, 1e6, "uV");

    public static final double VCC = 3.3;
    public static final int ADC_BITS = 10;
    private static final double ADC_RANGE = 1 << ADC_BITS; // 1024

    private final String label;
    private final int defaultAnalogInput;
    private final double gain;
    private final double unitScale;
    private final String unit;

    SensorType(String label, int defaultAnalogInput, double gain, double unitScale, String unit) {
        this.label = label;
        this.defaultAnalogInput = defaultAnalogInput;
        this.gain = gain;
        this.unitScale = unitScale;
        this.unit = unit;
    }

    public String getLabel() { return label; }

    /** 0-based analog input (0 = A1 ... 3 = A4). */
    public int getDefaultAnalogInput() { return defaultAnalogInput; }

    public double getGain() { return gain; }

    /** Physical unit of {@link #toPhysical(int)}: "mV" or "uV". */
    public String getUnit() { return unit; }

    /** Converts one raw ADC sample (0..1023) into the sensor's physical unit. */
    public double toPhysical(int adc) {
        return ((adc / ADC_RANGE) - 0.5) * VCC / gain * unitScale;
    }

    public double[] toPhysical(int[] adc) {
        double[] out = new double[adc.length];
        for (int i = 0; i < adc.length; i++) {
            out[i] = toPhysical(adc[i]);
        }
        return out;
    }
}
