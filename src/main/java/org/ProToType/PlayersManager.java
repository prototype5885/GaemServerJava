
package org.ProToType;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerData;

import java.time.Duration;
import java.time.Instant;

public class PlayersManager {
    public static void CalculatePlayerLatency(ConnectedPlayer connectedPlayer) {

        Duration duration = Duration.between(connectedPlayer.pingRequestTime, Instant.now());
        connectedPlayer.latency = duration.getNano();
    }


    public static PlayerData[] GetDataOfEveryConnectedPlayer(int maxPlayers) {
        PlayerData[] playerDataArray = new PlayerData[maxPlayers];
        for (byte i = 0; i < maxPlayers; i++) {
            if (Main.connectedPlayers[i] != null) {
                playerDataArray[i] = GetDataOfConnectedPlayer(i);
            }
        }
        return playerDataArray;
    }

    public static PlayerData GetDataOfConnectedPlayer(byte index) {
        PlayerData playerData = new PlayerData();
        playerData.i = index;
        playerData.un = Main.connectedPlayers[index].playerName;

        return playerData;
    }
}
