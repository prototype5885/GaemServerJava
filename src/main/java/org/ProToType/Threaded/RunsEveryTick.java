package org.ProToType.Threaded;

import org.ProToType.Static.PacketProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ProToType.Classes.ConnectedPlayer;
import org.ProToType.ClassesShared.PlayerPositionWithID;
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
            } catch (InterruptedException e) {
                logger.error(e.toString());
            }

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

            byte[] bytesToSend = null;
            try {
                bytesToSend = PacketProcessor.MakePacketForSending(4, playerPositionsWithID);
            } catch (JsonProcessingException e) {
                logger.error(e.toString());
                continue;
            }

            // sends it to each connected players
            for (ConnectedPlayer connectedPlayer : server.connectedPlayers) {
                if (connectedPlayer != null) {
                    server.SendTcp(bytesToSend, connectedPlayer.tcpClientSocket);
                }
            }
        }
    }
}