package alsclinic.server;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class AdminConsole implements Runnable {
    private final HospitalServer server;

    public AdminConsole(HospitalServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        Console console = System.console();
        if (console == null) {
            System.err.println("Start HospitalServer from a terminal to use admin commands.");
            return;
        }

        Path verifierFile = Path.of(
                System.getProperty("user.home"), "als-admin.verifier");

        String command;
        while ((command = console.readLine("Enter command (start/exit): ")) != null) {
            switch (command.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "start" ->
                        console.printf("Connected clients: %d%n", server.getConnectedClients());

                case "exit" -> {
                    if (!authenticate(console, verifierFile)) {
                        continue;
                    }

                    int connectedClients = server.getConnectedClients();
                    console.printf("Connected clients: %d%n", connectedClients);

                    if (connectedClients > 0) {
                        String confirmation = console.readLine(
                                "Clients are still connected. Shut down anyway? (yes/no): ");
                        if (confirmation == null) {
                            return;
                        }
                        if (!"yes".equalsIgnoreCase(confirmation.trim())) {
                            console.printf("Shutdown cancelled.%n");
                            continue;
                        }
                    }

                    server.shutdown();
                    return;
                }

                default -> console.printf("Unknown command.%n");
            }
        }
    }

    private boolean authenticate(Console console, Path verifierFile) {
        char[] password = console.readPassword("Administrator password: ");
        if (password == null) {
            return false;
        }

        try {
            if (AdminPassword.verify(password, verifierFile)) {
                return true;
            }

            console.printf("Password is incorrect.%n");
            return false;
        } catch (IOException | GeneralSecurityException e) {
            console.printf("Could not verify administrator password: %s%n", e.getMessage());
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}