package org.ProToType.Threaded;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ProcessPacket;

public class ReceiveTcpPacket implements Runnable {
    ConnectedPlayer connectedPlayer;

    public ReceiveTcpPacket(ConnectedPlayer connectedPlayer) {
        this.connectedPlayer = connectedPlayer;
    }

    @Override
    public void run() {
        try {
            System.out.println("started thread");
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
}
