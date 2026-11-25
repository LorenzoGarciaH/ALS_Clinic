package alsclinic.patient;

import bitalino.BITalinoException;

public class BitalinoManager implements AutoCloseable {
    private final bitalino.Bitalino device = new bitalino.Bitalino();

    public BitalinoManager(String bluetoothAddress, int samplingRateHz)
            throws BITalinoException {
        device.open(bluetoothAddress, samplingRateHz);
    }

    @Override
    public void close() throws BITalinoException {
        device.close();
    }
}