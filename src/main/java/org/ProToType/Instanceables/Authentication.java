package org.ProToType.Instanceables;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.Packet;
import org.ProToType.Classes.Player;
import org.ProToType.ClassesShared.InitialData;
import org.ProToType.ClassesShared.LoginData;
import org.ProToType.Main;
import org.ProToType.Static.*;
import org.ProToType.Threaded.SendTcp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

public class Authentication {
    private static final Logger logger = LogManager.getLogger(Authentication.class);


    public Player StartAuthentication(Socket tcpClientSocket) throws Exception {
        // starts RSA handshake that will return the AES and RSA encryption keys
        byte[] aesKey = new byte[]{};
        if (EncryptionAES.encryptionEnabled) {
            aesKey = ExchangeSymmetricKey(tcpClientSocket);
            ByteProcessor.PrintByteArrayAsHex(aesKey);
        }

        String clientIpAddress = tcpClientSocket.getInetAddress().toString();
        logger.debug("Started authentication for {}...", clientIpAddress);

        Player newPlayer = new Player();

        newPlayer.tcpClientSocket = tcpClientSocket;
        newPlayer.aesKey = aesKey;
        newPlayer.ipAddress = tcpClientSocket.getInetAddress();

        // runs if server is full
        logger.debug("Checking if server is full first time...");
        if (Main.playerCount == Main.maxPlayers) {
            logger.debug("Server is full, rejecting {}", clientIpAddress);
            Main.SendNegativeResponseAndDisconnect(newPlayer.tcpClientSocket, 7, newPlayer.aesKey);
            return null;
        }

        // Reading LoginData sent by the player
        logger.debug("Reading LoginData sent by {}", clientIpAddress);
        byte[] receivedBytes = PacketProcessor.ReceiveBytes(tcpClientSocket);

        // read and process the LoginData sent by the player
        logger.debug("Processing received LoginData from {}...", clientIpAddress);
        List<Packet> packets = PacketProcessor.ProcessReceivedBytes(receivedBytes, newPlayer);

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
                    Main.DisconnectPlayer(tcpClientSocket);
                    return null;
                }
                break;
            } else {
                logger.error("No LoginData type packet was received from {}, aborting authentication", clientIpAddress);
                Main.DisconnectPlayer(tcpClientSocket);
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
                Main.SendNegativeResponseAndDisconnect(tcpClientSocket, 5, newPlayer.aesKey);
                return null;
            }
            // Checks if the chosen name is already registered
            logger.debug("Checking if chosen name {} is already registered...", loginData.un);
            try (ResultSet resultSet = Database.SearchForPlayerInDatabase(loginData.un)) {
                if (resultSet != null) {
                    logger.debug("Player name {} already exists in the database, registration failed", loginData.un);
                    Main.SendNegativeResponseAndDisconnect(tcpClientSocket, 6, newPlayer.aesKey);
                    return null;
                }
            }

            // Adds the new player to the database
            logger.debug("Successful registration, adding new player {} to the database", loginData.un);
            Database.RegisterPlayer(loginData.un, loginData.pw);
        }

        // logs the player in
        logger.debug("Logging in player {}...", loginData.un);

        // searches if player is already connected to the server
        logger.debug("Checking if player {} is connected already...", loginData.un);
        for (Player player : Main.players) {
            if (player == null)
                continue;
            if (loginData.un.equals(player.playerName)) {
                logger.debug("Player {} is already connected, login failed", loginData.un);
                Main.SendNegativeResponseAndDisconnect(tcpClientSocket, 4, newPlayer.aesKey);
                return null;
            }
        }

        // searches for the player in the database
        logger.debug("Searching for player {} in the database...", loginData.un);
        try (ResultSet resultSet = Database.SearchForPlayerInDatabase(loginData.un)) {
            // if player was not found in database
            if (resultSet == null) {
                logger.debug("Player {} not found in database, login failed.", loginData.un);
                Main.SendNegativeResponseAndDisconnect(tcpClientSocket, 3, newPlayer.aesKey);
                return null;
            }

            // reads password from database then compares
            logger.debug("Comparing entered password for player {}...", loginData.un);
            String storedHashedPassword = resultSet.getString("Password");
            if (!loginData.pw.equals(storedHashedPassword)) {
                logger.debug("Player {} has entered wrong password, login failed", loginData.un);
                Main.SendNegativeResponseAndDisconnect(tcpClientSocket, 2, newPlayer.aesKey);
                return null;
            }

            // login was a success
            logger.debug("Successful login for player {}", loginData.un);
            newPlayer.databaseID = resultSet.getInt("id");
            newPlayer.playerName = resultSet.getString("PlayerName");
        }

        // sets last login ip address
        Database.UpdateLastLoginIpAddress(newPlayer);

        // returns success
        logger.info("Authentication of {} ({}) was success", tcpClientSocket.getInetAddress(), newPlayer.playerName);
        newPlayer.status = 1;


//        Thread.ofVirtual().start(new ReceiveTcpPacket(newPlayer));


        // returns the player object
        return newPlayer;
    }

    private byte[] ExchangeSymmetricKey(Socket tcpClientSocket) throws Exception {
        String clientIpAddress = tcpClientSocket.getInetAddress().toString();
        // Processing handshake request sent by the connecting player
        logger.debug("Waiting for {} to send a handshake request...", clientIpAddress);

        byte[] aloReceivedBytes = PacketProcessor.ReceiveBytes(tcpClientSocket);
        aloReceivedBytes = EncryptionAES.Decrypt(aloReceivedBytes, "zTF7QCAw5amV7OxHQKE82rZKwebXrPkp".getBytes());

        if (!Arrays.equals(aloReceivedBytes, "alo".getBytes())) {
            logger.error("Improper handshake request received from {}, aborting handshake", clientIpAddress);
            Main.DisconnectPlayer(tcpClientSocket);
            return null;
        }

        logger.debug("Handshake request received from {}, " +
                "sending public key to the player...", clientIpAddress);
//        logger.debug("Printing own key of length {}", EncryptionRSA.keypair.getPublic().getEncoded().length);

        // sending public key to player
//        Main.SendTcp(EncryptionRSA.keypair.getPublic().getEncoded(), tcpClientSocket);
        Thread.ofVirtual().start(new SendTcp(EncryptionRSA.keypair.getPublic().getEncoded(), tcpClientSocket));

        // waiting for player to send its own public key
        logger.debug("Waiting now for {} to send it's own public key...", clientIpAddress);

        byte[] encryptedKeys = PacketProcessor.ReceiveBytes(tcpClientSocket);

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
//        Main.SendTcp(encryptedAesKey, tcpClientSocket);
        Thread.ofVirtual().start(new SendTcp(encryptedAesKey, tcpClientSocket));

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


}
