package alsclinic.patient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * A block of consecutive samples read from the device (by default 1 second of data).
 *
 * Time reference: sample k of this block was taken at
 *   acquisitionStart + (firstSampleIndex + k) / samplingRateHz seconds.
 *
 * @param acquisitionStart  instant the acquisition started (same for every block of a session)
 * @param firstSampleIndex  index of the first sample of this block since acquisition start
 * @param samplingRateHz    sampling frequency
 * @param channels          what each row of {@code raw} is
 * @param raw               raw ADC values, raw[channel][sample], 0..1023
 * @param droppedFrames     frames lost in Bluetooth before/inside this block (from sequence numbers)
 */
public record SignalBlock(Instant acquisitionStart,
                          long firstSampleIndex,
                          int samplingRateHz,
                          List<AcquisitionChannel> channels,
                          int[][] raw,
                          int droppedFrames) {

    public int sampleCount() {
        return raw.length == 0 ? 0 : raw[0].length;
    }

    /** Time of the first sample of this block. */
    public Instant firstSampleTime() {
        long nanos = Math.round(firstSampleIndex * 1e9 / samplingRateHz);
        return acquisitionStart.plus(Duration.ofNanos(nanos));
    }

    /** Raw samples of a sensor, or null if that sensor is not in this block. */
    public int[] rawOf(SensorType sensor) {
        for (int c = 0; c < channels.size(); c++) {
            if (channels.get(c).sensor() == sensor) {
                return raw[c];
            }
        }
        return null;
    }

    /** Samples of a sensor converted to its physical unit, or null if not present. */
    public double[] physicalOf(SensorType sensor) {
        int[] r = rawOf(sensor);
        return r == null ? null : sensor.toPhysical(r);
    }
}
