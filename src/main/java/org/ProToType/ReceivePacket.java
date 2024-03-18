package org.ProToType;


import org.ProToType.Classes.ConnectedPlayer;

import java.net.DatagramPacket;

public class ReceivePacket {
    public static void ReceiveTcpPacket(ConnectedPlayer connectedPlayer) {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (true) {
                while ((bytesRead = connectedPlayer.inputStream.read(buffer)) != -1) {
                    ProcessPacket.ProcessReceivedBytes(connectedPlayer, buffer, bytesRead);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void ReceiveUdpPacket() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                Main.udpServerSocket.receive(udpPacket);

                ConnectedPlayer connectedPlayer = null;
                for (ConnectedPlayer player : Main.connectedPlayers) { // check authentication of sender, rejects packet if not actual connected player
                    if (player == null) continue;
                    if (player.ipAddress.equals(udpPacket.getAddress())) {
                        if (player.udpPort == 0) // this runs if sender is an actual player, but havent sent udp packet before
                            player.udpPort = udpPacket.getPort();
                        connectedPlayer = player;
                    }
                }
                if (connectedPlayer != null) { // runs if sender is a connected players
                    ProcessPacket.ProcessReceivedBytes(connectedPlayer, udpPacket.getData(), udpPacket.getLength());
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
