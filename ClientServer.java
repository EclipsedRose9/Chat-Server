import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientServer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt the user for the server's IP address and port
        System.out.print("Enter the server IP address: ");
        String serverAddress = scanner.nextLine();

        System.out.print("Enter the server port: ");
        int serverPort = Integer.parseInt(scanner.nextLine());

        try (Socket socket = new Socket(serverAddress, serverPort);
             Scanner serverScanner = new Scanner(socket.getInputStream());
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.print("Enter your ID: ");
            String clientId = scanner.nextLine();
            writer.println(clientId);

            // Set up shutdown hook to handle Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Ctrl+C detected. Sending quit message to the server.");
                writer.println("quit");
            }));

            Thread inputThread = new Thread(() -> {
                try {
                    while (true) {
                        String message = scanner.nextLine();
                        if (message.equalsIgnoreCase("/quit")) {
                            // Send the quit message to the server
                            writer.println("quit");
                            // Wait a bit to allow the quit message to be sent before exiting
                            Thread.sleep(100);
                            break;
                        } else if (message.startsWith("/private")) {
                            String[] parts = message.split(" ", 3);
                            if (parts.length == 3) {
                                writer.println(message);
                            } else {
                                System.out.println("Invalid private message format. Use /private recipientId message");
                            }
                        } else {
                            writer.println(message);
                        }
                    }
                } catch (Exception e) {
                    // Handle client exit
                }
            });
            inputThread.start();

            try {
                while (serverScanner.hasNextLine()) {
                    String serverMessage = serverScanner.nextLine();
                    System.out.println("Server: " + serverMessage);
                }
            } catch (Exception e) {
                // Handle client exit
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
