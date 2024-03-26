package org.ProToType.Threaded;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.Main;
import org.ProToType.Static.PlayersManager;
import org.ProToType.Static.SendPacket;

public class RunsEveryTick implements Runnable {
    @Override
    public void run() {
        PlayerPosition[] everyPlayersPosition = new PlayerPosition[Main.maxPlayers];

        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < Main.maxPlayers; i++) {
                if (PlayersManager.connectedPlayers[i] == null) {
                    everyPlayersPosition[i] = null;
                    continue;
                }
                everyPlayersPosition[i] = PlayersManager.connectedPlayers[i].position;
            }
            for (ConnectedPlayer connectedPlayer : PlayersManager.connectedPlayers) {
                if (connectedPlayer == null) continue;
                String jsonData = Main.gson.toJson(everyPlayersPosition);
                SendPacket.SendUdp(3, jsonData, connectedPlayer);
            }

        }
    }
}
