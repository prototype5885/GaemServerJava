package org.ProToType.Threaded;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.ProToType.Server;

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

//            // puts all the connected players' position in a list
//            List<PlayerPositionWithID> playerPositionsWithID = new ArrayList<>();
//            for (ConnectedPlayer connectedPlayer : server.connectedPlayers) {
//                if (connectedPlayer != null) {
//                    PlayerPositionWithID playerPositionWithID = new PlayerPositionWithID();
//                    playerPositionWithID.i = connectedPlayer.index;
//                    playerPositionWithID.pos = connectedPlayer.position;
//
//                    playerPositionsWithID.add(playerPositionWithID);
//                }
//            }
//
//            // sends it to each connected players
//            for (ConnectedPlayer connectedPlayer : server.connectedPlayers) {
//                if (connectedPlayer != null) {
//                    try {
//                        byte[] bytesToSend = PacketProcessor.MakePacketForSending(4, playerPositionsWithID, connectedPlayer.aesKey);
//                        server.SendTcp(bytesToSend, connectedPlayer.tcpClientSocket);
////                        Thread.ofVirtual().start(new SendTcp(server, connectedPlayer.tcpClientSocket, bytesToSend));
//                    } catch (JsonProcessingException e) {
//                        logger.error("Error sending player position to player: {}", connectedPlayer.playerName);
//                    }
//                }
//            }
        }
    }
}