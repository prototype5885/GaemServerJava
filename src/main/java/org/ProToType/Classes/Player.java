package org.ProToType.Classes;

import org.ProToType.ClassesShared.PlayerPosition;
import org.ProToType.Threaded.PlayerThread;

import java.net.InetAddress;
import java.net.Socket;

public class Player {
    public int databaseID = -1;
    public int status;
    public String playerName;
    public Socket tcpClientSocket;
    public PlayerThread playerThread;
    public InetAddress ipAddress;
    public byte[] aesKey;
    public PlayerPosition position = new PlayerPosition();

    public int getDatabaseID() {
        return databaseID;
    }

    public void setDatabaseID(int databaseID) {
        this.databaseID = databaseID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Socket getTcpClientSocket() {
        return tcpClientSocket;
    }

    public void setTcpClientSocket(Socket tcpClientSocket) {
        this.tcpClientSocket = tcpClientSocket;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public void setAesKey(byte[] aesKey) {
        this.aesKey = aesKey;
    }

    public PlayerPosition getPosition() {
        return position;
    }

    public void setPosition(PlayerPosition position) {
        this.position = position;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public PlayerThread getPlayerThread() {
        return playerThread;
    }

    public void setPlayerThread(PlayerThread playerThread) {
        this.playerThread = playerThread;
    }
}
