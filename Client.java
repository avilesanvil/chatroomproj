/* [existing comments] */

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 9012;  // Well-known port of the server
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter server IP address (default: localhost): ");
        String serverAddress = scanner.nextLine().trim();
        if (serverAddress.isEmpty()) {
        serverAddress = "127.0.0.1"; // Default to localhost
        System.out.println("No server address provided. Defaulting to " + serverAddress);
    
            serverAddress = "localhost";
        }

        try (Socket socket = new Socket(serverAddress, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Sending JOIN request
            System.out.println("Enter the room name to join:");
            String roomName = scanner.nextLine();
            out.println("JOIN " + roomName);

            // Starting a thread to listen for messages from the server
            Thread listener = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
        System.out.println("Server: " + serverMessage);
    
                        System.out.println("[" + sdf.format(new Date()) + "] " + serverMessage);
                    }
                } catch (IOException e) {
        e.printStackTrace();
        System.out.println("An error occurred: " + e.getMessage());
    
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            });
            listener.start();

            // Handling user input for chat and LEAVE request
            String userInput;
            while (!(userInput = scanner.nextLine()).equalsIgnoreCase("LEAVE")) {
        // Send user input to server
        out.println(userInput);
    
                String timestampedMessage = "[" + sdf.format(new Date()) + "] " + userInput;
                out.println(timestampedMessage); // Sending timestamped chat messages to the server
            }
            out.println("LEAVE " + roomName); // Sending LEAVE request with room name

            listener.join(); // Wait for listener thread to finish
        } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        System.out.println("An error occurred: " + e.getMessage());
    
            e.printStackTrace();
        }
    }

    // [rest of the existing methods and classes from the original Client.java]
}