
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static volatile boolean isServerRunning = true;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server is shutting down...");
            isServerRunning = false;
            try {
                serverSocket.close();
                for (ClientHandler client : clientHandlers) {
                    client.closeConnection();
                }
            } catch (IOException e) {
                System.out.println("Error closing server resources: " + e.getMessage());
            }
        }));
    }
    
    private static final int WELL_KNOWN_PORT = 9001;
    private static Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    private static List<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>();

    // ChatRoom inner class for managing chat rooms
    private static class ChatRoom {
        Set<ClientHandler> members = new HashSet<>();

        public synchronized void addMember(ClientHandler clientHandler, String roomName) {
            members.add(clientHandler);
            broadcastMessage("User " + clientHandler.getClientName() + " has joined the chat.", clientHandler);
            log("User " + clientHandler.getClientName() + " joined room: " + roomName);
        }

        public synchronized void removeMember(ClientHandler clientHandler, String roomName) {
            members.remove(clientHandler);
            broadcastMessage("User " + clientHandler.getClientName() + " has left the chat.", clientHandler);
            log("User " + clientHandler.getClientName() + " left room: " + roomName);
        }

        public synchronized void broadcastMessage(String message, ClientHandler sender) {
            for (ClientHandler member : members) {
                if (member != sender) {
                    member.sendMessage(message);
                }
            }
        }
    }

    // ClientHandler inner class for handling individual client connections
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String clientName;
        private String currentRoom = "";

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                InputStream input = socket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(input));
                OutputStream output = socket.getOutputStream();
                writer = new PrintWriter(output, true);
            } catch (IOException e) {
                log("Error setting up streams: " + e.getMessage());
            }
        }

        @Override

        public void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing client connection: " + e.getMessage());
            }
        }
            
        public void run() {
            try {
                clientName = reader.readLine(); // Read the client name
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if (clientMessage.startsWith("JOIN ")) {
                        String roomName = clientMessage.substring(5).trim();
                        leaveRoom(); // Leave the current room if any
                        joinRoom(roomName);
                    } else if (clientMessage.equalsIgnoreCase("LEAVE")) {
                        leaveRoom();
                        break;
                    } else {
                        // Broadcast the message to all in the current room
                        if (!currentRoom.isEmpty()) {
                            chatRooms.get(currentRoom).broadcastMessage(clientName + ": " + clientMessage, this);
                        }
                    }
                }
            } catch (IOException e) {
                log("Error in ClientHandler: " + e.getMessage());
            } finally {
                leaveRoom();
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Error closing socket: " + e.getMessage());
                }
            }
        }

        private void joinRoom(String roomName) {
            currentRoom = roomName;
            chatRooms.computeIfAbsent(roomName, k -> new ChatRoom()).addMember(this, roomName);
            log("Client " + clientName + " joined room: " + roomName);
        }

        private void leaveRoom() {
            if (!currentRoom.isEmpty() && chatRooms.containsKey(currentRoom)) {
                chatRooms.get(currentRoom).removeMember(this, currentRoom);
                log("Client " + clientName + " left room: " + currentRoom);
                currentRoom = "";
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
        }

        public String getClientName() {
            return clientName;
        }
    }

    // Main method to start the server
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(WELL_KNOWN_PORT)) {
            log("Server started on port " + WELL_KNOWN_PORT);

            while (isServerRunning) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            log("Server exception: " + e.getMessage());
        }
    }

    // Simple logging method
    private static void log(String message) {
        System.out.println("[Server]: " + message);
    }
}
