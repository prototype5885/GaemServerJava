package org.ProToType.Threaded;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Classes.Packet;
import org.ProToType.ClassesShared.InitialData;
import org.ProToType.ClassesShared.LoginData;
import org.ProToType.Main;
import org.ProToType.Server;
import org.ProToType.Static.ByteProcessor;
import org.ProToType.Static.EncryptionAES;
import org.ProToType.Static.PacketProcessor;
import org.ProToType.Static.EncryptionRSA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;


public class HandleNewPlayers implements Runnable {
    private static final Logger logger = LogManager.getLogger(HandleNewPlayers.class);

    private Server server;

    public HandleNewPlayers(Server server) {
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

                // starts RSA handshake that will return the AES and RSA encryption keys
                byte[] aesKey = ExchangeSymmetricKey(tcpClientSocket);
                ByteProcessor.PrintByteArrayAsHex(aesKey);

                // starts the authentication that will return player instance on success
                ConnectedPlayer connectedPlayer = StartAuthentication(tcpClientSocket, aesKey);

                if (connectedPlayer != null) {
                    logger.info("Authentication of {} ({}) was success", tcpClientSocket.getInetAddress(), connectedPlayer.playerName);
                    Thread.ofVirtual().start(new ReceiveTcpPacket(server, connectedPlayer));
//                    new Thread(new ReceiveTcpPacket(this, connectedPlayer)).start();
                } else {
                    logger.info("Authentication of {} was failure", tcpClientSocket.getInetAddress());
                }
                long endTime = System.nanoTime();
                long elapsedTimeInMillis = (endTime - startTime) / 1000000; // nano to milli
                logger.trace("Finished authentication in: {} ms", elapsedTimeInMillis);
            } catch (Exception e) {
                logger.error("Exception during authentication of {}, aborting... ", e.toString());
            }
        }
    }

    private byte[] ExchangeSymmetricKey(Socket tcpClientSocket) throws Exception {
        String clientIpAddress = tcpClientSocket.getInetAddress().toString();
        // Processing handshake request sent by the connecting player
        logger.debug("Waiting for {} to send a handshake request...", clientIpAddress);

        byte[] aloReceivedBytes = ReceiveTcpPacket.ReceiveBytes(tcpClientSocket);

        logger.trace("Received message {} from {}", new String(aloReceivedBytes, StandardCharsets.UTF_8), clientIpAddress);
        if (!Arrays.equals(aloReceivedBytes, "alo".getBytes())) {
            logger.error("Improper handshake request received from {}, aborting handshake", clientIpAddress);
            server.DisconnectPlayer(tcpClientSocket);
            return null;
        }
        logger.debug("Handshake request received from {}, " +
                "sending public key to the player...", clientIpAddress);
        logger.debug("Printing own key of length {}", EncryptionRSA.keypair.getPublic().getEncoded().length);

        // sending public key to player
        server.SendTcp(EncryptionRSA.keypair.getPublic().getEncoded(), tcpClientSocket);

        // waiting for player to send its own public key
        logger.debug("Waiting now for {} to send it's own public key...", clientIpAddress);


        byte[] encryptedKeys = ReceiveTcpPacket.ReceiveBytes(tcpClientSocket);

        // separate
        logger.trace("Separating encrypted client rsa public key...");
        byte[] encryptedClientPublicKey = new byte[576];
        System.arraycopy(encryptedKeys, 0, encryptedClientPublicKey, 0, 576);

        logger.trace("Separating encrypted client aes key...");
        byte[] encryptedClientAesKey = new byte[512];
        System.arraycopy(encryptedKeys, 576, encryptedClientAesKey, 0, 512);

        byte[] decryptedAesKey = EncryptionRSA.Decrypt(encryptedClientAesKey);
        byte[] decryptedClientPublicKey = EncryptionAES.Decrypt(encryptedClientPublicKey, decryptedAesKey);


        // decrypting the public key using local private key
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(decryptedClientPublicKey);
        PublicKey clientPublicKey = KeyFactory.getInstance("RSA").generatePublic(x509EncodedKeySpec);

        // sending a unique AES key to the player encrypted using the player's public key
        logger.debug("Sending a random AES key to {}", clientIpAddress);
        byte[] aesKey = EncryptionAES.GenerateRandomKey();
        byte[] encryptedAesKey = EncryptionRSA.Encrypt(aesKey, clientPublicKey);
        server.SendTcp(encryptedAesKey, tcpClientSocket);

        // testing if it works
//        logger.debug("Sending test...");
//        byte[] encryptedBytes = EncryptionAES.Encrypt("test from server", aesKey);
//        server.SendTcp(encryptedBytes, tcpClientSocket);
//
//        logger.trace("Waiting for response...");
//        byte[] testReceivedBytes = ReceiveTcpPacket.ReceiveBytes(tcpClientSocket);
//
//        logger.debug("Decrypting response...");
//        String decodedMessage = EncryptionAES.DecryptString(testReceivedBytes, aesKey);
//
//        logger.debug("Checking if test was successful...");
//        if (!decodedMessage.equals("test from client")) {
//            logger.error("Test failed, string doesn't match, aborting handshake");
//            server.DisconnectPlayer(tcpClientSocket);
//            return null;
//        }

        // success
        logger.debug("RSA handshake was successful with {}", clientIpAddress);

        return aesKey;
    }

    private ConnectedPlayer StartAuthentication(Socket tcpClientSocket, byte[] aesKey) throws SQLException, IOException {
        String clientIpAddress = tcpClientSocket.getInetAddress().toString();
        logger.debug("Started authentication for {}...", clientIpAddress);

        ConnectedPlayer connectedPlayer = new ConnectedPlayer();

        connectedPlayer.tcpClientSocket = tcpClientSocket;
        connectedPlayer.aesKey = aesKey;
        connectedPlayer.ipAddress = tcpClientSocket.getInetAddress();


        // find which player slot is free
        logger.debug("Searching a free slot for {}...", clientIpAddress);
        connectedPlayer.index = -1;
        for (int i = 0; i < server.connectedPlayers.length; i++) {
            if (server.connectedPlayers[i] == null) {
                connectedPlayer.index = i;
                logger.debug("Found free slot at slot {}", connectedPlayer.index);
                break;
            }
        }
        // runs if server is full
        if (connectedPlayer.index == -1) {
            logger.debug("Server is full, rejecting {}", clientIpAddress);
            SendNegativeResponseAndDisconnect(connectedPlayer.tcpClientSocket, 7, connectedPlayer.aesKey);
            return null;
        }

        // Reading LoginData sent by the player
        logger.debug("Reading LoginData sent by {}", clientIpAddress);
        byte[] buffer = new byte[512];
        int bytesRead = tcpClientSocket.getInputStream().read(buffer);

        // read and process the LoginData sent by the player
        logger.debug("Processing received LoginData from {}...", clientIpAddress);
        List<Packet> packets = PacketProcessor.ProcessReceivedBytes(buffer, bytesRead, connectedPlayer);

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
                SendNegativeResponseAndDisconnect(tcpClientSocket, 5, connectedPlayer.aesKey);
                return null;
            }
            // Checks if the chosen name is already registered
            logger.debug("Checking if chosen name {} is already registered...", loginData.un);
            try (ResultSet resultSet = server.database.SearchForPlayerInDatabase(loginData.un)) {
                if (resultSet != null) {
                    logger.debug("Player name {} already exists in the database, registration failed", loginData.un);
                    SendNegativeResponseAndDisconnect(tcpClientSocket, 6, connectedPlayer.aesKey);
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
                SendNegativeResponseAndDisconnect(tcpClientSocket, 4, connectedPlayer.aesKey);
                return null;
            }
        }

        // searches for the player in the database
        logger.debug("Searching for player {} in the database...", loginData.un);
        try (ResultSet resultSet = server.database.SearchForPlayerInDatabase(loginData.un)) {
            // if player was not found in database
            if (resultSet == null) {
                logger.debug("Player {} not found in database, login failed.", loginData.un);
                SendNegativeResponseAndDisconnect(tcpClientSocket, 3, connectedPlayer.aesKey);
                return null;
            }

            // reads password from database then compares
            logger.debug("Comparing entered password for player {}...", loginData.un);
            String storedHashedPassword = resultSet.getString("Password");
            if (!loginData.pw.equals(storedHashedPassword)) {
                logger.debug("Player {} has entered wrong password, login failed", loginData.un);
                SendNegativeResponseAndDisconnect(tcpClientSocket, 2, connectedPlayer.aesKey);
                return null;
            }

            // login was a success
            logger.debug("Successful login for player {}", loginData.un);
            connectedPlayer.databaseID = resultSet.getInt("id");
            connectedPlayer.playerName = resultSet.getString("PlayerName");
        }

        // creating InitialData object
        logger.debug("Creating InitialData object to be sent to {}", loginData.un);
        InitialData initialData = new InitialData();
        initialData.loginResultValue = 1;
        initialData.index = connectedPlayer.index;
        initialData.maxPlayers = server.maxPlayers;
        initialData.tickRate = server.tickRate;
        initialData.udpPort = server.udpPort;

        // reply back to the player about the authentication success
        logger.debug("Sending positive reply about authentication back to {}...", connectedPlayer.playerName);
        try {
            byte[] bytesToSend = PacketProcessor.MakePacketForSending(1, initialData, connectedPlayer.aesKey);
            server.SendTcp(bytesToSend, tcpClientSocket);
        } catch (Exception e) {
            logger.error(e.toString());
            server.DisconnectPlayer(tcpClientSocket);
            return null;
        }

        // sets last login ip address
        server.database.UpdateLastLoginIpAddress(connectedPlayer);

        // adds the new player to the list of connected players
        logger.debug("Adding player {} to the list of connected players...", connectedPlayer.playerName);
        server.connectedPlayers[connectedPlayer.index] = connectedPlayer;

        // returns success
        return connectedPlayer;
    }

    private void SendNegativeResponseAndDisconnect(Socket tcpClientSocket, int resultValue, byte[] aesKey) {
        try {
            logger.debug("Sending negative initial data with value {} to the failed player...", resultValue);

            InitialData initialData = new InitialData();
            initialData.loginResultValue = resultValue;

            byte[] bytesToSend = PacketProcessor.MakePacketForSending(1, initialData, aesKey);
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
