package alsclinic.patient;

import java.io.*;
import java.util.Scanner;
import java.util.UUID;


public class PatientApp {

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);

        System.out.print("Patient ID: ");
        String patientId = console.nextLine().trim();

        if (!patientId.matches("[A-Za-z0-9_-]{1,64}")) {
            System.out.println("Use 1–64 letters, digits, underscores, or hyphens.");
            return;
        }

        System.out.print("Symptoms: ");
        String symptoms = console.nextLine().trim();

        if (symptoms.isEmpty()) {
            System.out.println("Enter a symptom report or type 'No symptoms'.");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        System.out.println("Session ID: " + sessionId);

        PatientClinic clinic = new PatientClinic();

        try {
            clinic.submitSession(patientId, sessionId, symptoms);
            System.out.println("Session submitted successfully.");
        } catch (IOException e) {
            System.err.println("Could not submit session: " + e.getMessage());
        }
    }

}
