package org.ProToType.Static;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerData;
import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.Main;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class PlayersManager {

    public static ConnectedPlayer[] connectedPlayers;

    public static void CreateConnectedPlayerArray(int maxPlayers) {
        PlayersManager.connectedPlayers = new ConnectedPlayer[maxPlayers]; // creates an array that holds information of connected players
    }

    public static void CalculatePlayerLatency(ConnectedPlayer connectedPlayer) {

        Duration duration = Duration.between(connectedPlayer.pingRequestTime, Instant.now());
        connectedPlayer.latency = duration.getNano();
    }

    public static PlayerData[] GetDataOfEveryConnectedPlayer(int maxPlayers) {
        PlayerData[] playerDataArray = new PlayerData[maxPlayers];
        for (int i = 0; i < maxPlayers; i++) {
            if (connectedPlayers[i] != null) {
                playerDataArray[i] = GetDataOfConnectedPlayer(i);
            }
        }
        return playerDataArray;
    }

    public static PlayerData GetDataOfConnectedPlayer(int index) {
        PlayerData playerData = new PlayerData();
        playerData.i = index;
        playerData.un = connectedPlayers[index].playerName;

        return playerData;
    }

    public static void UpdatePlayerPosition(ConnectedPlayer connectedPlayer, String playerPosString) {
        try {
            connectedPlayer.position = Main.jackson.readValue(playerPosString, PlayerPosition.class);
        } catch (JsonProcessingException e) {
            Shortcuts.PrintWithTime(e.toString());
        }
    }

    public static void DisconnectPlayer(ConnectedPlayer connectedPlayer) {
        Shortcuts.PrintWithTime(String.format("Removing player {%s} from server...", connectedPlayer.playerName));

        try {
            connectedPlayer.tcpClientSocket.shutdownOutput();
            connectedPlayer.tcpClientSocket.shutdownInput();
            connectedPlayer.tcpClientSocket.close();
        } catch (IOException e) {
            Shortcuts.PrintWithTime(e.toString());
        } finally {
            Shortcuts.PrintWithTime(String.format("Is the socket closed for player {%s}: %s", connectedPlayer.playerName, connectedPlayer.tcpClientSocket.isClosed()));
            for (int i = 0; i < Main.maxPlayers; i++) {
                if (connectedPlayers[i] == null) continue;
                if (connectedPlayer.equals(connectedPlayers[i])) {
                    connectedPlayers[i] = null;
                    Shortcuts.PrintWithTime(String.format("Slot status for player {%s}: %s", connectedPlayer.playerName, connectedPlayers[i]));
                    break;
                }
            }
        }
    }

    public static int GetConnectedPlayersCount() {
        int playerCount = 0;
        for (ConnectedPlayer connectedPlayer : PlayersManager.connectedPlayers) {
            if (connectedPlayer != null) {
                playerCount++;
            }
        }
        return playerCount;
    }
}