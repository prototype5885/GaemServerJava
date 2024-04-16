package org.ProToType.Threaded;

import org.ProToType.ClassesShared.Packet;
import org.ProToType.Static.PacketProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.ProToType.Classes.ConnectedPlayer;

import org.ProToType.Server;

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
            while ((bytesRead = connectedPlayer.inputStream.read(buffer)) != -1) {
                String decodedMessage = PacketProcessor.Decode(buffer, bytesRead);
                List<Packet> packets = PacketProcessor.SeparatePackets(decodedMessage);
            }
        } catch (Exception e) {
            logger.debug("Error receiving Tcp packet, " + e.getMessage());
        } finally {
            server.RemovePlayerFromList(connectedPlayer);
            server.DisconnectPlayer(connectedPlayer.tcpClientSocket);
        }
    }
}
