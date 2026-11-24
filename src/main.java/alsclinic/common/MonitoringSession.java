package alsclinic.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MonitoringSession {
    private final String sessionId;
    private final String patientId;
    private final String startTime;
    private String endTime;
    private String symptoms;
    private final List<Measurement> measurements;
    public MonitoringSession(String patientId, String sessionId) {
        this.sessionId = sessionId;
        this.patientId = patientId;
        this.startTime = Instant.now().toString();
        this.measurements = new ArrayList<>();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }
    public void addMeasurement(Measurement measurement) {
        if (measurement == null||!patientId.equals(measurement.getPatientId())||!sessionId.equals(measurement.getSessionId())) {
            throw new IllegalArgumentException("Measurement does not belong to this session");
        }
        if (this.endTime != null) {
            throw new IllegalStateException("Cannot add measurements to a closed session");
        }
        this.measurements.add(measurement);
    }
    public void closeSession() {
        if (this.endTime != null) {
            throw new IllegalStateException("Session is already closed");
        }
        this.endTime = Instant.now().toString();
    }
    public boolean isActive() {
        return this.endTime == null;
    }
}
