
package alsclinic.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import alsclinic.common.Message;


public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DataStorage storage;
    public ClientHandler(Socket socket, DataStorage storage) {
        this.socket = socket;
        this.storage = storage;
    }
    @Override
    public void run() {
        String activePatientId = null;
        String activeSessionId = null;

        try (Socket client = socket;
             BufferedReader input = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(
                     new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8),
                     true)) {

            String line;

            while ((line = input.readLine()) != null) {
                String type = "";

                try {
                    JsonObject message = JsonParser.parseString(line).getAsJsonObject();
                    type = required(message, "type");

                    if (isDoctorMessage(type)) {
                        JsonObject response = handleDoctorMessage(type, message);
                        output.println(response.toString());
                        continue;
                    }

                    String patientId = required(message, "patientId");
                    String sessionId = required(message, "sessionId");

                    switch (type) {
                        case "START_SESSION":
                            requiredInstant(message, "startedAt");
                            break;
                        case "PATIENT_REPORT":
                            required(message, "symptoms");
                            requiredInstant(message, "submittedAt");
                            break;
                        case "MEASUREMENT":
                            break;
                        case "END_SESSION":
                            requiredInstant(message, "endedAt");
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid message type: " + type);
                    }

                    if ("START_SESSION".equals(type)) {
                        if (activeSessionId != null) {
                            throw new IllegalArgumentException("A session is already open");
                        }
                    } else if (!patientId.equals(activePatientId)
                            || !sessionId.equals(activeSessionId)) {
                        throw new IllegalArgumentException(
                                "Message must match the open patient and session");
                    }

                    storage.save(patientId, sessionId, message.toString());

                    if ("START_SESSION".equals(type)) {
                        activePatientId = patientId;
                        activeSessionId = sessionId;
                    } else if ("END_SESSION".equals(type)) {
                        activePatientId = null;
                        activeSessionId = null;
                    }

                    output.println("OK");
                } catch (RuntimeException | IOException e) {
                    if (isDoctorMessage(type)) {
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "ERROR");
                        response.addProperty("message", e.getMessage());
                        output.println(response.toString());
                    } else {
                        output.println("Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client connection closed: " + e.getMessage());
        } finally {
            if (activeSessionId != null) {
                JsonObject interruption = new JsonObject();
                interruption.addProperty("type", "SESSION_INTERRUPTED");
                interruption.addProperty("patientId", activePatientId);
                interruption.addProperty("sessionId", activeSessionId);
                interruption.addProperty("detectedAt", Instant.now().toString());
                interruption.addProperty("reason", "connection_closed_before_end");

                try {
                    storage.save(activePatientId, activeSessionId, interruption.toString());
                } catch (IOException e) {
                    System.err.println("Could not record interrupted session "
                            + activeSessionId + ": " + e.getMessage());
                }
            }
        }
    }


    private static boolean isDoctorMessage(String type){
        return Message.GET_PATIENTS.equals(type)||
                Message.GET_SESSIONS.equals(type)||
                Message.GET_HISTORY.equals(type)||
                Message.ADD_ANNOTATION.equals(type);
    }

    private JsonObject handleDoctorMessage(String type, JsonObject message) throws IOException {
        JsonObject response= new JsonObject();
        switch (type) {
            case Message.GET_PATIENTS:
                response.add("patient",storage.getPatients());
                break;
            case Message.GET_SESSIONS:
                response.add("sessions",storage.getSessions(required(message, "patientId")));
                break;
            case Message.GET_HISTORY:
                response.add("history",storage.getHistory(required(message, "patientId"), required(message, "sessionId")));
                break;
            case Message.ADD_ANNOTATION:
               storage.addAnnotation(required(message, "patientId"), required(message, "sessionId"), required(message, "comment"));
               break;
            default:
                throw new IllegalArgumentException("Invalid message type: " + type);
        }
        response.addProperty("status",Message.OK);
        return response;
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key)||json.get(key).isJsonNull()|| !json.get(key).getAsJsonPrimitive().isString()){
            throw new IllegalArgumentException("Missing or invalid field: "+key);
        }
        String value= json.get(key).getAsString();
        if (value.isBlank()){
            throw new IllegalArgumentException("Emty field: "+key);
        }
        return value;
    }

    private static Instant requiredInstant(JsonObject message, String field) {
        String value = required(message, field);

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    field + " must be a valid timestamp, for example 2026-11-25T16:00:00Z");
        }
    }
}
