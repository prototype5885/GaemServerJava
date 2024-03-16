import Classes.ConnectedPlayer;
import ClassesShared.ChatMessage;
import ClassesShared.PlayerPosition;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SendPacket {
    public static void SendTcp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            connectedPlayer.outputStream.write(messageBytes);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void SendUdp(int commandType, String message, ConnectedPlayer connectedPlayer) {
        try {
            if (connectedPlayer.udpPort == 0) return;
            byte[] messageBytes = EncodeMessage(commandType, message);
            // Monitoring here
            DatagramPacket udpPacket = new DatagramPacket(messageBytes, messageBytes.length, connectedPlayer.ipAddress, connectedPlayer.udpPort);
            Main.udpServerSocket.send(udpPacket);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
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
        for (ConnectedPlayer player : Main.connectedPlayers) {
            if (player == null) continue;
            SendTcp(2, jsonData, player);
        }
    }

    public static void SendPlayerPositions() {
        PlayerPosition[] everyPlayersPosition = new PlayerPosition[Main.maxPlayers];

        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (byte i = 0; i < Main.maxPlayers; i++) {
                if (Main.connectedPlayers[i] == null) {
                    everyPlayersPosition[i] = null;
                    continue;
                }
                everyPlayersPosition[i] = Main.connectedPlayers[i].position;
            }
            for (ConnectedPlayer connectedPlayer : Main.connectedPlayers) {
                if (connectedPlayer == null) continue;
                String jsonData = Main.gson.toJson(everyPlayersPosition);
                SendUdp(3, jsonData, connectedPlayer);
            }

        }
    }

//    public void SendPlayerDataToEveryone(int maxPlayers) {
//        String jsonData = gson.toJson(playersManager.GetDataOfEveryConnectedPlayer(maxPlayers));
//        for (ConnectedPlayer player : connectedPlayers) {
//            if (player == null) continue;
//        }
//    }
}
