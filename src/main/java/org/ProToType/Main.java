package org.ProToType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.*;
import org.ProToType.Threaded.SendTcp;
import org.ProToType.Threaded.WaitForPlayers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.Player;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Static.Database;
import org.ProToType.Static.*;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static final ObjectMapper jackson = new ObjectMapper();

    public static final int maxPlayers;
    public static final int tickRate;
    private static final int tcpPort;
    // public static final int udpPort;
    public static final ServerSocket tcpServerSocket;
    // public static final DatagramSocket udpServerSocket;

    public static final Player[] players;
    public static int playerCount = 0;

    public static final ConcurrentLinkedQueue<Packet> packetsToProcess = new ConcurrentLinkedQueue<Packet>();
    public static final ConcurrentLinkedQueue<Player> playersToAdd = new ConcurrentLinkedQueue<Player>();

    // this locks main loop if no players are present
    private static final Object mainLoopLock = new Object();

    static {
        try {
            // reads and sets up stuff from config file
            ConfigFile configFile = new ConfigFile();
            maxPlayers = configFile.maxPlayers;
            tickRate = configFile.tickRate;
            tcpPort = configFile.tcpPort;
            // udpPort = tcpPort + 1;

            // creates array that store information about players and empty slots
            players = new Player[maxPlayers];

            // starts tcp and udp servers
            tcpServerSocket = new ServerSocket(tcpPort);
            // udpServerSocket = new DatagramSocket(udpPort);

            // instantiates database
            Database.ConnectToDatabase(configFile);

            // SendTcp(new byte[5], new Socket());

        } catch (Exception e) {
            System.out.println("wtf");
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InterruptedException {
        EncryptionAES.Initialize();
        EncryptionRSA.Initialize();

        // Thread.ofVirtual().start(new ReceiveUdpPacket(playersManager));
        // Thread.ofVirtual().start(new RunsEverySecond(this));

        Thread.ofVirtual().start(new WaitForPlayers());

        // server loop
        long oneSecondTimer = System.currentTimeMillis();
        while (true) {
            // if (playerCount == 0) {
            // synchronized (mainLoopLock) {
            // logger.info("No player present on server, pausing main loop...");
            // mainLoopLock.wait();
            // logger.info("A player is present, continuing main loop...");
            // }
            // }

            // loop starts from here
            long startTime = System.currentTimeMillis();

            // adding a new player if somebody connected ----------------------------
            while (!playersToAdd.isEmpty()) {
                Player newPlayer = playersToAdd.poll();
                AddPlayer(newPlayer);
            }

            // processing received packets ------------------------------------
            while (!packetsToProcess.isEmpty()) {
                Packet packet = packetsToProcess.poll();
                if (packet == null)
                    continue;
                try {
                    switch (packet.type) {
                        // chat message
                        case 30:
                            logger.debug("Received chat message from {}, sending message to all: {}",
                                    packet.owner.playerName, packet.json);

                            ChatMessage chatMessage = Main.jackson.readValue(packet.json, ChatMessage.class);
                            chatMessage.i = GetIndexOfPlayer(packet.owner);

                            // send the message to every connected players
                            SendToEveryone(30, chatMessage);
                            break;
                        // player position
                        case 40:
                            logger.debug("Received then forwarding position data from {}, message: {}",
                                    packet.owner.playerName, packet.json);

                            packet.owner.position = Main.jackson.readValue(packet.json, PlayerPosition.class);
                            break;
                    }
                } catch (JsonProcessingException e) {
                    logger.error(e.toString());
                }
            }

            // sends the positions of each player to everyone if server isn't empty ---------------------
            if (playerCount != 0) {
                List<ObjectWithID> playerPositions = new ArrayList<>();
                for (int i = 0; i < maxPlayers; i++) {
                    if (players[i] == null)
                        continue;

                    ObjectWithID playerPosition = new ObjectWithID();
                    playerPosition.i = i;
                    playerPosition.o = players[i].position;

                    playerPositions.add(playerPosition);
                }
                SendToEveryone(40, playerPositions);
            }

            // runs every 1 second ----------------------------------------
            int secondTimerElapsedTime = (int) (System.currentTimeMillis() - oneSecondTimer);
            if (secondTimerElapsedTime >= 975) {
                // logger.trace("secondTimerElapsedTime: " + secondTimerElapsedTime);
                oneSecondTimer = System.currentTimeMillis();
            }

            // ends the loop ---------------------------------------
            long endTime = System.currentTimeMillis();
            int elapsedTime = (int) (endTime - startTime);
            int sleepTime = 99 - elapsedTime;

            if (sleepTime < 0) {
                logger.trace("No need for sleeping, execution took too long, sleepTime: {}", sleepTime);
                continue;
            }
            if (elapsedTime != 0) {
                logger.trace("Tick calculations took: {} ms", elapsedTime);
            }

            Thread.sleep(sleepTime);
            // logger.trace("Tick finished calculations in: {} ms, then slept for: {} ms",
            // elapsedTime, System.currentTimeMillis() - endTime);
        }
    }

    public static int GetIndexOfPlayer(Player player) {
        for (int i = 0; i < maxPlayers; i++) {
            if (player.equals(players[i])) {
                return i;
            }
        }
        return -1;
    }

    public static PlayerData GetDataOfPlayer(Player player) {
        // status means if player is connecting or disconnecting

        PlayerData playerData = new PlayerData();
        playerData.i = GetIndexOfPlayer(player);
        playerData.s = player.status;
        playerData.un = player.playerName;

        return playerData;
    }

    public static PlayerData[] GetDataOfEveryPlayers() {
        PlayerData[] playerDataArray = new PlayerData[maxPlayers];
        for (int i = 0; i < maxPlayers; i++) {
            if (players[i] == null)
                continue;

            PlayerData playerData = new PlayerData();
            playerData.i = i;
            playerData.s = players[i].status;
            playerData.un = players[i].playerName;

            playerDataArray[i] = playerData;
        }
        return playerDataArray;
    }

    public static void AddPlayer(Player newPlayer) {
        try {
            // find which player slot is free
            logger.debug("Searching for a free slot for {}...", newPlayer.playerName);
            int index = -1;
            for (int i = 0; i < Main.players.length; i++) {
                if (Main.players[i] == null) {
                    index = i;
                    logger.debug("Found free slot at slot {}", index);
                    break;
                }
            }

            // stops if no free slots were found
            if (index == -1) {
                logger.warn("Server is full, rejecting player {}...", newPlayer.playerName);
                SendNegativeResponseAndDisconnect(newPlayer.tcpClientSocket, 7, newPlayer.aesKey);
                return;
            }

            // adding and calculating connected players
            players[index] = newPlayer;
            CalculateConnectedPlayersCount();

            // creating InitialData object for replying
            logger.debug("Creating InitialData object to be sent to {}", newPlayer.playerName);
            InitialData initialData = new InitialData();
            initialData.loginResultValue = 1;
            initialData.index = index;
            initialData.maxPlayers = Main.maxPlayers;
            initialData.tickRate = Main.tickRate;
            initialData.playersData = Main.GetDataOfEveryPlayers();

            // reply back to the player about the authentication success
            logger.debug("Sending positive reply about authentication back to {}...", newPlayer.playerName);
            byte[] bytesToSend = PacketProcessor.MakePacketForSending(1, initialData, newPlayer.aesKey);
            // SendTcp(bytesToSend, newPlayer.tcpClientSocket);
            Thread.ofVirtual().start(new SendTcp(bytesToSend, newPlayer.tcpClientSocket));

            // sending info about it to other players
            logger.debug("Sending data of new player {} to everyone except the new player...", newPlayer.playerName);
            PlayerData playerData = GetDataOfPlayer(newPlayer);
            SendToEveryoneExcept(20, playerData, newPlayer);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static void DisconnectPlayer(Socket tcpClientSocket) {
        logger.info("Disconnecting {}...", tcpClientSocket.getInetAddress());
        try {
            tcpClientSocket.shutdownOutput();
            tcpClientSocket.shutdownInput();
            tcpClientSocket.close();
            // logger.debug("Closed socket for {}", tcpClientSocket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error closing socket for {}: {}", tcpClientSocket.getInetAddress(), e.toString());
        }

        logger.debug("Searching for {} in the player array to remove...", tcpClientSocket.getInetAddress());
        for (int i = 0; i < maxPlayers; i++) {
            if (players[i] != null && players[i].tcpClientSocket.equals(tcpClientSocket)) {
                logger.debug("Found {} in the player array, removing...", players[i].playerName);

                // removing and calculating connected players
                players[i] = null;
                CalculateConnectedPlayersCount();
                logger.info("Removed player, number of players on server: {}", playerCount);

                // sending info to other players
                logger.debug("Sending the disconnection info to each player...");
                PlayerData playerData = new PlayerData();
                playerData.i = i;
                playerData.s = 0;

                SendToEveryone(20, playerData);
            }
        }
    }

    public static void SendNegativeResponseAndDisconnect(Socket tcpClientSocket, int resultValue, byte[] aesKey) {
        try {
            logger.debug("Sending negative initial data with value {} to the failed player...", resultValue);

            InitialData initialData = new InitialData();
            initialData.loginResultValue = resultValue;

            byte[] bytesToSend = PacketProcessor.MakePacketForSending(1, initialData, aesKey);
            // SendTcp(bytesToSend, tcpClientSocket);
            Thread.ofVirtual().start(new SendTcp(bytesToSend, tcpClientSocket));

            logger.debug("Closing connection of the failed player in 1 second...");
            Thread.sleep(1000);
            Main.DisconnectPlayer(tcpClientSocket);

            logger.debug("Connection with {} has been terminated successfully", tcpClientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private static void CalculateConnectedPlayersCount() {
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
            synchronized (mainLoopLock) {
                mainLoopLock.notify();
            }
        }
    }

    public static void SendToOnePlayer(int type, Object obj, Player player) {
        try {
            logger.debug("Sending message type {} to: {}", type, player.playerName);
            byte[] bytesToSend = PacketProcessor.MakePacketForSending(type, obj, player.aesKey);
            // SendTcp(bytesToSend, player.tcpClientSocket);
            Thread.ofVirtual().start(new SendTcp(bytesToSend, player.tcpClientSocket));
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    public static void SendToEveryoneExcept(int type, Object obj, Player playerToSkip) {
        for (Player player : players) {
            if (player == null || player == playerToSkip)
                continue;
            SendToOnePlayer(type, obj, player);
        }
    }

    public static void SendToEveryone(int type, Object obj) {
        for (Player player : players) {
            if (player == null)
                continue;
            SendToOnePlayer(type, obj, player);
        }
    }

    // public static void SendTcp(byte[] bytesToSend, Socket tcpClientSocket) {
    // try {
    // tcpClientSocket.getOutputStream().write(bytesToSend);
    // } catch (IOException e) {
    // logger.error(e.toString());
    // DisconnectPlayer(tcpClientSocket);
    // }
    // }

    // public void SendUdp(String message, ConnectedPlayer connectedPlayer) {
    // try {
    // if (connectedPlayer.udpPort == 0)
    // return;
    // byte[] messageBytes = Shortcuts.EncodeMessage(message);
    // // Monitoring here
    // DatagramPacket udpPacket = new DatagramPacket(messageBytes,
    // messageBytes.length, connectedPlayer.ipAddress,
    // connectedPlayer.udpPort);
    // udpServerSocket.send(udpPacket);
    // logger.trace("Sent UDP message to {}: {}", connectedPlayer.ipAddress,
    // message);
    // } catch (IOException e) {
    // System.out.println(e.getMessage());
    // }
    // }
}
