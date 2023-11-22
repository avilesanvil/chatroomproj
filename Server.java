/*

	Richard Delforge, Cameron Devenport, Johnny Do
	Chat Room Project
	COSC 4333 - Distributed Systems
	Dr. Sun
	11/27/2023
	
*/

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int DEFAULT_PORT = 9025;
    private static final int MAX_PORT = 65535;
    private static Map<String, Set<PrintWriter>> chatRooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        // Declare and initialize ipAddress and port here
        String ipAddress = "localhost"; // Default IP address
        int port = DEFAULT_PORT;        // Default port

        // Update values based on command-line arguments
        if (args.length > 0) {
            ipAddress = args[0]; // Get IP address from command-line argument
            if (args.length > 1) {
                port = Integer.parseInt(args[1]); // Optional: Get port from command-line argument
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress))) {
            // Print the IP address and port to the console
            System.out.println("Server is listening on IP: " + serverSocket.getInetAddress().getHostAddress() + " Port: " + serverSocket.getLocalPort());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static int findAvailablePort(int startPort) {
        while (startPort <= MAX_PORT) {
            try (ServerSocket serverSocket = new ServerSocket(startPort)) {
                return startPort;
            } catch (IOException ignored) {
                startPort++;
            }
        }
        return -1;
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket; 
        private PrintWriter out; 
        private BufferedReader in; 
        private String currentRoom; 
        private String clientName; 

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                out.println("Enter your name:");
                clientName = in.readLine();
                out.println("Welcome " + clientName + "! You can join a room with JOIN <room_name>, leave with LEAVE, list existing chatrooms with LISTROOMS, or send messages.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("JOIN ")) {
                        joinChatRoom(inputLine.substring(5));
                    } else if ("LEAVE".equals(inputLine)) {
                        leaveChatRoom();
                    } else if ("LISTROOMS".equals(inputLine)) {
                        listChatRooms();
                    } else {
                        sendMessageToChatRoom(clientName + ": " + inputLine, this.out);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Server exception: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                leaveChatRoom();
                closeResources();
            }
        }

        private void joinChatRoom(String roomName) {
            leaveChatRoom();
            chatRooms.computeIfAbsent(roomName, k -> new HashSet<>()).add(out);
            currentRoom = roomName;
            out.println("Entered room: " + roomName);
            System.out.println(clientName + " has entered chat room: " + roomName);
        }

        private void leaveChatRoom() {
            if (currentRoom != null && chatRooms.containsKey(currentRoom)) {
                chatRooms.get(currentRoom).remove(out);
                if (chatRooms.get(currentRoom).isEmpty()) {
                    chatRooms.remove(currentRoom);
                }
                out.println("Left room: " + currentRoom);
                System.out.println(clientName + " has left chat room: " + currentRoom);
                currentRoom = null;
            }
        }

        private void listChatRooms() {
            out.println("Available chat rooms:");
            for (Map.Entry<String, Set<PrintWriter>> entry : chatRooms.entrySet()) {
                out.println(" - " + entry.getKey() + " (" + entry.getValue().size() + " users)");
            }
        }

        private void sendMessageToChatRoom(String message, PrintWriter senderOut) {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String formattedMessage = "\n" + "[" + time + "] " + message.trim();
            if (currentRoom != null && chatRooms.containsKey(currentRoom)) {
                for (PrintWriter writer : chatRooms.get(currentRoom)) {
                    if (writer != senderOut) { 
                        writer.println(formattedMessage);
                    }
                }
            }
        }

        private void closeResources() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
