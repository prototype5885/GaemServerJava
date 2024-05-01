package org.ProToType.Threaded;

import org.ProToType.Classes.Packet;
import org.ProToType.Static.PacketProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.ProToType.Classes.ConnectedPlayer;

import org.ProToType.Server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;


public class ReceiveTcpPacket implements Runnable {
    private static final Logger logger = LogManager.getLogger(ReceiveTcpPacket.class);

    Server server;
    ConnectedPlayer connectedPlayer;

    public ReceiveTcpPacket(Server server, ConnectedPlayer connectedPlayer) {
        this.server = server;
        this.connectedPlayer = connectedPlayer;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytesRead;
        try {
            while ((bytesRead = connectedPlayer.tcpClientSocket.getInputStream().read(buffer)) != -1) {
                logger.trace("Received message from {}", connectedPlayer.playerName);
                List<Packet> packets = PacketProcessor.ProcessReceivedBytes(buffer, bytesRead, connectedPlayer);
                server.packetsToProcess.addAll(packets);
            }
        } catch (Exception e) {
            logger.debug("Error receiving Tcp packet: {}", e.toString());
        } finally {
            server.DisconnectPlayer(connectedPlayer.tcpClientSocket);
        }
    }

    public static byte[] ReceiveBytes(Socket tcpClientSocket) throws IOException {
        byte[] buffer = new byte[2048];
        int bytesRead = tcpClientSocket.getInputStream().read(buffer);
        byte[] receivedBytes = new byte[bytesRead];
        System.arraycopy(buffer, 0, receivedBytes, 0, bytesRead);

        return receivedBytes;
    }
}
