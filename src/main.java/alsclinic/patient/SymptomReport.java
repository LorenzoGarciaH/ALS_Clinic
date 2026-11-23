package alsclinic.patient;

import java.time.Instant;

public record SymptomReport(String symptoms, Instant submittedAt) {
}