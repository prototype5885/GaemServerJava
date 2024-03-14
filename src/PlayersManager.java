

import org.GaemServer.Classes.ConnectedPlayer;
import org.GaemServer.ClassesShared.ChatMessage;
import org.GaemServer.ClassesShared.PlayerData;
import org.GaemServer.ClassesShared.PlayerPosition;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class PlayersManager {
    public static void ReplicatePlayerPositions(int maxPlayers) throws InterruptedException, IOException {
        PlayerPosition[] everyPlayersPosition = new PlayerPosition[maxPlayers];

        while (true) {
            Thread.sleep(10);
            for (byte i = 0; i < maxPlayers; i++) {
                if (Server.connectedPlayers[i] == null) {
                    everyPlayersPosition[i] = null;
                    continue;
                }
                everyPlayersPosition[i] = Server.connectedPlayers[i].position;
            }
            for (ConnectedPlayer connectedPlayer : Server.connectedPlayers) {
                if (connectedPlayer == null) continue;
                String jsonData = Server.gson.toJson(everyPlayersPosition);
                PacketProcessor.SendUdp(3, jsonData, connectedPlayer);
            }

        }
    }

    public static void CalculatePlayerLatency(ConnectedPlayer connectedPlayer) {

        Duration duration = Duration.between(connectedPlayer.pingRequestTime, Instant.now());
        connectedPlayer.latency = duration.getNano();
    }

    public static void SendChatMessageToEveryone(ConnectedPlayer messageSenderPlayer, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.i = messageSenderPlayer.index;
        chatMessage.m = message;

        String jsonData = Server.gson.toJson(chatMessage);
        for (ConnectedPlayer player : Server.connectedPlayers) {
            if (player == null) continue;
            PacketProcessor.SendTcp(2, jsonData, player);
        }
    }

    public static void SendPlayerDataToEveryone(int maxPlayers) {
        String jsonData = Server.gson.toJson(GetDataOfEveryConnectedPlayer(maxPlayers));
        for (ConnectedPlayer player : Server.connectedPlayers) {
            if (player == null) continue;
        }
    }

    public static PlayerData[] GetDataOfEveryConnectedPlayer(int maxPlayers) {
        PlayerData[] playerDataArray = new PlayerData[maxPlayers];
        for (byte i = 0; i < maxPlayers; i++) {
            if (Server.connectedPlayers[i] != null) {
                playerDataArray[i] = GetDataOfConnectedPlayer(i);
            }
        }
        return playerDataArray;
    }

    public static PlayerData GetDataOfConnectedPlayer(byte index) {
        PlayerData playerData = new PlayerData();
        playerData.i = index;
        playerData.un = Server.connectedPlayers[index].playerName;

        return playerData;
    }
}
