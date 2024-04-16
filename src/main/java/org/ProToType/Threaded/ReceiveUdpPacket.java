package org.ProToType.Threaded;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Server;
import org.ProToType.Static.PacketProcessor;


import java.net.DatagramPacket;

public class ReceiveUdpPacket implements Runnable {
    private Server server;

    public ReceiveUdpPacket(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                server.udpServerSocket.receive(udpPacket);

                ConnectedPlayer connectedPlayer = null;
                for (ConnectedPlayer player : server.connectedPlayers) { // check authentication of sender, rejects packet if not actual connected player
                    if (player == null) continue;
                    if (player.ipAddress.equals(udpPacket.getAddress())) {
                        if (player.udpPort == 0) // this runs if sender is an actual player, but havent sent udp packet before
                            player.udpPort = udpPacket.getPort();
                        connectedPlayer = player;
                    }
                }
                if (connectedPlayer != null) { // runs if sender is a connected players
                    String decodedMessage = PacketProcessor.Decode(udpPacket.getData(), udpPacket.getLength());
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
