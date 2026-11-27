package alsclinic.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerTestClient {
    public static void main(String[] arg){
        try (Socket socket= new Socket("localhost",9000)){
            socket.setSoTimeout(5000);
            PrintWriter writer=new PrintWriter(socket.getOutputStream(),true, StandardCharsets.UTF_8);
            BufferedReader reader=new BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // Test 1: Invalid JSON
            sendTest("Invalid JSON",
                    "This is not JSON",
                    writer, reader);

            // Test 2: Missing mandatory fields
            sendTest("Missing fields",
                    "{\"type\":\"START_SESSION\"}",
                    writer, reader);

            // Test 3: Unknown message type
            sendTest("Unknown message type",
                    "{\"type\":\"UNKNOWN\","
                            + "\"patientId\":\"TEST_INVALID\","
                            + "\"sessionId\":\"test-session\"}",
                    writer, reader);

            // Test 4: Measurement without an active session
            sendTest("Measurement without session",
                    "{\"type\":\"MEASUREMENT\","
                            + "\"patientId\":\"TEST_INVALID\","
                            + "\"sessionId\":\"test-session\","
                            + "\"eeg\":[512,514],"
                            + "\"emg\":[500,530]}",
                    writer, reader);

            // Test 5: Empty patient ID
            sendTest("Empty patient ID",
                    "{\"type\":\"START_SESSION\","
                            + "\"patientId\":\"\","
                            + "\"sessionId\":\"test-session\","
                            + "\"startedAt\":\"2026-11-27T12:00:00Z\"}",
                    writer, reader);
            System.out.println("\nAll tests completed.");
        }catch (Exception e){
            System.out.println("Test failed: "+e.getMessage());
        }
    }
    private static void sendTest(
            String testName,
            String message,
            PrintWriter output,
            BufferedReader input) throws IOException {

        System.out.println("\nTEST: " + testName);
        System.out.println("Sent: " + message);

        output.println(message);

        if (output.checkError()) {
            throw new IOException("Could not send test message");
        }

        String response = input.readLine();

        if (response == null) {
            throw new IOException("Server closed the connection");
        }

        System.out.println("Response: " + response);

        if (response.startsWith("Error:")) {
            System.out.println("RESULT: PASSED");
        } else {
            System.out.println("RESULT: FAILED");
        }
    }
}
