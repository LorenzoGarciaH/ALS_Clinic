package alsclinic.patient;

/**
 * One acquired signal: which sensor it is and which Bitalino analog input it is plugged into.
 *
 * @param sensor      sensor type (gives label, unit and conversion)
 * @param analogInput 0-based analog input, 0 = A1 ... 3 = A4 (only the 10-bit inputs are allowed)
 */
public record AcquisitionChannel(SensorType sensor, int analogInput) {

    public AcquisitionChannel {
        if (sensor == null) {
            throw new IllegalArgumentException("Sensor type is required");
        }
        if (analogInput < 0 || analogInput > 3) {
            throw new IllegalArgumentException(
                    "Analog input must be A1..A4 (0..3); A5/A6 are 6-bit and not supported here");
        }
    }

    /** Uses the sensor's default input (see {@link SensorType}). */
    public static AcquisitionChannel of(SensorType sensor) {
        return new AcquisitionChannel(sensor, sensor.getDefaultAnalogInput());
    }

    /** Human-readable input name, e.g. "A1". */
    public String inputName() {
        return "A" + (analogInput + 1);
    }
}
