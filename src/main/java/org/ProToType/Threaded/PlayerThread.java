package org.ProToType.Threaded;

import org.ProToType.Classes.Player;
import org.ProToType.Classes.Packet;
import org.ProToType.Instanceables.Authentication;
import org.ProToType.Main;
import org.ProToType.Static.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.util.List;

public class PlayerThread implements Runnable {
    private static final Logger logger = LogManager.getLogger(PlayerThread.class);

    private final Socket tcpClientSocket;

    public PlayerThread(Socket tcpClientSocket) {
        this.tcpClientSocket = tcpClientSocket;
    }

    @Override
    public void run() {
        logger.debug("Starting authentication of {}", tcpClientSocket.getInetAddress());
        Player player = null;
        try {
            long startTime = System.currentTimeMillis();

            // starts the authentication
            Authentication authentication = new Authentication();
            player = authentication.StartAuthentication(tcpClientSocket);

            // checks if failed
            if (player == null) {
                logger.warn("Authentication of {} failed", tcpClientSocket.getInetAddress());
                return;
            }

            // adds the player to the concurrentqueue for the main loop
            Main.playersToAdd.add(player);

            // ends authentication
            long endTime = System.currentTimeMillis();
            long elapsedTimeInMillis = endTime - startTime;
            logger.trace("Finished authentication in: {} ms", elapsedTimeInMillis);

            // starts listening to packets
            while (true) {
//            while ((bytesRead = connectedPlayer.tcpClientSocket.getInputStream().read(buffer)) != -1) {
                byte[] receivedBytes = PacketProcessor.ReceiveBytes(player.tcpClientSocket);
                logger.trace("Received message from {}", player.playerName);
                List<Packet> packets = PacketProcessor.ProcessReceivedBytes(receivedBytes, player);
                Main.packetsToProcess.addAll(packets);
            }
        } catch (Exception e) {
            logger.debug("Error receiving Tcp packet: {}", e.toString());
        } finally {
            Main.DisconnectPlayer(player.tcpClientSocket);
        }
    }
}

