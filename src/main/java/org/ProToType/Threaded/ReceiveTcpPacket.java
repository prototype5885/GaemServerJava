package org.ProToType.Threaded;

import org.ProToType.Classes.ConnectedPlayer;

import org.ProToType.Static.*;


public class ReceiveTcpPacket implements Runnable {
    ConnectedPlayer connectedPlayer = new ConnectedPlayer();

    public ReceiveTcpPacket(ConnectedPlayer connectedPlayer) {
        this.connectedPlayer = connectedPlayer;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (true) {
                while ((bytesRead = connectedPlayer.inputStream.read(buffer)) != -1) {
                    ProcessPacket.ProcessReceivedBytes(connectedPlayer, buffer, bytesRead);
                }
            }
        } catch (Exception e) {
            PrintWithTime.print("Error receiving Tcp packet, " + e.getMessage());
            PlayersManager.DisconnectPlayer(connectedPlayer);
        }
    }
}
