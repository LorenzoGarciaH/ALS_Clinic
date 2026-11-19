package alsclinic.server;

import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

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
}
