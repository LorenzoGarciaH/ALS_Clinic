package alsclinic.doctor;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DoctorClient implements AutoCloseable{
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    public DoctorClient(String host, int port) throws Exception {
        socket = new Socket(host, port);
        socket.setSoTimeout(1000);
        reader=new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }
    public JsonObject sendRequest(JsonObject request) throws IOException {
        writer.println(request.toString());
        if (writer.checkError()) {
            throw new IOException("Error sending request to server.");
        }
        String responseLine = reader.readLine();
        if (responseLine == null) {
            throw new IOException("Server closed the connection unexpectedly.");
        }
        try {
            return JsonParser.parseString(responseLine).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("Invalid JSON response from server: " + responseLine, e);
        }
    }
    public JsonObject getPatient()throws IOException{
        JsonObject request=new JsonObject();
        request.addProperty("type","getPatient");
        return sendRequest(request);
    }
    public JsonObject getSessions(String patientId)throws IOException{
        JsonObject request=new JsonObject();
        request.addProperty("type","getSessions");
        request.addProperty("patientId",patientId);
        return sendRequest(request);
    }
    public JsonObject getHistory(String patientId, String sessionId)throws IOException{
        JsonObject request=new JsonObject();
        request.addProperty("type","getHistory");
        request.addProperty("patientId",patientId);
        request.addProperty("sessionId",sessionId);
        return sendRequest(request);
    }
    public JsonObject addAnnotation(String patientId, String sessionId, String annotation)throws IOException{
        JsonObject request=new JsonObject();
        request.addProperty("type","addAnnotation");
        request.addProperty("patientId",patientId);
        request.addProperty("sessionId",sessionId);
        request.addProperty("annotation",annotation);
        return sendRequest(request);
    }
    @Override
    public void close() throws IOException {
        socket.close();
    }
}
