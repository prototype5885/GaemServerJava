package org.ProToType;

import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.*;
import org.ProToType.Threaded.HandleNewPlayers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.Player;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Instanceables.Database;
import org.ProToType.Static.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public final int maxPlayers;
    public final int tickRate;
    private final int tcpPort;
    //    public final int udpPort;
    public final ServerSocket tcpServerSocket;
    // public final DatagramSocket udpServerSocket;

    public final Player[] players;
    private int playerCount = 0;

    public final Database database;

    public final ConcurrentLinkedQueue<Packet> packetsToProcess = new ConcurrentLinkedQueue<Packet>();

    private final Object lock = new Object();

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
        players = new Player[maxPlayers];

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

        // server loop
        long startTime;
        long endTime;
        long elapsedTime;
        long sleepTime;
        while (true) {
            if (playerCount == 0) {
                synchronized (lock) {
                    logger.info("No player present on server, pausing main loop...");
                    lock.wait();
                    logger.info("A player is present, continuing main loop...");
                }
            }

            // starts the loop
            startTime = System.currentTimeMillis();

            ProcessPacketsSentByPlayers();

            // ends the loop
            endTime = System.currentTimeMillis();
            elapsedTime = endTime - startTime;
            sleepTime = 99 - elapsedTime;


            if (sleepTime < 0) {
                logger.trace("Skipped sleep time, sleepTime: {}", sleepTime);
                continue;
            }
            Thread.sleep(sleepTime);
            logger.trace("Tick finished calculations in: {} ms, then slept for: {} ms", elapsedTime, System.currentTimeMillis() - endTime);
        }
    }

    public PlayerData GetDataOfPlayer(Player player) {
        // status means if player is connecting or disconnecting
        PlayerData playerData = new PlayerData();
        playerData.i = player.index;
        playerData.s = player.status;
        playerData.un = player.playerName;

        return playerData;
    }

    public PlayerData[] GetDataOfEveryPlayers() {
        PlayerData[] playerDataArray = new PlayerData[maxPlayers];
        for (int i = 0; i < maxPlayers; i++) {
            if (players[i] == null) continue;

            PlayerData playerData = new PlayerData();
            playerData.i = players[i].index;
            playerData.s = players[i].status;
            playerData.un = players[i].playerName;

            playerDataArray[i] = playerData;
        }
        return playerDataArray;
    }

    private void UpdatePlayerPosition(Player player, String playerPosString) {
        try {
            player.position = Main.jackson.readValue(playerPosString, PlayerPosition.class);
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
//            logger.debug("Closed socket for {}", tcpClientSocket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error closing socket for {}: {}", tcpClientSocket.getInetAddress(), e.toString());
        }

        logger.debug("Searching for {} in the player array to remove...", tcpClientSocket.getInetAddress());
        for (int i = 0; i < maxPlayers; i++) {
            if (players[i] != null && players[i].tcpClientSocket.equals(tcpClientSocket)) {
                logger.debug("Found {} in the player array, removing...", players[i].playerName);
                DeletePlayer(i);
            }
        }
    }

    public void DeletePlayer(int index) {
        // removing and calculating connected players
        players[index] = null;
        CalculateConnectedPlayersCount();
        logger.info("Removed player, number of players on server: {}", playerCount);

        // sending to other players
        logger.debug("Sending the disconnection info to each player...");
        PlayerData playerData = new PlayerData();
        playerData.i = index;
        playerData.s = 0;

        SendToEveryone(20, playerData);
    }

    public void AddPlayer(Player newPlayer) {
        // adding and calculating connected players
        players[newPlayer.index] = newPlayer;
        CalculateConnectedPlayersCount();
        logger.info("Added player, number of players on server : {}", playerCount);

        // sending to other players
        logger.debug("Sending data of player {} to everyone except the new player...", newPlayer.playerName);
        PlayerData playerData = GetDataOfPlayer(newPlayer);
        SendToEveryoneExcept(20, playerData, newPlayer);

        logger.debug("Sending every player's data to the new player {}...", newPlayer.playerName);
        PlayerData[] playerDataArray = GetDataOfEveryPlayers();
        SendToOnePlayer(21, playerDataArray, newPlayer);
    }

    private void CalculateConnectedPlayersCount() {
        // calculated player count
        int count = 0;
        for (Player player : players) {
            if (player != null) {
                count++;
            }
        }
        playerCount = count;

        // prints current player count
        if (playerCount > 1) {
            logger.info("There are currently {} players on the server", playerCount);
        } else {
            logger.info("There is currently {} player on the server", playerCount);
        }

        // resumes main loop if there is at least a player present
        if (playerCount > 0) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public void SendToOnePlayer(int type, Object obj, Player player) {
        try {
            logger.debug("Sending message type {} to: {}", type, player.playerName);
            byte[] bytesToSend = PacketProcessor.MakePacketForSending(type, obj, player.aesKey);
            SendTcp(bytesToSend, player.tcpClientSocket);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    public void SendToEveryoneExcept(int type, Object obj, Player playerToSkip) {
        for (Player player : players) {
            if (player == null || player == playerToSkip) continue;
            try {
                SendToOnePlayer(type, obj, player);
            } catch (Exception e) {
                logger.error(e.toString());
            }
        }
    }

    public void SendToEveryone(int type, Object obj) {
        for (Player player : players) {
            if (player == null) continue;
            try {
                SendToOnePlayer(type, obj, player);
            } catch (Exception e) {
                logger.error(e.toString());
            }
        }
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
        Packet packet;
        while ((packet = packetsToProcess.poll()) != null) {
//            Packet packet = packetsToProcess.poll();
            if (packet != null) {
                switch (packet.type) {
                    case 30:
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

    public void SendChatMessageToEveryone(Player msgSender, String chatMessageJson) {
        try {
            logger.trace("Making ChatMessage object and then packet for the message that player {} sent", msgSender.playerName);
            ChatMessage chatMessage = Main.jackson.readValue(chatMessageJson, ChatMessage.class);
            chatMessage.i = msgSender.index;

            // send the message to every connected players
            SendToEveryone(30, chatMessage);
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
    }
}
