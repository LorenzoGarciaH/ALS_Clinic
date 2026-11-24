package alsclinic.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DataStorage {
    private static final Pattern VALID_ID= Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private final Path dataDirectory;
    public DataStorage(String directory)throws IOException{
        dataDirectory= Path.of(directory);
        Files.createDirectories(dataDirectory);
    }
    public synchronized void save(String patientId, String sessionId, String jsonMessage)throws IOException{
        if(patientId==null||!VALID_ID.matcher(patientId).matches()||sessionId==null||!VALID_ID.matcher(sessionId).matches()){
            throw new IllegalArgumentException("Invalied patient or session ID");
        }

        Path patientDirectory= dataDirectory.resolve(patientId);
        Files.createDirectories(patientDirectory);
        Path file= patientDirectory.resolve(sessionId+".jsonl");
        Files.writeString(file,jsonMessage+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    public synchronized JsonArray getPatients()throws IOException{
        JsonArray patients= new JsonArray();
        try (DirectoryStream<Path> directories= Files.newDirectoryStream(dataDirectory)) {
            for (Path entry:directories){
                if (Files.isDirectory(entry)&&VALID_ID.matcher(entry.getFileName().toString()).matches()){
                    patients.add(entry.getFileName().toString());
                }
            }
        }
        return patients;
    }
    public synchronized JsonArray getSessions(String patientId)throws IOException{
        validateId(patientId);
        JsonArray sessions=new JsonArray();
        Path patientDirectory= dataDirectory.resolve(patientId);
        if (!Files.exists(patientDirectory)||!Files.isDirectory(patientDirectory)) {
            return sessions;
        }
        try (Stream<Path> files= Files.list(patientDirectory)){
            files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".jsonl")).forEach(name -> sessions.add(name.substring(0, name.length() - 6)));
        }
        return sessions;
    }
    public synchronized JsonArray getHistory(String patientId, String sessionId)throws IOException{
        validateId(patientId);
        validateId(sessionId);
        JsonArray history=new JsonArray();
        Path file= dataDirectory.resolve(patientId).resolve(sessionId+".jsonl");
        if (!Files.isRegularFile(file)){
            throw new IllegalArgumentException("Session file does not exist for patient: "+patientId+" and session: "+sessionId);
        }
        for (String line: Files.readAllLines(file, StandardCharsets.UTF_8)){
            if(!line.isBlank()){
                history.add(JsonParser.parseString(line));
            }
        }
        return history;
    }
    public synchronized void addAnnotation(String patientId, String sessionId, String comment)throws IOException {
        validateId(patientId);
        validateId(sessionId);
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be null or blank");
        }
        Path patientDirectory= dataDirectory.resolve(patientId);
        Path sessionFile= patientDirectory.resolve(sessionId+".jsonl");
        if (!Files.exists(sessionFile)) {
            throw new IllegalArgumentException("Session file does not exist for patient: " + patientId + " and session: " + sessionId);
        }
        JsonObject annotation= new JsonObject();
        annotation.addProperty("type", "annotation");
        annotation.addProperty("comment", comment);
        annotation.addProperty("patientId", patientId);
        annotation.addProperty("sessionId", sessionId);
        annotation.addProperty("timestamp", Instant.now().toString());
        Path annotationsDirectory=patientDirectory.resolve("annotations");
        Files.createDirectories(annotationsDirectory);
        Path annotationFile=annotationsDirectory.resolve(sessionId+".jsonl");
        Files.writeString(annotationFile, annotation.toString() + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    private void validateId(String id) {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid ID: " + id);
        }
    }
}
