import Classes.ConnectedPlayer;
import ClassesShared.ChatMessage;
import ClassesShared.PlayerData;
import ClassesShared.PlayerPosition;
import com.google.gson.Gson;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class PlayersManager {

    private ConnectedPlayer[] connectedPlayers;
    private Gson gson;
    private PacketProcessor packetProcessor;

    public PlayersManager(ConnectedPlayer[] connectedPlayers, Gson gson, PacketProcessor packetProcessor) {
        this.connectedPlayers = connectedPlayers;
        this.gson = gson;
        this.packetProcessor = packetProcessor;
    }

    public void ReplicatePlayerPositions(int maxPlayers) throws InterruptedException, IOException {
        PlayerPosition[] everyPlayersPosition = new PlayerPosition[maxPlayers];

        while (true) {
            Thread.sleep(10);
            for (byte i = 0; i < maxPlayers; i++) {
                if (connectedPlayers[i] == null) {
                    everyPlayersPosition[i] = null;
                    continue;
                }
                everyPlayersPosition[i] = connectedPlayers[i].position;
            }
            for (ConnectedPlayer connectedPlayer : connectedPlayers) {
                if (connectedPlayer == null) continue;
                String jsonData = gson.toJson(everyPlayersPosition);
                packetProcessor.SendUdp(3, jsonData, connectedPlayer);
            }

        }
    }

    public void CalculatePlayerLatency(ConnectedPlayer connectedPlayer) {

        Duration duration = Duration.between(connectedPlayer.pingRequestTime, Instant.now());
        connectedPlayer.latency = duration.getNano();
    }

    public void SendChatMessageToEveryone(ConnectedPlayer messageSenderPlayer, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.i = messageSenderPlayer.index;
        chatMessage.m = message;

        String jsonData = gson.toJson(chatMessage);
        for (ConnectedPlayer player : connectedPlayers) {
            if (player == null) continue;
            packetProcessor.SendTcp(2, jsonData, player);
        }
    }

    public void SendPlayerDataToEveryone(int maxPlayers) {
        String jsonData = gson.toJson(GetDataOfEveryConnectedPlayer(maxPlayers));
        for (ConnectedPlayer player : connectedPlayers) {
            if (player == null) continue;
        }
    }

    public PlayerData[] GetDataOfEveryConnectedPlayer(int maxPlayers) {
        PlayerData[] playerDataArray = new PlayerData[maxPlayers];
        for (byte i = 0; i < maxPlayers; i++) {
            if (connectedPlayers[i] != null) {
                playerDataArray[i] = GetDataOfConnectedPlayer(i);
            }
        }
        return playerDataArray;
    }

    public PlayerData GetDataOfConnectedPlayer(byte index) {
        PlayerData playerData = new PlayerData();
        playerData.i = index;
        playerData.un = connectedPlayers[index].playerName;

        return playerData;
    }
}
