package alsclinic.patient;

import bitalino.Bitalino;
import bitalino.BITalinoErrorTypes;
import bitalino.BITalinoException;
import bitalino.Frame;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Real Bitalino device for ALS_Clinic.
 *
 * <pre>
 * try (BitalinoManager bitalino = new BitalinoManager("20:16:02:14:75:76", 1000,
 *         List.of(AcquisitionChannel.of(SensorType.EMG), AcquisitionChannel.of(SensorType.EEG)))) {
 *     bitalino.start();
 *     SignalBlock block = bitalino.read(1000);   // 1 s of data
 *     bitalino.stop();
 * }
 * </pre>
 *
 * Disconnection handling: every read runs on a helper thread with a time limit. If no data arrives within
 * (block duration + {@value #READ_MARGIN_MS} ms) the Bluetooth link is closed and an
 * ACQUISITION_TIMEOUT BITalinoException is thrown, so the app never hangs when the
 * device is switched off or goes out of range. A broken stream throws LOST_COMMUNICATION.
 */
public class BitalinoManager implements SignalSource {

    /** Extra time allowed on top of the expected block duration before declaring the device lost. */
    public static final long READ_MARGIN_MS = 5000;

    private final Bitalino device = new Bitalino();
    private final int samplingRateHz;
    private final List<AcquisitionChannel> channels;
    /** For each channel, its position inside Frame.analog[] (device returns inputs in ascending order). */
    private final int[] framePosition;
    private final int[] analogInputs;

    private Instant acquisitionStart;
    private long samplesRead;
    private int lastSeq = -1;
    private long totalDroppedFrames;
    private volatile boolean closed;

    /**
     * Connects to the device (takes ~3 s).
     *
     * @param bluetoothAddress MAC address "xx:xx:xx:xx:xx:xx"
     * @param samplingRateHz   1, 10, 100 or 1000 (use 1000 for EMG/EEG)
     * @param channels         signals to acquire, each on a different input A1..A4
     */
    public BitalinoManager(String bluetoothAddress, int samplingRateHz, List<AcquisitionChannel> channels)
            throws BITalinoException {
        if (!Bitalino.isSupportedSamplingRate(samplingRateHz)) {
            throw new BITalinoException(BITalinoErrorTypes.SAMPLING_RATE_NOT_DEFINED);
        }
        if (channels == null || channels.isEmpty()) {
            throw new BITalinoException(BITalinoErrorTypes.ANALOG_CHANNELS_NOT_VALID);
        }
        Set<Integer> used = new HashSet<>();
        for (AcquisitionChannel ch : channels) {
            if (!used.add(ch.analogInput())) {
                throw new BITalinoException(BITalinoErrorTypes.ANALOG_CHANNELS_NOT_VALID);
            }
        }

        this.samplingRateHz = samplingRateHz;
        this.channels = List.copyOf(channels);
        this.analogInputs = this.channels.stream().mapToInt(AcquisitionChannel::analogInput).toArray();

        int[] sorted = analogInputs.clone();
        Arrays.sort(sorted);
        this.framePosition = new int[analogInputs.length];
        for (int c = 0; c < analogInputs.length; c++) {
            framePosition[c] = Arrays.binarySearch(sorted, analogInputs[c]);
        }

        device.open(bluetoothAddress, samplingRateHz);
    }

    @Override
    public int getSamplingRateHz() {
        return samplingRateHz;
    }

    @Override
    public List<AcquisitionChannel> getChannels() {
        return channels;
    }

    public long getTotalDroppedFrames() {
        return totalDroppedFrames;
    }

    public boolean isConnected() {
        return !closed && device.isOpen();
    }

    @Override
    public void start() throws BITalinoException {
        if (closed) {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        device.start(analogInputs);
        acquisitionStart = Instant.now();
        samplesRead = 0;
        lastSeq = -1;
        totalDroppedFrames = 0;
    }

    @Override
    public SignalBlock read(int nSamples) throws BITalinoException {
        if (closed) {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        if (!device.isAcquiring()) {
            throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IN_ACQUISITION_MODE);
        }
        if (nSamples <= 0) {
            throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
        }

        long timeoutMs = (nSamples * 1000L) / samplingRateHz + READ_MARGIN_MS;

        // device.read() blocks until the data arrives. It runs on a helper thread so that
        // this thread can wait for it with a time limit (join) instead of waiting forever.
        Frame[][] result = new Frame[1][];
        BITalinoException[] failure = new BITalinoException[1];
        Thread readerThread = new Thread(() -> {
            try {
                result[0] = device.read(nSamples);
            } catch (BITalinoException e) {
                failure[0] = e;
            } catch (RuntimeException e) {
                failure[0] = new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION, e);
            }
        }, "bitalino-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        try {
            readerThread.join(timeoutMs);           // wait at most timeoutMs milliseconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
            throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION, e);
        }

        if (readerThread.isAlive()) {               // still waiting: the device went silent
            close();                                // closing the socket unblocks the stuck read
            throw new BITalinoException(BITalinoErrorTypes.ACQUISITION_TIMEOUT);
        }
        if (failure[0] != null) {                   // the read failed
            close();
            throw failure[0];
        }
        Frame[] frames = result[0];

        int[][] raw = new int[channels.size()][nSamples];
        int dropped = 0;
        for (int i = 0; i < frames.length; i++) {
            Frame f = frames[i];
            if (lastSeq >= 0) {
                int expected = (lastSeq + 1) & 0x0F;
                dropped += (f.seq - expected) & 0x0F; // 4-bit sequence counter
            }
            lastSeq = f.seq;
            for (int c = 0; c < channels.size(); c++) {
                raw[c][i] = f.analog[framePosition[c]];
            }
        }

        SignalBlock block = new SignalBlock(acquisitionStart, samplesRead, samplingRateHz, channels, raw, dropped);
        samplesRead += nSamples;
        totalDroppedFrames += dropped;
        return block;
    }

    @Override
    public void stop() {
        try {
            device.stop();
        } catch (BITalinoException ignored) {
            // device already gone: nothing else to do
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            device.close();
        } catch (BITalinoException ignored) {
            // close is best-effort
        }
    }
}