package org.ProToType.Threaded;

import org.ProToType.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public class SendTcp implements Runnable {
    private static final Logger logger = LogManager.getLogger(SendTcp.class);

    private Socket tcpClientSocket;
    private byte[] bytesToSend;

    public SendTcp(byte[] bytesToSend, Socket tcpClientSocket) {
        this.bytesToSend = bytesToSend;
        this.tcpClientSocket = tcpClientSocket;
    }

    @Override
    public void run() {
        try {
//            logger.trace("Sending TCP message of length {} to {}", bytesToSend.length, tcpClientSocket.getInetAddress());
            tcpClientSocket.getOutputStream().write(bytesToSend);
        } catch (IOException e) {
            logger.error(e.toString());
            Main.DisconnectPlayer(tcpClientSocket);
        }
    }
}