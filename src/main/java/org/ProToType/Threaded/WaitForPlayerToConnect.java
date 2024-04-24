package org.ProToType.Threaded;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Classes.LoginResult;
import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.InitialData;
import org.ProToType.ClassesShared.LoginData;
import org.ProToType.Main;
import org.ProToType.Server;
import org.ProToType.Static.PacketProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WaitForPlayerToConnect implements Runnable {
    private static final Logger logger = LogManager.getLogger(WaitForPlayerToConnect.class);

    private Server server;

    public WaitForPlayerToConnect(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // waits for a player to connect
                logger.debug("Waiting for a player to connect...");
                Socket tcpClientSocket = server.tcpServerSocket.accept();
                long startTime = System.nanoTime();
                logger.info("A player from ip {} connected...", tcpClientSocket.getInetAddress());

                // starts the authentication that will return player instance on success
                ConnectedPlayer connectedPlayer = StartAuthentication(tcpClientSocket);

                if (connectedPlayer != null) {
                    logger.info("Authentication of {} ({}) was success", tcpClientSocket.getInetAddress(), connectedPlayer.playerName);
                    Thread.ofVirtual().start(new ReceiveTcpPacket(server, connectedPlayer));
//                    new Thread(new ReceiveTcpPacket(this, connectedPlayer)).start();
                }
//                else {
//                    logger.info("Authentication of {} was failure", tcpClientSocket.getInetAddress());
//                }
                long endTime = System.nanoTime();
                long elapsedTimeInMillis = (endTime - startTime) / 1000000; // nano to milli
                logger.trace("Finished authentication in: {} ms", elapsedTimeInMillis);
            } catch (Exception e) {
                logger.error("Exception during authentication of a connecting player: {}", e.toString());
            }
        }
    }

    private ConnectedPlayer StartAuthentication(Socket tcpClientSocket) throws SQLException {
        String clientIpAddress = tcpClientSocket.getInetAddress().toString();
        logger.debug("Started authentication for {}...", clientIpAddress);

        // find which player slot is free
        logger.debug("Searching a free slot for {}...", clientIpAddress);
        int foundSlot = -1;
        for (int i = 0; i < server.connectedPlayers.length; i++) {
            if (server.connectedPlayers[i] == null) {
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
            server.DisconnectPlayer(tcpClientSocket);
            return null;
        }

        // read and process the LoginData sent by the player
        logger.debug("Processing received LoginData from {}...", clientIpAddress);
        String decodedMessage = PacketProcessor.Decode(buffer, bytesRead);
        List<Packet> packets = PacketProcessor.SeparatePackets(decodedMessage, null);

        LoginData loginData = null;
        for (Packet packet : packets) {
            if (packet.type == 1) {
                try {
                    loginData = Main.jackson.readValue(packet.json, LoginData.class);
                    logger.debug("reg: {}", loginData.reg);
                    logger.debug("un: {}", loginData.un);
                    logger.debug("pw: {}", loginData.pw);
                    logger.debug("Player connecting from {} identifies as {}", clientIpAddress, loginData.un);
                } catch (JsonProcessingException e) {
                    logger.error("Error processing data received from {}: {}", clientIpAddress,
                            e.toString());
                    server.DisconnectPlayer(tcpClientSocket);
                    return null;
                }
                break;
            } else {
                logger.error("No LoginData type packet was received from {}, aborting authentication", clientIpAddress);
                server.DisconnectPlayer(tcpClientSocket);
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
            try (ResultSet resultSet = server.database.SearchForPlayerInDatabase(loginData.un)) {
                if (resultSet != null) {
                    logger.debug("Player name {} already exists in the database, registration failed", loginData.un);
                    SendNegativeResponseAndDisconnect(tcpClientSocket, 6);
                    return null;
                }
            }
            // Adds the new player to the database
            logger.debug("Successful registration, adding new player {} to the database", loginData.un);
            server.database.RegisterPlayer(loginData.un, loginData.pw);
        }

        // logs the player in
        logger.debug("Logging in player {}...", loginData.un);

        // searches if player is already connected to the server
        logger.debug("Checking if player {} is connected already...", loginData.un);
        for (ConnectedPlayer player : server.connectedPlayers) {
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
        try (ResultSet resultSet = server.database.SearchForPlayerInDatabase(loginData.un)) {
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
            // login was a success, puts result in a login result
            logger.debug("Successful login for player {}", loginData.un);
            loginResult.setResultValue(1);
            loginResult.setDbindex(resultSet.getInt("id"));
            loginResult.setPlayerName(resultSet.getString("PlayerName"));
        }

        // creates the ConnectedPlayer object
        logger.debug("Creating ConnectedPlayer object for {}", loginData.un);
        ConnectedPlayer connectedPlayer = new ConnectedPlayer();
        try {
            connectedPlayer.index = foundSlot;
            connectedPlayer.databaseID = loginResult.getDbindex();
            connectedPlayer.playerName = loginResult.getPlayerName();
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
        initialData.setLoginResult(loginResult.getResultValue());
        initialData.setIndex(foundSlot);
        initialData.setMaxPlayers(server.maxPlayers);
        initialData.setTickRate(server.tickRate);
        initialData.setUdpPort(server.udpPort);

        // reply back to the player about the authentication success
        logger.debug("Sending positive reply about authentication back to {}...", loginData.un);
        try {
            byte[] bytesToSend = PacketProcessor.MakePacketForSending(1, initialData);
            server.SendTcp(bytesToSend, tcpClientSocket);
        } catch (Exception e) {
            logger.error(e.toString());
            server.DisconnectPlayer(tcpClientSocket);
            return null;
        }

        // sets last login ip address
        server.database.UpdateLastLoginIpAddress(connectedPlayer);

        // adds the new player to the list of connected players
        logger.debug("Adding player {} to the list of connected players...", loginResult.getPlayerName());
        server.connectedPlayers[foundSlot] = connectedPlayer;

        // returns success
        return connectedPlayer;
    }

    private void SendNegativeResponseAndDisconnect(Socket tcpClientSocket, int resultValue) {
        try {
            logger.debug("Sending negative initial data with value {} to the failed player...", resultValue);

            InitialData initialData = new InitialData();
            initialData.setLoginResult(resultValue);

            byte[] bytesToSend = PacketProcessor.MakePacketForSending(1, initialData);
            server.SendTcp(bytesToSend, tcpClientSocket);

            logger.debug("Closing connection of the failed player in 1 second...");
            Thread.sleep(1000);
            server.DisconnectPlayer(tcpClientSocket);

            logger.debug("Connection with {} has been terminated successfully", tcpClientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }
}
