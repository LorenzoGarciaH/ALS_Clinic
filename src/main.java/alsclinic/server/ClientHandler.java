
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


public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DataStorage storage;
    public ClientHandler(Socket socket, DataStorage storage) {
        this.socket = socket;
        this.storage = storage;
    }
    public void run() {
        try (Socket client = socket; BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)); PrintWriter output = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String line;
            while ((line = input.readLine()) != null) {
                try {
                    JsonObject message = JsonParser.parseString(line).getAsJsonObject();
                    String type = required(message, "type");
                    String patientId = required(message, "patientId");
                    String sessionId = required(message, "sessionId");
                    switch (type){
                        case "PATIENT_REPORT":
                            required(message, "symptoms");
                            String submittedAt = required(message, "submittedAt");

                            try {
                                Instant.parse(submittedAt);
                            } catch (DateTimeParseException e) {
                                throw new IllegalArgumentException(
                                        "submittedAt must be a valid timestamp, for example 2026-11-23T10:15:30Z");
                            }

                            break;
                        case "START_SESSION":
                        case "MEASUREMENT":
                        case "END_SESSION":
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid message type: " + type);
                    }
                    storage.save(patientId, sessionId, message.toString());
                    output.println("OK");
                } catch (IllegalArgumentException|IllegalStateException|IOException e) {
                    output.println("Error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Client connection closed: " + e.getMessage());
        }
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
}
