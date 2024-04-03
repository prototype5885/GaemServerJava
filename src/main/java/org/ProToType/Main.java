package org.ProToType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Instanceables.PlayerAuthenticator;
import org.ProToType.Static.*;
import org.ProToType.Threaded.ReceiveUdpPacket;
import org.ProToType.Threaded.RunsEverySecond;
import org.ProToType.Threaded.RunsEveryTick;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Main {
    public static final ObjectMapper jackson = new ObjectMapper();

    public static int maxPlayers;
    public static int tickRate;
    public static int tcpPort;
    public static int udpPort;
    public static ServerSocket tcpServerSocket;
    public static DatagramSocket udpServerSocket;

//    public static SwingGUI swingGUI;


    public static void main(String[] args) throws Exception {
//        swingGUI = new SwingGUI();
//
//        JFrame window = new JFrame("alo");
//        window.setSize(800, 600);
//        window.setContentPane(swingGUI.rootPanel);
//        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        window.setVisible(true);


//        long firstTime = System.nanoTime();
//        while (true) {
//            long difference = System.nanoTime() - firstTime;
//            if (difference >= 100000000) {
//                System.out.println(difference / 1000000);
//                break;
//            }
//        }

        ConfigFile configFile = new ConfigFile(); // reads and sets up stuff from config file
        Encryption.SetEncryptionKey(configFile.encryptionKey);
        maxPlayers = configFile.maxPlayers;
        tickRate = configFile.tickRate;
        tcpPort = configFile.tcpPort;
        udpPort = tcpPort + 1;

        PlayersManager.CreateConnectedPlayerArray(maxPlayers);

        tcpServerSocket = new ServerSocket(tcpPort); // starts the TCP server
        udpServerSocket = new DatagramSocket(udpPort); // starts the UDP server

        Database.ConnectToDatabase(configFile);

        configFile = null; // nullifies the config file instance as it won't be neede   d anymore
        
        Thread.ofVirtual().start(new ReceiveUdpPacket());
        Thread.ofVirtual().start(new RunsEveryTick());
        Thread.ofVirtual().start(new RunsEverySecond());

        // Handle new players joining
        while (true) {
            try {
                Shortcuts.PrintWithTime("Waiting for a player to connect...");
                Socket tcpClientSocket = tcpServerSocket.accept();
                new PlayerAuthenticator().StartAuthentication(tcpClientSocket);
//                playerAuthenticator.StartAuthentication(tcpClientSocket);
            } catch (Exception e) {
                Shortcuts.PrintWithTime(e.getMessage());
            }
        }

    }
}