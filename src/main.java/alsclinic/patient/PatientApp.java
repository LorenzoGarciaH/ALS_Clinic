package alsclinic.patient;

import java.io.*;
import java.net.Socket;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Scanner;
import java.util.UUID;

public class PatientApp {

    public static void main(String[] args)  {
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
        try (Socket socket = new Socket("localhost", 9000);) {
            System.out.println("connected to " + socket.getInetAddress().getHostName());
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // START_SESSION
            JsonObject start = createMessage("START_SESSION", patientId, sessionId);
            output.println(start.toString());

            // PATIENT_REPORT
            JsonObject report = createMessage("PATIENT_REPORT", patientId, sessionId);
            report.addProperty("symptoms", symptoms);
            output.println(report.toString());

            // MEASUREMENT
            Gson gson = new Gson();
            JsonObject measurement = createMessage("MEASUREMENT", patientId, sessionId);
            measurement.add("eeg", gson.toJsonTree(new int[]{512, 514, 511}));
            measurement.add("emg", gson.toJsonTree(new int[]{500, 530, 502}));
            output.println(measurement.toString());

            // END_SESSION
            JsonObject end = createMessage("END_SESSION", patientId, sessionId);
            output.println(end.toString());



        } catch (IOException e) {
            System.err.println("could not connect to the hospital server: " + e.getMessage());
       }
    }

    private static JsonObject createMessage(String type, String patientId, String sessionId) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.addProperty("patientId", patientId);
        message.addProperty("sessionId", sessionId);
        return message;
    }
}
