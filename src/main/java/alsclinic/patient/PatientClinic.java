package alsclinic.patient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class PatientClinic {
    private static final int REPLY_TIMEOUT_MS = 5000;

    public void submitSession(String patientId, String sessionId, String symptoms)
            throws IOException {
        try (Socket socket = new Socket("localhost", 9000)) {
            socket.setSoTimeout(REPLY_TIMEOUT_MS);

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            JsonObject start = createMessage("START_SESSION", patientId, sessionId);
            start.addProperty("startedAt", Instant.now().toString());
            sendAndRequireOk(start, output, input);

            SymptomReport symptomReport = new SymptomReport(symptoms, Instant.now());
            JsonObject report = createMessage("PATIENT_REPORT", patientId, sessionId);
            report.addProperty("symptoms", symptomReport.symptoms());
            report.addProperty("submittedAt", symptomReport.submittedAt().toString());
            sendAndRequireOk(report, output, input);

            Gson gson = new Gson();
            JsonObject measurement = createMessage("MEASUREMENT", patientId, sessionId);
            measurement.add("eeg", gson.toJsonTree(new int[]{512, 514, 511}));
            measurement.add("emg", gson.toJsonTree(new int[]{500, 530, 502}));
            sendAndRequireOk(measurement, output, input);
            // Prueba para ver si pueden estar conectados a la vez
            System.out.println("Session active. Waiting 120 seconds...");
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Session interrupted", e);
            }


            JsonObject end = createMessage("END_SESSION", patientId, sessionId);
            end.addProperty("endedAt", Instant.now().toString());
            sendAndRequireOk(end, output, input);
        }
    }

    private static void sendAndRequireOk(
            JsonObject message, PrintWriter output, BufferedReader input)
            throws IOException {
        String type = message.get("type").getAsString();

        output.println(message);
        if (output.checkError()) {
            throw new IOException(type + ": could not send message");
        }

        String reply;
        try {
            reply = input.readLine();
        } catch (IOException e) {
            throw new IOException(
                    type + ": could not read server reply: " + e.getMessage(), e);
        }

        if (reply == null) {
            throw new IOException(type + ": server closed the connection without replying");
        }

        if (!"OK".equals(reply)) {
            throw new IOException(type + ": " + reply);
        }
    }

    private static JsonObject createMessage(
            String type, String patientId, String sessionId) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.addProperty("patientId", patientId);
        message.addProperty("sessionId", sessionId);
        return message;
    }
}

