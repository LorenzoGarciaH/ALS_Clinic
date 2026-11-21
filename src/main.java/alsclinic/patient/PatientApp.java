package alsclinic.patient;

import java.io.*;
import java.net.Socket;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class PatientApp {

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 9000);) {
            System.out.println("connected to " + socket.getInetAddress().getHostName());
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String message = """
                    {"type":"START_SESSION","patientId":"test-patient","sessionId":"demo-001"}""" ;
            output.println(message);

            String reply = input.readLine();
            System.out.println("server response: " + reply);

            String report = """
        {"type":"PATIENT_REPORT","patientId":"test-patient","sessionId":"demo-001","symptoms":"Hand fatigue after writing"}""";

            output.println(report);
            String reportReply = input.readLine();
            System.out.println("Report response: " + reportReply);

            if ("OK".equals(reportReply)) {
                String measurement = """
            {"type":"MEASUREMENT","patientId":"test-patient","sessionId":"demo-001","eeg":[512,514,511],"emg":[500,530,502]}""";

                output.println(measurement);
                String measurementReply = input.readLine();
                System.out.println("Measurement response: " + measurementReply);
                if ("OK".equals(measurementReply)) {
                    String end = """
            {"type":"END_SESSION","patientId":"test-patient","sessionId":"demo-001"}""";

                    output.println(end);
                    String endReply = input.readLine();
                    System.out.println("End session response: " + endReply);
                }
            }


        } catch (IOException e) {
            System.err.println("could not connect to the hospital server: " + e.getMessage());
       }
    }
}
