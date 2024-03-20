package org.ProToType;

import com.google.gson.Gson;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Static.Encryption;
import org.ProToType.Static.PrintWithTime;
import org.ProToType.Threaded.ReceiveTcpPacket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.sql.SQLException;

public class Main {
    public static final Gson gson = new Gson();

    public static int maxPlayers;
    public static int tickRate;
    public static int tcpPort;
    public static int udpPort;
    public static ServerSocket tcpServerSocket;
    public static DatagramSocket udpServerSocket;
    public static ConnectedPlayer[] connectedPlayers;
//    public static GUI gui;


    public static void main(String[] args) throws SQLException, IOException {
        ConfigFile configFile = new ConfigFile(); // reads and sets up stuff from config file
        Encryption.SetEncryptionKey(configFile.encryptionKey);
        maxPlayers = configFile.maxPlayers;
        tickRate = configFile.tickRate;
        tcpPort = configFile.tcpPort;
        udpPort = tcpPort + 1;

        connectedPlayers = new ConnectedPlayer[configFile.maxPlayers]; // creates an array that holds information of connected players

        tcpServerSocket = new ServerSocket(tcpPort); // starts the TCP server
        udpServerSocket = new DatagramSocket(udpPort); // starts the UDP server

        Database.ConnectToDatabase(configFile);

        configFile = null; // nullifies the config file instance as it won't be needed anymore

//        database.RegisterPlayer("User", "testpassword");

        new Thread(ReceivePacket::ReceiveUdpPacket).start(); // starts thread that listens to incoming udp connections from anyone
        new Thread(SendPacket::SendPlayerPositions).start(); // starts thread that sends the current player positions to each player

        // Handle new players joining
        Authentication authentication = new Authentication(); // handles authentication of connecting client
        while (true) {
            ConnectedPlayer connectedPlayer = authentication.HandleNewConnections();
            if (connectedPlayer != null) { // runs if the authentication of connecting player was successful
                ReceiveTcpPacket receiveTcpPacket = new ReceiveTcpPacket(connectedPlayer);
                Thread thread = new Thread(receiveTcpPacket);
                thread.start();
            } else {
                PrintWithTime.Print("Authentication of new player failed horribly.");
            }
        }
    }
}