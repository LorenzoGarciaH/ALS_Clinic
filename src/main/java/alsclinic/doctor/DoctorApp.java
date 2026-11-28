package alsclinic.doctor;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Scanner;

public class DoctorApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverHost=System.getProperty("als.server.host","localhost");
        int serverPort=Integer.getInteger("als.server.port",9000);
        try(DoctorClient client=new DoctorClient(serverHost,serverPort)){
            System.out.println("Connected to hospital server.");
            boolean running= true;
            while(running){
                System.out.println("\n====DOCTOR MENU====");
                System.out.println("1. View patient list");
                System.out.println("2. View patient sessions");
                System.out.println("3. view session history");
                System.out.println("4. Add clinical annotations");
                System.out.println("5. Exit");
                System.out.print("Select an option: ");
                String option = scanner.nextLine().trim();
                try{
                    JsonObject response;
                    switch (option){
                        case "1":
                            response=client.getPatients();
                            System.out.println(response);
                            break;
                        case "2":
                            System.out.print("Enter patient ID: ");
                            String patientId = scanner.nextLine();
                            response=client.getSessions(patientId);
                            System.out.println(response);
                            break;
                        case "3":
                            System.out.println("Enter patient ID: ");
                            String patientIdForHistory = scanner.nextLine();
                            System.out.println("Enter session ID: ");
                            String sessionId = scanner.nextLine();
                            response=client.getHistory(patientIdForHistory,sessionId);
                            System.out.println(response);
                            break;
                        case "4":
                            System.out.print("Enter patient ID: ");
                            String annotationPatien = scanner.nextLine();
                            System.out.println("Enter session ID: ");
                            String annotationSession = scanner.nextLine();
                            System.out.println("Enter annotation text: ");
                            String annotationText = scanner.nextLine();
                            response=client.addAnnotation(annotationPatien,annotationSession,annotationText);
                            System.out.println(response);
                            break;
                        case "5":
                            running=false;
                            System.out.println("Exiting doctor application.");
                            break;
                        default:
                            System.out.println("Invalid option. Please try again.");
                    }
                }catch (IOException e){
                    System.out.println("Error: "+e.getMessage());
                    running=false;
                }
            }
        }catch (IOException e){
            System.out.println("Error connecting to server: "+e.getMessage());
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
