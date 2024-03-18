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
    public PlayerPosition position;

    public ConnectedPlayer() {
        databaseID = -1;
        udpPingAnswered = true;
        timeUntilTimeout = 10;
        status = 1;
        udpPort = 0;
    }


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

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public boolean isUdpPingAnswered() {
        return udpPingAnswered;
    }

    public void setUdpPingAnswered(boolean udpPingAnswered) {
        this.udpPingAnswered = udpPingAnswered;
    }

    public byte getTimeUntilTimeout() {
        return timeUntilTimeout;
    }

    public void setTimeUntilTimeout(byte timeUntilTimeout) {
        this.timeUntilTimeout = timeUntilTimeout;
    }

    public int getLatency() {
        return latency;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }

    public PlayerPosition getPosition() {
        return position;
    }

    public void setPosition(PlayerPosition position) {
        this.position = position;
    }

    public void setPingRequestTime(Instant pingRequestTime) {
        this.pingRequestTime = pingRequestTime;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Instant getPingRequestTime() {
        return pingRequestTime;
    }
}
