import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int SERVER_PORT = 69;
    private static final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private static String coordinatorId = null;

    private static class ClientInfo {
        private final PrintWriter writer;
        private final Socket socket;

        public ClientInfo(PrintWriter writer, Socket socket) {
            this.writer = writer;
            this.socket = socket;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public Socket getSocket() {
            return socket;
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Chat Server is running on port " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Scanner clientScanner = new Scanner(clientSocket.getInputStream());
                PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read client ID
                String clientId = clientScanner.nextLine();

                // Set up coordinator if none exists
                if (coordinatorId == null) {
                    coordinatorId = clientId;
                    System.out.println("Client " + clientId + " is now the coordinator.");
                }

                // Send existing members' details to the joining client
                sendMembersDetails(clientWriter);

                // Broadcast the joining message to existing clients
                broadcast(clientId + " has joined the chat.");

                // Create a new thread to handle the client
                Thread clientThread = new Thread(() -> {
                    clients.put(clientId, new ClientInfo(clientWriter, clientSocket));

                    try {
                        while (true) {
                            String clientMessage = clientScanner.nextLine();
                            if (clientMessage.equalsIgnoreCase("quit")) {
                                clients.remove(clientId);
                                broadcast(clientId + " has left the chat.");
                                if (clientId.equals(coordinatorId)) {
                                    handleCoordinatorExit();
                                }
                                break;
                            } else if (clientMessage.equals("/requestMembers")) {
                                sendMembersDetails(clientWriter);
                            } else if (clientMessage.startsWith("/msg")) {
                                // Handle private message logic
                                // Format: /private recipientId message
                                String[] parts = clientMessage.split(" ", 3);
                                if (parts.length == 3) {
                                    String recipientId = parts[1];
                                    String privateMessage = parts[2];
                                    sendPrivateMessage(clientId, recipientId, privateMessage);
                                } else {
                                    System.out.println("Invalid private message format. Use /private recipientId message");
                                }
                            } else {
                                broadcast(clientId + ": " + clientMessage);
                            }
                        }
                    } catch (Exception e) {
                        // Handle client exit
                    } finally {
                        clients.remove(clientId);
                        if (clientId.equals(coordinatorId)) {
                            handleCoordinatorExit();
                        }
                    }
                });
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String message) {
        for (ClientInfo client : clients.values()) {
            client.getWriter().println(message);
        }
        System.out.println(message);
    }

    private static void sendMembersDetails(PrintWriter clientWriter) {
        clientWriter.println("Current members:");
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            String memberId = entry.getKey();
            ClientInfo clientInfo = entry.getValue();
            Socket socket = clientInfo.getSocket();
            String ipAddress = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();
            clientWriter.println("ID: " + memberId + " | IP: " + ipAddress + " | Port: " + port);
        }
        clientWriter.println("Coordinator: " + coordinatorId);
    }

    private static void sendPrivateMessage(String senderId, String recipientId, String message) {
        ClientInfo recipientInfo = clients.get(recipientId);
        if (recipientInfo != null) {
            recipientInfo.getWriter().println("[Private from " + senderId + "]: " + message);
        } else {
            System.out.println("Recipient " + recipientId + " not found.");
        }
    }

    private static void handleCoordinatorExit() {
        if (!clients.isEmpty()) {
            // Assign a new coordinator if clients exist
            String newCoordinatorId = clients.keySet().iterator().next();
            broadcast("Client " + newCoordinatorId + " is now the coordinator.");
            coordinatorId = newCoordinatorId;
        } else {
            // No clients left, reset coordinator to null
            coordinatorId = null;
        }
    }
}
