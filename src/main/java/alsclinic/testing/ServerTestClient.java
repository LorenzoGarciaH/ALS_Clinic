package alsclinic.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class ServerTestClient {
    private static int passedTests=0;
    private static int failedTests=0;
    public static void main(String[] arg){
        String serverHost=System.getProperty("als.server.host","localhost");
        int serverPort=Integer.getInteger("als.server.port",9000);
        try (Socket socket= new Socket(serverHost,serverPort)){
            socket.setSoTimeout(5000);
            PrintWriter writer=new PrintWriter(socket.getOutputStream(),true, StandardCharsets.UTF_8);
            BufferedReader reader=new BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // Test 1: Invalid JSON
            sendTest("Invalid JSON",
                    "This is not JSON",
                    true, writer, reader);

            // Test 2: Missing mandatory fields
            sendTest("Missing fields",
                    "{\"type\":\"START_SESSION\"}",
                    true, writer, reader);

            // Test 3: Unknown message type
            sendTest("Unknown message type",
                    "{\"type\":\"UNKNOWN\","
                            + "\"patientId\":\"TEST_INVALID\","
                            + "\"sessionId\":\"test-session\"}",
                    true, writer, reader);

            // Test 4: Measurement without an active session
            sendTest("Measurement without session",
                    createMeasurement("TEST_INVALID", "test-session").toString(),true, writer, reader);

            // Test 5: Empty patient ID
            sendTest("Empty patient ID",
                    "{\"type\":\"START_SESSION\","
                            + "\"patientId\":\"\","
                            + "\"sessionId\":\"test-session\","
                            + "\"startedAt\":\"2026-11-27T12:00:00Z\"}",
                    true, writer, reader);
            // Test 6: Start a valid session
            String patientId = "TEST_VALID";
            String sessionId = UUID.randomUUID().toString();

            JsonObject startSession = new JsonObject();

            startSession.addProperty("type", "START_SESSION");
            startSession.addProperty("patientId", patientId);
            startSession.addProperty("sessionId", sessionId);
            startSession.addProperty("startedAt", Instant.now().toString());
            sendTest("Valid session start",
                    startSession.toString(),
                    false,
                    writer, reader);
            // Test 7: Invalid signal type
            JsonObject invalidSignal = createMeasurement(patientId, sessionId);
            invalidSignal.addProperty("signalType", "INVALID_SIGNAL");
            sendTest("Invalid signal type",
                    invalidSignal.toString(),
                    true, writer, reader);
            // Test 8: Invalid channel
            JsonObject invalidChannel=createMeasurement(patientId,sessionId);
            invalidChannel.addProperty("channel", "A9");
            sendTest("Invalid channel",
                    invalidChannel.toString(),
                    true, writer, reader);
            // Test 9: Invalid sampling rate
            JsonObject incalidRate= createMeasurement(patientId,sessionId);
            incalidRate.addProperty("samplingRateHz", 500);
            sendTest("Invalid sampling rate",
                    incalidRate.toString(),
                    true, writer, reader);
            // Test 10: Invalid timestamp
            JsonObject invalidTimestamp=createMeasurement(patientId,sessionId);
            invalidTimestamp.addProperty("firstSampleAt","invalid-date");
            sendTest("Invalid timestamp",
                    invalidTimestamp.toString(),
                    true, writer, reader);
            // Test 11: Invalid unit
            JsonObject invalidUnit = createMeasurement(patientId, sessionId);
            invalidUnit.addProperty("unit", "INVALID");

            sendTest("Invalid unit",
                    invalidUnit.toString(), true, writer, reader);

            // Test 12: Sample above maximum
            JsonObject invalidSamples = createMeasurement(patientId, sessionId);
            JsonArray samples = new JsonArray();
            samples.add(1500);
            invalidSamples.add("samples", samples);

            sendTest("Sample above maximum",
                    invalidSamples.toString(), true, writer, reader);

            // Test 13: Negative sample
            JsonObject negativeSample = createMeasurement(patientId, sessionId);
            JsonArray negativeSamples = new JsonArray();
            negativeSamples.add(-1);
            negativeSample.add("samples", negativeSamples);

            sendTest("Negative sample",
                    negativeSample.toString(), true, writer, reader);

            // Test 14: Empty samples
            JsonObject emptySamples = createMeasurement(patientId, sessionId);
            emptySamples.add("samples", new JsonArray());

            sendTest("Empty samples",
                    emptySamples.toString(), true, writer, reader);

            // Test 15: Negative droppedFrames
            JsonObject invalidDropped = createMeasurement(patientId, sessionId);
            invalidDropped.addProperty("droppedFrames", -1);

            sendTest("Negative droppedFrames",
                    invalidDropped.toString(), true, writer, reader);

            // Test 16: Invalid simulated field
            JsonObject invalidSimulated = createMeasurement(patientId, sessionId);
            invalidSimulated.addProperty("simulated", "yes");

            sendTest("Invalid simulated field",
                    invalidSimulated.toString(), true, writer, reader);

            // Test 17: Valid patient report
            JsonObject report = new JsonObject();
            report.addProperty("type", "PATIENT_REPORT");
            report.addProperty("patientId", patientId);
            report.addProperty("sessionId", sessionId);
            report.addProperty("symptoms", "Test muscle weakness");
            report.addProperty("submittedAt", Instant.now().toString());

            sendTest("Valid patient report",
                    report.toString(), false, writer, reader);

            // Test 18: Valid ECG measurement
            sendTest("Valid ECG measurement",
                    createMeasurement(patientId, sessionId).toString(),
                    false, writer, reader);

            // Test 19: Valid EMG without droppedFrames
            JsonObject validEmg = createMeasurement(patientId, sessionId);
            validEmg.addProperty("signalType", "EMG");
            validEmg.addProperty("channel", "A1");
            validEmg.remove("droppedFrames");

            sendTest("Valid EMG without droppedFrames",
                    validEmg.toString(), false, writer, reader);

            // Test 20: End session
            JsonObject endSession = new JsonObject();
            endSession.addProperty("type", "END_SESSION");
            endSession.addProperty("patientId", patientId);
            endSession.addProperty("sessionId", sessionId);
            endSession.addProperty("endedAt", Instant.now().toString());

            sendTest("Valid session end",
                    endSession.toString(), false, writer, reader);
            System.out.println("\n==== TEST SUMMARY ====");
            System.out.println("Passed tests: " + passedTests);
            System.out.println("Failed tests: " + failedTests);
        }catch (Exception e){
            System.out.println("Test failed: "+e.getMessage());
        }
    }
    private static JsonObject createMeasurement(String patientId, String sessionId){
        JsonObject measurement=new JsonObject();
        measurement.addProperty("type","MEASUREMENT");
        measurement.addProperty("patientId",patientId);
        measurement.addProperty("sessionId",sessionId);
        measurement.addProperty("signalType","ECG");
        measurement.addProperty("channel","A2");
        measurement.addProperty("samplingRateHz",1000);
        measurement.addProperty("unit","ADC_COUNTS");
        measurement.addProperty("firstSampleAt", Instant.now().toString());
        measurement.addProperty("simulated",true);
        measurement.addProperty("droppedFrames",0);
        JsonArray samples=new JsonArray();
        samples.add(512);
        samples.add(514);
        samples.add(511);
        measurement.add("samples",samples);
        return measurement;
    }
    private static void sendTest(
            String testName,
            String message,
            boolean expectetError,
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
        boolean passed;
        if (expectetError) {
            passed = response.startsWith("Error");
        } else {
            passed = response.equals("OK");
        }
        if (passed) {
            passedTests++;
            System.out.println("Test passed.");
        } else {
            failedTests++;
            System.out.println("Test failed.");
        }
    }
}
