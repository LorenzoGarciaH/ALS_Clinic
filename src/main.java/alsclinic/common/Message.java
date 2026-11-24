package alsclinic.common;

import com.google.gson.JsonObject;

public class Message {
    public static final String START_SESSION="START_SESSION";
    public static final String END_SESSION="END_SESSION";
    public static final String PATIENT_REPORT="PATIENT_REPORT";
    public static final String MEEASUREMENT="MEASUREMENT";

    public static final String GET_PATIENT="GET_PATIENT";
    public static final String GET_SESSIONS="GET_SESSIONS";
    public static final String GET_HISTORY="GET_HISTORY";
    public static final String ADD_ANNOTATION="ADD_ANNOTATION";

    public static final String OK="OK";
    public static final String ERROR="ERROR";

    private Message(){

    }
    public static JsonObject create(String type){
        JsonObject message=new JsonObject();
        message.addProperty("type", type);
        return message;
    }
    public static JsonObject create(String type,String patientId){
        JsonObject message= create(type);
        message.addProperty("patientId", patientId);
        return message;
    }
    public static JsonObject create(String type,String patientId,String sessionId){
        JsonObject message= create(type,patientId);
        message.addProperty("sessionId", sessionId);
        return message;
    }
}
