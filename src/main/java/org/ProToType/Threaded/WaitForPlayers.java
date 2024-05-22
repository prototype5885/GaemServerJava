package org.ProToType.Threaded;

import org.ProToType.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public class WaitForPlayers implements Runnable {
    private static final Logger logger = LogManager.getLogger(WaitForPlayers.class);

    @Override
    public void run() {
        while (true) {
            try {
                // waits for a player to connect
                logger.debug("Waiting for a player to connect...");
                Socket tcpClientSocket = Main.tcpServerSocket.accept();
                logger.info("A player from ip {} connected...", tcpClientSocket.getInetAddress());

                // starts the authentication on separate thread
                Thread.ofVirtual().start(new PlayerThread(tcpClientSocket));
            } catch (IOException e) {
                logger.warn(e);
            }
        }
    }
}
