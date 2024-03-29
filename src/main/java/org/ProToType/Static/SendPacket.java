package org.ProToType.Static;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.ChatMessage;
import org.ProToType.Main;

import java.io.IOException;
import java.net.DatagramPacket;

public class SendPacket {
    public static void SendTcp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            connectedPlayer.outputStream.write(messageBytes);
        } catch (Exception e) {
            Shortcuts.PrintWithTime("Error sending Tcp packet, " + e.getMessage());
            PlayersManager.DisconnectPlayer(connectedPlayer);
        }
    }

    public static void SendUdp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            if (connectedPlayer.udpPort == 0) return;
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            DatagramPacket udpPacket = new DatagramPacket(messageBytes, messageBytes.length, connectedPlayer.ipAddress, connectedPlayer.udpPort);
            Main.udpServerSocket.send(udpPacket);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static byte[] EncodeMessage(int commandType, String message) {
        if (Encryption.encryptionEnabled) {
            String messageString = "#" + commandType + "#" + "$" + message + "$";
            return Encryption.Encrypt(messageString);
        } else {
            return ("#" + commandType + "#" + "$" + message + "$").getBytes();
        }
    }

    public static void SendChatMessageToEveryone(ConnectedPlayer messageSenderPlayer, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.i = messageSenderPlayer.index;
        chatMessage.m = message;

        String jsonData = Main.gson.toJson(chatMessage);
        for (ConnectedPlayer player : PlayersManager.connectedPlayers) {
            if (player == null) continue;
            SendTcp(2, jsonData, player);
        }
    }


//    public void SendPlayerDataToEveryone(int maxPlayers) {
//        String jsonData = gson.toJson(playersManager.GetDataOfEveryConnectedPlayer(maxPlayers));
//        for (ConnectedPlayer player : connectedPlayers) {
//            if (player == null) continue;
//        }
//    }
}
