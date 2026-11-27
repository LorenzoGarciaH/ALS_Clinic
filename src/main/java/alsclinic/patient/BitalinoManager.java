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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.;
import java.util.concurrent.TimeoutException;

public class BitalinoManager implements SignalSource {

    /** Extra time allowed on top of the expected block duration before declaring the device lost. */
    public static final long READ_MARGIN_MS = 5000;

    private final Bitalino device = new Bitalino();
    private final int samplingRateHz;
    private final List<AcquisitionChannel> channels;
    /** For each channel, its position inside Frame.analog[] (device returns inputs in ascending order). */
    private final int[] framePosition;
    private final int[] analogInputs;

    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bitalino-reader");
        t.setDaemon(true);
        return t;
    });

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

        try {
            device.open(bluetoothAddress, samplingRateHz);
        } catch (BITalinoException e) {
            reader.shutdownNow();
            throw e;
        }
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
        Future<Frame[]> pending = reader.submit(() -> device.read(nSamples));
        Frame[] frames;
        try {
            frames = pending.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.cancel(true);
            close(); // closing the socket unblocks the stuck read
            throw new BITalinoException(BITalinoErrorTypes.ACQUISITION_TIMEOUT, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            close();
            if (cause instanceof BITalinoException be) {
                throw be;
            }
            throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION, cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.cancel(true);
            throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION, e);
        }

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
        reader.shutdownNow();
    }
}
