package alsclinic.patient;

import bitalino.BITalino;
import bitalino.BITalinoException;

public class BitalinoManager implements AutoCloseable {
    private final BITalino device = new BITalino();

    public BitalinoManager(String bluetoothAddress, int samplingRateHz)
            throws BITalinoException {
        device.open(bluetoothAddress, samplingRateHz);
    }

    @Override
    public void close() throws BITalinoException {
        device.close();
    }
}