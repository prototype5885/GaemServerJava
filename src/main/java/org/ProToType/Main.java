package org.ProToType;

import com.google.gson.Gson;
import org.ProToType.Static.*;
import org.ProToType.Threaded.ReceiveTcpPacket;
import org.ProToType.Threaded.ReceiveUdpPacket;
import org.ProToType.Threaded.RunsEverySecond;
import org.ProToType.Threaded.RunsEveryTick;

import javax.swing.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static final Gson gson = new Gson();

    public static int maxPlayers;
    public static int tickRate;
    public static int tcpPort;
    public static int udpPort;
    public static ServerSocket tcpServerSocket;
    public static DatagramSocket udpServerSocket;

    public static SwingGUI swingGUI;


    public static void main(String[] args) throws Exception {
        swingGUI = new SwingGUI();

        JFrame window = new JFrame("alo");
        window.setSize(800, 600);
        window.setContentPane(swingGUI.rootPanel);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);


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

        configFile = null; // nullifies the config file instance as it won't be needed anymore

        ReceiveUdpPacket receiveUdpPacket = new ReceiveUdpPacket();
        Thread udpReceiverThread = new Thread(receiveUdpPacket); // starts thread that listens to incoming udp connections from anyone
        udpReceiverThread.start();

        RunsEveryTick runsEveryTick = new RunsEveryTick(); // starts thread that sends the current player positions to each player
        Thread tickThread = new Thread(runsEveryTick);
        tickThread.start();

        RunsEverySecond runsEverySecond = new RunsEverySecond();
        Thread oneSecondThread = new Thread(runsEverySecond);
        oneSecondThread.start();

        // Handle new players joining
        while (true) {
            try {
                PrintWithTime.print("Waiting for a player to connect...");
                Socket tcpClientSocket = tcpServerSocket.accept();
                PlayerAuthenticator playerAuthenticator = new PlayerAuthenticator();
                playerAuthenticator.StartAuthentication(tcpClientSocket);
            } catch (Exception e) {
                PrintWithTime.print(e.getMessage());
            }
        }

    }
}