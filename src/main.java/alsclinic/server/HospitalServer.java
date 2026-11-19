package alsclinic.server;

import java.net.ServerSocket;
import java.net.Socket;

public class HospitalServer {
    public static void main(String[] args) {
       try (ServerSocket serverSocket= new ServerSocket(9000)){
           System.out.println("Hospital server strated on port 9000");
           while (true){
               Socket client = serverSocket.accept();
               System.out.println("new patient connected");
               client.close();
           }
       }catch (Exception e) {
           System.out.println("Error al iniciar el servidor: " + e.getMessage());

       }
    }
}
