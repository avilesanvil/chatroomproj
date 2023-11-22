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
import java.util.Date;
import java.util.Scanner;

public class Client {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter server IP address (default localhost): ");
        String serverIp = scanner.nextLine();
        if (serverIp.isEmpty()) {
            serverIp = "localhost";
        }

        System.out.print("Enter server port number: ");
        int port = Integer.parseInt(scanner.nextLine());

        try (Socket socket = new Socket(serverIp, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to server on " + serverIp + ":" + port);

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading from server: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                if (userInput.startsWith("JOIN ") || !userInput.equals("LEAVE")) {
                    String time = sdf.format(new Date());
                    System.out.println("[" + time + "] You: " + userInput);
                }
            }
        } catch (UnknownHostException ex) {
            System.err.println("Host unknown: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

