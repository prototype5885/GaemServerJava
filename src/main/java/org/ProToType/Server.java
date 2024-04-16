package org.ProToType;

import org.ProToType.ClassesShared.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Classes.LoginResult;
import org.ProToType.ClassesShared.InitialData;
import org.ProToType.ClassesShared.LoginData;
import org.ProToType.ClassesShared.PlayerData;
import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.Instanceables.ConfigFile;
import org.ProToType.Instanceables.Database;
import org.ProToType.Static.*;
import org.ProToType.Threaded.ReceiveTcpPacket;
import org.ProToType.Threaded.ReceiveUdpPacket;
import org.ProToType.Threaded.RunsEverySecond;
import org.ProToType.Threaded.RunsEveryTick;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public int maxPlayers;
    public int tickRate;
    public int tcpPort;
    public int udpPort;
    public ServerSocket tcpServerSocket;
    public DatagramSocket udpServerSocket;

    public ConnectedPlayer[] connectedPlayers;

    public Database database;

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
        udpServerSocket = new DatagramSocket(udpPort);

        // instantiates database
        database = new Database();
        database.ConnectToDatabase(configFile);

        // Thread.ofVirtual().start(new ReceiveUdpPacket(playersManager));
        // Thread.ofVirtual().start(new RunsEveryTick(playersManager));
        // Thread.ofVirtual().start(new RunsEverySecond(playersManager, database));

        new Thread(new ReceiveUdpPacket(this)).start();
        new Thread(new RunsEveryTick(this)).start();
        new Thread(new RunsEverySecond(this)).start();

        // Handle new players joining
        while (true) {
            try {
                // waits for a player to connect
                logger.debug("Waiting for a player to connect...");
                Socket tcpClientSocket = tcpServerSocket.accept();
                logger.info("A player from ip {} connected", tcpClientSocket.getInetAddress());

                // starts the authentication that will return player instance on success
                ConnectedPlayer connectedPlayer = StartAuthentication(tcpClientSocket);

                if (connectedPlayer != null) {
                    logger.info("Authentication of {} was success", connectedPlayer.playerName);
                    // Thread.ofVirtual().start(new ReceiveTcpPacket(playersManager,
                    // connectedPlayer));
                    new Thread(new ReceiveTcpPacket(this, connectedPlayer));
                } else {
                    logger.info("Authentication of {} was failure", tcpClientSocket.getInetAddress());
                }
            } catch (Exception e) {
                logger.debug("Exception during authentication of a connecting player: {}", e.toString());
            }
        }
    }

    private ConnectedPlayer StartAuthentication(Socket tcpClientSocket) throws SQLException {
        String clientIpAddress = tcpClientSocket.getInetAddress().toString();
        logger.debug("Started authentication for {}...", clientIpAddress);

        // find which player slot is free
        logger.debug("Searching a free slot for {}...", clientIpAddress);
        int foundSlot = -1;
        for (int i = 0; i < connectedPlayers.length; i++) {
            if (connectedPlayers[i] == null) {
                foundSlot = i;
                logger.debug("Found free slot at slot {}", foundSlot);
                break;
            }
        }
        // runs if server is full
        if (foundSlot == -1) {
            logger.debug("Server is full, rejecting {}", clientIpAddress);
            SendNegativeResponseAndDisconnect(tcpClientSocket, 7);
            return null;
        }

        // Reading LoginData sent by the player
        logger.debug("Reading LoginData sent by {}", clientIpAddress);
        byte[] buffer = new byte[512];
        int bytesRead = 0;
        try {
            bytesRead = tcpClientSocket.getInputStream().read(buffer);
        } catch (IOException e) {
            logger.error("Failed reading from inputstream of {}, aborting authentication", clientIpAddress);
            DisconnectPlayer(tcpClientSocket);
            return null;
        }

        // read and process the LoginData sent by the player
        logger.debug("Processing received LoginData from {}...", clientIpAddress);
        String decodedMessage = PacketProcessor.Decode(buffer, bytesRead);
        List<Packet> packets = PacketProcessor.SeparatePackets(decodedMessage);

        LoginData loginData = null;
        for (Packet packet : packets) {
            if (packet.type == 0) {
                try {
                    loginData = Main.jackson.readValue(packet.json, LoginData.class);
                    logger.debug("Player connecting from {} identifies as {}", clientIpAddress, loginData.un);
                } catch (JsonProcessingException e) {
                    logger.error("Error processing data received from {}: {}", clientIpAddress, e.toString());
                    DisconnectPlayer(tcpClientSocket);
                    return null;
                }
                break;
            } else {
                logger.error("No LoginData packet was received from {}, aborting authentication", clientIpAddress);
                DisconnectPlayer(tcpClientSocket);
                return null;
            }
        }

        // runs if player wants to register first
        if (loginData.reg) {
            logger.debug("Registering player {}...", loginData.un);
            // Checks if chosen name is longer than 16 or shorter than 2 characters
            logger.debug("Checking chosen name's length for player {}...", loginData.un);
            if (loginData.un.length() < 2 || loginData.un.length() > 16) {
                logger.debug("Player has {} chosen too long or too short name, registration failed", loginData.un);
                SendNegativeResponseAndDisconnect(tcpClientSocket, 5);
                return null;
            }
            // Checks if the chosen name is already registered
            logger.debug("Checking if chosen name {} is already registered...", loginData.un);
            try (ResultSet resultSet = database.SearchForPlayerInDatabase(loginData.un)) {
                if (resultSet != null) {
                    logger.debug("Player name {} already exists in the database, registration failed", loginData.un);
                    SendNegativeResponseAndDisconnect(tcpClientSocket, 6);
                    return null;
                }
            }
            // Adds the new player to the database
            logger.debug("Successful registration, adding new player {} to the database", loginData.un);
            database.RegisterPlayer(loginData.un, loginData.pw);
        }

        // logs the player in
        logger.debug("Logging in player {}...", loginData.un);

        // searches if player is already connected to the server
        logger.debug("Checking if player {} is connected already...", loginData.un);
        for (ConnectedPlayer player : connectedPlayers) {
            if (player == null)
                continue;
            if (loginData.un.equals(player.playerName)) {
                logger.debug("Player {} is already connected, login failed", loginData.un);
                SendNegativeResponseAndDisconnect(tcpClientSocket, 4);
                return null;
            }
        }

        // searches for the player in the database
        logger.debug("Searching for player {} in the database...", loginData.un);
        LoginResult loginResult = new LoginResult();
        try (ResultSet resultSet = database.SearchForPlayerInDatabase(loginData.un)) {
            // if player was not found in database
            if (resultSet == null) {
                logger.debug("Player {} not found in database, login failed.", loginData.un);
                SendNegativeResponseAndDisconnect(tcpClientSocket, 3);
                return null;
            }

            // reads password from database then compares
            logger.debug("Comparing entered password for player {}...", loginData.un);
            String storedHashedPassword = resultSet.getString("Password");
            if (!loginData.pw.equals(storedHashedPassword)) {
                logger.debug("Player {} has entered wrong password, login failed", loginData.un);
                SendNegativeResponseAndDisconnect(tcpClientSocket, 2);
                return null;
            }

            // login was a success
            logger.debug("Successful login for player {}", loginData.un);
            loginResult.resultValue = 1;
            loginResult.dbindex = resultSet.getInt("id");
            loginResult.playerName = resultSet.getString("PlayerName");
        }

        // creates the ConnectedPlayer object
        logger.debug("Creating ConnectedPlayer object for {}", loginData.un);
        ConnectedPlayer connectedPlayer = new ConnectedPlayer();
        try {
            connectedPlayer.databaseID = loginResult.dbindex;
            connectedPlayer.playerName = loginResult.playerName;
            connectedPlayer.tcpClientSocket = tcpClientSocket;
            connectedPlayer.inputStream = tcpClientSocket.getInputStream();
            connectedPlayer.outputStream = tcpClientSocket.getOutputStream();
            connectedPlayer.ipAddress = tcpClientSocket.getInetAddress();
            connectedPlayer.tcpPort = tcpClientSocket.getPort();
        } catch (IOException e) {
            logger.error("Failed getting input or output streams of {}, aborting authentication",
                    tcpClientSocket.getInetAddress());
        }

        // creating InitialData object
        logger.debug("Creating InitialData object to be sent to {}", loginData.un);
        InitialData initialData = new InitialData();
        initialData.loginResult = loginResult.resultValue;
        initialData.index = foundSlot;
        initialData.maxPlayers = maxPlayers;
        initialData.tickRate = tickRate;
        initialData.udpPort = udpPort;

        // reply back to the player about the authentication success
        logger.debug("Sending positive reply about authentication back to {}...", loginData.un);
        String jsonData = null;
        try {
            Packet packet = new Packet();
            packet.type = 0;
            packet.json = Main.jackson.writeValueAsString(initialData);

            String message = Main.jackson.writeValueAsString(packet);

            SendTcp(message, tcpClientSocket);
        } catch (Exception e) {
            logger.error(e.toString());
            DisconnectPlayer(tcpClientSocket);
            return null;
        }

        // sets last login ip address
        database.UpdateLastLoginIpAddress(connectedPlayer);

        // adds the new player to the list of connected players
        logger.debug("Adding player {} to the list of connected players...", loginResult.playerName);
        connectedPlayers[foundSlot] = connectedPlayer;

        // returns success
        return connectedPlayer;
    }

    private void SendNegativeResponseAndDisconnect(Socket tcpClientSocket, int resultValue) {
        try {
            logger.debug("Sending negative initial data with value {} to the failed player...", resultValue);

            InitialData initialData = new InitialData();
            initialData.loginResult = resultValue;

            Packet packet = new Packet();
            packet.type = 0;
            packet.json = Main.jackson.writeValueAsString(initialData);

            String message = Main.jackson.writeValueAsString(packet);

            SendTcp(message, tcpClientSocket);

            logger.debug("Closing connection of the failed player in 1 second...");
            Thread.sleep(1000);
            DisconnectPlayer(tcpClientSocket);

            logger.debug("Connection with {} has been terminated successfully", tcpClientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private void CalculatePlayerLatency(ConnectedPlayer connectedPlayer) {
        Duration duration = Duration.between(connectedPlayer.pingRequestTime, Instant.now());
        connectedPlayer.latency = duration.getNano();
    }


    private PlayerData GetDataOfConnectedPlayer(int index) {
        PlayerData playerData = new PlayerData();
        playerData.i = index;
        playerData.un = connectedPlayers[index].playerName;

        return playerData;
    }

    private void UpdatePlayerPosition(ConnectedPlayer connectedPlayer, String playerPosString) {
        try {
            connectedPlayer.position = Main.jackson.readValue(playerPosString, PlayerPosition.class);
        } catch (JsonProcessingException e) {
            logger.debug(e.toString());
        }
    }

    public void DisconnectPlayer(Socket tcpClientSocket) {
        try {
            tcpClientSocket.shutdownOutput();
            tcpClientSocket.shutdownInput();
            tcpClientSocket.close();
            logger.info("Closed socket for {}", tcpClientSocket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error closing socket for {}: {}", tcpClientSocket.getInetAddress(), e.toString());
        }

    }

    public void RemovePlayerFromList(ConnectedPlayer connectedPlayer) {
        logger.debug("Removing player {} from connected players list...", connectedPlayer.playerName);
        for (int i = 0; i < maxPlayers; i++) {
            if (connectedPlayers[i] != null && connectedPlayer.equals(connectedPlayers[i])) {
                connectedPlayers[i] = null;
                logger.trace("Slot status for player {}: {}", connectedPlayer.playerName, connectedPlayers[i]);
                logger.debug("Removed player {} from connected players' list successfully", connectedPlayer.playerName);
                break;
            }
        }
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

//     public void SeparatePackets(List<Packet> packets) {
//     // Processes each packet
//     for (Packet packet : packets) {
//     if (packet == null)
//     continue;
//
//     switch (packet.type) {
//     case 0:
//     connectedPlayer.udpPingAnswered = true;
//     connectedPlayer.status = 1;
//     // Calculate latency here
//     break;
//     case 2:
//     // playersManager.SendChatMessageToEveryone(connectedPlayer, packet.data);
//     break;
//     case 3:
//     UpdatePlayerPosition(connectedPlayer, packet.data);
//     break;
//     }
//     }
//     }

    private void SendTcp(String message, Socket tcpClientSocket) throws IOException {
        byte[] messageBytes = EncodeMessage(message);
        tcpClientSocket.getOutputStream().write(messageBytes);
        logger.trace("Sent TCP message to {}: {}", tcpClientSocket.getInetAddress(), message);
    }

    public void SendUdp(String message, ConnectedPlayer connectedPlayer) {
        try {
            if (connectedPlayer.udpPort == 0)
                return;
            byte[] messageBytes = EncodeMessage(message);
            // Monitoring here
            DatagramPacket udpPacket = new DatagramPacket(messageBytes, messageBytes.length, connectedPlayer.ipAddress,
                    connectedPlayer.udpPort);
            udpServerSocket.send(udpPacket);
            logger.trace("Sent UDP message to {}: {}", connectedPlayer.ipAddress, message);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private byte[] EncodeMessage(String message) {
        String messageString = message + "\n";
        if (Encryption.encryptionEnabled) {
            return Encryption.Encrypt(messageString);
        } else {
            return (messageString).getBytes();
        }
    }

    // public void SendChatMessageToEveryone(ConnectedPlayer messageSenderPlayer,
    // String message) throws JsonProcessingException {
    // ChatMessage chatMessage = new ChatMessage();
    // chatMessage.i = messageSenderPlayer.index;
    // chatMessage.m = message;
    //
    // String jsonData = Main.jackson.writeValueAsString(chatMessage);
    // for (ConnectedPlayer player : PlayersManager.connectedPlayers) {
    // if (player == null) continue;
    // SendTcp(2, jsonData, player);
    // }
    // }
    //
    //
    // public void SendPlayerDataToEveryone(int maxPlayers) {
    // String jsonData =
    // gson.toJson(playersManager.GetDataOfEveryConnectedPlayer(maxPlayers));
    // for (ConnectedPlayer player : connectedPlayers) {
    // if (player == null) continue;
    // }
    // }
}
