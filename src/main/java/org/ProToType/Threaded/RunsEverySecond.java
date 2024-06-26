package org.ProToType.Threaded;

import org.ProToType.Classes.Player;
import org.ProToType.Main;
import org.ProToType.Static.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunsEverySecond implements Runnable {
    private static final Logger logger = LogManager.getLogger(RunsEverySecond.class);

    @Override
    public void run() {
        long startTime;
        long elapsedTime;
        long sleepTime;
        while (true) {
            startTime = System.currentTimeMillis();

//            UpdatePositionInDatabase();
//            server.SendDataOfConnectedPlayers();

            elapsedTime = System.currentTimeMillis() - startTime;
            sleepTime = 1000 - elapsedTime;
            if (sleepTime < 0) sleepTime = 0;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void UpdatePositionInDatabase() {
        try {
            for (Player player : Main.players) {
                if (player == null)
                    continue;
                String jsonPlayerPosition = Main.jackson.writeValueAsString(player.position);
                Database.UpdatePlayerPosition(player.playerName, jsonPlayerPosition);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
