package org.ProToType.Threaded;

import org.ProToType.ClassesShared.Packet;
import org.ProToType.Instanceables.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerPositionWithID;
import org.ProToType.Main;
import org.ProToType.Server;

import java.util.ArrayList;
import java.util.List;

public class RunsEveryTick implements Runnable {
    private static final Logger logger = LogManager.getLogger(RunsEveryTick.class);

    private Server server;

    public RunsEveryTick(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10);

                // puts all the connected players' position in a list
                List<PlayerPositionWithID> playerPositionsWithID = new ArrayList<>();
                for (int i = 0; i < server.maxPlayers; i++) {
                    if (server.connectedPlayers[i] != null) {
                        PlayerPositionWithID playerPositionWithID = new PlayerPositionWithID();
                        playerPositionWithID.i = i;
                        playerPositionWithID.pos = server.connectedPlayers[i].position;

                        playerPositionsWithID.add(playerPositionWithID);
                    }
                }

                // sends it to each connected players
                for (ConnectedPlayer connectedPlayer : server.connectedPlayers) {
                    if (connectedPlayer == null) continue;

                    Packet packet = new Packet();
                    packet.type = 3;
                    packet.json = Main.jackson.writeValueAsString(playerPositionsWithID);

                    String message = Main.jackson.writeValueAsString(packet);

                    server.SendUdp(message, connectedPlayer);
                }
            } catch (InterruptedException | JsonProcessingException e) {
                logger.error(e.toString());
            }
        }
    }
}