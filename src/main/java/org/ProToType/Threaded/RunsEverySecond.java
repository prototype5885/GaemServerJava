package org.ProToType.Threaded;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Main;
import org.ProToType.Static.Database;
import org.ProToType.Static.PlayersManager;
import org.ProToType.Static.PrintWithTime;

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
                Database.UpdatePlayerPosition(connectedPlayer.playerName, connectedPlayer.position);
            }
//            PrintWithTime.print("Position to database");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            PrintWithTime.print(e.toString());
        }
    }
}
