package alsclinic.doctor;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import alsclinic.common.Message;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DoctorClient implements AutoCloseable{
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    public DoctorClient(String host, int port) throws Exception {
        socket = new Socket(host, port);
        socket.setSoTimeout(5000);
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
    public JsonObject getPatients()throws IOException{
        return sendRequest(Message.create(Message.GET_PATIENTS));
    }
    public JsonObject getSessions(String patientId)throws IOException{
        return sendRequest(Message.create(Message.GET_SESSIONS,patientId));
    }
    public JsonObject getHistory(String patientId, String sessionId)throws IOException{
        return sendRequest(Message.create(Message.GET_HISTORY,patientId,sessionId));
    }
    public JsonObject addAnnotation(String patientId, String sessionId, String comment)throws IOException{
        JsonObject request=Message.create(Message.ADD_ANNOTATION,patientId,sessionId);
        request.addProperty("comment",comment);
        return sendRequest(request);
    }
    @Override
    public void close() throws IOException {
        socket.close();
    }
}
