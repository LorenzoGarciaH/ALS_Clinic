package alsclinic.patient;

import bitalino.BITalinoException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs one monitoring-session acquisition:
 * reads the signal source block by block (1 s per block by default) and hands each
 * block to a listener (e.g. the code that sends a MEASUREMENT message to the server).
 *
 * The acquisition ends when:
 *  - the configured duration is reached            -> DURATION_REACHED
 *  - the patient calls {@link #requestStop()}       -> STOPPED_BY_PATIENT
 *  - the device disconnects / stops sending data    -> DEVICE_ERROR
 *  - the listener fails (e.g. server unreachable)   -> SEND_ERROR
 *
 * Blocks already handed to the listener are never touched again, so a failure in the
 * middle of a session does not affect data that was already sent and stored.
 * The source is always stopped at the end; it is NOT closed (the caller owns it).
 */
public class AcquisitionRecorder {
    public enum EndReason { DURATION_REACHED, STOPPED_BY_PATIENT, DEVICE_ERROR, SEND_ERROR }

    @FunctionalInterface
    public interface BlockListener {
        void onBlock(SignalBlock block) throws IOException;
    }

    /** Summary of a finished acquisition. {@code error} is null unless something failed. */
    public record Result(EndReason reason, Instant start, Instant end,
                         long samplesPerChannel, long droppedFrames, Exception error) {
        public boolean isSuccessful() {
            return reason == EndReason.DURATION_REACHED || reason == EndReason.STOPPED_BY_PATIENT;
        }
    }

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final double blockSeconds;

    public AcquisitionRecorder() {
        this(1.0);
    }

    /** @param blockSeconds how much data each block (and each MEASUREMENT message) contains */
    public AcquisitionRecorder(double blockSeconds) {
        if (blockSeconds <= 0) {
            throw new IllegalArgumentException("Block length must be positive");
        }
        this.blockSeconds = blockSeconds;
    }

    /** Can be called from another thread (e.g. when the patient presses Enter). */
    public void requestStop() {
        stopRequested.set(true);
    }

    /**
     * @param source      the Bitalino or the simulator (must be open)
     * @param maxDuration automatic end after this time; null = run until {@link #requestStop()}
     * @param listener    receives each block in order
     */
    public Result record(SignalSource source, Duration maxDuration, BlockListener listener) {
        stopRequested.set(false);
        int fs = source.getSamplingRateHz();
        int blockSize = Math.max(1, (int) Math.round(blockSeconds * fs));
        long targetSamples = maxDuration == null
                ? Long.MAX_VALUE
                : Math.max(1, maxDuration.toMillis() * fs / 1000);

        Instant start = Instant.now();
        long samples = 0;
        long dropped = 0;
        EndReason reason;
        Exception error = null;

        try {
            source.start();
            while (true) {
                if (stopRequested.get()) {
                    reason = EndReason.STOPPED_BY_PATIENT;
                    break;
                }
                if (samples >= targetSamples) {
                    reason = EndReason.DURATION_REACHED;
                    break;
                }
                int n = (int) Math.min(blockSize, targetSamples - samples);
                SignalBlock block = source.read(n);
                samples += block.sampleCount();
                dropped += block.droppedFrames();
                listener.onBlock(block);
            }
        } catch (BITalinoException e) {
            reason = EndReason.DEVICE_ERROR;
            error = e;
        } catch (IOException e) {
            reason = EndReason.SEND_ERROR;
            error = e;
        } finally {
            source.stop();
        }
        return new Result(reason, start, Instant.now(), samples, dropped, error);
    }
}
