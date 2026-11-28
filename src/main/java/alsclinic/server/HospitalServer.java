package alsclinic.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class HospitalServer {
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ServerSocket serverSocket;
    private final DataStorage storage;
    public HospitalServer()throws IOException{
        storage=new DataStorage("data");
    }
    public void start()throws IOException{
        serverSocket= new ServerSocket(9000);
        System.out.println("Hospital server started on port 9000");
        Thread adminThread= new Thread(new AdminConsole(this),"admin-console");
        adminThread.setDaemon(true);
        adminThread.start();
        while (running.get()){
            try {
                Socket client = serverSocket.accept();
                clients.add(client);
                if (!running.get()) {
                    clients.remove(client);
                    client.close();
                    break;
                }
                Thread worker = new Thread(()->{
                    try {
                        new ClientHandler(client,storage).run();
                    }
                    catch (Exception e){
                        System.out.println("Error handling client: "+e.getMessage());
                    }finally {
                        clients.remove(client);
                        System.out.println("Client disconnected. Total connected clients: "+clients.size());
                    }
                });
                worker.start();
                System.out.println("New client connected. Total connected clients: " + clients.size());
            }catch (SocketException e){
                if (running.get()){
                   throw e;
                }
            }
        }
    }
    public int getConnectedClients(){
        return clients.size();
    }
    public void shutdown(){
        if (!running.compareAndSet(true,false)){
            return;
        }
        System.out.println("Shutting down server...");
        try {
            if (serverSocket!=null&&!serverSocket.isClosed()){
                serverSocket.close();
            }
        }catch (IOException e){
            System.out.println("Error shutting down server: "+e.getMessage());
        }
        for (Socket client:clients) {
            try {
                client.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
        System.out.println("Hospital server shutdown requested.");
    }
    public static void main(String[] args) {
       try {
              HospitalServer server = new HospitalServer();
              server.start();
       }catch (Exception e) {
           System.out.println("Server error: " + e.getMessage());

       }
    }
}
