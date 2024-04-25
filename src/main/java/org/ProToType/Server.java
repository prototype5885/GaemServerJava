package org.ProToType;

import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.*;
import org.ProToType.Threaded.WaitForPlayerToConnect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Instanceables.Database;
import org.ProToType.Static.*;
import org.ProToType.Threaded.RunsEverySecond;
import org.ProToType.Threaded.RunsEveryTick;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public int maxPlayers;
    public int tickRate;
    public int tcpPort;
    public int udpPort;
    public ServerSocket tcpServerSocket;
    // public DatagramSocket udpServerSocket;

    public ConnectedPlayer[] connectedPlayers;

    public Database database;

    public final ConcurrentLinkedQueue<Packet> packetsToProcess = new ConcurrentLinkedQueue<Packet>();

    // public static SwingGUI swingGUI;
    public Server() throws Exception {
        // swingGUI = new SwingGUI();
        //
        // JFrame window = new JFrame("alo");
        // window.setSize(800, 600);
        // window.setContentPane(swingGUI.rootPanel);
        // window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // window.setVisible(true);

        // long firstTime = System.nanoTime();
        // while (true) {
        // long difference = System.nanoTime() - firstTime;
        // if (difference >= 100000000) {
        // System.out.println(difference / 1000000);
        // break;
        // }
        // }

        // reads and sets up stuff from config file
        ConfigFile configFile = new ConfigFile();
        Encryption.SetEncryptionKey(configFile.encryptionKey);
        maxPlayers = configFile.maxPlayers;
        tickRate = configFile.tickRate;
        tcpPort = configFile.tcpPort;
        udpPort = tcpPort + 1;

        // creates array that store information about players and empty slots
        connectedPlayers = new ConnectedPlayer[maxPlayers];

        // starts tcp and udp servers
        tcpServerSocket = new ServerSocket(tcpPort);
        // udpServerSocket = new DatagramSocket(udpPort);

        // instantiates database
        database = new Database();
        database.ConnectToDatabase(configFile);

//        Thread.ofVirtual().start(new ReceiveUdpPacket(playersManager));
//        Thread.ofVirtual().start(new RunsEveryTick(this));
//        Thread.ofVirtual().start(new RunsEverySecond(this));
        Thread.ofVirtual().start(new WaitForPlayerToConnect(this));

        while (true) {
            ProcessPacketsSentByPlayers();
        }
    }

    private void CalculatePlayerLatency(ConnectedPlayer connectedPlayer) {
        Duration duration = Duration.between(connectedPlayer.pingRequestTime, Instant.now());
        connectedPlayer.latency = duration.getNano();
    }

    private void SendDataOfConnectedPlayers() {
        // making a list
        logger.debug("Making list of connected players...");
        List<PlayerData> playerDataList = new ArrayList<>();
        for (int i = 0; i < maxPlayers; i++) {
            if (connectedPlayers[i] == null) continue;

            PlayerData playerData = new PlayerData();
            playerData.i = i;
            playerData.un = connectedPlayers[i].playerName;

            playerDataList.add(playerData);
        }
        // making it into a package
        byte[] bytesToSend = null;
        try {
            bytesToSend = PacketProcessor.MakePacketForSending(2, playerDataList);
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
            return;
        }
        if (bytesToSend == null) return;

        // sending it
        logger.debug("Sending it to each player...");
        for (int i = 0; i < maxPlayers; i++) {
            if (connectedPlayers[i] != null) {
                logger.debug("Sending it to: {}", connectedPlayers[i].playerName);
                SendTcp(bytesToSend, connectedPlayers[i].tcpClientSocket);
            }
        }
    }

    private void UpdatePlayerPosition(ConnectedPlayer connectedPlayer, String playerPosString) {
        try {
            connectedPlayer.position = Main.jackson.readValue(playerPosString, PlayerPosition.class);
        } catch (JsonProcessingException e) {
            logger.debug(e.toString());
        }
    }

    public void DisconnectPlayer(Socket tcpClientSocket) {
        logger.info("Disconnecting {}...", tcpClientSocket.getInetAddress());
        try {
            tcpClientSocket.shutdownOutput();
            tcpClientSocket.shutdownInput();
            tcpClientSocket.close();
            logger.debug("Closed socket for {}", tcpClientSocket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error closing socket for {}: {}", tcpClientSocket.getInetAddress(), e.toString());
        }

        logger.debug("Searching for {} in the player list to remove using tcp socket...",
                tcpClientSocket.getInetAddress());
        for (int i = 0; i < maxPlayers; i++) {
            if (connectedPlayers[i] != null && connectedPlayers[i].tcpClientSocket.equals(tcpClientSocket)) {
                ConnectedPlayer playerToDisconnect = connectedPlayers[i];
                logger.debug("Found {} in the player list, removing...", playerToDisconnect.playerName);
                connectedPlayers[i] = null;
                logger.debug("Removed player from the player list, slot status: {}", connectedPlayers[i]);
                return;
            }
        }
        logger.debug("Player not present in the player list was disconnected successfully");
    }

    private int GetConnectedPlayersCount() {
        int playerCount = 0;
        for (ConnectedPlayer connectedPlayer : connectedPlayers) {
            if (connectedPlayer != null) {
                playerCount++;
            }
        }
        return playerCount;
    }


    public void SendTcp(byte[] bytesToSend, Socket tcpClientSocket) {
        try {
            tcpClientSocket.getOutputStream().write(bytesToSend);
        } catch (IOException e) {
            logger.error(e.toString());
            DisconnectPlayer(tcpClientSocket);
        }
    }

//    public void SendUdp(String message, ConnectedPlayer connectedPlayer) {
//        try {
//            if (connectedPlayer.udpPort == 0)
//                return;
//            byte[] messageBytes = Shortcuts.EncodeMessage(message);
//            // Monitoring here
//            DatagramPacket udpPacket = new DatagramPacket(messageBytes,
//                    messageBytes.length, connectedPlayer.ipAddress,
//                    connectedPlayer.udpPort);
//            udpServerSocket.send(udpPacket);
//            logger.trace("Sent UDP message to {}: {}", connectedPlayer.ipAddress,
//                    message);
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//    }

    public void ProcessPacketsSentByPlayers() {
        while (!packetsToProcess.isEmpty()) {
            Packet packet = packetsToProcess.poll();
            if (packet != null) {
                switch (packet.type) {
                    case 2:
                        logger.debug("Received a chat message from {}, message: {}", packet.owner.playerName, packet.json);
                        SendChatMessageToEveryone(packet.owner, packet.json);
                        break;
                    //            case 3:
                    //                UpdatePlayerPosition(connectedPlayer, packet.data);
                    //                break;
                }
            }
        }
    }

    public void SendChatMessageToEveryone(ConnectedPlayer msgSender, String chatMessageJson) {
        try {
            logger.trace("Making ChatMessage object and then packet for the message {} sent", msgSender.playerName);
            // prepares object
            ChatMessage chatMessage = Main.jackson.readValue(chatMessageJson, ChatMessage.class);
            chatMessage.i = msgSender.index;

            // makes package
            byte[] bytesToSend = PacketProcessor.MakePacketForSending(2, chatMessage);

            // send the message to each connected player
            logger.trace("Sending chat message from {} to all players...", msgSender.playerName);
            for (ConnectedPlayer player : connectedPlayers) {
                if (player != null) {
                    SendTcp(bytesToSend, player.tcpClientSocket);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
    }
}
