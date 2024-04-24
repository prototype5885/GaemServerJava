package org.ProToType.ClassesShared;

public class InitialData {
    private int loginResult; // login result, response to the client about how the login went
    private int index; // client index so player knows what slot he/she/it is in
    private int maxPlayers; // max player amount so client will also know it
    private int tickRate; // tick rate
    private int udpPort; // udp port

    public int getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(int loginResult) {
        this.loginResult = loginResult;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getTickRate() {
        return tickRate;
    }

    public void setTickRate(int tickRate) {
        this.tickRate = tickRate;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }
}
