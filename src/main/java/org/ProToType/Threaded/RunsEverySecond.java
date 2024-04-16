package org.ProToType.Threaded;

import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.Main;
import org.ProToType.Server;

public class RunsEverySecond implements Runnable {
    private Server server;

    public RunsEverySecond(Server server) {
        this.server = server;
    }


    @Override
    public void run() {
        long startTime;
        long elapsedTime;
        long sleepTime;
        while (true) {
            startTime = System.currentTimeMillis();

            UpdatePositionInDatabase();

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
            for (ConnectedPlayer connectedPlayer : server.connectedPlayers) {
                if (connectedPlayer == null)
                    continue;
                String jsonPlayerPosition = Main.jackson.writeValueAsString(connectedPlayer.position);
                server.database.UpdatePlayerPosition(connectedPlayer.playerName, jsonPlayerPosition);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
