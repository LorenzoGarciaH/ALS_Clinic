package alsclinic.patient;

import bitalino.BITalinoException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class PatientClinic {
    private static final int REPLY_TIMEOUT_MS = 5000;
    private static final int SAMPLING_RATE_HZ = 1000;
    private static final Duration RECORDING_DURATION = Duration.ofSeconds(5);
    private static final Gson GSON = new Gson();

    public void submitSession(String patientId, String sessionId, String symptoms)
            throws IOException {
        String serverHost = System.getProperty("als.server.host", "localhost");
        int serverPort = Integer.getInteger("als.server.port", 9000);
        String bluetoothAddress = System.getProperty("als.bitalino.address", "").trim();

        try (Socket socket = new Socket(serverHost, serverPort)) {
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

            if (bluetoothAddress.isBlank()) {
                sendSimulatedMeasurements(patientId, sessionId, output, input);
            } else {
                recordAndSend(bluetoothAddress, patientId, sessionId, output, input);
            }

            JsonObject end = createMessage("END_SESSION", patientId, sessionId);
            end.addProperty("endedAt", Instant.now().toString());
            sendAndRequireOk(end, output, input);
        }
    }

    private static void sendSimulatedMeasurements(
            String patientId, String sessionId, PrintWriter output, BufferedReader input)
            throws IOException {
        Instant firstSampleAt = Instant.now();

        JsonObject ecg = measurementMessage(
                patientId, sessionId, "ECG", "A2", SAMPLING_RATE_HZ,
                firstSampleAt, true, new int[]{512, 514, 511});
        sendAndRequireOk(ecg, output, input);

        JsonObject emg = measurementMessage(
                patientId, sessionId, "EMG", "A1", SAMPLING_RATE_HZ,
                firstSampleAt, true, new int[]{500, 530, 502});
        sendAndRequireOk(emg, output, input);
    }

    private static void recordAndSend(
            String bluetoothAddress, String patientId, String sessionId,
            PrintWriter output, BufferedReader input) throws IOException {
        List<AcquisitionChannel> channels = List.of(
                AcquisitionChannel.of(SensorType.ECG),
                AcquisitionChannel.of(SensorType.EMG));

        try (BitalinoManager source = new BitalinoManager(
                bluetoothAddress, SAMPLING_RATE_HZ, channels)) {
            AcquisitionRecorder.Result result = new AcquisitionRecorder().record(
                    source, RECORDING_DURATION, block -> {
                        for (AcquisitionChannel channel : channels) {
                            JsonObject message = measurementMessage(
                                    patientId,
                                    sessionId,
                                    channel.sensor().name(),
                                    channel.inputName(),
                                    source.getSamplingRateHz(),
                                    block.firstSampleTime(),
                                    false,
                                    block.rawOf(channel.sensor()));

                            message.addProperty("droppedFrames", block.droppedFrames());
                            sendAndRequireOk(message, output, input);
                        }
                    });

            if (!result.isSuccessful()) {
                Exception error = result.error();
                String detail = error == null ? "" : " - " + error.getMessage();
                throw new IOException("Recording stopped: " + result.reason() + detail, error);
            }
        } catch (BITalinoException e) {
            throw new IOException("Could not open BITalino: " + e.getMessage(), e);
        }
    }

    private static JsonObject measurementMessage(
            String patientId, String sessionId, String signalType,
            String channel, int samplingRateHz, Instant firstSampleAt,
            boolean simulated, int[] samples) {
        JsonObject message = createMessage("MEASUREMENT", patientId, sessionId);
        message.addProperty("signalType", signalType);
        message.addProperty("channel", channel);
        message.addProperty("samplingRateHz", samplingRateHz);
        message.addProperty("unit", "ADC_COUNTS");
        message.addProperty("firstSampleAt", firstSampleAt.toString());
        message.addProperty("simulated", simulated);
        message.add("samples", GSON.toJsonTree(samples));
        return message;
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
