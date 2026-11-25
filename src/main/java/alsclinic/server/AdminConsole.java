package alsclinic.server;

import java.io.Console;
import java.util.Scanner;

public class AdminConsole implements Runnable {
    private final HospitalServer server;

    public AdminConsole(HospitalServer server) {
        this.server = server;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter command (start/exit):");
            if (!scanner.hasNextLine()) {
                return;
            }
            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("start")) {
                System.out.println("conected clients: " + server.getConnectedClients());
            } else if (command.equalsIgnoreCase("exit")) {
                String expectedPassword = System.getenv("ADMIN_PASSWORD");
                if (expectedPassword == null || expectedPassword.isBlank()) {
                    System.out.println("Error: ADMIN_PASSWORD environment variable is not set.");
                    continue;
                }
                Console console = System.console();
                String password;
                if (console != null) {
                    char[] entered = console.readPassword();
                    password = entered == null ? "" : new String(entered);
                    java.util.Arrays.fill(entered == null ? new char[0] : entered, '\0');
                } else {
                    System.out.print("Enter password: ");
                    if (!scanner.hasNextLine()) {
                        return;
                    }
                    password = scanner.nextLine();
                }
                if (!password.equals(expectedPassword)) {
                    System.out.println("Error: Incorrect password.");
                    continue;
                }
                int activeClients = server.getConnectedClients();
                System.out.println("Connected clients: " + activeClients);
                if (activeClients > 0) {
                    System.out.println("Warning: There are still connected clients. Are you sure you want to exit? (yes/no)");
                    if (!scanner.hasNextLine()) {
                        return;
                    }
                    if(!scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                        System.out.println("Exit aborted.");
                        continue;
                    }
                }
                server.shutdown();
                return;
            }else {
                System.out.println("Unknown command: " + command);
            }
        }
    }
}