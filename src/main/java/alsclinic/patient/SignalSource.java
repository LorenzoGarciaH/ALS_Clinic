package alsclinic.patient;

import bitalino.BITalinoException;

import java.util.List;

/**
 * Something that produces physiological samples: the real Bitalino
 * ({@link BitalinoManager}) or a simulator ({@link SimulatedSignalSource})
 * used to test several concurrent patients without hardware.
 */
public interface SignalSource extends AutoCloseable {

    int getSamplingRateHz();

    List<AcquisitionChannel> getChannels();

    /** Starts acquiring. */
    void start() throws BITalinoException;

    /**
     * Blocks until {@code nSamples} samples per channel are available.
     * Throws if the device disconnects or stops sending data.
     */
    SignalBlock read(int nSamples) throws BITalinoException;

    /** Stops acquiring (never throws; safe if already stopped). */
    void stop();

    /** Releases the device (never throws; safe to call twice). */
    @Override
    void close();
}
