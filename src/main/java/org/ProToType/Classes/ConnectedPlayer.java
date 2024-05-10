package org.ProToType.Classes;

import org.ProToType.ClassesShared.PlayerPosition;

import java.net.InetAddress;
import java.net.Socket;

public class ConnectedPlayer {
    public int index;
    public int databaseID = -1;
    public String playerName;
    public Socket tcpClientSocket;
    public InetAddress ipAddress;
    public byte[] aesKey;
    public PlayerPosition position = new PlayerPosition();
}
