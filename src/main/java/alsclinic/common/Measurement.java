package alsclinic.common;

import java.time.Instant;

public class Measurement {
    private final String patientId;
    private final String sessionId;
    private final String timestamp;
    private final int samplingRateHz;
    private final int[]ecg;
    private final int[] emg;
    public Measurement(String patientId, String sessionId, int samplingRateHz, int[] ecg, int[] emg) {
        if (samplingRateHz<=0){
            throw new IllegalArgumentException("Sampling rate must be greater than 0");
        }
        if (ecg == null || emg == null||ecg.length == 0||ecg.length!=emg.length) {
            throw new IllegalArgumentException("Invalid signal samples");
        }
        this.patientId = patientId;
        this.sessionId = sessionId;
        this.timestamp = Instant.now().toString();
        this.samplingRateHz = samplingRateHz;
        this.ecg = ecg;
        this.emg = emg;
    }
    public String getPatientId() {
        return patientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getSamplingRateHz() {
        return samplingRateHz;
    }

    public int[] getEcg() {
        return ecg;
    }

    public int[] getEmg() {
        return emg;
    }
}
