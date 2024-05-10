package org.ProToType;

import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.*;
import org.ProToType.Threaded.HandleNewPlayers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Instanceables.Database;
import org.ProToType.Static.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public int maxPlayers;
    public int tickRate;
    public int tcpPort;
    //    public int udpPort;
    public ServerSocket tcpServerSocket;
    // public DatagramSocket udpServerSocket;

    public ConnectedPlayer[] connectedPlayers;

    public Database database;

    public final ConcurrentLinkedQueue<Packet> packetsToProcess = new ConcurrentLinkedQueue<Packet>();

    public Server() throws Exception {
        EncryptionAES.Initialize();
        EncryptionRSA.Initialize();

        // reads and sets up stuff from config file
        ConfigFile configFile = new ConfigFile();
        maxPlayers = configFile.maxPlayers;
        tickRate = configFile.tickRate;
        tcpPort = configFile.tcpPort;
//        udpPort = tcpPort + 1;

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

        Thread.ofVirtual().start(new HandleNewPlayers(this));

        while (true) {
            ProcessPacketsSentByPlayers();
        }
    }

    public void SendDataOfConnectedPlayers() {
        // making a list
        logger.debug("Making list of each player's data for sending to everyone...");
        List<PlayerData> playerDataList = new ArrayList<>();
        for (ConnectedPlayer player : connectedPlayers) {
            if (player == null) continue;

            PlayerData playerData = new PlayerData();
            playerData.i = player.index;
            playerData.un = player.playerName;

            playerDataList.add(playerData);
        }

        // sending it to each player
        for (ConnectedPlayer player : connectedPlayers) {
            if (player != null) {
                logger.debug("Sending list of each player's data to: {}", player.playerName);
                try {
                    byte[] bytesToSend = PacketProcessor.MakePacketForSending(3, playerDataList, player.aesKey);
                    SendTcp(bytesToSend, player.tcpClientSocket);
                } catch (Exception e) {
                    logger.error(e.toString());
                }
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
                    case 4:
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

            // send the message to each connected player
            logger.trace("Sending chat message from {} to all players...", msgSender.playerName);
            for (ConnectedPlayer player : connectedPlayers) {
                if (player != null) {
                    byte[] bytesToSend = PacketProcessor.MakePacketForSending(4, chatMessage, player.aesKey);
                    SendTcp(bytesToSend, player.tcpClientSocket);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
    }
}
