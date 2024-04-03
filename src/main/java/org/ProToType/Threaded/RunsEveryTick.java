package org.ProToType.Threaded;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.ClassesShared.PlayerPositionWithID;
import org.ProToType.Main;
import org.ProToType.Static.PlayersManager;
import org.ProToType.Static.SendPacket;
import org.ProToType.Static.Shortcuts;

import java.util.ArrayList;
import java.util.List;

public class RunsEveryTick implements Runnable {
    @Override
    public void run() {
//        PlayerPosition[] everyPlayersPosition = new PlayerPosition[Main.maxPlayers];

        while (true) {
            try {
                Thread.sleep(10);

                List<PlayerPositionWithID> playerPositionsWithID = new ArrayList<>();
                for (ConnectedPlayer connectedPlayer : PlayersManager.connectedPlayers) {
                    if (connectedPlayer != null) {
                        PlayerPositionWithID playerPositionWithID = new PlayerPositionWithID();
                        playerPositionWithID.i = connectedPlayer.index;
                        playerPositionWithID.pos = connectedPlayer.position;

                        playerPositionsWithID.add(playerPositionWithID);
                    }
                }
                for (ConnectedPlayer connectedPlayer : PlayersManager.connectedPlayers) {
                    if (connectedPlayer == null) continue;
                    String jsonData = Main.jackson.writeValueAsString(playerPositionsWithID);
                    SendPacket.SendUdp(3, jsonData, connectedPlayer);
                }
            } catch (InterruptedException | JsonProcessingException e) {
                Shortcuts.PrintWithTime(e.toString());
            }
        }
    }
}