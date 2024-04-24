package org.ProToType.Classes;

import org.ProToType.ClassesShared.PlayerPosition;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Instant;

public class ConnectedPlayer {
    public int index;
    public int databaseID;
    public String playerName;
    public Socket tcpClientSocket;
    public InputStream inputStream;
    public OutputStream outputStream;
    public InetAddress ipAddress;
    public int tcpPort;
    public int udpPort;
    public byte status;
    public boolean udpPingAnswered;
    public byte timeUntilTimeout;
    public Instant pingRequestTime;
    public int latency;
    public PlayerPosition position = new PlayerPosition();

    public ConnectedPlayer() {
        databaseID = -1;
        udpPingAnswered = true;
        timeUntilTimeout = 10;
        status = 1;
        udpPort = 0;
    }
}
