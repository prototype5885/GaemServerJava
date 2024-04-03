package org.ProToType.Threaded;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Main;
import org.ProToType.Static.Database;
import org.ProToType.Static.PlayersManager;
import org.ProToType.Static.Shortcuts;

import java.sql.SQLException;

public class RunsEverySecond implements Runnable {
    @Override
    public void run() {
        while (true) {
            UpdatePositionInDatabase();
        }
    }

    private void UpdatePositionInDatabase() {
        try {
            for (ConnectedPlayer connectedPlayer : PlayersManager.connectedPlayers) {
                if (connectedPlayer == null) continue;
                String jsonPlayerPosition = Main.jackson.writeValueAsString(connectedPlayer.position);
                Database.UpdatePlayerPosition(connectedPlayer.playerName, jsonPlayerPosition);
            }
//            PrintWithTime.print("Position to database");
            Thread.sleep(1000);
        } catch (JsonProcessingException | SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
