package alsclinic.common;

public class Patient {
    private final String patientId;
    public Patient(String patientId) {
        if (patientId == null || patientId.isEmpty()||!patientId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Patient invalid");
        }
        this.patientId = patientId;
    }
    public String getPatientId() {
        return patientId;
    }
}
